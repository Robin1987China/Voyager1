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

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.socket.ServiceFileTailWatcher;
import io.voyager1.system.BaseSystemConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.Charset;

/**
 * @since 23/12/25 025
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ConfigurationProperties("voyager1.system")
public class SystemConfig extends BaseSystemConfig {

    @Override
    public void setLogCharset(Charset logCharset) {
        super.setLogCharset(logCharset);
        ServiceFileTailWatcher.setCharset(getLogCharset());
    }
}
