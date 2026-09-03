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
 * HTTP 状态码常量。
 * <p>
 * 兼容  {@code io.voyager1.util.HttpStatus} 的 API 表面。
 * </p>
 */
public class HttpStatus {

	private HttpStatus() {
	}

	/** 请求成功 */
	public static final int HTTP_OK = 200;
	/** 临时重定向（HTTP 302 Found，旧称 Moved Temporarily） */
	public static final int HTTP_MOVED_TEMP = 302;
	/** 参见其它（HTTP 303 See Other） */
	public static final int HTTP_SEE_OTHER = 303;
	/** 请求方法不被允许（HTTP 405 Method Not Allowed） */
	public static final int HTTP_BAD_METHOD = 405;
}
