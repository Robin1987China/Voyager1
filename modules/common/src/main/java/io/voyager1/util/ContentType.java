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

import java.nio.charset.Charset;

/**
 * 常用 Content-Type 类型枚举。
 * <p>
 * 兼容 {@code io.voyager1.util.ContentType} 的 API 表面。
 * </p>
 */
public enum ContentType {

	/**
	 * 标准表单编码
	 */
	FORM_URLENCODED("application/x-www-form-urlencoded"),
	/**
	 * 文件上传编码
	 */
	MULTIPART("multipart/form-data"),
	/**
	 * Rest 请求 JSON 编码
	 */
	JSON("application/json"),
	/**
	 * Rest 请求 XML 编码
	 */
	XML("application/xml"),
	/**
	 * text/plain 编码
	 */
	TEXT_PLAIN("text/plain"),
	/**
	 * Rest 请求 text/xml 编码
	 */
	TEXT_XML("text/xml"),
	/**
	 * text/html 编码
	 */
	TEXT_HTML("text/html"),
	/**
	 * application/octet-stream 编码
	 */
	OCTET_STREAM("application/octet-stream"),
	/**
	 * text/event-stream 编码
	 */
	EVENT_STREAM("text/event-stream");

	private final String value;

	ContentType(String value) {
		this.value = value;
	}

	/**
	 * 获取 value 值
	 *
	 * @return value 值
	 */
	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return getValue();
	}

	/**
	 * 输出 Content-Type 字符串，附带编码信息
	 *
	 * @param charset 编码
	 * @return Content-Type 字符串
	 */
	public String toString(Charset charset) {
		return build(this.value, charset);
	}

	/**
	 * 是否为默认 Content-Type，默认包括 {@code null} 和 application/x-www-form-urlencoded
	 *
	 * @param contentType 内容类型
	 * @return 是否为默认 Content-Type
	 */
	public static boolean isDefault(String contentType) {
		return null == contentType || isFormUrlEncode(contentType);
	}

	/**
	 * 是否为 application/x-www-form-urlencoded
	 *
	 * @param contentType 内容类型
	 * @return 是否为 application/x-www-form-urlencoded
	 */
	public static boolean isFormUrlEncode(String contentType) {
		if (contentType == null) {
			return false;
		}
		return contentType.trim().regionMatches(true, 0, FORM_URLENCODED.value, 0, FORM_URLENCODED.value.length());
	}

	/**
	 * 从请求参数的 body 中判断请求的 Content-Type 类型，支持 application/json 与 application/xml
	 *
	 * @param body 请求参数体
	 * @return Content-Type 类型，如果无法判断返回 null
	 */
	public static ContentType get(String body) {
		if (body == null || body.trim().isEmpty()) {
			return null;
		}
		char firstChar = body.trim().charAt(0);
		switch (firstChar) {
			case '{':
			case '[':
				return JSON;
			case '<':
				return XML;
			default:
				return null;
		}
	}

	/**
	 * 输出 Content-Type 字符串，附带编码信息
	 *
	 * @param contentType Content-Type 类型
	 * @param charset     编码
	 * @return Content-Type 字符串
	 */
	public static String build(String contentType, Charset charset) {
		return contentType + ";charset=" + charset.name();
	}

	/**
	 * 输出 Content-Type 字符串，附带编码信息
	 *
	 * @param contentType Content-Type 枚举类型
	 * @param charset     编码
	 * @return Content-Type 字符串
	 */
	public static String build(ContentType contentType, Charset charset) {
		return build(contentType.getValue(), charset);
	}
}
