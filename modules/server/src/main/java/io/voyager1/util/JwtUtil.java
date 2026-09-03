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

package io.voyager1.util;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.DateField;
import io.voyager1.util.DateTime;
import io.voyager1.util.DateUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.util.JWT;
import io.voyager1.util.JWTHeader;
import io.voyager1.util.JWTValidator;
import io.voyager1.util.JWTSignerUtil;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.UserConfig;
import io.voyager1.model.user.UserModel;
import io.voyager1.system.ServerConfig;

/**
 * jwt 工具类
 *
 * @since 2020/7/25
 */
@Slf4j
public class JwtUtil {

    /**
     * 加密算法
     */
    private static final String ALGORITHM = "HS256";
    /**
     * token的的加密key
     */
    private static byte[] KEY;
    public static final String KEY_USER_ID = "userId";

    public static JWT parseBody(String token) {
        if ((token == null || token.isEmpty())) {
            return null;
        }
        ServerConfig serverConfig = SpringContextHolder.getBean(ServerConfig.class);
        UserConfig user = serverConfig.getUser();
        JWT jwt = JWT.of(token);
        if (jwt.verify(JWTSignerUtil.hs256(user.getTokenJwtKeyByte()))) {
            return jwt;
        }
        return null;
    }


    /**
     * 读取token 信息 过期也能读取
     *
     * @param token token
     * @return claims
     */
    public static JWT readBody(String token) {
        try {
            return parseBody(token);
        } catch (Exception e) {
            log.warn("token 解析失败：" + token, e);
            return null;
        }
    }

    /**
     * 读取用户id
     *
     * @param jwt jwt
     * @return 用户id
     */
    public static String readUserId(JWT jwt) {
        return ConvertUtil.toStr(jwt.getPayload(KEY_USER_ID));
    }

    /**
     * 获取jwt的唯一身份标识
     *
     * @param jwt jwt
     * @return id
     */
    public static String getId(JWT jwt) {
        if (null == jwt) {
            return null;
        }
        return ConvertUtil.toStr(jwt.getPayload(JWT.JWT_ID));
    }

    /**
     * 判断是否过期
     *
     * @param jwt    claims
     * @param leeway 容忍空间，单位：秒。当不能晚于当前时间时，向后容忍；不能早于向前容忍。
     * @return 是否过期
     */
    public static boolean expired(JWT jwt, long leeway) {
        if (jwt == null) {
            return true;
        }
        try {
            JWTValidator of = JWTValidator.of(jwt);
            of.validateDate(DateUtil.date(), leeway);
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    /**
     * 生成token
     *
     * @param userModel 用户
     * @return token
     */
    public static String builder(UserModel userModel, String jwtId) {
        ServerConfig serverConfig = SpringContextHolder.getBean(ServerConfig.class);
        UserConfig user = serverConfig.getUser();
        //
        DateTime now = DateTime.now();
        JWT jwt = JWT.create();
        jwt.setHeader(JWTHeader.ALGORITHM, ALGORITHM);
        jwt.setPayload(KEY_USER_ID, userModel.getId())
            .setJWTId(jwtId)
            .setIssuer("Voyager1")
            .setIssuedAt(now)
            .setExpiresAt(now.offsetNew(DateField.HOUR, user.getTokenExpired()));
        return jwt.sign(JWTSignerUtil.hs256(user.getTokenJwtKeyByte()));
    }


}
