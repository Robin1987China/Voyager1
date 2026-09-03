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

import java.io.Serializable;

/**
 * 操作系统信息，"" 的 OsInfo。
 */
public class OsInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String osName;
    private final String osArch;
    private final String osVersion;

    public OsInfo() {
        this.osName = System.getProperty("os.name", "");
        this.osArch = System.getProperty("os.arch", "");
        this.osVersion = System.getProperty("os.version", "");
    }

    public String getName() {
        return osName;
    }

    public String getArch() {
        return osArch;
    }

    public String getVersion() {
        return osVersion;
    }

    public String getLineSeparator() {
        return System.lineSeparator();
    }

    public boolean isWindows() {
        return contains("windows");
    }

    public boolean isLinux() {
        return contains("linux");
    }

    public boolean isMac() {
        return contains("mac") || contains("darwin");
    }

    public boolean isMacOsX() {
        return contains("mac os x");
    }

    public boolean isIrix() {
        return contains("irix");
    }

    public boolean isHpUx() {
        return contains("hp-ux") || contains("hpux");
    }

    public boolean isAix() {
        return contains("aix");
    }

    public boolean isSolaris() {
        return contains("sunos") || contains("solaris");
    }

    private boolean contains(String key) {
        return osName != null && osName.toLowerCase().contains(key);
    }

    @Override
    public String toString() {
        return osName + " " + osArch + " " + osVersion;
    }
}
