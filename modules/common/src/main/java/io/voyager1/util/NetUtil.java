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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 网络工具，"" {@code io.voyager1.util.NetUtil}。
 */
public class NetUtil {

    public static final String LOCAL = "127.0.0.1";

    public static final String LOCAL_IP = "127.0.0.1";

    public static boolean isValidPort(int port) {
        return port >= 0 && port <= 65535;
    }

    public static boolean isOpen(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isOpen(InetSocketAddress address, int timeout) {
        if (address == null) {
            return false;
        }
        try (Socket socket = new Socket()) {
            socket.connect(address, timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean ping(String host) {
        return ping(host, 3000);
    }

    public static boolean ping(String host, int timeout) {
        if (host == null || host.isEmpty()) {
            return false;
        }
        try {
            return InetAddress.getByName(host).isReachable(timeout);
        } catch (Exception e) {
            return false;
        }
    }

    public static long ipv4ToLong(String ipv4) {
        if (ipv4 == null) {
            return 0;
        }
        String[] parts = ipv4.split("\\.");
        if (parts.length != 4) {
            return 0;
        }
        return (Long.parseLong(parts[0]) << 24)
            + (Long.parseLong(parts[1]) << 16)
            + (Long.parseLong(parts[2]) << 8)
            + Long.parseLong(parts[3]);
    }

    public static String getIpByHost(String host) {
        if (host == null || host.isEmpty()) {
            return null;
        }
        try {
            return InetAddress.getByName(host).getHostAddress();
        } catch (Exception e) {
            return host;
        }
    }

    public static String getLocalHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return null;
        }
    }

    public static Collection<NetworkInterface> getNetworkInterfaces() {
        List<NetworkInterface> result = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                result.add(interfaces.nextElement());
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    public static void netCat(String host, int port, byte[] data) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            if (data != null && data.length > 0) {
                socket.getOutputStream().write(data);
                socket.getOutputStream().flush();
            }
        } catch (Exception e) {
            throw new RuntimeException("网络不可达: " + host + ":" + port, e);
        }
    }

    public static InetSocketAddress createAddress(String host, int port) {
        return new InetSocketAddress(host, port);
    }

    public static java.util.LinkedHashSet<InetAddress> localAddressList(Predicate<NetworkInterface> networkFilter, Predicate<InetAddress> addressFilter) {
        java.util.LinkedHashSet<InetAddress> result = new LinkedHashSet<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (networkFilter != null && !networkFilter.test(ni)) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addressFilter == null || addressFilter.test(addr)) {
                        result.add(addr);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    public static java.util.LinkedHashSet<InetAddress> localAddressList() {
        return localAddressList(null, null);
    }

    public static String getLocalhostStr() {
        return ipv4();
    }

    public static String ipv4() {
        Set<InetAddress> list = localAddressList(null, addr -> !addr.isLoopbackAddress() && addr.getAddress().length == 4);
        return list.isEmpty() ? LOCAL : list.iterator().next().getHostAddress();
    }
}
