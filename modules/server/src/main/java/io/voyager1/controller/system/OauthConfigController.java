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

package io.voyager1.controller.system;

import io.voyager1.util.Tuple;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.oauth2.BaseOauth2Config;
import io.voyager1.oauth2.Oauth2Factory;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.system.SystemParametersServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * @since 2023/3/26
 */
@RestController
@RequestMapping(value = "system/oauth-config")
@Feature(cls = ClassFeature.OAUTH_CONFIG)
@SystemPermission
public class OauthConfigController {

    private final SystemParametersServer systemParametersServer;

    public OauthConfigController(SystemParametersServer systemParametersServer) {
        this.systemParametersServer = systemParametersServer;
    }

    @GetMapping(value = "oauth2", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<BaseOauth2Config> oauth2(String provide) {
        Tuple tuple = BaseOauth2Config.getDbKey(provide);
        Assert.notNull(tuple, "没有对应的类型");
        BaseOauth2Config configDefNewInstance = systemParametersServer.getConfigDefNewInstance(tuple.get(0), tuple.get(1));
        return ApiResult.success("", configDefNewInstance);
    }

    @PostMapping(value = "oauth2-save", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> saveOauth2(HttpServletRequest request, String provide) {
        Tuple tuple = BaseOauth2Config.getDbKey(provide);
        Assert.notNull(tuple, "没有对应的类型");
        Class<BaseOauth2Config> oauth2ConfigClass = tuple.get(1);
        BaseOauth2Config oauth2Config = JakartaServletUtil.toBean(request, oauth2ConfigClass, true);
        Assert.notNull(tuple, "没有对应的类型");
        if (oauth2Config.enabled()) {
            oauth2Config.check();
        }
        systemParametersServer.upsert(tuple.get(0), oauth2Config, oauth2Config.provide());
        //
        Oauth2Factory.put(oauth2Config);
        return ApiResult.success("保存成功");
    }
}
