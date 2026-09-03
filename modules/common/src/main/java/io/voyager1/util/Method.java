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
 * HTTP 方法枚举。
 * <p>
 * 兼容  {@code io.voyager1.util.Method} 的 API 表面。
 * </p>
 */
public enum Method {
	GET, POST, HEAD, OPTIONS, PUT, DELETE, TRACE, CONNECT, PATCH
}
