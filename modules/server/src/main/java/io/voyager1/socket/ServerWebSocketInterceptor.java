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

package io.voyager1.socket;

import io.voyager1.util.BeanUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.Tuple;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.common.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import io.voyager1.common.Const;
import io.voyager1.common.ServerConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.interceptor.PermissionInterceptor;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.func.assets.server.MachineNodeServer;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.user.UserBindWorkspaceModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.user.UserBindWorkspaceService;
import io.voyager1.service.user.UserService;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.function.Supplier;

/**
 * socket 拦截器、鉴权
 *
 * @since 2019/4/19
 */
@Slf4j
@Configuration
public class ServerWebSocketInterceptor implements HandshakeInterceptor {

    private final UserService userService;
    private final NodeService nodeService;
    private final UserBindWorkspaceService userBindWorkspaceService;
    private final MachineNodeServer machineNodeServer;

    public ServerWebSocketInterceptor(UserService userService,
                                      NodeService nodeService,
                                      UserBindWorkspaceService userBindWorkspaceService,
                                      MachineNodeServer machineNodeServer) {
        this.userService = userService;
        this.nodeService = nodeService;
        this.userBindWorkspaceService = userBindWorkspaceService;
        this.machineNodeServer = machineNodeServer;
    }

    private boolean checkNode(HttpServletRequest httpServletRequest, Map<String, Object> attributes, UserModel userModel) {
        // 验证 node 权限
        String nodeId = httpServletRequest.getParameter("nodeId");
        if (!Const.SYSTEM_ID.equals(nodeId)) {
            NodeModel nodeModel = nodeService.getByKey(nodeId, userModel);
            if (nodeModel == null) {
                return false;
            }
            //
            attributes.put("nodeInfo", nodeModel);
        }
        // 验证机器权限
        String machineId = httpServletRequest.getParameter("machineId");
        if (StringUtils.isNotEmpty(machineId)) {
            if (!userModel.checkSystemUser()) {
                // 没有权限
                return false;
            }
            MachineNodeModel machine = machineNodeServer.getByKey(machineId);
            if (machine == null) {
                return false;
            }
            attributes.put("machine", machine);
        }
        return true;
    }

    private HandlerType fromType(HttpServletRequest httpServletRequest) {
        // 判断拦截类型
        String type = httpServletRequest.getParameter("type");
        HandlerType handlerType = EnumUtil.fromString(HandlerType.class, type, null);
        if (handlerType == null) {
            log.warn("传入的类型错误：{}", type);
        }
        return handlerType;
    }

