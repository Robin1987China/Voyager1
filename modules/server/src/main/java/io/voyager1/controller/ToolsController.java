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

package io.voyager1.controller;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.DateUtil;
import io.voyager1.util.Validator;
import io.voyager1.util.Ipv4Util;
import io.voyager1.util.NetUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.CronPatternUtil;
import io.voyager1.util.JSONArray;
import io.voyager1.util.JSONObject;
import io.voyager1.core.api.ApiResult;
import lombok.Lombok;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 工具类
 *
 * @since 2023/3/10
 */
@RestController
@RequestMapping(value = "/tools")
public class ToolsController {

    @GetMapping(value = "cron", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<Long>> cron(@ValidatorItem String cron, @ValidatorItem int count, String date, boolean isMatchSecond) {
        Date startDate = null;
        Date endDate = null;
        if ((date != null && !date.isEmpty())) {
            List<String> split = io.voyager1.util.ConvertUtil.splitTrim(date, "~");
            try {
                startDate = DateUtil.parse(split.get(0));
                startDate = DateUtil.beginOfDay(startDate);
                endDate = DateUtil.parse(split.get(1));
                endDate = DateUtil.endOfDay(endDate);
            } catch (Exception e) {
                return new ApiResult<>(405, "日期格式错误:" + e.getMessage());
            }
        }
        try {
            List<Date> dateList;
            if (startDate != null) {
                dateList = CronPatternUtil.matchedDates(cron, startDate, endDate, count, isMatchSecond);
            } else {
                dateList = CronPatternUtil.matchedDates(cron, DateTime.now(), count, isMatchSecond);
            }
            return ApiResult.success("", dateList.stream().map(Date::getTime).collect(Collectors.toList()));
        } catch (Exception e) {
            return new ApiResult<>(405, "cron 表达式不正确," + e.getMessage());
        }
    }

    @GetMapping(value = "ip-list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<JSONObject>> ipList() {
        Collection<NetworkInterface> networkInterfaces = NetUtil.getNetworkInterfaces();
        List<JSONObject> collect = networkInterfaces.stream()
            .sorted((o1, o2) -> 0)
            .map(networkInterface -> {
                boolean virtual = networkInterface.isVirtual();
                String name = networkInterface.getName();
                String displayName = networkInterface.getDisplayName();
                JSONObject jsonObject = new JSONObject();
                jsonObject.set("name", name);
                jsonObject.set("displayName", displayName);
                jsonObject.set("virtual", virtual);
                try {
                    jsonObject.set("loopback", networkInterface.isLoopback());
                } catch (SocketException e) {
                    throw Lombok.sneakyThrow(e);
                }
                final Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                JSONArray ips = new JSONArray();
                while (inetAddresses.hasMoreElements()) {
                    final InetAddress inetAddress = inetAddresses.nextElement();
                    if (inetAddress != null && !inetAddress.isLinkLocalAddress()) {
                        String hostAddress = inetAddress.getHostAddress();
                        // 处理 Mac  ip 地址
                        hostAddress = StrUtil.subBefore(hostAddress, "%", true);
                        JSONObject parseIp = parseIp(hostAddress);
                        parseIp.set("ip", hostAddress);
                        ips.add(parseIp);
                    }
                }
                if ((ips == null || ips.isEmpty())) {
                    return null;
                }
                jsonObject.set("ips", ips);
                return jsonObject;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        return ApiResult.success("", collect);
    }

    @GetMapping(value = "net-ping", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> ping(@ValidatorItem String host, @ValidatorItem int timeout) {
        boolean ping = NetUtil.ping(host, (int) TimeUnit.SECONDS.toMillis(Math.max(1, timeout)));
        //
        JSONObject jsonObject = this.parseIp(host);
        jsonObject.set("ping", ping);
        return ApiResult.success("", jsonObject);
    }

    @GetMapping(value = "net-telnet", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> telnet(@ValidatorItem String host, int port, @ValidatorItem int timeout) {
        InetSocketAddress address = NetUtil.createAddress(host, port);
        boolean open = NetUtil.isOpen(address, (int) TimeUnit.SECONDS.toMillis(Math.max(1, timeout)));
        //
        JSONObject jsonObject = this.parseIp(host);
        jsonObject.set("open", open);
        return ApiResult.success("", jsonObject);
    }

    /**
     * 解析 host 的 IP 地址类型
     *
     * @param host 主机地址
     * @return json
     */
    private JSONObject parseIp(String host, String... appendLabels) {
        boolean ipv4 = Validator.isIpv4(host);
        boolean ipv6 = Validator.isIpv6(host);
        JSONObject jsonObject = new JSONObject();
        //
        JSONArray labels = new JSONArray();
        for (String appendLabel : appendLabels) {
            labels.put(appendLabel);
        }
        if (ipv4) {
            labels.put("IPV4");
            //
            String type = detectionType(host);
            if (type != null) {
                labels.add(type);
            }
        }
        if (ipv6) {
            labels.put("IPV6");
        }
        if (!ipv4 && !ipv6) {
            String ipByHost = NetUtil.getIpByHost(host);
            if (!java.util.Objects.equals(ipByHost, host)) {
                jsonObject.set("originalIP", ipByHost);
            }
            labels.put("DOMAIN");
        }

        jsonObject.set("labels", labels);
        return jsonObject;
    }


    private static String detectionType(String ipAddress) {
        if (Ipv4Util.LOCAL_IP.equals(ipAddress)) {
            return "LOCAL";
        }
        long ipNum = Ipv4Util.ipv4ToLong(ipAddress);

        long aBegin = Ipv4Util.ipv4ToLong("10.0.0.0");
        long aEnd = Ipv4Util.ipv4ToLong("10.255.255.255");
        if (isInclude(ipNum, aBegin, aEnd)) {
            return "A";
        }

        long bBegin = Ipv4Util.ipv4ToLong("172.16.0.0");
        long bEnd = Ipv4Util.ipv4ToLong("172.31.255.255");
        if (isInclude(ipNum, bBegin, bEnd)) {
            return "B";
        }

        long cBegin = Ipv4Util.ipv4ToLong("192.168.0.0");
        long cEnd = Ipv4Util.ipv4ToLong("192.168.255.255");
        if (isInclude(ipNum, cBegin, cEnd)) {
            return "C";
        }

        long pBegin = Ipv4Util.ipv4ToLong("20.0.0.0");
        long pEnd = Ipv4Util.ipv4ToLong("223.255.255.255");
        if (isInclude(ipNum, pBegin, pEnd)) {
            return "PUBLIC";
        }
        return null;
    }

    private static boolean isInclude(long userIp, long begin, long end) {
        return (userIp >= begin) && (userIp <= end);
    }
}
