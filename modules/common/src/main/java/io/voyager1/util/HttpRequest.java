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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * HTTP 请求封装。
 * <p>
 * 兼容  {@code io.voyager1.util.HttpRequest} 的 API 表面，底层使用 {@link HttpClient}。
 * </p>
 */
public class HttpRequest {

	/**
	 * 默认超时时间（毫秒），与  全局默认超时保持一致
	 */
	private static final int DEFAULT_TIMEOUT = 5000;

	private final String url;
	private Method method = Method.GET;
	private final Map<String, String> headers = new LinkedHashMap<>();
	private Map<String, Object> form;
	private String body;
	private int timeout = DEFAULT_TIMEOUT;
	private boolean followRedirects = false;

	public HttpRequest(String url) {
		this.url = url;
	}

	public HttpRequest method(Method method) {
		this.method = (method == null ? Method.GET : method);
		return this;
	}

	public HttpRequest setFollowRedirects(boolean isFollowRedirects) {
		this.followRedirects = isFollowRedirects;
		return this;
	}

	public String getUrl() {
		return this.url;
	}

	// ---------------------------------------------------------------- Header start

	public HttpRequest header(String name, String value) {
		if (name != null && value != null) {
			this.headers.put(name.trim(), value);
		}
		return this;
	}

	public HttpRequest header(Header name, String value) {
		return (name == null ? this : header(name.getValue(), value));
	}

