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

package io.voyager1.common.interceptor;

import io.voyager1.util.ArrayUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.exception.AgentException;
import io.voyager1.model.BaseNodeModel;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.user.UserBindWorkspaceModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.*;
import io.voyager1.core.jpa.WorkspaceContext;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.user.UserBindWorkspaceService;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;

/**
 * 权限拦截器
 *
 * @since 2019/03/16.
 */
@Configuration
public class PermissionInterceptor implements HandlerMethodInterceptor {

    @Resource
    private NodeService nodeService;
    @Resource
    private UserBindWorkspaceService userBindWorkspaceService;
    public static final Supplier<String> DEMO_TIP = () -> "演示账号不能使用该功能";
    /**
     * demo 账号不能使用的功能
     */
    private static final MethodFeature[] DEMO = new MethodFeature[]{
        MethodFeature.DEL,
        MethodFeature.UPLOAD,
        MethodFeature.REMOTE_DOWNLOAD,
        MethodFeature.EXECUTE};


    private SystemPermission getSystemPermission(HandlerMethod handlerMethod) {
        SystemPermission systemPermission = handlerMethod.getMethodAnnotation(SystemPermission.class);
        if (systemPermission == null) {
            systemPermission = handlerMethod.getBeanType().getAnnotation(SystemPermission.class);
        }
        return systemPermission;
    }

    private NodeDataPermission getNodeDataPermission(HandlerMethod handlerMethod) {
        NodeDataPermission nodeDataPermission = handlerMethod.getMethodAnnotation(NodeDataPermission.class);
        if (nodeDataPermission == null) {
            nodeDataPermission = handlerMethod.getBeanType().getAnnotation(NodeDataPermission.class);
        }
        return nodeDataPermission;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
        this.addNode(request);
        UserModel userModel = BaseServerController.getUserModel();
        if (userModel == null || userModel.isSuperSystemUser()) {
            // 没有登录、或者超级管理直接放过
            return true;
        }
        //
        boolean permission = this.checkSystemPermission(userModel, request, response, handlerMethod);
        if (!permission) {
            return false;
        }
        permission = this.checkNodeDataPermission(userModel, request, response, handlerMethod);
        if (!permission) {
            return false;
        }
        Feature feature = handlerMethod.getMethodAnnotation(Feature.class);
        if (feature == null) {
            return true;
        }
        MethodFeature method = feature.method();
        if (ArrayUtil.contains(DEMO, method) && userModel.isDemoUser()) {
            this.errorMsg(response, DEMO_TIP.get());
            return false;
        }
        ClassFeature classFeature = feature.cls();
        if (classFeature == ClassFeature.NULL) {
            Feature feature1 = handlerMethod.getBeanType().getAnnotation(Feature.class);
            if (feature1 != null && feature1.cls() != ClassFeature.NULL) {
                classFeature = feature1.cls();
            }
        }
        // 判断功能权限
        if (method != MethodFeature.LIST) {
            String workspaceId = WorkspaceContext.getWorkspaceId(request);
            UserBindWorkspaceModel.PermissionResult permissionResult = userBindWorkspaceService.checkPermission(userModel, workspaceId + "-" + method.name());
            if (!permissionResult.isSuccess()) {
                this.errorMsg(response, permissionResult.errorMsg(String.format("对应功能【%s-%s】", I18nMessageUtil.get(classFeature.getName().get()), I18nMessageUtil.get(method.getName().get()))));
                return false;
            }
        }
        return true;
    }

    /**
     * 检查管理员权限
     *
     * @param userModel     用户
     * @param response      响应
     * @param handlerMethod 拦截到到方法
     * @return true 有权限
     */
    private boolean checkNodeDataPermission(UserModel userModel, HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) {
        NodeDataPermission nodeDataPermission = this.getNodeDataPermission(handlerMethod);
        if (nodeDataPermission == null || userModel.isSuperSystemUser()) {
            return true;
        }
        NodeModel node = (NodeModel) request.getAttribute("node");
        if (node != null) {
            String parameterName = nodeDataPermission.parameterName();
            io.voyager1.core.jpa.DataService<?> baseNodeService = SpringContextHolder.getBean(nodeDataPermission.cls());
            String dataId = request.getParameter(parameterName);
            if ((dataId != null && !dataId.isEmpty())) {
                BaseNodeModel data = (BaseNodeModel) baseNodeService.getData(node.getId(), dataId);
                if (data != null) {
                    UserBindWorkspaceModel.PermissionResult permissionResult = userBindWorkspaceService.checkPermission(userModel, data.getWorkspaceId());

                    if (!permissionResult.isSuccess()) {
                        this.errorMsg(response, permissionResult.errorMsg());
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 检查管理员权限
     *
     * @param userModel     用户
     * @param response      响应
     * @param handlerMethod 拦截到到方法
     * @return true 有权限
     */
    private boolean checkSystemPermission(UserModel userModel, HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) {
        SystemPermission systemPermission = this.getSystemPermission(handlerMethod);
        if (systemPermission == null) {
            return true;
        }
        if (systemPermission.superUser() && !userModel.isSuperSystemUser()) {
            this.errorMsg(response, "您不是超级管理员没有权限:-2");
            return false;
        }
        if (!userModel.checkSystemUser()) {
            this.errorMsg(response, "您没有服务端管理权限:-2");
            return false;
        }
        return true;
    }

    private void addNode(HttpServletRequest request) {
        String nodeId = request.getParameter("nodeId");
        if (!StrUtil.isBlankOrUndefined(nodeId)) {
            // 节点信息
            NodeModel nodeModel = nodeService.getByKey(nodeId);
            if (nodeModel != null && !nodeModel.isOpenStatus()) {
                throw new AgentException(nodeModel.getName() + "节点未启用");
            }
            request.setAttribute("node", nodeModel);
        }
    }

    private void errorMsg(HttpServletResponse response, String msg) {
        ApiResult<String> jsonMessage = new ApiResult<>(302, msg);
        JakartaServletUtil.write(response, jsonMessage.toString(), MediaType.APPLICATION_JSON_VALUE);
    }
}
