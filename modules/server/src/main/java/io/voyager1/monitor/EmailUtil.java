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

package io.voyager1.monitor;

import io.voyager1.util.StrUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.data.MailAccountModel;
import io.voyager1.model.data.MonitorModel;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.system.SystemParametersServer;

import java.util.HashMap;
import java.util.Map;

/**
 * 邮件工具
 *
 */
@Slf4j
public class EmailUtil implements INotify {

    private static SystemParametersServer systemParametersServer;
    private static MailAccountModel config;

    @Override
    public void send(MonitorModel.Notify notify, String title, String context) throws Exception {
        String value = notify.getValue();
        EmailUtil.send(value, title, context);
    }

    private static void init() {
        if (systemParametersServer == null) {
            systemParametersServer = SpringContextHolder.getBean(SystemParametersServer.class);
        }
    }

    /**
     * 加载配置信息
     */
    public static void refreshConfig() {
        if (config == null) {
            init();
        }
        config = systemParametersServer.getConfig(MailAccountModel.ID, MailAccountModel.class);
    }


    /**
     * 发送邮箱
     *
     * @param email   收件人
     * @param title   标题
     * @param context 内容
     */
    public static void send(String email, String title, String context) throws Exception {
        if (config == null) {
            // 没有数据才加载
            refreshConfig();
        }
        if (config == null || (config.getHost() == null || config.getHost().isEmpty())) {
            log.error("未配置邮箱服务不能发送邮件：{} {}", email, title);
            return;
        }
        //
        Map<String, Object> mailMap = new HashMap<>(10);
        mailMap.put("toEmail", email);
        mailMap.put("title", title);
        mailMap.put("context", context);
        //
        IPlugin plugin = PluginFactory.getPlugin("email");
        plugin.execute(JSON.toJSON(config), mailMap);
    }
}