	public String header(String name) {
		if (name == null) {
			return null;
		}
		for (Map.Entry<String, String> entry : this.headers.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(name)) {
				return entry.getValue();
			}
		}
		return null;
	}

	public String header(Header name) {
		return (name == null ? null : header(name.getValue()));
	}

	public Map<String, List<String>> headers() {
		Map<String, List<String>> result = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : this.headers.entrySet()) {
			List<String> values = new ArrayList<>(1);
			values.add(entry.getValue());
			result.put(entry.getKey(), values);
		}
		return result;
	}

	/**
	 * 设置 contentType
	 *
	 * @param contentType contentType
	 * @return this
	 */
	public HttpRequest contentType(String contentType) {
		return header(Header.CONTENT_TYPE, contentType);
	}

	// ---------------------------------------------------------------- Form start

	public HttpRequest form(String name, Object value) {
		if (name == null || name.isBlank() || value == null) {
			return this;
		}
		if (this.form == null) {
			this.form = new LinkedHashMap<>();
		}
		this.form.put(name, String.valueOf(value));
		// 使用 form 时停止 body
		this.body = null;
		return this;
	}

	public HttpRequest form(Map<String, Object> formMap) {
		if (formMap != null && !formMap.isEmpty()) {
			formMap.forEach(this::form);
		}
		return this;
	}

	/**
	 * 获取表单数据
	 *
	 * @return 表单 Map
	 */
	public Map<String, Object> form() {
		return this.form;
	}

	// ---------------------------------------------------------------- Body start

	public HttpRequest body(String body) {
		this.body = body;
		this.form = null;
		if (body != null && header(Header.CONTENT_TYPE) == null) {
			String detected = HttpUtil.getContentTypeByRequestBody(body);
			if (detected != null) {
				contentType(detected);
			}
		}
		return this;
	}

	public HttpRequest body(String body, String contentType) {
		this.body = body;
		this.form = null;
		if (contentType != null) {
			contentType(contentType);
		}
		return this;
	}

	public HttpRequest body(byte[] body) {
		return body(body == null ? null : new String(body, java.nio.charset.StandardCharsets.UTF_8));
	}

	// ---------------------------------------------------------------- Timeout start

	/**
	 * 设置超时，单位：毫秒。负值表示不设置超时。
	 *
	 * @param milliseconds 超时毫秒数
	 * @return this
	 */
	public HttpRequest timeout(int milliseconds) {
		this.timeout = milliseconds;
		return this;
	}

	// ---------------------------------------------------------------- Execute start

	public HttpResponse execute() {
		try {
			return doExecute();
		} catch (HttpException e) {
			throw e;
		} catch (Exception e) {
			throw new HttpException(e);
		}
	}

	/**
	 * 执行 Request 请求后，对响应内容后续处理，处理结束后关闭连接
	 *
	 * @param function 响应内容处理函数
	 * @param <T>      处理结果类型
	 * @return 处理结果
	 */
	public <T> T thenFunction(Function<HttpResponse, T> function) {
		try (HttpResponse response = execute()) {
			return function.apply(response);
		}
	}

	private HttpResponse doExecute() throws Exception {
		Method method = (this.method == null ? Method.GET : this.method);
		String methodName = method.name();
		String targetUrl = this.url;

		String formBody = null;
		if (this.form != null && !this.form.isEmpty()) {
			String encoded = encodeForm(this.form);
			if (isQueryMethod(method)) {
				targetUrl = appendQuery(targetUrl, encoded);
			} else {
				formBody = encoded;
			}
		}

		String requestBody = this.body;
		if (requestBody == null) {
			requestBody = formBody;
		}

		Map<String, String> requestHeaders = buildRequestHeaders();
		// 以表单提交且未指定 Content-Type 时补全默认类型
		if (requestBody != null && this.body == null
				&& getHeaderIgnoreCase(requestHeaders, Header.CONTENT_TYPE.getValue()) == null) {
			requestHeaders.put(Header.CONTENT_TYPE.getValue(), ContentType.FORM_URLENCODED.getValue());
		}

		HttpClient.Builder clientBuilder = HttpClient.newBuilder();
		clientBuilder.followRedirects(this.followRedirects ? HttpClient.Redirect.NORMAL : HttpClient.Redirect.NEVER);
		HttpClient client = clientBuilder.build();

		java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
				.uri(URI.create(targetUrl));
		for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
			builder.header(entry.getKey(), entry.getValue());
		}
		if (this.timeout > 0) {
			builder.timeout(Duration.ofMillis(this.timeout));
		}
		if (requestBody != null) {
			builder.method(methodName, java.net.http.HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));
		} else {
			builder.method(methodName, java.net.http.HttpRequest.BodyPublishers.noBody());
		}

		java.net.http.HttpResponse<String> response = client.send(builder.build(),
				java.net.http.HttpResponse.BodyHandlers.ofString());

		Map<String, List<String>> respHeaders = new HashMap<>(response.headers().map());
		String respBody = response.body();
		if (respBody == null) {
			respBody = "";
		}
		return new HttpResponse(response.statusCode(), respBody, respHeaders);
	}

	private Map<String, String> buildRequestHeaders() {
		Map<String, String> merged = new LinkedHashMap<>();
		Map<String, List<String>> global = GlobalHeaders.INSTANCE.headers();
		if (global != null) {
			for (Map.Entry<String, List<String>> entry : global.entrySet()) {
				if (entry.getValue() != null && !entry.getValue().isEmpty()) {
					merged.put(entry.getKey(), entry.getValue().get(0));
				}
			}
		}
		merged.putAll(this.headers);
		return merged;
	}

	private static boolean isQueryMethod(Method method) {
		switch (method) {
			case GET:
			case HEAD:
			case DELETE:
			case OPTIONS:
			case TRACE:
				return true;
			default:
				return false;
		}
	}

	private static String encodeForm(Map<String, Object> form) {
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<String, Object> entry : form.entrySet()) {
			if (entry.getKey() == null || entry.getValue() == null) {
				continue;
			}
			if (sb.length() > 0) {
				sb.append('&');
			}
			sb.append(encode(entry.getKey()));
			sb.append('=');
			sb.append(encode(String.valueOf(entry.getValue())));
		}
		return sb.toString();
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String appendQuery(String url, String query) {
		if (query == null || query.isEmpty()) {
			return url;
		}
		if (url.contains("?")) {
			if (url.endsWith("?") || url.endsWith("&")) {
				return url + query;
			}
			return url + "&" + query;
		}
		return url + "?" + query;
	}

	private static String getHeaderIgnoreCase(Map<String, String> map, String name) {
		if (map == null || name == null) {
			return null;
		}
		for (Map.Entry<String, String> entry : map.entrySet()) {
			if (entry.getKey().equalsIgnoreCase(name)) {
				return entry.getValue();
			}
		}
		return null;
	}
}
