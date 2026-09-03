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

import java.util.List;
import java.util.Map;

/**
 * HTTP 响应封装。
 * <p>
 * 兼容  {@code io.voyager1.util.HttpResponse} 的 API 表面。
 * </p>
 */
public class HttpResponse implements AutoCloseable {

	private final int status;
	private String body;
	private final Map<String, List<String>> headers;

	public HttpResponse(int status, String body, Map<String, List<String>> headers) {
		this.status = status;
		this.body = body;
		this.headers = headers;
	}

	/**
	 * 获取状态码
	 *
	 * @return 状态码
	 */
	public int getStatus() {
		return this.status;
	}

	/**
	 * 请求是否成功，判断依据为：状态码范围在 200~299 内。
	 *
	 * @return 是否成功请求
	 */
	public boolean isOk() {
		return this.status >= 200 && this.status < 300;
	}

	/**
	 * 获取响应体字符串
	 *
	 * @return 响应体字符串
	 */
	public String body() {
		return this.body;
	}

	/**
	 * 根据 name 获取头信息
	 *
	 * @param name Header 名
	 * @return Header 值
	 */
	public String header(String name) {
		List<String> values = headerList(name);
		return (values == null || values.isEmpty()) ? null : values.get(0);
	}

	/**
	 * 根据 name 获取头信息
	 *
	 * @param name Header 名
	 * @return Header 值
	 */
	public String header(Header name) {
		return (name == null ? null : header(name.getValue()));
	}

	/**
	 * 根据 name 获取头信息列表
	 *
	 * @param name Header 名
	 * @return Header 值列表
	 */
	public List<String> headerList(String name) {
		if (name == null || this.headers == null) {
			return null;
		}
		for (Map.Entry<String, List<String>> entry : this.headers.entrySet()) {
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(name)) {
				return entry.getValue();
			}
		}
		return null;
	}

	/**
	 * 获取响应头
	 *
	 * @return Headers Map
	 */
	public Map<String, List<String>> headers() {
		return this.headers;
	}

	@Override
	public void close() {
		// 响应体已被完全读取，无底层资源需要释放，仅保留 AutoCloseable 语义
	}
}
