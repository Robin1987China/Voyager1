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

package io.voyager1.system.init;

import io.voyager1.util.ReUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.event.ICacheTask;
import com.alibaba.fastjson2.JSONArray;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.ILoadEvent;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.service.system.SystemParametersServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 全局代理配置
 *
 * @since 2022/7/4
 */
@Slf4j
@Configuration
public class ProxySelectorConfig extends ProxySelector implements ILoadEvent, ICacheTask {

    public static final String KEY = "global_proxy";

    private final SystemParametersServer systemParametersServer;
    private volatile List<ProxyConfigItem> proxyConfigItems;
    private ProxySelector defaultProxySelector;

    public ProxySelectorConfig(SystemParametersServer systemParametersServer) {
        this.systemParametersServer = systemParametersServer;
    }

    @Override
    public List<Proxy> select(URI uri) {
        String url = uri.toString();
        return Optional.ofNullable(proxyConfigItems)
                .flatMap(proxyConfigItems -> proxyConfigItems.stream()
                        .filter(proxyConfigItem -> {
                            if (java.util.Objects.equals(proxyConfigItem.getPattern(), "*")) {
                                return true;
                            }
                            if (ReUtil.isMatch(proxyConfigItem.getPattern(), url)) {
                                // 满足正则条件
                                return true;
                            }
                            return StrUtil.containsIgnoreCase(url, proxyConfigItem.getPattern());
                        })
                        .map(proxyConfigItem -> NodeForward.crateProxy(proxyConfigItem.getProxyType(), proxyConfigItem.getProxyAddress()))
                        .filter(Objects::nonNull)
                        .findFirst()
                        .map(Collections::singletonList)
                ).orElseGet(() -> {
                    // revert to the default behaviour
                    return defaultProxySelector == null ? Collections.singletonList(Proxy.NO_PROXY) : defaultProxySelector.select(uri);
                });
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        if (uri == null || sa == null || ioe == null) {
            throw new IllegalArgumentException(
                    "Arguments can't be null.");
        }
    }

    /**
     * 刷新
     */
    @Override
    public void refreshCache() {
        JSONArray array = systemParametersServer.getConfigDefNewInstance(ProxySelectorConfig.KEY, JSONArray.class);
        proxyConfigItems = array.toJavaList(ProxyConfigItem.class)
                .stream()
                .filter(proxyConfigItem -> StrUtil.isAllNotEmpty(proxyConfigItem.pattern, proxyConfigItem.proxyAddress, proxyConfigItem.proxyType))
                .collect(Collectors.toList());
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        if (ProxySelector.getDefault() != this) {
            defaultProxySelector = ProxySelector.getDefault();
            //
            ProxySelector.setDefault(this);
        }
        // 立马配置 全局代理
        this.refreshCache();
    }


    /**
     * @since 2022/7/4
     */
    @Data
    public static class ProxyConfigItem {

        private String pattern;

        /**
         * @see Proxy.Type
         */
        private String proxyType;

        /**
         * 127.0.0.1:8888
         */
        private String proxyAddress;
    }

}
