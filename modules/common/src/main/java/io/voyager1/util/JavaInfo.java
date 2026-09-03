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
 * Java 运行时信息，"" {@code io.voyager1.util.JavaInfo}。
 */
public class JavaInfo {

    public String getVersion() {
        return System.getProperty("java.version");
    }

    public String getVendor() {
        return System.getProperty("java.vendor");
    }

    public String getVendorUrl() {
        return System.getProperty("java.vendor.url");
    }

    public String getHome() {
        return System.getProperty("java.home");
    }
}
