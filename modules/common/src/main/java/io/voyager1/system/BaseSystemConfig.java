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

package io.voyager1.system;

import io.voyager1.common.RemoteVersion;
import io.voyager1.common.Voyager1ApplicationEvent;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.util.CronUtil;
import lombok.Data;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


@Data
public abstract class BaseSystemConfig {

    /**
     * 是否开启秒级匹配
     */
    private boolean timerMatchSecond = false;
    /**
     * 允许降级
     */
    private boolean allowedDowngrade = false;
    /**
     * 旧包文件保留个数
     */
    private int oldJarsCount = 2;
    /**
     * 远程更新地址
     */
    private String remoteVersionUrl;
    /**
     * 系统日志编码格式
     */
    private Charset logCharset;
    /**
     * 控制台编码格式
     */
    private Charset consoleCharset;
    /**
     * 执行系统主要命名是否填充 sudo(sudo xxx)
     * 使用前提需要配置 sudo 免密
     */
    private boolean commandUseSudo = false;
    /**
     * 系统语言：zh-CN、en-US
     */
    private String lang;

    public void setTimerMatchSecond(boolean timerMatchSecond) {
        this.timerMatchSecond = timerMatchSecond;
        // 开启秒级
        CronUtil.setMatchSecond(timerMatchSecond);
    }

    public void setOldJarsCount(int oldJarsCount) {
        this.oldJarsCount = oldJarsCount;
        Voyager1ApplicationEvent.setOldJarsCount(oldJarsCount);
    }

    public void setRemoteVersionUrl(String remoteVersionUrl) {
        this.remoteVersionUrl = remoteVersionUrl;
        RemoteVersion.setRemoteVersionUrl(remoteVersionUrl);
    }

    public void setLang(String lang) {
        this.lang = lang;
        System.setProperty("VOYAGER1_LANG", lang);
    }

    /**
     * 默认 utf-8
     *
     * @return 日志文件编码格式
     */
    public Charset getLogCharset() {
        return (logCharset != null ? logCharset : StandardCharsets.UTF_8);
    }


    public void setConsoleCharset(Charset consoleCharset) {
        this.consoleCharset = consoleCharset;
        ExtConfigBean.setConsoleLogCharset(consoleCharset);
    }

    public void setAllowedDowngrade(boolean allowedDowngrade) {
        this.allowedDowngrade = allowedDowngrade;
        Voyager1Manifest.setAllowedDowngrade(allowedDowngrade);
    }

    public void setCommandUseSudo(boolean commandUseSudo) {
        this.commandUseSudo = commandUseSudo;
        System.setProperty("VOYAGER1_COMMAND_USE_SUDO", String.valueOf(commandUseSudo));
    }
}
