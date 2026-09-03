/*
 * Copyright (c) 2026 Voyager1
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.voyager1.core.jpa;

import io.voyager1.util.DateUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.ThreadUtil;
import io.voyager1.util.ReflectUtil;
import io.voyager1.util.JakartaServletUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Const;
import io.voyager1.common.ServerConst;
import io.voyager1.exception.AgentAuthorizeException;
import io.voyager1.exception.AgentException;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.model.BaseNodeModel;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.data.WorkspaceModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.system.WorkspaceService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 节点同步型 JPA 服务基类（清洁室实现，取代 BaseNodeService）。
 * <p>
 * 继承 JpaGlobalOrWorkspaceService（全局 + 工作空间），保留节点同步契约：
 * 子类仅需实现 {@code getItem/getLitDataArray/lonelyDataArray}。
 */
@Slf4j
public abstract class JpaNodeService<T extends BaseNodeModel, E extends WorkspaceEntity>
    extends JpaGlobalOrWorkspaceService<T, E> {

    protected final NodeService nodeService;
    protected final WorkspaceService workspaceService;
    protected final Class<T> tClass;
    private final String dataName;

    protected JpaNodeService(NodeService nodeService,
                             WorkspaceService workspaceService,
                             String dataName) {
        this.nodeService = nodeService;
        this.workspaceService = workspaceService;
        this.dataName = dataName;
        this.tClass = this.modelClass();
    }

    @Override
    public List<T> listByWorkspace(HttpServletRequest request) {
        String workspaceId = this.getCheckUserWorkspace(request);
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        String nodeId = paramMap.get("nodeId");
        io.voyager1.core.db.Entity entity = new io.voyager1.core.db.Entity();
        entity.set("workspaceId", new ArrayList<>(Arrays.asList(workspaceId, ServerConst.WORKSPACE_GLOBAL)));
        if (nodeId != null && !nodeId.isEmpty()) {
            entity.set("nodeId", nodeId);
        }
        return this.listByEntity(entity);
    }

    /**
     * 同步所有节点的项目
     */
    public void syncAllNode() {
        ThreadUtil.execute(() -> {
            List<NodeModel> list = nodeService.list();
            if ((list == null || list.isEmpty())) {
                log.debug("没有任何节点");
                return;
            }
            // 排序 避免项目被个节点绑定
            list.sort((o1, o2) -> {
                if (java.util.Objects.equals(o1.getWorkspaceId(), Const.WORKSPACE_DEFAULT_ID)) {
                    return 1;
                }
                if (java.util.Objects.equals(o2.getWorkspaceId(), Const.WORKSPACE_DEFAULT_ID)) {
                    return 1;
                }
                return 0;
            });
            for (NodeModel nodeModel : list) {
                this.syncNode(nodeModel);
            }
        });
    }

    /**
     * 同步节点的项目
     *
     * @param nodeModel 节点
     */
    public void syncNode(final NodeModel nodeModel) {
        ThreadUtil.execute(() -> this.syncExecuteNode(nodeModel));
    }

    /**
     * 检查孤独数据
     *
     * @param jsonArray 数据
     * @param machineId 机器 ID
     * @return list
     */
    protected List<T> checkLonelyDataArray(JSONArray jsonArray, String machineId) {
        if ((jsonArray == null || jsonArray.isEmpty())) {
            return null;
        }
        // 分组
        Map<String, List<T>> map = jsonArray.stream().map(o -> {
            JSONObject jsonObject = (JSONObject) o;
            return jsonObject.to(tClass);
        }).collect(Collectors.groupingBy(
            t -> (t.getNodeId() == null || t.getNodeId().isEmpty() ? "" : t.getNodeId()) + "," + t.getWorkspaceId(),
            Collectors.mapping(t -> t, Collectors.toList())
        ));
        // 查询不存在的节点
        Map<String, String> nodeIdMap = new HashMap<>();
        return map.entrySet()
            .stream()
            .filter(entry -> {
                String key = entry.getKey();
                if ((key != null && key.startsWith(","))) {
                    // 旧数据没有节点 ID
                    List<String> list = io.voyager1.util.ConvertUtil.splitTrim(key, ",");
                    String workspaceId = (list == null || list.isEmpty() ? null : list.get(list.size() - 1));
                    NodeModel nodeModel = new NodeModel();
                    nodeModel.setMachineId(machineId);
                    nodeModel.setWorkspaceId(workspaceId);
                    // 更新推荐节点ID
                    NodeModel queryByBean = nodeService.queryByBean(nodeModel);
                    if (queryByBean != null) {
                        String beanId = queryByBean.getId();
                        String s = nodeIdMap.put(key, beanId);
                        if ((s != null && !s.isEmpty()) && !java.util.Objects.equals(s, beanId)) {
                            // 对比已经存在的数据
                            log.error("项目数据工作空间ID[{}]查询出节点ID不一致, 旧数据: {}, 新数据: {}", key, s, beanId);
                        }
                    }
                    return true;
                }
                List<String> list = io.voyager1.util.ConvertUtil.splitTrim(key, ",");
                if ((list == null ? 0 : list.size()) != 2) {
                    return true;
                }
                String workspaceId = list.get(1);
                String id = list.get(0);
                if (java.util.Objects.equals(workspaceId, ServerConst.WORKSPACE_GLOBAL)) {
                    // 判断全局工作空间ID ,判断节点不存在
                    return !nodeService.exists(id);
                }
                NodeModel nodeModel = new NodeModel();
                nodeModel.setId(id);
                nodeModel.setWorkspaceId(workspaceId);
                return !nodeService.exists(nodeModel);

            })
            .peek(entry -> {
                String key = entry.getKey();
                String nodeId = nodeIdMap.get(key);
                if (nodeId != null) {
                    // 更新节点ID
                    List<T> value = entry.getValue();
                    for (T t : value) {
                        t.setNodeId(nodeId);
                    }
                }
            })
            .flatMap(entry -> entry.getValue().stream())
            .collect(Collectors.toList());
    }

    /**
     * 同步执行 同步节点信息
     *
     * @param nodeModel 节点信息
     * @return json
     */
    public String syncExecuteNode(NodeModel nodeModel) {
        String nodeModelName = nodeModel.getName();
        if (!nodeModel.isOpenStatus()) {
            log.debug("{} 节点未启用", nodeModelName);
            return "节点未启用";
        }
        try {
            JSONArray jsonArray = this.getLitDataArray(nodeModel);
            if ((jsonArray == null || jsonArray.isEmpty())) {
                io.voyager1.core.db.Entity entity = new io.voyager1.core.db.Entity();
                entity.set("nodeId", nodeModel.getId());
                int del = this.del(entity);
                //
                log.debug("{} 节点没有拉取到任何 {},但是删除了数据：{}", nodeModelName, dataName, del);
                return "节点没有拉取到任何" + dataName;
            }
            // 查询现在存在的项目
            T where = ReflectUtil.newInstance(this.tClass);
            // where.setWorkspaceId(nodeModel.getWorkspaceId());
            where.setNodeId(nodeModel.getId());
            List<T> cacheAll = this.listByBean(where);
            cacheAll = (cacheAll != null ? cacheAll : Collections.emptyList());
            Set<String> needDelete = new HashSet<>();
            Set<String> cacheIds = cacheAll.stream()
                .map(BaseNodeModel::dataId)
                .collect(Collectors.toSet());
            // 转换数据修改时间
            List<T> projectInfoModels = jsonArray.stream()
                .map(o -> {
                    // modifyTime,createTime
                    JSONObject jsonObject = (JSONObject) o;
                    T t = jsonObject.to(tClass);
                    Opt.ofBlankAble(jsonObject.getString("createTime"))
                        .map(s -> {
                            try {
                                return DateUtil.parse(s);
                            } catch (Exception e) {
                                log.warn("数据创建时间格式不正确 {} {}", s, jsonObject);
                                return null;
                            }
                        }).ifPresent(s -> t.setCreateTimeMillis(s.getTime()));
                    //
                    Opt.ofBlankAble(jsonObject.getString("modifyTime"))
                        .map(s -> {
                            try {
                                return DateUtil.parse(s);
                            } catch (Exception e) {
                                log.warn("数据修改时间格式不正确 {} {}", s, jsonObject);
                                return null;
                            }
                        })
                        .ifPresent(s -> t.setModifyTimeMillis(s.getTime()));
                    return t;
                })
                .peek(item -> this.fullData(item, nodeModel))
                // 只保留自己节点的数据
                .filter(t -> java.util.Objects.equals(t.getNodeId(), nodeModel.getId()))
                .filter(item -> {
                    if (java.util.Objects.equals(item.getWorkspaceId(), ServerConst.WORKSPACE_GLOBAL)) {
                        return true;
                    }
                    // 检查对应的工作空间 是否存在
                    return workspaceService.exists(new WorkspaceModel(item.getWorkspaceId()));
                })
                .filter(item -> {
                    if (java.util.Objects.equals(item.getWorkspaceId(), ServerConst.WORKSPACE_GLOBAL)) {
                        return true;
                    }
                    // 避免重复同步
                    return java.util.Objects.equals(nodeModel.getWorkspaceId(), item.getWorkspaceId());
                })
                .peek(item -> {
                    item.setNodeName(nodeModel.getName());
                    WorkspaceModel workspaceModel = workspaceService.getByKey(nodeModel.getWorkspaceId());
                    item.setWorkspaceName(Optional.ofNullable(workspaceModel).map(WorkspaceModel::getName).orElse("数据不存在"));
                    cacheIds.remove(item.dataId());
                    // 需要删除相反的工作空间的数据（避免出现一个脚本同步出2条数据的问题）
                    if (java.util.Objects.equals(item.getWorkspaceId(), ServerConst.WORKSPACE_GLOBAL)) {
                        needDelete.add(BaseNodeModel.fullId(nodeModel.getWorkspaceId(), nodeModel.getId(), item.dataId()));
                    } else {
                        needDelete.add(BaseNodeModel.fullId(ServerConst.WORKSPACE_GLOBAL, nodeModel.getId(), item.dataId()));
                    }
                })
                .collect(Collectors.toList());
            // 设置 临时缓存，便于放行检查
            BaseServerController.resetInfo(UserModel.EMPTY);
            //
            projectInfoModels.forEach(this::upsert);
            // 删除项目
            int delCount = 0;
            Set<String> strings = cacheIds.stream()
                .flatMap((Function<String, Stream<String>>) s -> Stream.of(
                    BaseNodeModel.fullId(nodeModel.getWorkspaceId(), nodeModel.getId(), s),
                    BaseNodeModel.fullId(ServerConst.WORKSPACE_GLOBAL, nodeModel.getId(), s)))
                .collect(Collectors.toSet());
            //
            needDelete.addAll(strings);
            if ((needDelete != null && !needDelete.isEmpty())) {
                delCount = this.delByKey(needDelete, null);
            }
            int size = (projectInfoModels == null ? 0 : projectInfoModels.size());
            String template = "{} 物理节点拉取到 {} 个{},当前工作空间逻辑节点已经缓存 {} 个{},更新 {} 个{},删除 {} 个缓存";
            String format = String.format(template, nodeModelName, (jsonArray == null ? 0 : jsonArray.size()), dataName, (cacheAll == null ? 0 : cacheAll.size()), dataName, size, dataName, delCount);
            this.refreshCacheStat(nodeModel.getId(), size);
            log.debug(format);
            return format;
        } catch (Exception e) {
            return this.checkException(e, nodeModelName);
        } finally {
            BaseServerController.removeEmpty();
        }
    }

    /**
     * 刷新缓存统计
     *
     * @param nodeId    节点id
     * @param dataCount 数据总数
     */
    protected void refreshCacheStat(String nodeId, int dataCount) {

    }

    protected String checkException(Exception e, String nodeModelName) {
        if (e instanceof AgentException) {
            AgentException agentException = (AgentException) e;
            log.error("{} 同步失败 {}", nodeModelName, agentException.getMessage());
            return "同步失败" + agentException.getMessage();
        } else if (e instanceof AgentAuthorizeException) {
            AgentAuthorizeException agentAuthorizeException = (AgentAuthorizeException) e;
            log.error("{} 授权异常 {}", nodeModelName, agentAuthorizeException.getMessage());
            return "授权异常" + agentAuthorizeException.getMessage();
        }
        log.error("同步节点{}失败:{}", dataName, nodeModelName, e);
        return String.format("同步节点 %s 失败 %s", dataName, e.getMessage());
    }

    /**
     * 同步节点的项目
     *
     * @param nodeModel 节点
     * @param id        项目id
     */
    public void syncNode(final NodeModel nodeModel, String id) {
        String nodeModelName = nodeModel.getName();
        if (!nodeModel.isOpenStatus()) {
            log.debug("{} 节点未启用", nodeModelName);
            return;
        }
        ThreadUtil.execute(() -> {
            try {
                JSONObject data = this.getItem(nodeModel, id);
                if (data == null) {
                    // 删除
                    String fullId = BaseNodeModel.fullId(nodeModel.getWorkspaceId(), nodeModel.getId(), id);
                    this.delByKey(fullId);
                    return;
                }
                T projectInfoModel = data.toJavaObject(this.tClass);
                this.fullData(projectInfoModel, nodeModel);
                // 设置 临时缓存，便于放行检查
                BaseServerController.resetInfo(UserModel.EMPTY);
                //
                this.upsert(projectInfoModel);
            } catch (Exception e) {
                this.checkException(e, nodeModelName);
            } finally {
                BaseServerController.removeEmpty();
            }
        });
    }

    /**
     * 填充数据ID
     *
     * @param item      对象
     * @param nodeModel 节点
     */
    private void fullData(T item, NodeModel nodeModel) {
        item.dataId(item.getId());
        if ((item.getNodeId() == null || item.getNodeId().isEmpty())) {
            item.setNodeId(nodeModel.getId());
        }
        if ((item.getWorkspaceId() == null || item.getWorkspaceId().isEmpty())) {
            item.setWorkspaceId(nodeModel.getWorkspaceId());
        }
        item.setId(item.fullId());
    }

    /**
     * 删除节点 工作空间缓存
     *
     * @param nodeId  节点
     * @param request 请求
     * @return 影响行数
     */
    public int delCache(String nodeId, HttpServletRequest request) {
        String checkUserWorkspace = this.getCheckUserWorkspace(request);
        io.voyager1.core.db.Entity entity = new io.voyager1.core.db.Entity();
        entity.set("nodeId", nodeId);
        entity.set("workspaceId", checkUserWorkspace);
        return this.del(entity);
    }

    /**
     * 删除节点 工作空间缓存
     *
     * @param dataId  数据ID
     * @param nodeId  节点
     * @param request 请求
     * @return 影响行数
     */
    public int delCache(String dataId, String nodeId, HttpServletRequest request) {
        return this.delByWorkspace(request, entity -> {
            T data = ReflectUtil.newInstance(this.tClass);
            data.setNodeId(nodeId);
            data.dataId(dataId);
            io.voyager1.core.db.Entity entity1 = dataBeanToEntity(data);
            entity.putAll(entity1);
        });
    }

    /**
     * 根据 节点和数据ID查询数据
     *
     * @param nodeId 节点ID
     * @param dataId 数据ID
     * @return data
     */
    @Override
    public T getData(String nodeId, String dataId) {
        T data = ReflectUtil.newInstance(this.tClass);
        data.setNodeId(nodeId);
        data.dataId(dataId);
        return this.queryByBean(data);
    }

    /**
     * 查询远端项目
     *
     * @param nodeModel 节点
     * @param id        项目ID
     * @return json
     */
    public abstract JSONObject getItem(NodeModel nodeModel, String id);

    /**
     * 查询列表数据
     *
     * @param nodeModel 节点
     * @return json
     */
    public abstract JSONArray getLitDataArray(NodeModel nodeModel);

    /**
     * 查询孤立的数据
     *
     * @param machineNodeModel 资产
     * @return json
     */
    public abstract List<T> lonelyDataArray(MachineNodeModel machineNodeModel);
}
