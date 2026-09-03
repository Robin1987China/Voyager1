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

import io.voyager1.util.UrlBuilder;
import com.alibaba.fastjson2.JSONObject;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.core.auth.AgentCredential;
import io.voyager1.core.auth.AgentTokenSigner;
import io.voyager1.encrypt.EncryptFactory;
import io.voyager1.encrypt.Encryptor;
import io.voyager1.transport.i18n.TransportI18nMessageUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 插件端消息传输服务
 *
 * @since 2022/12/18
 */
@Slf4j
public class HttpTransportServer implements TransportServer {

    private static final String JSON_CONTENT_TYPE = "application/json";

    private HttpRequest createRequest(INodeInfo nodeInfo, IUrlItem urlItem, HttpRequest.Method method) {
        String url = nodeInfo.scheme() + "://" + nodeInfo.url() + "/";
        UrlBuilder urlBuilder = UrlBuilder.of(url).addPath(urlItem.path());
        HttpRequest httpRequest = HttpRequest.of(urlBuilder);
        httpRequest.setMethod(method);
        // 添加请求头
        Map<String, String> header = urlItem.header();
        httpRequest.headerMap(header, true);

        Optional.ofNullable(urlItem.timeout()).ifPresent(integer -> httpRequest.timeout(integer * 1000));

        httpRequest.header(TRANSPORT_ENCRYPTION, nodeInfo.transportEncryption() + "");

        httpRequest.header(VOYAGER1_AGENT_AUTHORIZE, signToken(nodeInfo));
        //
        httpRequest.header(WORKSPACE_ID_REQ_HEADER, urlItem.workspaceId());
        Optional.ofNullable(nodeInfo.proxy()).ifPresent(httpRequest::setProxy);
        return httpRequest;
    }

    private HttpRequest createRequest(INodeInfo nodeInfo, IUrlItem urlItem) {
        return createRequest(nodeInfo, urlItem, HttpRequest.Method.POST);
    }

