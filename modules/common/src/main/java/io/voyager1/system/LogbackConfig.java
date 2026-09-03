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

import ch.qos.logback.core.PropertyDefinerBase;
import io.voyager1.util.*;
import org.springframework.util.Assert;

import java.io.File;


public abstract class LogbackConfig extends PropertyDefinerBase {
    static String VOYAGER1_LOG = "VOYAGER1_LOG";

    public static String getPath() {
        String voyager1Log = SystemUtil.get(VOYAGER1_LOG);
        Assert.hasText(voyager1Log, "没有配置 VOYAGER1_LOG");
        return voyager1Log;
    }

    @Override
    public String getPropertyValue() {
        String voyager1Log = SystemUtil.get(VOYAGER1_LOG);
        return Opt.ofBlankAble(voyager1Log).orElseGet(() -> {
            String locationPath = ClassUtil.getLocationPath(this.getClass());
            // 兼容 spring-boot fat jar 路径（形如 xxx.jar!/BOOT-INF/classes/），剥离 jar! 段
            locationPath = StrUtil.subBefore(locationPath, "!/", false);
            File file = FileUtil.file(FileUtil.getParent(locationPath, 2), "logs");
            String path = FileUtil.getAbsolutePath(file);
            System.setProperty(VOYAGER1_LOG, path);
            return path;
        });
    }
}
