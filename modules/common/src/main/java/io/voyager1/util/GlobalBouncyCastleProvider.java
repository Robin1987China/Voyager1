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

/**
 *  {@code io.voyager1.util.GlobalBouncyCastleProvider} 的兼容实现。
 *
 * <p>本项目统一使用 JDK 自带的安全提供者，不依赖 BouncyCastle，
 * 因此此处仅保留开关语义（no-op）。</p>
 */
public final class GlobalBouncyCastleProvider {

    private static volatile boolean useBouncyCastle = false;

    private GlobalBouncyCastleProvider() {
    }

    public static void setUseBouncyCastle(boolean useBouncyCastle) {
        GlobalBouncyCastleProvider.useBouncyCastle = useBouncyCastle;
    }

    public static boolean isUseBouncyCastle() {
        return useBouncyCastle;
    }
}