    @SuppressWarnings("unchecked")
    private void appendRequestData(HttpRequest httpRequest, IUrlItem urlItem, Object data, INodeInfo nodeInfo) {
        DataContentType dataContentType = urlItem.contentType();
        Optional.ofNullable(data).ifPresent(o -> {
            Encryptor encryptor;
            try {
                encryptor = EncryptFactory.createEncryptor(nodeInfo.transportEncryption());
                if (dataContentType == DataContentType.FORM_URLENCODED) {
                    if (o instanceof Map) {
                        Map<String, Object> map = (Map<String, Object>) o;
                        Map<String, Object> encryptedMap = new HashMap<>();
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            String encryptedKey = encryptor.encrypt(entry.getKey());
                            Object value = entry.getValue();
                            Object newValue;
                            if (value instanceof String[]) {
                                String[] valueStr = (String[]) value;
                                for (int i = 0; i < valueStr.length; i++) {
                                    valueStr[i] = encryptor.encrypt(valueStr[i]);
                                }
                                newValue = valueStr;
                            } else if (value instanceof java.io.File || value instanceof byte[] || value instanceof java.io.InputStream) {
                                newValue = value;
                            } else {
                                newValue = encryptor.encrypt(entry.getValue() == null ? null : entry.getValue().toString());
                            }
                            encryptedMap.put(encryptedKey, newValue);
                        }
                        httpRequest.form(encryptedMap);
                    } else {
                        throw new IllegalArgumentException("不支持的类型:" + o.getClass());
                    }
                } else if (dataContentType == DataContentType.JSON) {
                    httpRequest.body(encryptor.encrypt(JSONObject.toJSONString(o)), JSON_CONTENT_TYPE);
                } else {
                    throw new IllegalArgumentException("不支持的 contentType");
                }
            } catch (Exception e) {
                log.error("编码异常", e);
                throw new TransportAgentException("节点传输信息编码异常:" + e.getMessage());
            }
        });
    }

    private String executeRequest(HttpRequest httpRequest, INodeInfo nodeInfo, IUrlItem urlItem) {
        //
        if (log.isDebugEnabled()) {
            log.debug("{}[{}] -> {} {}", nodeInfo.name(), httpRequest.getUrl(), urlItem.workspaceId(), httpRequest.form() == null ? "-" : httpRequest.form());
        }
        return httpRequest.thenFunction(response -> {
            int status = response.getStatus();
            String body = response.body();
            log.debug("Completed {}", body);
            if (status != HttpRequest.HttpStatus.HTTP_OK) {
                log.warn("{} 响应异常 状态码错误：{} {}", nodeInfo.name(), status, body);
                throw new TransportAgentException(nodeInfo.name() + " 节点响应异常,状态码错误：" + status);
            }
            return body;
        });
    }

    @Override
    public String execute(INodeInfo nodeInfo, IUrlItem urlItem, Object data) {
        HttpRequest httpRequest = this.createRequest(nodeInfo, urlItem);
        this.appendRequestData(httpRequest, urlItem, data, nodeInfo);
        try {
            return this.executeRequest(httpRequest, nodeInfo, urlItem);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(TransformServerFactory.get().transformException(e, nodeInfo));
        }
    }


    @Override
    public void download(INodeInfo nodeInfo, IUrlItem urlItem, Object data, Consumer<DownloadCallback> consumer) {
        HttpRequest httpRequest = this.createRequest(nodeInfo, urlItem, HttpRequest.Method.GET);
        httpRequest.setFollowRedirects(true);
        this.appendRequestData(httpRequest, urlItem, data, nodeInfo);
        try (HttpRequest.Response response1 = httpRequest.execute()) {
            String contentDisposition = response1.header(HttpRequest.Header.CONTENT_DISPOSITION);
            String contentType = response1.header(HttpRequest.Header.CONTENT_TYPE);
            DownloadCallback build = DownloadCallback.builder()
                .contentDisposition(contentDisposition).contentType(contentType).inputStream(response1.bodyStream())
                .build();
            consumer.accept(build);
        } catch (Exception e) {
            throw Lombok.sneakyThrow(TransformServerFactory.get().transformException(e, nodeInfo));
        }
    }

    @Override
    public IProxyWebSocket websocket(INodeInfo nodeInfo, IUrlItem urlItem, Object... parameters) {
        String url = nodeInfo.scheme() + "://" + nodeInfo.url() + "/";
        UrlBuilder urlBuilder = UrlBuilder.of(url).addPath(urlItem.path());
        //
        urlBuilder.addQuery(VOYAGER1_AGENT_AUTHORIZE, signToken(nodeInfo));
        //
        urlBuilder.addQuery(WORKSPACE_ID_REQ_HEADER, urlItem.workspaceId());
        for (int i = 0; i < parameters.length; i += 2) {
            Object parameter = parameters[i + 1];
            String value = parameter == null ? "" : parameter.toString();
            urlBuilder.addQuery(parameters[i].toString(), value);
        }
        String uriTemplate = urlBuilder.build();
        uriTemplate = removePrefixIgnoreCase(uriTemplate, nodeInfo.scheme());
        String wss = "wss";
        String ws = "ws";
        String protocol = "https".equalsIgnoreCase(nodeInfo.scheme()) ? wss : ws;
        uriTemplate = protocol + uriTemplate;
        //
        if (log.isDebugEnabled()) {
            log.debug("{}[{}] -> {}", nodeInfo.name(), uriTemplate, urlItem.workspaceId());
        }
        Integer timeout = urlItem.timeout();
        return new ServletWebSocketClientHandler(uriTemplate, timeout);
    }

    /**
     * 为节点签署一次性请求令牌（HMAC-SHA256 + nonce + 短 TTL），取代旧 {@code sha1(name@pwd)}。
     */
    private String signToken(INodeInfo nodeInfo) {
        String legacyAuthorize = nodeInfo.authorize();
        if (legacyAuthorize == null || legacyAuthorize.isEmpty()) {
            return null;
        }
        AgentCredential credential = AgentCredential.fromLegacyAuthorize(nodeInfo.loginName(), legacyAuthorize);
        String nonce = java.util.UUID.randomUUID().toString().replace("-", "");
        return new AgentTokenSigner().sign(credential, System.currentTimeMillis() / 1000, nonce, 300);
    }

    private static String removePrefixIgnoreCase(String str, String prefix) {
        if (str != null && prefix != null && str.toLowerCase().startsWith(prefix.toLowerCase())) {
            return str.substring(prefix.length());
        }
        return str;
    }
}
