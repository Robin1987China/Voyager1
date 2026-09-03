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

package io.voyager1.oauth2;

import io.voyager1.util.Tuple;

import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.request.AuthRequest;
import io.voyager1.common.ILoadEvent;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.service.system.SystemParametersServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Map;

/**
 * @since 2023/3/30
 */
@Configuration
@Slf4j
public class Oauth2Factory implements ILoadEvent {

    private static final Map<String, AuthRequest> AUTH_REQUEST = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, BaseOauth2Config> AUTH_CONFIG = new java.util.concurrent.ConcurrentHashMap<>();

    private final SystemParametersServer systemParametersServer;

    public Oauth2Factory(SystemParametersServer systemParametersServer) {
        this.systemParametersServer = systemParametersServer;
    }

    /**
     * 查询已经开启的平台
     *
     * @return 平台名称
     */
    public static Collection<String> provides() {
        return AUTH_CONFIG.keySet();
    }

    /**
     * 更新配置
     *
     * @param oauth2Config 配置
     */
    public static void put(BaseOauth2Config oauth2Config) {
        if (oauth2Config.enabled()) {
            AUTH_REQUEST.put(oauth2Config.provide(), oauth2Config.authRequest());
            AUTH_CONFIG.put(oauth2Config.provide(), oauth2Config);
        } else {
            AUTH_REQUEST.remove(oauth2Config.provide());
            AUTH_CONFIG.remove(oauth2Config.provide());
        }
    }

    /**
     * 获取配置
     *
     * @param provide 平台
     * @return config
     */
    public static BaseOauth2Config getConfig(String provide) {
        BaseOauth2Config oauth2Config = AUTH_CONFIG.get(provide);
        Assert.notNull(oauth2Config, "没有找到对应的 oauth2," + provide);
        return oauth2Config;
    }

    public static AuthRequest get(String provide) {
        AuthRequest authRequest = AUTH_REQUEST.get(provide);
        Assert.notNull(authRequest, "没有找到对应的 oauth2," + provide);
        return authRequest;
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        for (Map.Entry<String, Tuple> entry : BaseOauth2Config.DB_KEYS.entrySet()) {
            Tuple value = entry.getValue();
            String dbKey = value.get(0);
            BaseOauth2Config baseOauth2Config = systemParametersServer.getConfigDefNewInstance(dbKey, value.get(1));
            put(baseOauth2Config);
            log.debug("加载 oauth2 配置 ：{} {}", entry.getKey(), dbKey);
        }
    }
}
