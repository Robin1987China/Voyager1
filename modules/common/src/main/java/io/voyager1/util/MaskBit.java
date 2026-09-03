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
 * 子网掩码，"" {@code io.voyager1.util.MaskBit}。
 */
public class MaskBit {

    /**
     * 根据掩码位数获取掩码，如 24 返回 {@code 255.255.255.0}。
     *
     * @param maskBit 掩码位数，范围 0 ~ 32
     * @return 点分十进制掩码
     */
    public static String get(int maskBit) {
        return longToIpv4(getMaskLong(maskBit));
    }

    private static long getMaskLong(int maskBit) {
        if (maskBit < 0 || maskBit > 32) {
            throw new IllegalArgumentException("掩码位数必须在 0 ~ 32 之间: " + maskBit);
        }
        if (maskBit == 0) {
            return 0L;
        }
        return (0xffffffffL << (32 - maskBit)) & 0xffffffffL;
    }

    private static String longToIpv4(long longIp) {
        return (longIp >>> 24) + "." + ((longIp >>> 16) & 0xff) + "." + ((longIp >>> 8) & 0xff) + "." + (longIp & 0xff);
    }
}
