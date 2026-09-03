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

package io.voyager1.service.dblog;

import io.voyager1.core.jpa.WorkspaceContext;

import io.voyager1.util.BeanPath;
import io.voyager1.util.CollUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.core.entity.UserOperateLogEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.UserOperateLogRepository;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.MonitorModel;
import io.voyager1.model.data.MonitorUserOptModel;
import io.voyager1.model.data.WorkspaceModel;
import io.voyager1.model.log.UserOperateLogV1;
import io.voyager1.model.user.UserModel;
import io.voyager1.monitor.NotifyUtil;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.monitor.MonitorUserOptService;
import io.voyager1.service.system.WorkspaceService;
import io.voyager1.service.user.UserService;
import io.voyager1.system.init.OperateLogController;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

/**
 * 操作日志
 *
 * @since 2019/7/20
 */
@Service
@Slf4j
public class DbUserOperateLogService extends JpaWorkspaceService<UserOperateLogV1, UserOperateLogEntity> {

    private final MonitorUserOptService monitorUserOptService;
    private final UserService userService;
    private final WorkspaceService workspaceService;
    private final UserOperateLogRepository userOperateLogRepository;
    /**
     * 通用 bean 的名称字段 bean-path
     */
    private static final BeanPath[] NAME_BEAN_PATHS = new BeanPath[]{BeanPath.create("name"), BeanPath.create("title")};

    public DbUserOperateLogService(MonitorUserOptService monitorUserOptService,
                                   UserService userService,
                                   WorkspaceService workspaceService,
                                   UserOperateLogRepository userOperateLogRepository) {
        this.monitorUserOptService = monitorUserOptService;
        this.userService = userService;
        this.workspaceService = workspaceService;
        this.userOperateLogRepository = userOperateLogRepository;
    }

    @Override
    protected JpaRepository<UserOperateLogEntity, String> repository() {
        return userOperateLogRepository;
    }

    @Override
    protected JpaSpecificationExecutor<UserOperateLogEntity> specExecutor() {
        return userOperateLogRepository;
    }

    @Override
    protected Class<UserOperateLogEntity> entityClass() {
        return UserOperateLogEntity.class;
    }

    @Override
    protected Class<UserOperateLogV1> modelClass() {
        return UserOperateLogV1.class;
    }

    /**
     * 查询指定用户的操作日志
     *
     * @param request 请求信息
     * @param userId  用户id
     * @return page
     */
    public PageResultDto<UserOperateLogV1> listPageByUserId(HttpServletRequest request, String userId) {
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        paramMap.put("userId", userId);
        return super.listPage(paramMap);
    }

    /**
     * 根据 数据ID 和 节点ID 查询相关数据名称
     *
     * @param classFeature     功能
     * @param cacheInfo        操作缓存
     * @param userOperateLogV1 操作日志
     * @return map
     */
    private Map<String, Object> buildDataMsg(ClassFeature classFeature, OperateLogController.CacheInfo cacheInfo, UserOperateLogV1 userOperateLogV1) {
        Map<String, Object> optDataNameMap = cacheInfo.getOptDataNameMap();
        if (optDataNameMap != null) {
            return optDataNameMap;
        }
        return this.buildDataMsg(classFeature, userOperateLogV1.getDataId(), userOperateLogV1.getNodeId());
    }

