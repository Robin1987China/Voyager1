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
 * Http 头域。
 * <p>
 * 兼容  {@code io.voyager1.util.Header} 的 API 表面。
 * </p>
 */
public enum Header {

	//------------------------------------------------------------- 通用头域
	AUTHORIZATION("Authorization"),
	PROXY_AUTHORIZATION("Proxy-Authorization"),
	DATE("Date"),
	CONNECTION("Connection"),
	MIME_VERSION("MIME-Version"),
	TRAILER("Trailer"),
	TRANSFER_ENCODING("Transfer-Encoding"),
	UPGRADE("Upgrade"),
	VIA("Via"),
	CACHE_CONTROL("Cache-Control"),
	PRAGMA("Pragma"),
	CONTENT_TYPE("Content-Type"),

	//------------------------------------------------------------- 请求头域
	HOST("Host"),
	REFERER("Referer"),
	ORIGIN("Origin"),
	USER_AGENT("User-Agent"),
	ACCEPT("Accept"),
	ACCEPT_LANGUAGE("Accept-Language"),
	ACCEPT_ENCODING("Accept-Encoding"),
	ACCEPT_CHARSET("Accept-Charset"),
	COOKIE("Cookie"),
	CONTENT_LENGTH("Content-Length"),

	//------------------------------------------------------------- 响应头域
	WWW_AUTHENTICATE("WWW-Authenticate"),
	SET_COOKIE("Set-Cookie"),
	CONTENT_ENCODING("Content-Encoding"),
	CONTENT_DISPOSITION("Content-Disposition"),
	ETAG("ETag"),
	LOCATION("Location");

	private final String value;

	Header(String value) {
		this.value = value;
	}

	/**
	 * 获取值
	 *
	 * @return 值
	 */
	public String getValue() {
		return this.value;
	}

	@Override
	public String toString() {
		return getValue();
	}
}
