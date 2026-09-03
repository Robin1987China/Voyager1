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

import io.voyager1.util.StrUtil;
import lombok.Data;
import io.voyager1.common.i18n.I18nMessageUtil;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @since 23/12/25 025
 */
@Data
@ConfigurationProperties("voyager1.web")
public class WebConfig {
    /**
     * 前端接口 超时时间 单位秒
     */
    private int apiTimeout = 20;

    public int getApiTimeout() {
        return Math.max(this.apiTimeout, 5);
    }

    /**
     * 系统名称
     */
    private String name;

    /**
     * 系统副名称（标题） 建议4个汉字以内
     */
    private String subTitle;

    /**
     * 登录页标题
     */
    private String loginTitle;

    /**
     * logo 文件路径
     */
    private String logoFile;

    /**
     * icon 文件路径
     */
    private String iconFile;

    /**
     * 禁用页面引导导航
     */
    private boolean disabledGuide = false;
    /**
     * 禁用登录图形验证码
     */
    private boolean disabledCaptcha = false;

    /**
     * 前端消息弹出位置，可选 topLeft topRight bottomLeft bottomRight
     */
    private String notificationPlacement;
    /**
     * 消息传输加密或者编码
     * NONE
     * <p>
     * BASE64
     */
    private String transportEncryption = "NONE";

    public String getName() {
        return (name == null || name.isEmpty() ? "Voyager1 持续交付平台" : name);
    }

    public String getSubTitle() {
        return (subTitle == null || subTitle.isEmpty() ? "持续交付" : subTitle);
    }

    public String getLoginTitle() {
        return (loginTitle == null || loginTitle.isEmpty() ? "登录VOYAGER1" : loginTitle);
    }
}
