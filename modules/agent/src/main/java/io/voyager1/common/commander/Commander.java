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

package io.voyager1.common.commander;

import io.voyager1.util.OsInfo;
import io.voyager1.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.system.Voyager1RuntimeException;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @since 23/12/29 029
 */
@Configuration
@Slf4j
public class Commander {

    public Commander() {
        OsInfo osInfo = SystemUtil.getOsInfo();
        if (osInfo.isLinux()) {
            // Linux系统
            log.debug("当前系统为：linux");
        } else if (osInfo.isWindows()) {
            // Windows系统
            log.debug("当前系统为：windows");
        } else if (osInfo.isMac()) {
            log.debug("当前系统为：mac");
        } else {
            throw new Voyager1RuntimeException("不支持的：" + osInfo.getName());
        }
    }

    public static class Windows implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return SystemUtil.getOsInfo().isWindows();
        }
    }

    public static class Linux implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return SystemUtil.getOsInfo().isLinux();
        }
    }

    public static class Mac implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return SystemUtil.getOsInfo().isMac();
        }
    }
}
