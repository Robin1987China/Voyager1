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

import java.io.File;

/**
 * 下载封装，下载统一使用 GET 请求，默认支持 30x 跳转。
 * <p>
 * 兼容  {@code io.voyager1.util.HttpDownloader} 的 API 表面。
 * </p>
 */
public class HttpDownloader {

	/**
	 * 下载远程文件
	 *
	 * @param url      请求的 url
	 * @param destFile 目标文件或目录
	 * @return 下载的文件大小
	 */
	public static long download(String url, File destFile) {
		File file = HttpUtil.downloadFileFromUrl(url, destFile);
		return file.length();
	}

	/**
	 * 下载远程文件，返回文件
	 *
	 * @param url             请求的 url
	 * @param targetFileOrDir 目标文件或目录
	 * @param timeout         超时，单位毫秒，-1 表示不设置超时
	 * @param streamProgress  进度条
	 * @return 文件
	 */
	public static File downloadForFile(String url, File targetFileOrDir, int timeout, StreamProgress streamProgress) {
		return HttpUtil.downloadFileFromUrl(url, targetFileOrDir, timeout, streamProgress);
	}

	/**
	 * 下载远程文件
	 *
	 * @param url             请求的 url
	 * @param targetFileOrDir 目标文件或目录
	 * @param timeout         超时，单位毫秒，-1 表示不设置超时
	 * @param streamProgress  进度条
	 * @return 文件大小
	 */
	public static long downloadFile(String url, File targetFileOrDir, int timeout, StreamProgress streamProgress) {
		File file = HttpUtil.downloadFileFromUrl(url, targetFileOrDir, timeout, streamProgress);
		return file.length();
	}
}
