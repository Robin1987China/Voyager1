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

package io.voyager1.configuration;

import io.voyager1.util.FileUtil;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.RandomUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.core.auth.AgentCredential;
import io.voyager1.core.auth.AgentTokenVerifier;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.system.AgentAutoUser;
import io.voyager1.system.Voyager1RuntimeException;
import io.voyager1.util.JsonFileUtil;
import io.voyager1.util.JvmUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * agent 端授权账号信息
 *
 * @since 2019/4/17
 */
@Slf4j
@Data
@ConfigurationProperties("voyager1.authorize")
public class AgentAuthorize {
    /**
     * 账号
     */
    private String agentName;
    /**
     * 密码
     */
    private String agentPwd;
    /**
     * 授权加密字符串
     */
    private String authorize;

    /**
     * 新令牌校验器（HMAC-SHA256 + nonce 防重放）
     */
    private final AgentTokenVerifier tokenVerifier = new AgentTokenVerifier();

    public void setAuthorize(String authorize) {
        // 不能外部 set
    }

    public String getAuthorize() {
        return null;
    }


    /**
     * 判断授权是否正确（旧 sha1 方案，过渡期回退）
     *
     * @param authorize 授权
     * @return true 正确
     * @deprecated 由 {@link #checkSignedToken(String)} 取代
     */
    @Deprecated
    public boolean checkAuthorize(String authorize) {
        return java.util.Objects.equals(authorize, this.authorize);
    }

    /**
     * 校验新签名令牌（优先于旧 sha1）。
     *
     * @param token 令牌（格式 agentId.iat.nonce.exp.sig）
     * @return true 合法
     */
    public boolean checkSignedToken(String token) {
        if (this.authorize == null || this.authorize.isEmpty()) {
            return false;
        }
        AgentCredential credential = AgentCredential.fromLegacyAuthorize(this.agentName, this.authorize);
        return tokenVerifier.verify(credential, token, System.currentTimeMillis() / 1000);
    }

    /**
     * 检查是否配置密码
     */
    private void checkPwd(Voyager1Application configBean) {
        File path = FileUtil.file(configBean.getDataPath(), Const.AUTHORIZE);
        if ((agentPwd != null && !agentPwd.isEmpty())) {
            // 有指定密码 清除旧密码信息
            FileUtil.del(path);
            log.info("Authorization information has been customized,account：{}", this.agentName);
            return;
        }
        if (FileUtil.exist(path)) {
            // 读取旧密码
            String json = FileUtil.readString(path, StandardCharsets.UTF_8);
            AgentAutoUser autoUser = JSONObject.parseObject(json, AgentAutoUser.class);
            if (!java.util.Objects.equals(autoUser.getAgentName(), this.agentName)) {
                throw new Voyager1RuntimeException("The existing login name is inconsistent with the configured login name");
            }
            String oldAgentPwd = autoUser.getAgentPwd();
            if ((oldAgentPwd != null && !oldAgentPwd.isEmpty())) {
                this.agentPwd = oldAgentPwd;
                log.info("Already authorized account:{} password:{} Authorization information storage location：{}", this.agentName, this.agentPwd, FileUtil.getAbsolutePath(path));
                return;
            }
        }
        this.agentPwd = RandomUtil.randomString(10);
        AgentAutoUser autoUser = new AgentAutoUser();
        autoUser.setAgentName(this.agentName);
        autoUser.setAgentPwd(this.agentPwd);
        // 写入文件中
        JsonFileUtil.saveJson(path, autoUser.toJson());
        log.info("Automatically generate authorized account:{}  password:{}  Authorization information storage location：{}", this.agentName, this.agentPwd, FileUtil.getAbsolutePath(path));
    }

    public void init(Voyager1Application configBean) {
        if ((this.agentName == null || this.agentName.isEmpty())) {
            throw new Voyager1RuntimeException("The agent login name cannot be empty");
        }
        if ((this.authorize == null || this.authorize.isEmpty())) {
            this.checkPwd(configBean);
            // 生成密码授权字符串
            this.authorize = DigestUtil.sha1(this.agentName + "@" + this.agentPwd);
        } else {
            log.warn("authorized 不能重复加载");
        }
        //
        JvmUtil.checkJpsNormal();
    }
}
