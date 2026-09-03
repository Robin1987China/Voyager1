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

package io.voyager1.email;

import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.MailAccount;
import io.voyager1.util.MailException;
import io.voyager1.util.MailUtil;
import io.voyager1.plugin.PluginConfig;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.plugin.IDefaultPlugin;

import javax.mail.Session;
import javax.mail.Transport;
import java.util.List;
import java.util.Map;

/**
 * @since 2021/12/22
 */
@PluginConfig(name = "email")
@Slf4j
public class DefaultEmailPluginImpl implements IDefaultPlugin {

    @Override
    public Object execute(Object main, Map<String, Object> parameter) throws Exception {
        if (main instanceof JSONObject) {
            MailAccount mailAccount = getAccount(main);
            //
            String toEmail = (String) parameter.get("toEmail");
            String title = (String) parameter.get("title");
            String context = (String) parameter.get("context");
            List<String> list = java.util.Arrays.asList(toEmail.split(","));
            try {
                return MailUtil.send(mailAccount, list, title, context, false);
            } catch (MailException mailException) {
                Exception cause = (Exception) mailException.getCause();
                if (cause != null) {
                    throw cause;
                }
                throw mailException;
            }
        } else if (main instanceof String && java.util.Objects.equals("checkInfo", main.toString())) {
            try {
                Object data = parameter.get("data");
                MailAccount account = this.getAccount(data);
                Session session = MailUtil.getSession(account, false);
                try (Transport transport = session.getTransport("smtp")) {
                    transport.connect();
                }
                return true;
            } catch (Exception e) {
                log.warn("检查邮箱信息错误：{}", e.getMessage());
                return false;
            }
        }
        throw new IllegalArgumentException("不支持的类型：" + main);
    }

    /**
     * 创建邮件对象
     *
     * @param main 传人参数
     * @return MailAccount
     */
    private MailAccount getAccount(Object main) {
        if (!(main instanceof JSONObject)) {
            throw new IllegalArgumentException("插件端使用参数不正确");
        }
        JSONObject data = (JSONObject) main;
        MailAccount mailAccount = new MailAccount();
        String user = data.getString("user");
        String pass = data.getString("pass");
        String from = data.getString("from");
        Integer port = data.getInteger("port");
        String host = data.getString("host");
        mailAccount.setUser(user);
        mailAccount.setPass(pass);
        mailAccount.setFrom(from);
        mailAccount.setPort(port);
        mailAccount.setHost(host);
        //
        Integer timeout = data.getInteger("timeout");
        timeout = (timeout != null ? timeout : 10);
        timeout = Math.max(3, timeout);
        mailAccount.setTimeout(timeout * 1000);
        mailAccount.setConnectionTimeout(timeout * 1000);
        boolean sslEnable = data.getBooleanValue("sslEnable");
        //
        mailAccount.setSslEnable(sslEnable);
        //Integer socketFactoryPort = data.getInteger("socketFactoryPort");
//			if (socketFactoryPort != null) {
        if (sslEnable) {
            mailAccount.setSocketFactoryPort(port);
        }
//			}
        mailAccount.setAuth(true);
        return mailAccount;
    }
}
