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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 *  {@code io.voyager1.util.ScriptUtil} 的兼容实现。
 *
 * <p>基于 JDK 的 {@link ScriptEngineManager}，仅覆盖代码库实际使用到的 API。
 * 注意：JDK 15+ 已移除 Nashorn，纯 JDK 环境可能无法获得 JavaScript 引擎。</p>
 */
public final class ScriptUtil {

    private ScriptUtil() {
    }

    public static ScriptEngine getJavaScriptEngine() {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("javascript");
        if (engine == null) {
            engine = manager.getEngineByName("js");
        }
        if (engine == null) {
            engine = manager.getEngineByName("nashorn");
        }
        return engine;
    }

    public static ScriptEngine getScript(String engineName) {
        return new ScriptEngineManager().getEngineByName(engineName);
    }
}
