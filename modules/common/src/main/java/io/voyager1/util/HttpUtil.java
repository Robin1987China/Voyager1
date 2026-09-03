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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP 请求工具类。
 * <p>
 * 兼容  {@code io.voyager1.util.HttpUtil} 的 API 表面，底层使用 {@link HttpClient}。
 * </p>
 */
public class HttpUtil {

	/**
	 * 检测是否 https
	 *
	 * @param url URL
	 * @return 是否 https
	 */
	public static boolean isHttps(String url) {
		return url != null && url.regionMatches(true, 0, "https:", 0, 6);
	}

	/**
	 * 检测是否 http
	 *
	 * @param url URL
	 * @return 是否 http
	 */
	public static boolean isHttp(String url) {
		return url != null && url.regionMatches(true, 0, "http:", 0, 5);
	}

	/**
	 * 创建 Http 请求对象
	 *
	 * @param method 方法枚举
	 * @param url    请求的 URL
	 * @return {@link HttpRequest}
	 */
	public static HttpRequest createRequest(Method method, String url) {
		return new HttpRequest(url).method(method);
	}

	/**
	 * 创建 Http GET 请求对象
	 *
	 * @param url 请求的 URL
	 * @return {@link HttpRequest}
	 */
	public static HttpRequest createGet(String url) {
		return createGet(url, false);
	}

	/**
	 * 创建 Http GET 请求对象
	 *
	 * @param url               请求的 URL
	 * @param isFollowRedirects 是否打开重定向
	 * @return {@link HttpRequest}
	 */
	public static HttpRequest createGet(String url, boolean isFollowRedirects) {
		return new HttpRequest(url).method(Method.GET).setFollowRedirects(isFollowRedirects);
	}

	/**
	 * 创建 Http POST 请求对象
	 *
	 * @param url 请求的 URL
	 * @return {@link HttpRequest}
	 */
	public static HttpRequest createPost(String url) {
		return new HttpRequest(url).method(Method.POST);
	}

	/**
	 * 发送 GET 请求
	 *
	 * @param urlString 网址
	 * @return 返回内容
	 */
	public static String get(String urlString) {
		return createGet(urlString).execute().body();
	}

	/**
	 * 从请求参数的 body 中判断请求的 Content-Type 类型
	 *
	 * @param body 请求参数体
	 * @return Content-Type 字符串，如果无法判断返回 null
	 */
	public static String getContentTypeByRequestBody(String body) {
		ContentType contentType = ContentType.get(body);
		return (contentType == null) ? null : contentType.toString();
	}

	// ---------------------------------------------------------------------------------------- download

	/**
	 * 下载远程文件
	 *
	 * @param url  请求的 url
	 * @param dest 目标文件或目录
	 * @return 下载的文件对象
	 */
	public static File downloadFileFromUrl(String url, String dest) {
		return downloadFileFromUrl(url, FileUtil.file(dest));
	}

	/**
	 * 下载远程文件
	 *
	 * @param url      请求的 url
	 * @param destFile 目标文件或目录
	 * @return 下载的文件对象
	 */
	public static File downloadFileFromUrl(String url, File destFile) {
		return downloadFileFromUrl(url, destFile, -1, null);
	}

	/**
	 * 下载远程文件
	 *
	 * @param url      请求的 url
	 * @param destFile 目标文件或目录
	 * @param timeout  超时，单位毫秒，-1 表示不设置超时
	 * @return 下载的文件对象
	 */
	public static File downloadFileFromUrl(String url, File destFile, int timeout) {
		return downloadFileFromUrl(url, destFile, timeout, null);
	}

	/**
	 * 下载远程文件
	 *
	 * @param url            请求的 url
	 * @param destFile       目标文件或目录
	 * @param streamProgress 进度条
	 * @return 下载的文件对象
	 */
	public static File downloadFileFromUrl(String url, File destFile, StreamProgress streamProgress) {
		return downloadFileFromUrl(url, destFile, -1, streamProgress);
	}

	/**
	 * 下载远程文件
	 *
	 * @param url            请求的 url
	 * @param destFile       目标文件或目录
	 * @param timeout        超时，单位毫秒，-1 表示不设置超时
	 * @param streamProgress 进度条
	 * @return 下载的文件对象
	 */
	public static File downloadFileFromUrl(String url, File destFile, int timeout, StreamProgress streamProgress) {
		if (url == null || url.isBlank()) {
			throw new HttpException("[url] is blank !");
		}
		if (destFile == null) {
			throw new HttpException("[destFile] is null !");
		}
		try {
			HttpClient.Builder clientBuilder = HttpClient.newBuilder()
					.followRedirects(HttpClient.Redirect.NORMAL);
			HttpClient client = clientBuilder.build();

			java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
					.uri(URI.create(url))
					.GET();
			if (timeout > 0) {
				builder.timeout(Duration.ofMillis(timeout));
			}

			java.net.http.HttpResponse<InputStream> response = client.send(builder.build(),
					java.net.http.HttpResponse.BodyHandlers.ofInputStream());

			int status = response.statusCode();
			if (status < 200 || status >= 300) {
				try (InputStream in = response.body()) {
					// 读取并丢弃响应体，避免连接泄漏
					byte[] buffer = new byte[8192];
					// noinspection StatementWithEmptyBody
					while (in.read(buffer) != -1) {
						// ignore
					}
				}
				throw new HttpException("Server response error with status code: [{}]", status);
			}

			File target = resolveDownloadFile(url, destFile);
			long total = contentLength(response);
			try (InputStream in = response.body()) {
				writeToFile(in, target, total, streamProgress);
			}
			return target;
		} catch (HttpException e) {
			throw e;
		} catch (Exception e) {
			throw new HttpException(e);
		}
	}

	private static long contentLength(java.net.http.HttpResponse<InputStream> response) {
		Map<String, List<String>> headers = response.headers().map();
		if (headers == null) {
			return -1;
		}
		for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
			if ("content-length".equalsIgnoreCase(entry.getKey()) && entry.getValue() != null && !entry.getValue().isEmpty()) {
				try {
					return Long.parseLong(entry.getValue().get(0));
				} catch (NumberFormatException ignore) {
					return -1;
				}
			}
		}
		return -1;
	}

	private static File resolveDownloadFile(String url, File destFile) {
		if (destFile.isDirectory()) {
			String fileName = fileNameFromUrl(url);
			if (fileName == null || fileName.isEmpty()) {
				fileName = "download";
			}
			return new File(destFile, fileName);
		}
		return destFile;
	}

	private static String fileNameFromUrl(String url) {
		try {
			String path = URI.create(url).getPath();
			if (path == null) {
				return null;
			}
			int slashIndex = path.lastIndexOf('/');
			if (slashIndex < 0 || slashIndex >= path.length() - 1) {
				return null;
			}
			return URLDecoder.decode(path.substring(slashIndex + 1), StandardCharsets.UTF_8);
		} catch (Exception ignore) {
			return null;
		}
	}

	private static void writeToFile(InputStream in, File target, long total, StreamProgress streamProgress) throws IOException {
		if (streamProgress != null) {
			streamProgress.start();
		}
		File parent = target.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		long written = 0;
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(target))) {
			byte[] buffer = new byte[8192];
			int read;
			while ((read = in.read(buffer)) != -1) {
				out.write(buffer, 0, read);
				written += read;
				if (streamProgress != null) {
					streamProgress.progress(total, written);
				}
			}
			out.flush();
		} finally {
			if (streamProgress != null) {
				streamProgress.finish();
			}
		}
	}
}
