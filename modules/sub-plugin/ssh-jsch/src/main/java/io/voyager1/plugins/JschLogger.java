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

package io.voyager1.plugins;

import io.voyager1.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;

/**
 */
@Slf4j
public class JschLogger implements com.jcraft.jsch.Logger {

    public static final JschLogger LOGGER = new JschLogger();

    @Override
    public boolean isEnabled(int level) {
        switch (level) {
            case DEBUG:
                return log.isDebugEnabled();
            case INFO:
                return log.isInfoEnabled();
            case WARN:
                return log.isWarnEnabled();
            case ERROR:
            case FATAL:
                return log.isErrorEnabled();
            default:
                log.warn("未知的 jsch 日志级别：{}", level);
                return false;
        }
    }

    @Override
    public void log(int level, String message) {
        switch (level) {
            case DEBUG:
                // info 日志太多 记录维 debug
            case INFO:
                log.debug(message);
                break;
            case WARN:
                if (StrUtil.isWrap(message, "Permanently added", "to the list of known hosts.")) {
                    // 避免过多日志
                    log.debug(message);
                } else {
                    log.warn(message);
                }
                break;
            case ERROR:
            case FATAL:
                log.error(message);
                break;
            default:
                log.warn("未知的 jsch 日志级别：{} {}", level, message);
        }
    }
}
