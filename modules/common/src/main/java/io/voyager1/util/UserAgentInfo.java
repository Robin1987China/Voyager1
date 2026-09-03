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

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * User-agent 信息。
 * <p>
 * 兼容  {@code io.voyager1.util.UserAgentInfo} 的 API 表面。
 * </p>
 */
public class UserAgentInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * 未知类型
	 */
	public static final String NameUnknown = "Unknown";

	/**
	 * 信息名称
	 */
	private final String name;
	/**
	 * 信息匹配模式
	 */
	private final Pattern pattern;

	public UserAgentInfo(String name, String regex) {
		this(name, (null == regex) ? null : Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
	}

	public UserAgentInfo(String name, Pattern pattern) {
		this.name = name;
		this.pattern = pattern;
	}

	public String getName() {
		return name;
	}

	public Pattern getPattern() {
		return pattern;
	}

	/**
	 * 指定内容中是否包含匹配此信息的内容
	 *
	 * @param content User-Agent 字符串
	 * @return 是否包含匹配此信息的内容
	 */
	public boolean isMatch(String content) {
		return this.pattern != null && this.pattern.matcher(content).find();
	}

	/**
	 * 是否为 Unknown
	 *
	 * @return 是否为 Unknown
	 */
	public boolean isUnknown() {
		return NameUnknown.equals(this.name);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		UserAgentInfo other = (UserAgentInfo) obj;
		if (name == null) {
			return other.name == null;
		}
		return name.equals(other.name);
	}

	@Override
	public String toString() {
		return this.name;
	}
}
