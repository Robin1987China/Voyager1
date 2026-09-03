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
 * IPv4 工具，"" {@code io.voyager1.util.Ipv4Util}。
 */
public class Ipv4Util {

    public static final String LOCAL_IP = "127.0.0.1";

    /**
     * IP 与掩码位分隔符
     */
    public static final String IP_MASK_SPLIT_MARK = "/";

    /**
     * IPv4 转 long。
     *
     * @param ipv4 IPv4 地址
     * @return long 值
     */
    public static long ipv4ToLong(String ipv4) {
        if (ipv4 == null || ipv4.isEmpty()) {
            return 0;
        }
        String[] parts = ipv4.split("\\.");
        if (parts.length != 4) {
            return 0;
        }
        return (Long.parseLong(parts[0]) & 0xffL) << 24
            | (Long.parseLong(parts[1]) & 0xffL) << 16
            | (Long.parseLong(parts[2]) & 0xffL) << 8
            | (Long.parseLong(parts[3]) & 0xffL);
    }

    /**
     * long 转 IPv4。
     *
     * @param longIp long 值
     * @return IPv4 地址
     */
    public static String longToIpv4(long longIp) {
        return (longIp >>> 24) + "." + ((longIp >>> 16) & 0xff) + "." + ((longIp >>> 8) & 0xff) + "." + (longIp & 0xff);
    }

    /**
     * 获取网络段起始地址。
     *
     * @param ip      IPv4 地址
     * @param maskBit 掩码位数
     * @return 起始 IPv4 地址
     */
    public static String getBeginIpStr(String ip, int maskBit) {
        return longToIpv4(getBeginIpLong(ip, maskBit));
    }

    /**
     * 获取网络段起始地址的 long 值。
     *
     * @param ip      IPv4 地址
     * @param maskBit 掩码位数
     * @return 起始地址 long 值
     */
    public static long getBeginIpLong(String ip, int maskBit) {
        return ipv4ToLong(ip) & getMaskLong(maskBit);
    }

    /**
     * 获取网络段结束地址（广播地址）。
     *
     * @param ip      IPv4 地址
     * @param maskBit 掩码位数
     * @return 结束 IPv4 地址
     */
    public static String getEndIpStr(String ip, int maskBit) {
        return longToIpv4(getEndIpLong(ip, maskBit));
    }

    /**
     * 获取网络段结束地址的 long 值。
     *
     * @param ip      IPv4 地址
     * @param maskBit 掩码位数
     * @return 结束地址 long 值
     */
    public static long getEndIpLong(String ip, int maskBit) {
        long mask = getMaskLong(maskBit);
        return (ipv4ToLong(ip) & mask) | (0xffffffffL ^ mask);
    }

    private static long getMaskLong(int maskBit) {
        if (maskBit <= 0) {
            return 0L;
        }
        if (maskBit >= 32) {
            return 0xffffffffL;
        }
        return (0xffffffffL << (32 - maskBit)) & 0xffffffffL;
    }
}
