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

package io.voyager1.common;

import io.voyager1.util.CharsetUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.URLUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.service.manage.ProjectInfoService;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;

/**
 * agent 端
 *
 * @since 2019/4/17
 */
public abstract class BaseAgentController extends BaseVoyager1Controller {

    @Resource
    protected ProjectInfoService projectInfoService;


    /**
     * 获取server 端操作人
     *
     * @param request req
     * @return name
     */
    private static String getUserName(HttpServletRequest request) {
        String name = JakartaServletUtil.getHeaderIgnoreCase(request, Const.VOYAGER1_SERVER_USER_NAME);
        name = CharsetUtil.convert(name, java.nio.charset.StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8);
        name = (name == null || name.isEmpty() ? "-" : name);
        return URLUtil.decode(name, StandardCharsets.UTF_8);
    }

    /**
     * 获取server 端操作人
     *
     * @return name
     */
    public static String getNowUserName() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes == null) {
            return "-";
        }
        HttpServletRequest request = servletRequestAttributes.getRequest();
        return getUserName(request);
    }

    protected String getWorkspaceId() {
        return JakartaServletUtil.getHeader(getRequest(), Const.WORKSPACE_ID_REQ_HEADER, StandardCharsets.UTF_8);
    }

    /**
     * 获取拦截器中缓存的项目信息
     *
     * @return NodeProjectInfoModel
     */
    protected NodeProjectInfoModel getProjectInfoModel() {
        String id = getParameter("id");
        NodeProjectInfoModel nodeProjectInfoModel = tryGetProjectInfoModel(id);
        Assert.notNull(nodeProjectInfoModel, "获取项目信息失败:" + id);
        return nodeProjectInfoModel;
    }

    /**
     * 根据 项目ID 获取项目信息
     *
     * @return NodeProjectInfoModel
     */
    protected NodeProjectInfoModel getProjectInfoModel(String id) {
        NodeProjectInfoModel nodeProjectInfoModel = tryGetProjectInfoModel(id);
        Assert.notNull(nodeProjectInfoModel, "获取项目信息失败:" + id);
        return nodeProjectInfoModel;
    }

    protected NodeProjectInfoModel tryGetProjectInfoModel() {
        String id = getParameter("id");
        return tryGetProjectInfoModel(id);
    }

    protected NodeProjectInfoModel tryGetProjectInfoModel(String id) {
        if ((id != null && !id.isEmpty())) {
            return projectInfoService.getItem(id);
        }
        return null;
    }
}