    private boolean checkHandlerType(HandlerType handlerType, UserModel userModel, HttpServletRequest httpServletRequest, Map<String, Object> attributes) {
        switch (handlerType) {
            case console: {
                //控制台
                Object dataItem = this.checkData(handlerType, userModel, httpServletRequest);
                if (dataItem == null) {
                    return false;
                }

                attributes.put("projectId", BeanUtil.getProperty(dataItem, "projectId"));
                attributes.put("dataItem", dataItem);
                break;
            }
            case nodeScript: {
                // 节点脚本模板
                Object dataItem = this.checkData(handlerType, userModel, httpServletRequest);
                if (dataItem == null) {
                    return false;
                }
                attributes.put("dataItem", dataItem);
                attributes.put("scriptId", BeanUtil.getProperty(dataItem, "scriptId"));
                break;
            }
            case script: {
                // 脚本模板
                Object dataItem = this.checkData(handlerType, userModel, httpServletRequest);
                if (dataItem == null) {
                    return false;
                }
                attributes.put("dataItem", dataItem);
                attributes.put("scriptId", BeanUtil.getProperty(dataItem, "id"));
                break;
            }
            case systemLog:
            case agentLog:
                break;
            case dockerLog: {
                Tuple dataItem = this.checkAssetsData(handlerType, userModel, httpServletRequest);
                if (dataItem == null) {
                    return false;
                }
                attributes.put("dataItem", dataItem.get(2));
                attributes.put("isAssetsManager", dataItem.get(1));
                attributes.put("machineDocker", dataItem.get(0));
                break;
            }
            case ssh: {
                Tuple dataItem = this.checkAssetsData(handlerType, userModel, httpServletRequest);
                if (dataItem == null) {
                    return false;
                }
                attributes.put("dataItem", dataItem.get(2));
                attributes.put("isAssetsManager", dataItem.get(1));
                attributes.put("machineSsh", dataItem.get(0));
                break;
            }
            case docker:
                Tuple dataItem = this.checkAssetsData(handlerType, userModel, httpServletRequest);
                if (dataItem == null) {
                    return false;
                }
                attributes.put("dataItem", dataItem.get(2));
                attributes.put("isAssetsManager", dataItem.get(1));
                attributes.put("machineDocker", dataItem.get(0));
                attributes.put("containerId", httpServletRequest.getParameter("containerId"));
                break;
            case nodeUpdate:
                break;
            case freeScript:
                //
                MachineNodeModel machine = (MachineNodeModel) attributes.get("machine");
                if (machine == null) {
                    return false;
                }
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        if (request instanceof ServletServerHttpRequest) {
            ServletServerHttpRequest serverHttpRequest = (ServletServerHttpRequest) request;
            HttpServletRequest httpServletRequest = serverHttpRequest.getServletRequest();
            // 判断用户
            String userId = httpServletRequest.getParameter("userId");
            String workspaceId = httpServletRequest.getParameter(ServerConst.WORKSPACE_ID_REQ_HEADER);
            attributes.put("workspaceId", workspaceId);
            attributes.put("lang", httpServletRequest.getParameter("lang"));
            UserModel userModel = userService.checkUser(userId);
            if (userModel == null) {
                String string = "用户不存在";
                attributes.put("permissionMsg", string);
                return true;
            }
            HandlerType handlerType = this.fromType(httpServletRequest);
            if (handlerType == null) {
                String string = "未匹配到合适的处理类型";
                attributes.put("permissionMsg", string);
                return true;
            }
            boolean checkNode = this.checkNode(httpServletRequest, attributes, userModel);
            if (!checkNode) {
                String string = "未匹配到合适的权限不足";
                attributes.put("permissionMsg", string);
                return true;
            }
            if (!this.checkHandlerType(handlerType, userModel, httpServletRequest, attributes)) {
                String string = "未找到匹配的数据";
                attributes.put("permissionMsg", string);
                return true;
            }
            // 判断权限
            String permissionMsg = this.checkPermission(userModel, attributes, handlerType);
            attributes.put("permissionMsg", permissionMsg);

            //
            String ip = JakartaServletUtil.getClientIP(httpServletRequest);
            attributes.put("ip", ip);
            //
            String userAgent = JakartaServletUtil.getHeaderIgnoreCase(httpServletRequest, HttpHeaders.USER_AGENT);
            attributes.put(HttpHeaders.USER_AGENT, userAgent);
            attributes.put("userInfo", userModel);
            return true;
        }
        return false;
    }

    /**
     * 检查权限
     *
     * @param userInfo    用户
     * @param attributes  属性
     * @param handlerType 功能类型
     * @return 错误消息
     */
    private String checkPermission(UserModel userInfo, Map<String, Object> attributes, HandlerType handlerType) {
        Object dataItem = attributes.get("dataItem");
        Object nodeInfo = attributes.get("nodeInfo");
        Object optData = dataItem == null ? nodeInfo : dataItem;
        String workspaceId = BeanUtil.getProperty(optData, "workspaceId");
        //?  : BeanUtil.getProperty(dataItem, "workspaceId");
        String useWorkspaceId;
        if (java.util.Objects.equals(workspaceId, ServerConst.WORKSPACE_GLOBAL)) {
            // 操作工作空间
            useWorkspaceId = (String) attributes.get("workspaceId");
        } else {
            // 数据工作空间
            useWorkspaceId = workspaceId;
            if (optData instanceof BaseWorkspaceModel && !java.util.Objects.equals(workspaceId, (String) attributes.get("workspaceId"))) {
                return "数据工作空间和操作工作空间不一致";
            }
        }
        if (optData instanceof BaseWorkspaceModel) {
            if ((useWorkspaceId == null || useWorkspaceId.isEmpty())) {
                return "没有找到数据对应的工作空间,不能进行操作";
            }
            // 将数据的工作空间设置为当前操作的工作空间
            BeanUtil.setProperty(optData, "workspaceId", useWorkspaceId);
        }
        //
        if (userInfo.isSuperSystemUser()) {
            return "";
        }
        if (userInfo.isDemoUser()) {
            return PermissionInterceptor.DEMO_TIP.get();
        }
        boolean isAssetsManager = ConvertUtil.toBool(attributes.get("isAssetsManager"), false);
        if (isAssetsManager && !userInfo.checkSystemUser()) {
            // 判断资产权限
            return "您没有资产管理权限";
        }
        Supplier<String> nodeUpgradeName = ClassFeature.NODE_UPGRADE.getName();
        if (handlerType == HandlerType.nodeUpdate) {
            return String.format("您没有对应功能【%s】管理权限", I18nMessageUtil.get(nodeUpgradeName.get()));
        }
        Class<?> handlerClass = handlerType.getHandlerClass();
        SystemPermission systemPermission = handlerClass.getAnnotation(SystemPermission.class);
        if (systemPermission != null) {
            if (!userInfo.isSuperSystemUser()) {
                return String.format("您没有对应功能【%s】管理权限", I18nMessageUtil.get(nodeUpgradeName.get()));
            }
        }
        Feature feature = handlerClass.getAnnotation(Feature.class);
        MethodFeature method = feature.method();
        ClassFeature cls = feature.cls();
        UserBindWorkspaceModel.PermissionResult permissionResult = userBindWorkspaceService.checkPermission(userInfo, useWorkspaceId + "-" + method.name());
        if (permissionResult.isSuccess()) {
            return "";
        }
        return permissionResult.errorMsg(String.format("对应功能【%s-%s】", I18nMessageUtil.get(cls.getName().get()), I18nMessageUtil.get(method.getName().get())));
    }

    private BaseWorkspaceModel checkData(HandlerType handlerType, UserModel userModel, HttpServletRequest httpServletRequest) {
        String id = httpServletRequest.getParameter("id");
        io.voyager1.core.jpa.DataService<?> workspaceService = SpringContextHolder.getBean(handlerType.getServiceClass());
        return (BaseWorkspaceModel) workspaceService.getByKey(id, userModel);
    }

    /**
     * 解析参数，获取对应的数据
     *
     * @param handlerType        操作类型
     * @param userModel          用户
     * @param httpServletRequest 请求信息
     * @return 数据
     */
    private Tuple checkAssetsData(HandlerType handlerType, UserModel userModel, HttpServletRequest httpServletRequest) {
        String id = httpServletRequest.getParameter("id");
        return Opt.ofBlankAble(id).map(s -> {
            io.voyager1.core.jpa.DataService<?> workspaceService = SpringContextHolder.getBean(handlerType.getServiceClass());
            BaseWorkspaceModel workspaceModel = (BaseWorkspaceModel) workspaceService.getByKey(s, userModel);
            String assetsLinkDataId = BeanUtil.getProperty(workspaceModel, handlerType.getAssetsLinkDataId());
            io.voyager1.core.jpa.DataService<?> assetsServiceClass = SpringContextHolder.getBean(handlerType.getAssetsServiceClass());
            return new Tuple(assetsServiceClass.getByKey(assetsLinkDataId, false), false, workspaceModel);
        }).orElseGet(() -> {
            String assetsLinkDataId = httpServletRequest.getParameter(handlerType.getAssetsLinkDataId());
            if ((assetsLinkDataId == null || assetsLinkDataId.isEmpty())) {
                return null;
            }
            io.voyager1.core.jpa.DataService<?> assetsServiceClass = SpringContextHolder.getBean(handlerType.getAssetsServiceClass());
            return new Tuple(assetsServiceClass.getByKey(assetsLinkDataId, false), true, null);
        });
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        if (exception != null) {
            log.error("afterHandshake", exception);
        }
    }
}
