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

import java.io.Closeable;
import java.io.File;

/**
 * 归档数据解包封装，用于将zip、tar等包解包为文件（兼容 io.voyager1.util.Extractor）。
 */
public interface Extractor extends Closeable {

	/**
	 * 释放（解压）到指定目录，结束后自动关闭流，此方法只能调用一次
	 *
	 * @param targetDir 目标目录
	 */
	default void extract(File targetDir) {
		extract(targetDir, 0);
	}

	/**
	 * 释放（解压）到指定目录，结束后自动关闭流，此方法只能调用一次
	 *
	 * @param targetDir       目标目录
	 * @param stripComponents 清除(剥离)压缩包里面的 n 级文件夹名
	 */
	void extract(File targetDir, int stripComponents);

	/**
	 * 无异常关闭
	 */
	@Override
	void close();
}
