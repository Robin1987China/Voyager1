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

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * 压缩工具类，基于commons-compress的压缩解压封装（兼容 io.voyager1.util.CompressUtil）。
 */
public class CompressUtil {

	/**
	 * 获取压缩输入流，用于解压缩指定内容
	 *
	 * @param compressorName 压缩名称，null表示自动检测
	 * @param in             输入流
	 * @return {@link CompressorInputStream}
	 */
	public static CompressorInputStream getIn(String compressorName, InputStream in) {
		if (!in.markSupported()) {
			in = new BufferedInputStream(in);
		}
		try {
			if (isBlank(compressorName)) {
				compressorName = CompressorStreamFactory.detect(in);
			}
			return new CompressorStreamFactory().createCompressorInputStream(compressorName, in);
		} catch (CompressorException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 创建归档器，支持 zip、tar、tar.gz(tgz) 等
	 *
	 * @param charset      编码
	 * @param archiverName 归档类型名称
	 * @param file         归档输出的文件
	 * @return Archiver
	 */
	public static Archiver createArchiver(Charset charset, String archiverName, File file) {
		String type = resolveArchiveType(archiverName, file == null ? null : file.getName());
		return new StreamArchiver(charset, type, file);
	}

	/**
	 * 创建归档解包器
	 *
	 * @param charset 编码
	 * @param file    归档文件
	 * @return {@link Extractor}
	 */
	public static Extractor createExtractor(Charset charset, File file) {
		String type = resolveArchiveType(null, file == null ? null : file.getName());
		return new StreamExtractor(charset, type, file);
	}

	/**
	 * 创建归档解包器
	 *
	 * @param charset 编码
	 * @param in      归档输入的流
	 * @return {@link Extractor}
	 */
	public static Extractor createExtractor(Charset charset, InputStream in) {
		return new StreamExtractor(charset, null, in);
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private static Charset charset(Charset charset) {
		return charset == null ? Charset.defaultCharset() : charset;
	}

	private static String resolveArchiveType(String archiverName, String fileName) {
		if (fileName != null) {
			String lower = fileName.toLowerCase();
			if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
				return "tar.gz";
			}
			if (lower.endsWith(".tar.bz2") || lower.endsWith(".tbz2")) {
				return "tar.bz2";
			}
			if (lower.endsWith(".tar")) {
				return "tar";
			}
			if (lower.endsWith(".zip")) {
				return "zip";
			}
		}
		if (archiverName == null) {
			return null;
		}
		String a = archiverName.trim();
		if (a.startsWith(".")) {
			a = a.substring(1);
		}
		a = a.toLowerCase();
		if ("tgz".equals(a)) {
			return "tar.gz";
		}
		if ("tbz2".equals(a)) {
			return "tar.bz2";
		}
		return a;
	}

	private static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[8192];
		int n;
		while ((n = in.read(buffer)) != -1) {
			out.write(buffer, 0, n);
		}
	}

	/**
	 * 数据归档器实现
	 */
	private static final class StreamArchiver implements Archiver {

		private final ArchiveOutputStream out;

		StreamArchiver(Charset charset, String archiverName, File file) {
			try {
				OutputStream target = new FileOutputStream(file);
				if ("tar.gz".equalsIgnoreCase(archiverName) || "tgz".equalsIgnoreCase(archiverName)) {
					this.out = new TarArchiveOutputStream(new GzipCompressorOutputStream(target));
				} else {
					this.out = new ArchiveStreamFactory(charset(charset).name()).createArchiveOutputStream(archiverName, target);
				}
			} catch (IOException | ArchiveException e) {
				throw new RuntimeException(e);
			}
			if (this.out instanceof TarArchiveOutputStream) {
				((TarArchiveOutputStream) this.out).setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
			}
		}

		@Override
		public Archiver add(File file) {
			try {
				addInternal(file, null);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return this;
		}

		private void addInternal(File file, String path) throws IOException {
			final String entryName = (path != null && !path.isEmpty()) ? path + "/" + file.getName() : file.getName();
			out.putArchiveEntry(out.createArchiveEntry(file, entryName));
			if (file.isDirectory()) {
				final File[] files = file.listFiles();
				if (files != null) {
					for (File childFile : files) {
						addInternal(childFile, entryName);
					}
				}
				out.closeArchiveEntry();
			} else {
				if (file.isFile()) {
					try (InputStream in = new FileInputStream(file)) {
						copy(in, out);
					}
				}
				out.closeArchiveEntry();
			}
		}

		@Override
		public Archiver finish() {
			try {
				out.finish();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return this;
		}

		@Override
		public void close() {
			try {
				finish();
			} catch (Exception ignore) {
				// ignore
			}
			try {
				out.close();
			} catch (IOException ignore) {
				// ignore
			}
		}
	}

	/**
	 * 数据解压器实现
	 */
	private static final class StreamExtractor implements Extractor {

		private final ArchiveInputStream in;

		StreamExtractor(Charset charset, String archiverName, File file) {
			this(charset, archiverName, openFileInput(file));
		}

		private static InputStream openFileInput(File file) {
			try {
				return new FileInputStream(file);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		StreamExtractor(Charset charset, String archiverName, InputStream in) {
			final String encoding = charset(charset).name();
			final ArchiveStreamFactory factory = new ArchiveStreamFactory(encoding);
			try {
				in = new BufferedInputStream(in);
				if (isBlank(archiverName)) {
					this.in = factory.createArchiveInputStream(in);
				} else if ("tar.gz".equalsIgnoreCase(archiverName) || "tgz".equalsIgnoreCase(archiverName)) {
					this.in = new TarArchiveInputStream(new GzipCompressorInputStream(in), encoding);
				} else if ("tar.bz2".equalsIgnoreCase(archiverName) || "tbz2".equalsIgnoreCase(archiverName)) {
					this.in = new TarArchiveInputStream(new BZip2CompressorInputStream(in), encoding);
				} else {
					this.in = factory.createArchiveInputStream(archiverName, in);
				}
			} catch (Exception e) {
				try {
					in.close();
				} catch (IOException ignore) {
					// ignore
				}
				throw new RuntimeException(e);
			}
		}

		@Override
		public void extract(File targetDir, int stripComponents) {
			try {
				extractInternal(targetDir, stripComponents);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				close();
			}
		}

		private void extractInternal(File targetDir, int stripComponents) throws IOException {
			if (targetDir == null || (targetDir.exists() && !targetDir.isDirectory())) {
				throw new IllegalArgumentException("target must be dir.");
			}
			ArchiveEntry entry;
			while ((entry = in.getNextEntry()) != null) {
				if (!in.canReadEntryData(entry)) {
					// 无法读取的文件直接跳过
					continue;
				}
				String entryName = stripName(entry.getName(), stripComponents);
				if (entryName == null) {
					// 剥离文件夹层级
					continue;
				}
				File outItemFile = new File(targetDir, entryName);
				if (entry.isDirectory()) {
					// 创建对应目录
					outItemFile.mkdirs();
				} else {
					File parent = outItemFile.getParentFile();
					if (parent != null && !parent.exists()) {
						parent.mkdirs();
					}
					try (OutputStream os = new FileOutputStream(outItemFile)) {
						copy(in, os);
					}
				}
			}
		}

		private String stripName(String name, int stripComponents) {
			if (stripComponents <= 0) {
				return name;
			}
			String[] nameList = name.split("/");
			if (nameList.length > stripComponents) {
				return String.join("/", Arrays.copyOfRange(nameList, stripComponents, nameList.length));
			}
			return null;
		}

		@Override
		public void close() {
			try {
				in.close();
			} catch (IOException ignore) {
				// ignore
			}
		}
	}
}
