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

package io.voyager1.transport;

import com.alibaba.fastjson2.TypeReference;

import java.util.function.Consumer;

/**
 * 插件端消息传输服务
 **/
public interface TransportServer {

    /**
     * 请求 header
     */
    String WORKSPACE_ID_REQ_HEADER = "workspaceId";

    String VOYAGER1_AGENT_AUTHORIZE = "Voyager1-Agent-Authorize";

    String TRANSPORT_ENCRYPTION = "transport-encryption";

    /**
     * 执行请求
     *
     * @param nodeInfo 节点信息
     * @param urlItem  请求 item
     * @param data     参数
     * @return 响应的字符串
     */
    String execute(INodeInfo nodeInfo, IUrlItem urlItem, Object data);

    /**
     * 执行请求，返回响应的所有数据
     *
     * @param nodeInfo       节点信息
     * @param urlItem        请求 item
     * @param data           参数
     * @param tTypeReference 返回的泛型
     * @param <T>            泛型
     * @return 响应的字符串
     */
    default <T> T executeToType(INodeInfo nodeInfo, IUrlItem urlItem, Object data, TypeReference<T> tTypeReference) {
        String body = this.execute(nodeInfo, urlItem, data);
        return TransformServerFactory.get().transform(body, tTypeReference);
    }

    /**
     * 执行请求,仅返回成功的数据
     *
     * @param nodeInfo 节点信息
     * @param urlItem  请求 item
     * @param data     参数
     * @param tClass   返回的泛型
     * @param <T>      泛型
     * @return 响应的字符串
     */
    default <T> T executeToTypeOnlyData(INodeInfo nodeInfo, IUrlItem urlItem, Object data, Class<T> tClass) {
        String body = this.execute(nodeInfo, urlItem, data);
        return TransformServerFactory.get().transformOnlyData(body, tClass);
    }

    /**
     * 下载文件
     *
     * @param nodeInfo 节点信息
     * @param urlItem  请求 item
     * @param data     参数
     * @param consumer 回调
     */
    void download(INodeInfo nodeInfo, IUrlItem urlItem, Object data, Consumer<DownloadCallback> consumer);

    /**
     * 创建 websocket 连接
     *
     * @param nodeInfo   节点信息
     * @param urlItem    请求 item
     * @param parameters 参数
     * @return websocket
     */
    IProxyWebSocket websocket(INodeInfo nodeInfo, IUrlItem urlItem, Object... parameters);
}
