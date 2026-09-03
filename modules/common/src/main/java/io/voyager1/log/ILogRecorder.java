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

package io.voyager1.log;

/**
 * @since 2023/2/11
 */
public interface ILogRecorder {

    /**
     * 记录单行日志
     *
     * @param info 日志
     * @param vals 变量参数
     * @return 格式化后的字符串
     */
    String info(String info, Object... vals);

    /**
     * 记录单行日志
     *
     * @param info 日志
     * @param vals 变量参数
     * @return 格式化后的字符串
     */
    String system(String info, Object... vals);

    /**
     * 记录单行日志
     *
     * @param info 日志
     * @param vals 变量参数
     * @return 格式化后的字符串
     */
    String systemError(String info, Object... vals);

    /**
     * 记录单行日志
     *
     * @param info 日志
     * @param vals 变量参数
     * @return 格式化后的字符串
     */
    String systemWarning(String info, Object... vals);
}
