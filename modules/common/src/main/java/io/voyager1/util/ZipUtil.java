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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * 压缩工具类（兼容 io.voyager1.util.ZipUtil）。
 */
public class ZipUtil {

	private static final Charset DEFAULT_CHARSET = Charset.defaultCharset();

	/**
	 * 将Zip文件转换为{@link ZipFile}
	 *
	 * @param file    zip文件
	 * @param charset 解析zip文件的编码，null表示UTF-8
	 * @return {@link ZipFile}
	 */
	public static ZipFile toZipFile(File file, Charset charset) {
		try {
			return new ZipFile(file, charset == null ? StandardCharsets.UTF_8 : charset);
		} catch (IOException e) {
			// 可能编码错误提示
			if (e instanceof ZipException && e.getMessage() != null && e.getMessage().contains("invalid CEN header")) {
				try {
					// 尝试使用不同编码
					return new ZipFile(file, StandardCharsets.UTF_8.equals(charset) ? Charset.forName("GBK") : StandardCharsets.UTF_8);
				} catch (final IOException ex) {
					throw new RuntimeException(ex);
				}
			}
			throw new RuntimeException(e);
		}
	}

	/**
	 * 获取指定{@link ZipEntry}的流，用于读取这个entry的内容
	 *
	 * @param zipFile  {@link ZipFile}
	 * @param zipEntry {@link ZipEntry}
	 * @return 流
	 */
	public static InputStream getStream(ZipFile zipFile, ZipEntry zipEntry) {
		try {
			return zipFile.getInputStream(zipEntry);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 对文件或文件目录进行压缩，不包含被打包目录
	 *
	 * @param srcPath 要压缩的源文件路径
	 * @param zipPath 压缩文件保存的路径，包括文件名
	 * @return 压缩好的Zip文件
	 */
	public static File zip(String srcPath, String zipPath) {
		return zip(srcPath, zipPath, false);
	}

	/**
	 * 对文件或文件目录进行压缩
	 *
	 * @param srcPath    要压缩的源文件路径
	 * @param zipPath    压缩文件保存的路径，包括文件名
	 * @param withSrcDir 是否包含被打包目录
	 * @return 压缩文件
	 */
	public static File zip(String srcPath, String zipPath, boolean withSrcDir) {
		return doZip(new File(srcPath), new File(zipPath), DEFAULT_CHARSET, withSrcDir);
	}

	private static File doZip(File srcFile, File zipFile, Charset charset, boolean withSrcDir) {
		if (srcFile == null || !srcFile.exists()) {
			throw new IllegalArgumentException("srcFile must not be empty!");
		}
		if (zipFile == null) {
			throw new IllegalArgumentException("zipFile must not be null!");
		}
		File parent = zipFile.getParentFile();
		if (parent != null && !parent.exists()) {
			parent.mkdirs();
		}
		Charset cs = charset == null ? DEFAULT_CHARSET : charset;
		try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile), cs)) {
			if (srcFile.isDirectory()) {
				addDir(out, srcFile, withSrcDir ? srcFile.getName() : "");
			} else {
				addFile(out, srcFile, srcFile.getName());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return zipFile;
	}

	private static void addDir(ZipOutputStream out, File dir, String path) throws IOException {
		String dirName = path.isEmpty() ? "" : (path.endsWith("/") ? path : path + "/");
		if (!dirName.isEmpty()) {
			out.putNextEntry(new ZipEntry(dirName));
			out.closeEntry();
		}
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		for (File child : files) {
			String childPath = dirName.isEmpty() ? child.getName() : dirName + child.getName();
			if (child.isDirectory()) {
				addDir(out, child, childPath);
			} else {
				addFile(out, child, childPath);
			}
		}
	}

	private static void addFile(ZipOutputStream out, File file, String path) throws IOException {
		out.putNextEntry(new ZipEntry(path));
		try (InputStream in = new FileInputStream(file)) {
			copy(in, out);
		}
		out.closeEntry();
	}

	/**
	 * 解压
	 *
	 * @param zipFile zip文件
	 * @param outFile 解压到的目录
	 * @return 解压的目录
	 */
	public static File unzip(File zipFile, File outFile) {
		return unzip(zipFile, outFile, DEFAULT_CHARSET);
	}

	private static File unzip(File zipFile, File outFile, Charset charset) {
		return unzip(toZipFile(zipFile, charset), outFile);
	}

	private static File unzip(ZipFile zipFile, File outFile) {
		if (outFile.exists() && outFile.isFile()) {
			throw new IllegalArgumentException("Target path [" + outFile.getAbsolutePath() + "] exist!");
		}
		try (ZipFile zf = zipFile) {
			Enumeration<? extends ZipEntry> zipEntries = zf.entries();
			while (zipEntries.hasMoreElements()) {
				final ZipEntry zipEntry = zipEntries.nextElement();
				File out = new File(outFile, zipEntry.getName());
				if (zipEntry.isDirectory()) {
					out.mkdirs();
				} else {
					File parent = out.getParentFile();
					if (parent != null && !parent.exists()) {
						parent.mkdirs();
					}
					try (InputStream in = zf.getInputStream(zipEntry);
						 OutputStream os = new FileOutputStream(out)) {
						copy(in, os);
					}
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return outFile;
	}

	/**
	 * Gzip压缩处理
	 *
	 * @param buf 被压缩的bytes
	 * @return 压缩后的bytes
	 */
	public static byte[] gzip(byte[] buf) {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bos)) {
			gzipOutputStream.write(buf);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return bos.toByteArray();
	}

	/**
	 * Gzip压缩处理
	 *
	 * @param in 被压缩的流
	 * @return 压缩后的bytes
	 */
	public static byte[] gzip(InputStream in) {
		return gzip(readBytes(in));
	}

	/**
	 * Gzip解压缩处理
	 *
	 * @param buf 被解压的bytes
	 * @return 解压后的bytes
	 */
	public static byte[] unGzip(byte[] buf) {
		return unGzip(new ByteArrayInputStream(buf));
	}

	/**
	 * Gzip解压处理
	 *
	 * @param in 被解压的流
	 * @return 解压后的bytes
	 */
	public static byte[] unGzip(InputStream in) {
		try (GZIPInputStream gzipInputStream = new GZIPInputStream(in)) {
			return readBytes(gzipInputStream);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[8192];
		int n;
		while ((n = in.read(buffer)) != -1) {
			out.write(buffer, 0, n);
		}
	}

	private static byte[] readBytes(InputStream in) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			copy(in, out);
			return out.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
