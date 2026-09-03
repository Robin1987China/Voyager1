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
 * HTTP 异常。
 * <p>
 * 兼容  {@code io.voyager1.util.HttpException} 的 API 表面。
 * </p>
 */
public class HttpException extends RuntimeException {

	private static final long serialVersionUID = 8247610319171014183L;

	public HttpException(Throwable e) {
		super(e.getMessage(), e);
	}

	public HttpException(String message) {
		super(message);
	}

	public HttpException(String messageTemplate, Object... params) {
		super(format(messageTemplate, params));
	}

	public HttpException(String message, Throwable throwable) {
		super(message, throwable);
	}

	public HttpException(String message, Throwable throwable, boolean enableSuppression, boolean writableStackTrace) {
		super(message, throwable, enableSuppression, writableStackTrace);
	}

	public HttpException(Throwable throwable, String messageTemplate, Object... params) {
		super(format(messageTemplate, params), throwable);
	}

	/**
	 * 简单实现  风格的 {@code {}} 占位符格式化。
	 *
	 * @param template 模板
	 * @param params   参数
	 * @return 格式化结果
	 */
	private static String format(String template, Object... params) {
		if (template == null) {
			return null;
		}
		if (params == null || params.length == 0) {
			return template;
		}
		StringBuilder builder = new StringBuilder(template.length() + 16);
		int paramIndex = 0;
		int cursor = 0;
		int braceIndex;
		while ((braceIndex = template.indexOf("{}", cursor)) >= 0) {
			builder.append(template, cursor, braceIndex);
			if (paramIndex < params.length) {
				builder.append(params[paramIndex++]);
			} else {
				builder.append("{}");
			}
			cursor = braceIndex + 2;
		}
		builder.append(template, cursor, template.length());
		return builder.toString();
	}
}
