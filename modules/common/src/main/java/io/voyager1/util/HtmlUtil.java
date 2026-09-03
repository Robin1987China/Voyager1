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

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * HTML 工具类。
 * <p>
 * 兼容  {@code io.voyager1.util.HtmlUtil} 的 API 表面。
 * </p>
 */
public class HtmlUtil {

	public static final String RE_HTML_MARK = "(<[^<]*?>)|(<[\\s]*?/[^<]*?>)|(<[^<]*?/[\\s]*?>)";

	/**
	 * 转义文本中的 HTML 字符为安全的字符
	 *
	 * @param text 被转义的文本
	 * @return 转义后的文本
	 */
	public static String escape(String text) {
		return (text == null ? null : StringEscapeUtils.escapeHtml4(text));
	}

	/**
	 * 还原被转义的 HTML 特殊字符
	 *
	 * @param htmlStr 包含转义符的 HTML 内容
	 * @return 转换后的字符串
	 */
	public static String unescape(String htmlStr) {
		if (htmlStr == null || htmlStr.isEmpty()) {
			return htmlStr;
		}
		return StringEscapeUtils.unescapeHtml4(htmlStr);
	}

	/**
	 * 清除所有 HTML 标签，但是不删除标签内的内容
	 *
	 * @param content 文本
	 * @return 清除标签后的文本
	 */
	public static String cleanHtmlTag(String content) {
		return (content == null ? null : content.replaceAll(RE_HTML_MARK, ""));
	}
}