    /**
     * 根据 数据ID 和 节点ID 查询相关数据名称
     *
     * @param classFeature 功能
     * @param dataId       数据ID
     * @param nodeId       节点ID
     * @return map
     */
    public Map<String, Object> buildDataMsg(ClassFeature classFeature, String dataId, String nodeId) {
        if (classFeature == null) {
            return null;
        }
        Class<? extends io.voyager1.core.jpa.DataService<?>> dbService = classFeature.getDbService();
        if (dbService == null) {
            return null;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("数据id", dataId);
        io.voyager1.core.jpa.DataService<?> baseDbCommonService = SpringContextHolder.getBean(dbService);
        Object data = baseDbCommonService.getData(nodeId, dataId);
        map.put("数据名称", this.tryGetBeanName(data));
        //
        map.put("节点id", nodeId);
        ClassFeature parent = classFeature.getParent();
        if (parent == ClassFeature.NODE) {
            Class<? extends io.voyager1.core.jpa.DataService<?>> dbServiceParent = parent.getDbService();
            io.voyager1.core.jpa.DataService<?> baseDbCommonServiceParent = SpringContextHolder.getBean(dbServiceParent);
            Object dataParent = baseDbCommonServiceParent.getData(nodeId, dataId);
            map.put("节点名称", this.tryGetBeanName(dataParent));
        }
        return map;
    }

    private Object tryGetBeanName(Object data) {
        for (BeanPath beanPath : NAME_BEAN_PATHS) {
            Object o = beanPath.get(data);
            if (o != null) {
                return o;
            }
        }
        return null;
    }

    private String buildContent(UserModel optUserItem, Map<String, Object> dataMap, WorkspaceModel workspaceModel, String optTypeMsg, UserOperateLogV1 userOperateLogV1) {
        Map<String, Object> map = new LinkedHashMap<>(10);
        map.put("操作用户", optUserItem.getName());
        map.put("操作状态码", userOperateLogV1.getOptStatus());
        map.put("操作类型", optTypeMsg);
        if (workspaceModel != null) {
            map.put("所属工作空间", workspaceModel.getName());
        }
        map.put("操作IP", userOperateLogV1.getIp());
        map.put("操作时间", DateTime.now().toString());
        if (dataMap != null) {
            map.putAll(dataMap);
        }
        List<String> list = map.entrySet()
            .stream()
            .filter(entry -> entry.getValue() != null)
            .map(entry -> entry.getKey() + "：" + entry.getValue())
            .collect(Collectors.toList());
        //
        return String.join("\n", list);
    }

    /**
     * 判断当前操作是否需要报警
     *
     * @param userOperateLogV1 操作信息
     * @param cacheInfo        操作缓存相关
     * @return 解析后的相关数据
     */
    private Map<String, Object> checkMonitor(UserOperateLogV1 userOperateLogV1, OperateLogController.CacheInfo cacheInfo) {
        ClassFeature classFeature = EnumUtil.fromString(ClassFeature.class, userOperateLogV1.getClassFeature(), null);
        MethodFeature methodFeature = EnumUtil.fromString(MethodFeature.class, userOperateLogV1.getMethodFeature(), null);
        UserModel optUserItem = userService.getByKey(userOperateLogV1.getUserId());
        if (classFeature == null || methodFeature == null || optUserItem == null) {
            return null;
        }
        Map<String, Object> dataMap = this.buildDataMsg(classFeature, cacheInfo, userOperateLogV1);
        WorkspaceModel workspaceModel = workspaceService.getByKey(userOperateLogV1.getWorkspaceId());

        String optTypeMsg = String.format(" 【%s】->【%s】", I18nMessageUtil.get(classFeature.getName().get()), I18nMessageUtil.get(methodFeature.getName().get()));
        List<MonitorUserOptModel> monitorUserOptModels = monitorUserOptService.listByType(userOperateLogV1.getWorkspaceId(),
            classFeature,
            methodFeature,
            userOperateLogV1.getUserId());
        if ((monitorUserOptModels == null || monitorUserOptModels.isEmpty())) {
            return dataMap;
        }
        String context = this.buildContent(optUserItem, dataMap, workspaceModel, optTypeMsg, userOperateLogV1);
        for (MonitorUserOptModel monitorUserOptModel : monitorUserOptModels) {
            List<String> notifyUser = monitorUserOptModel.notifyUser();
            if ((notifyUser == null || notifyUser.isEmpty())) {
                continue;
            }
            for (String userId : notifyUser) {
                UserModel item = userService.getByKey(userId);
                if (item == null) {
                    continue;
                }
                // 邮箱
                String email = item.getEmail();
                if ((email != null && !email.isEmpty())) {
                    MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.mail, email);
                    I18nThreadUtil.execute(() -> {
                        try {
                            NotifyUtil.send(notify1, "用户操作报警", context);
                        } catch (Exception e) {
                            log.error("发送报警信息错误", e);
                        }
                    });

                }
                // dingding
                String dingDing = item.getDingDing();
                if ((dingDing != null && !dingDing.isEmpty())) {
                    MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.dingding, dingDing);
                    I18nThreadUtil.execute(() -> {
                        try {
                            NotifyUtil.send(notify1, "用户操作报警", context);
                        } catch (Exception e) {
                            log.error("发送报警信息错误", e);
                        }
                    });
                }
                // 企业微信
                String workWx = item.getWorkWx();
                if ((workWx != null && !workWx.isEmpty())) {
                    MonitorModel.Notify notify1 = new MonitorModel.Notify(MonitorModel.NotifyType.workWx, workWx);
                    I18nThreadUtil.execute(() -> {
                        try {
                            NotifyUtil.send(notify1, "用户操作报警", context);
                        } catch (Exception e) {
                            log.error("发送报警信息错误", e);
                        }
                    });
                }
            }
        }
        return dataMap;
    }

    /**
     * 插入操作日志
     *
     * @param userOperateLogV1 日志信息
     * @param cacheInfo        当前操作相关信息
     */
    public void insert(UserOperateLogV1 userOperateLogV1, OperateLogController.CacheInfo cacheInfo) {
        super.insert(userOperateLogV1);
        I18nThreadUtil.execute(() -> {
            // 更新用户名和工作空间名
            try {
                UserOperateLogV1 update = new UserOperateLogV1();
                update.setId(userOperateLogV1.getId());
                UserModel userModel = userService.getByKey(userOperateLogV1.getUserId());
                Optional.ofNullable(userModel).ifPresent(userModel1 -> update.setUsername(userModel1.getName()));
                WorkspaceModel workspaceModel = workspaceService.getByKey(userOperateLogV1.getWorkspaceId());
                Optional.ofNullable(workspaceModel).ifPresent(workspaceModel1 -> update.setWorkspaceName(workspaceModel1.getName()));
                this.updateById(update);
            } catch (Exception e) {
                log.error("更新操作日志失败", e);
            }
            // 检查操作监控
            try {
                Map<String, Object> monitor = this.checkMonitor(userOperateLogV1, cacheInfo);
                if (monitor != null) {
                    String dataName = Optional.ofNullable(monitor.get("数据名称")).map(StrUtil::toStringOrNull).orElse("-");
                    UserOperateLogV1 userOperateLogV11 = new UserOperateLogV1();
                    userOperateLogV11.setDataName(dataName);
                    userOperateLogV11.setId(userOperateLogV1.getId());
                    super.updateById(userOperateLogV11);
                }
            } catch (Exception e) {
                log.error("执行操作监控错误", e);
            }
        });
    }

    @Override
    public PageResultDto<UserOperateLogV1> listPage(HttpServletRequest request) {
        // 验证工作空间权限
        Map<String, String> paramMap = JakartaServletUtil.getParamMap(request);
        //String workspaceId = this.getCheckUserWorkspace(request);
        //paramMap.put("workspaceId:in", workspaceId + "," + "");
        return super.listPage(paramMap);
    }

    @Override
    public String getCheckUserWorkspace(HttpServletRequest request) {
        // 忽略检查
        return WorkspaceContext.getWorkspaceId(request);
        // String header = JakartaServletUtil.getHeader(request, Const.WORKSPACE_ID_REQ_HEADER, StandardCharsets.UTF_8);
        // return (header != null ? header : "");
    }

    @Override
    protected void checkUserWorkspace(String workspaceId, UserModel userModel) {
        // 忽略检查
    }

    @Override
    protected String[] clearTimeColumns() {
        return new String[]{"optTime", "createTimeMillis"};
    }
}
