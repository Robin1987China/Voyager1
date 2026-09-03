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
 * 数据归档封装，归档即将几个文件或目录打成一个压缩包（兼容 io.voyager1.util.Archiver）。
 */
public interface Archiver extends Closeable {

	/**
	 * 将文件或目录加入归档，目录采取递归读取方式按照层级加入
	 *
	 * @param file 文件或目录
	 * @return this
	 */
	Archiver add(File file);

	/**
	 * 结束已经增加的文件归档
	 *
	 * @return this
	 */
	Archiver finish();

	/**
	 * 无异常关闭
	 */
	@Override
	void close();
}
