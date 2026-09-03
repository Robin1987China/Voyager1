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

package io.voyager1.common.forward;

import io.voyager1.util.Opt;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.common.Const;
import io.voyager1.configuration.NodeConfig;
import io.voyager1.system.ServerConfig;
import io.voyager1.transport.DataContentType;
import io.voyager1.transport.IUrlItem;

import java.util.Map;
import java.util.Optional;

/**
 * @since 2023/2/18
 */
public class DefaultUrlItem implements IUrlItem {
    private final NodeUrl nodeUrl;
    private final Integer timeout;
    private final String workspaceId;
    private final DataContentType dataContentType;
    private final Map<String, String> header;

    public DefaultUrlItem(NodeUrl nodeUrl, Integer timeout, String workspaceId, DataContentType dataContentType, Map<String, String> header) {
        this.nodeUrl = nodeUrl;
        this.timeout = timeout;
        this.workspaceId = workspaceId;
        this.dataContentType = dataContentType;
        this.header = header;
    }

    @Override
    public String path() {
        return nodeUrl.getUrl();
    }

    @Override
    public Integer timeout() {
        if (nodeUrl.isFileTimeout()) {
            ServerConfig serverConfig = SpringContextHolder.getBean(ServerConfig.class);
            NodeConfig configNode = serverConfig.getNode();
            return configNode.getUploadFileTimeout();
        } else {
            return Optional.of(nodeUrl.getTimeout())
                .flatMap(timeOut -> {
                    if (timeOut == 0) {
                        // 读取节点配置的超时时间
                        return Optional.ofNullable(timeout);
                    }
                    // 值 < 0  url 指定不超时
                    return timeOut > 0 ? Optional.of(timeOut) : Optional.empty();
                })
                .map(timeOut -> {
                    if (timeOut <= 0) {
                        return null;
                    }
                    // 超时时间不能小于 2 秒
                    return Math.max(timeOut, 2);
                })
                .orElse(null);
        }
    }

    @Override
    public String workspaceId() {
        return (workspaceId == null || workspaceId.isEmpty() ? Const.WORKSPACE_DEFAULT_ID : workspaceId);
    }

    @Override
    public DataContentType contentType() {
        return dataContentType;
    }

    @Override
    public Map<String, String> header() {
        return header;
    }
}
