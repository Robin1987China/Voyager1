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

package io.voyager1.common;

import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;

import jakarta.servlet.http.HttpServletRequest;
import java.util.function.Function;

/**
 * url 重定向
 * 配置nginx 代理实现
 *
 * @since 2019/11/14
 */
public class UrlRedirectUtil {

//	/**
//	 * 获取 protocol 协议完全跳转
//	 *
//	 * @param request 请求
//	 * @param url     跳转url
//	 * @see jakarta.servlet.http.HttpUtils#getRequestURL
//	 */
//	public static String getRedirect(HttpServletRequest request, String url) {
//		int port = getPort(request);
//		return getRedirect(request, url, port);
//	}

//	/**
//	 * 获取 protocol 协议完全跳转
//	 *
//	 * @param request 请求
//	 * @param url     跳转url
//	 * @see jakarta.servlet.http.HttpUtils#getRequestURL
//	 */
//	public static String getRedirect(HttpServletRequest request, String url, int port) {
//		String proto = JakartaServletUtil.getHeaderIgnoreCase(request, "X-Forwarded-Proto");
//		if (proto == null) {
//			return url;
//		} else {
//			String host = request.getHeader(HttpHeaders.HOST);
//			if ((host == null || host.isEmpty())) {
//				throw new RuntimeException("请配置host header");
//			}
//			if ("http".equals(proto) && port == 0) {
//				port = 80;
//			} else if ("https".equals(proto) && port == 0) {
//				port = 443;
//			}
//			String format = String.format("%s://%s:%s%s", proto, host, port, url);
//			return URLUtil.normalize(format);
//		}
//	}

//	/**
//	 * 获取 protocol 协议完全跳转
//	 *
//	 * @param request  请求
//	 * @param response 响应
//	 * @param url      跳转url
//	 * @throws IOException io
//	 * @see jakarta.servlet.http.HttpUtils#getRequestURL
//	 */
//	public static void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url, int port) throws IOException {
//		String toUrl = getRedirect(request, url, port);
//		response.sendRedirect(toUrl);
//	}

//
//	/**
//	 * 获取 protocol 协议完全跳转
//	 *
//	 * @param request  请求
//	 * @param response 响应
//	 * @param url      跳转url
//	 * @throws IOException io
//	 * @see jakarta.servlet.http.HttpUtils#getRequestURL
//	 */
//	public static void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
//		int port = getPort(request);
//		sendRedirect(request, response, url, port);
//	}

    private static int getPort(HttpServletRequest request) {
        String proxyPort = JakartaServletUtil.getHeaderIgnoreCase(request, "X-Forwarded-Port");
        int port = 0;
        if ((proxyPort != null && !proxyPort.isEmpty())) {
            port = Integer.parseInt(proxyPort);
        }
        return port;
    }

    /**
     * 二级代理路径
     *
     * @param request req
     * @return context-path+nginx配置
     */
    public static String getHeaderProxyPath(HttpServletRequest request, String headName) {
        return getHeaderProxyPath(request, headName, null);
    }

    /**
     * 二级代理路径
     *
     * @param request req
     * @return context-path+nginx配置
     */
    public static String getHeaderProxyPath(HttpServletRequest request, String headName, Function<String, String> function) {
        String proxyPath = JakartaServletUtil.getHeaderIgnoreCase(request, headName);
        //
        if ((proxyPath == null || proxyPath.isEmpty())) {
            return request.getContextPath();
        }
        // 回调处理
        if (function != null) {
            proxyPath = function.apply(proxyPath);
        }
        //
        proxyPath = FileUtil.normalize(request.getContextPath() + "/" + proxyPath);
        if (proxyPath.endsWith("/")) {
            proxyPath = proxyPath.substring(0, proxyPath.length() - 1);
        }
        return proxyPath;
    }
}
