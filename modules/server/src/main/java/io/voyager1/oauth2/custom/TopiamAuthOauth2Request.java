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

package io.voyager1.oauth2.custom;

import io.voyager1.util.StrUtil;
import io.voyager1.util.HttpRequest;
import io.voyager1.util.HttpResponse;
import io.voyager1.util.HttpUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Lombok;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthDefaultRequest;
import me.zhyd.oauth.utils.Base64Utils;
import me.zhyd.oauth.utils.UrlBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * TOPIAM 认证请求
 *
 * Created by support@topiam.cn on  2024/02/12
 */
public class TopiamAuthOauth2Request extends AuthDefaultRequest {


    public TopiamAuthOauth2Request(AuthConfig config, AuthSource source) {
        super(config, source);
    }


    @Override
    protected AuthToken getAccessToken(AuthCallback authCallback) {
        String body = getAccessToken(authCallback.getCode());
        JSONObject response = JSONObject.parseObject(body);
        checkResponse(response);
        return AuthToken.builder()
                .accessToken(response.getString("access_token"))
                .refreshToken(response.getString("refresh_token"))
                .idToken(response.getString("id_token"))
                .tokenType(response.getString("token_type"))
                .scope(response.getString("scope"))
                .build();
    }

    @Override
    protected AuthUser getUserInfo(AuthToken authToken) {
        String body = doGetUserInfo(authToken);
        JSONObject result = JSONObject.parseObject(body);
        checkResponse(result);
        return AuthUser.builder()
                .uuid(result.getString("sub"))
                .username(result.getString("preferred_username"))
                .nickname(result.getString("nickname"))
                .avatar(result.getString("picture"))
                .email(result.getString("email"))
                .token(authToken)
                .source(source.toString())
                .build();
    }

    @Override
    protected String doGetUserInfo(AuthToken authToken) {
        HttpRequest httpRequest = HttpUtil.createGet(source.userInfo());
        httpRequest.header("Authorization", "Bearer " + authToken.getAccessToken());
        try (HttpResponse execute = httpRequest.execute()) {
            return execute.body();
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
    }

    @Override
    public String authorize(String state) {
        return UrlBuilder.fromBaseUrl(super.authorize(state))
                .queryParam("scope", StrUtil.join(" ", this.config.getScopes())).build();
    }

    public static void checkResponse(JSONObject object) {
        // oauth/token 验证异常
        if (object.containsKey("error")) {
            throw new AuthException(object.getString("error_description"));
        }
        // user 验证异常
        if (object.containsKey("message")) {
            throw new AuthException(object.getString("message"));
        }
    }

    protected String getAccessToken(String code) {
        HttpRequest httpRequest = HttpUtil.createPost(source.accessToken());

        httpRequest.header("Authorization", getBasic(config.getClientId(), config.getClientSecret()));
        Map<String, Object> form = new HashMap<>(7);
        form.put("code", code);
        form.put("grant_type", "authorization_code");
        form.put("redirect_uri", config.getRedirectUri());
        httpRequest.form(form);
        try (HttpResponse execute = httpRequest.execute()) {
            return execute.body();
        } catch (Exception e) {
            throw Lombok.sneakyThrow(e);
        }
    }

    private String getBasic(String appKey, String appSecret) {
        StringBuilder sb = new StringBuilder();
        String encodeToString = Base64Utils.encode((appKey + ":" + appSecret).getBytes());
        sb.append("Basic").append(" ").append(encodeToString);
        return sb.toString();
    }

}
