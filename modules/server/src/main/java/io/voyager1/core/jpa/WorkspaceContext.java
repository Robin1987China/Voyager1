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

import io.voyager1.common.Const;
import io.voyager1.util.JakartaServletUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;

/**
 * 工作空间上下文工具（清洁室实现，取代承继框架 BaseWorkspaceService 的静态 getWorkspaceId）。
 */
public final class WorkspaceContext {

    private WorkspaceContext() {
    }

    /**
     * 从请求参数或请求头解析当前工作空间 ID，缺省返回默认工作空间。
     */
    public static String getWorkspaceId(HttpServletRequest request) {
        String workspaceId = request.getParameter(Const.WORKSPACE_ID_REQ_HEADER);
        if (workspaceId == null || workspaceId.isEmpty()) {
            workspaceId = JakartaServletUtil.getHeader(request, Const.WORKSPACE_ID_REQ_HEADER, StandardCharsets.UTF_8);
        }
        return (workspaceId == null || workspaceId.isEmpty() ? Const.WORKSPACE_DEFAULT_ID : workspaceId);
    }
}
