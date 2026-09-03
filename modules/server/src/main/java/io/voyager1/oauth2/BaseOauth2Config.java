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

import io.voyager1.util.RegexPool;
import io.voyager1.util.Tuple;
import io.voyager1.util.Validator;
import io.voyager1.util.ClassUtil;
import io.voyager1.util.ReflectUtil;
import lombok.Data;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.request.AuthRequest;
import io.voyager1.common.i18n.I18nMessageUtil;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * @since 2023/3/30
 */
@Data
public abstract class BaseOauth2Config {

    public static final Map<String, Tuple> DB_KEYS = new HashMap<>();

    static {
        Set<Class<?>> classes = ClassUtil.scanPackageBySuper(BaseOauth2Config.class.getPackage().getName(), BaseOauth2Config.class);
        for (Class<?> aClass : classes) {
            if (ClassUtil.isAbstract(aClass)) {
                continue;
            }
            Field field = ReflectUtil.getField(aClass, "KEY");
            Assert.notNull(field, "没有配置 KEY 字段," + aClass.getName());
            String staticFieldValue = (String) ReflectUtil.getStaticFieldValue(field);
            BaseOauth2Config baseOauth2Config = (BaseOauth2Config) ReflectUtil.newInstanceIfPossible(aClass);
            DB_KEYS.put(baseOauth2Config.provide(), new Tuple(staticFieldValue, aClass));
        }
    }

    /**
     * 数据库存储的 key
     *
     * @param provide 平台名
     * @return 配置对象
     */
    public static Tuple getDbKey(String provide) {
        return DB_KEYS.get(provide);
    }

    protected Boolean enabled;
    protected String clientId;
    protected String clientSecret;
    protected String redirectUri;
    /**
     * 是否自动创建用户
     */
    protected Boolean autoCreteUser;
    protected Boolean ignoreCheckState;
    /**
     * 创建用户后，自动关联权限组
     */
    protected String permissionGroup;


    /**
     * 是否开启
     *
     * @return true 开启
     */
    public boolean enabled() {
        return enabled != null && enabled;
    }

    /**
     * 是否自动创建用户
     *
     * @return true 开启
     */
    public boolean autoCreteUser() {
        return autoCreteUser != null && autoCreteUser;
    }


    /**
     * 验证数据
     */
    public void check() {
        Assert.hasText(this.clientId, "没有配置 clientId");
        Assert.hasText(this.clientSecret, "没有配置 clientSecret");
        Validator.validateMatchRegex(RegexPool.URL_HTTP, this.redirectUri, "请配置正确的重定向 url");
    }

    /**
     * 供应商
     *
     * @return 返回供应商
     */
    public abstract String provide();

    /**
     * oauth2 请求对象
     *
     * @return AuthRequest
     */
    public abstract AuthRequest authRequest();

    public <T> T getValue(Function<BaseOauth2Config, T> function, T defaultValue) {
        return Optional.ofNullable(function.apply(this)).orElse(defaultValue);
    }

    public AuthConfig authConfig() {
        return AuthConfig.builder()
            .clientId(this.clientId)
            .clientSecret(this.clientSecret)
            .redirectUri(this.redirectUri)
            .ignoreCheckState(this.getValue(BaseOauth2Config::getIgnoreCheckState, false))
            .build();
    }
}
