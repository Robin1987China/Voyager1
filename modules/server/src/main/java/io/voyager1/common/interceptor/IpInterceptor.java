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

package io.voyager1.common.interceptor;

import io.voyager1.util.Validator;
import io.voyager1.util.Ipv4Util;
import io.voyager1.util.NetUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.data.SystemIpConfigModel;
import io.voyager1.service.system.SystemParametersServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.method.HandlerMethod;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * ip 访问限制拦截器
 *
 * @since 2021/4/18
 */
@Configuration
@Slf4j
public class IpInterceptor implements HandlerMethodInterceptor {

    private static final int IP_ACCESS_CODE = 999;

    @Resource
    private SystemParametersServer systemParametersServer;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {
        String clientIp = JakartaServletUtil.getClientIP(request);
        if (java.util.Objects.equals(NetUtil.LOCAL_IP, clientIp) || !Validator.isIpv4(clientIp)) {
            // 本地 或者 非 ipv4 直接放开
            return true;
        }
        SystemIpConfigModel config = systemParametersServer.getConfig(SystemIpConfigModel.ID, SystemIpConfigModel.class);
        if (config == null) {
            return true;
        }
        // 判断不允许访问
        String prohibited = config.getProhibited();
        try {
            if ((prohibited != null && !prohibited.isEmpty()) && this.checkIp(prohibited, clientIp, false)) {
                JakartaServletUtil.write(response, ApiResult.getString(IP_ACCESS_CODE, "Prohibition of access"), MediaType.APPLICATION_JSON_VALUE);
                return false;
            }
            String allowed = config.getAllowed();
            if ((allowed == null || allowed.isEmpty()) || this.checkIp(allowed, clientIp, true)) {
                return true;
            }
        } catch (Exception e) {
            log.warn("IP授权拦截异常,请检查配置是否正确", e);
            return true;
        }
        JakartaServletUtil.write(response, ApiResult.getString(IP_ACCESS_CODE, "Prohibition of access"), MediaType.APPLICATION_JSON_VALUE);
        return false;
    }


    /**
     * 检查ip 地址是否可以访问
     *
     * @param value    配置的值
     * @param ip       被检查的 ip 地址
     * @param checkAll 是否检查开放所有、避免禁止所有 ip 访问
     * @return true 命中检查项
     */
    private boolean checkIp(String value, String ip, boolean checkAll) {
        long ipNum = NetUtil.ipv4ToLong(ip);
        String[] split = value.split(java.util.regex.Pattern.quote("\n"));
        boolean check;
        for (String itemIp : split) {
            itemIp = itemIp.trim();
            if (itemIp.startsWith("#")) {
                continue;
            }
            if (checkAll && java.util.Objects.equals(itemIp, "0.0.0.0")) {
                // 开放所有
                return true;
            }
            if ((itemIp != null && itemIp.contains(Ipv4Util.IP_MASK_SPLIT_MARK))) {
                // ip段
                String[] itemIps = itemIp.split(java.util.regex.Pattern.quote(Ipv4Util.IP_MASK_SPLIT_MARK));
                int count1 = StrUtil.count(itemIps[0], ".");
                int count2 = StrUtil.count(itemIps[1], ".");
                if (count1 == 3 && count2 == 3) {
                    //192.168.1.0/192.168.1.200
                    long aBegin = NetUtil.ipv4ToLong(itemIps[0]);
                    long aEnd = NetUtil.ipv4ToLong(itemIps[1]);
                    check = (ipNum >= aBegin) && (ipNum <= aEnd);
                } else if (count1 == 3 && count2 == 0) {
                    //192.168.1.0/24
                    String startIp = Ipv4Util.getBeginIpStr(itemIps[0], Integer.parseInt(itemIps[1]));
                    String endIp = Ipv4Util.getEndIpStr(itemIps[0], Integer.parseInt(itemIps[1]));
                    long aBegin = NetUtil.ipv4ToLong(startIp);
                    long aEnd = NetUtil.ipv4ToLong(endIp);
                    check = (ipNum >= aBegin) && (ipNum <= aEnd);
                } else {
                    check = false;
                }

            } else {
                check = java.util.Objects.equals(itemIp, ip);
            }
            if (check) {
                return true;
            }
        }
        return false;
    }
}
