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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 全局头部信息。
 * <p>
 * 所有 Http 请求将共用此全局头部信息，除非在 {@link HttpRequest} 中自定义头部信息覆盖之。
 * 兼容  {@code io.voyager1.util.GlobalHeaders} 的 API 表面。
 * </p>
 */
public enum GlobalHeaders {
	INSTANCE;

	/**
	 * 存储头信息
	 */
	private final Map<String, List<String>> headers = new HashMap<>();

	GlobalHeaders() {
		putDefault();
	}

	/**
	 * 加入默认的头部信息
	 *
	 * @return this
	 */
	public GlobalHeaders putDefault() {
		header(Header.ACCEPT, "text/html,application/json,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8", true);
		header(Header.ACCEPT_ENCODING, "gzip, deflate", true);
		header(Header.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/75.0.3770.142 Safari/537.36", true);
		return this;
	}

	/**
	 * 根据 name 获取头信息
	 *
	 * @param name Header 名
	 * @return Header 值
	 */
	public String header(String name) {
		if (name == null) {
			return null;
		}
		List<String> values = this.headers.get(name.trim());
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
	 * 设置一个 header
	 *
	 * @param name       Header 名
	 * @param value      Header 值
	 * @param isOverride 是否覆盖已有值
	 * @return this
	 */
	public synchronized GlobalHeaders header(String name, String value, boolean isOverride) {
		if (name != null && value != null) {
			String key = name.trim();
			List<String> values = this.headers.get(key);
			if (isOverride || values == null || values.isEmpty()) {
				List<String> valueList = new ArrayList<>(1);
				valueList.add(value);
				this.headers.put(key, valueList);
			} else {
				values.add(value.trim());
			}
		}
		return this;
	}

	/**
	 * 设置一个 header
	 *
	 * @param name       Header 名
	 * @param value      Header 值
	 * @param isOverride 是否覆盖已有值
	 * @return this
	 */
	public GlobalHeaders header(Header name, String value, boolean isOverride) {
		return (name == null ? this : header(name.getValue(), value, isOverride));
	}

	/**
	 * 获取 headers
	 *
	 * @return Headers Map
	 */
	public Map<String, List<String>> headers() {
		return Collections.unmodifiableMap(this.headers);
	}
}
