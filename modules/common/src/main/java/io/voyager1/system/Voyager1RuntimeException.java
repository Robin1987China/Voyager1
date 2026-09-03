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

/**
 * Voyager1 运行错误
 */
public class Voyager1RuntimeException extends RuntimeException {

    /**
     * 程序是否需要关闭
     */
    private Integer exitCode;

    public Voyager1RuntimeException(String message) {
        super(message);
    }

    public Voyager1RuntimeException(String message, Integer exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public Voyager1RuntimeException(String message, Throwable throwable) {
        super(String.format("%s %s", message, (throwable.getMessage() == null || throwable.getMessage().isEmpty() ? "" : throwable.getMessage())), throwable);
    }

    public Integer getExitCode() {
        return exitCode;
    }
}
