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

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.SftpProgressMonitor;

import java.io.Closeable;
import java.io.File;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

/**
 * SFTP是Secure File Transfer Protocol的缩写，安全文件传送协议。
 * 此类为基于jsch的SFTP实现（兼容 io.voyager1.util.Sftp）。
 */
public class Sftp implements Closeable {

	private Session session;
	private ChannelSftp channel;

	/**
	 * 构造
	 *
	 * @param session {@link Session}
	 * @param charset 编码
	 * @param timeOut 超时时间，单位毫秒
	 */
	public Sftp(Session session, Charset charset, long timeOut) {
		this.session = session;
		ChannelSftp sftp = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP, (int) timeOut);
		init(sftp, charset);
	}

	/**
	 * 构造
	 *
	 * @param channel {@link ChannelSftp}
	 * @param charset 编码
	 * @param timeOut 超时时间，单位毫秒
	 */
	public Sftp(ChannelSftp channel, Charset charset, long timeOut) {
		init(channel, charset);
	}

	private void init(ChannelSftp channel, Charset charset) {
		if (charset == null) {
			charset = StandardCharsets.UTF_8;
		}
		channel.setFilenameEncoding(charset);
		this.channel = channel;
	}

	/**
	 * 获取SFTP通道客户端
	 *
	 * @return 通道客户端
	 */
	public ChannelSftp getClient() {
		return this.channel;
	}

	/**
	 * 远程当前目录
	 *
	 * @return 远程当前目录
	 */
	public String pwd() {
		try {
			return getClient().pwd();
		} catch (SftpException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 获取HOME路径
	 *
	 * @return HOME路径
	 */
	public String home() {
		try {
			return getClient().getHome();
		} catch (SftpException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 是否为目录
	 *
	 * @param dir 目录路径
	 * @return 是否为目录
	 */
	public boolean isDir(String dir) {
		final SftpATTRS sftpATTRS;
		try {
			sftpATTRS = getClient().stat(dir);
		} catch (SftpException e) {
			final String msg = e.getMessage();
			if (msg != null && (msg.toLowerCase().contains("no such file") || msg.toLowerCase().contains("does not exist"))) {
				// 文件不存在直接返回false
				return false;
			}
			throw new RuntimeException(e);
		}
		return sftpATTRS.isDir();
	}

	/**
	 * 判断文件或目录是否存在
	 *
	 * @param path 路径
	 * @return 是否存在
	 */
	public boolean exist(String path) {
		if (path == null || path.isEmpty()) {
			return false;
		}
		try {
			getClient().stat(path);
			return true;
		} catch (SftpException e) {
			return false;
		}
	}

	/**
	 * 创建目录
	 *
	 * @param dir 目录
	 * @return 是否创建成功（目录已存在也返回true）
	 */
	public boolean mkdir(String dir) {
		if (isDir(dir)) {
			// 目录已经存在，创建直接返回
			return true;
		}
		try {
			getClient().mkdir(dir);
			return true;
		} catch (SftpException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 递归创建目录
	 *
	 * @param dir 目录
	 */
	public void mkDirs(String dir) {
		if (dir == null || dir.isEmpty()) {
			return;
		}
		String[] dirs = dir.split("[\\\\/]+");
		StringBuilder dirPath = new StringBuilder();
		for (String sub : dirs) {
			if (sub.isEmpty()) {
				continue;
			}
			dirPath.append('/').append(sub);
			this.mkdir(dirPath.toString());
		}
	}

	/**
	 * 删除文件
	 *
	 * @param filePath 要删除的文件绝对路径
	 * @return 是否删除成功
	 */
	public boolean delFile(String filePath) {
		try {
			getClient().rm(filePath);
			return true;
		} catch (SftpException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 删除文件夹及其文件夹下的所有文件
	 *
	 * @param dirPath 文件夹路径
	 * @return 是否删除成功
	 */
	public boolean delDir(String dirPath) {
		final ChannelSftp client = getClient();
		Vector<ChannelSftp.LsEntry> list;
		try {
			list = client.ls(dirPath);
		} catch (SftpException e) {
			throw new RuntimeException(e);
		}
		String fileName;
		for (ChannelSftp.LsEntry entry : list) {
			fileName = entry.getFilename();
			if (!".".equals(fileName) && !"..".equals(fileName)) {
				String child = dirPath + "/" + fileName;
				if (entry.getAttrs().isDir()) {
					delDir(child);
				} else {
					delFile(child);
				}
			}
		}
		// 删除空目录
		try {
			client.rmdir(dirPath);
			return true;
		} catch (SftpException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 将本地文件或者文件夹同步（覆盖）上传到远程路径
	 *
	 * @param file       文件或者文件夹
	 * @param remotePath 远程路径
	 */
	public void syncUpload(File file, String remotePath) {
		if (file == null || !file.exists()) {
			return;
		}
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files == null) {
				return;
			}
			for (File fileItem : files) {
				if (fileItem.isDirectory()) {
					String mkdir = FileUtil.normalize(remotePath + "/" + fileItem.getName());
					this.syncUpload(fileItem, mkdir);
				} else {
					this.syncUpload(fileItem, remotePath);
				}
			}
		} else {
			this.mkDirs(remotePath);
			this.upload(remotePath, file);
		}
	}

	/**
	 * 将本地文件上传到目标服务器，覆盖模式
	 *
	 * @param destPath 目标路径
	 * @param file     本地文件
	 * @return 是否上传成功
	 */
	public boolean upload(String destPath, File file) {
		put(FileUtil.getAbsolutePath(file), destPath);
		return true;
	}

	/**
	 * 将本地文件上传到目标服务器，覆盖模式
	 *
	 * @param srcFilePath 本地文件路径
	 * @param destPath    目标路径
	 * @return this
	 */
	public Sftp put(String srcFilePath, String destPath) {
		return put(srcFilePath, destPath, Mode.OVERWRITE);
	}

	/**
	 * 将本地文件上传到目标服务器
	 *
	 * @param srcFilePath 本地文件路径
	 * @param destPath    目标路径
	 * @param mode        {@link Mode} 模式
	 * @return this
	 */
	public Sftp put(String srcFilePath, String destPath, Mode mode) {
		return put(srcFilePath, destPath, null, mode);
	}

	/**
	 * 将本地文件上传到目标服务器
	 *
	 * @param srcFilePath 本地文件路径
	 * @param destPath    目标路径，{@code null}表示当前路径
	 * @param monitor     上传进度监控
	 * @param mode        {@link Mode} 模式
	 * @return this
	 */
	public Sftp put(String srcFilePath, String destPath, SftpProgressMonitor monitor, Mode mode) {
		if (destPath == null) {
			destPath = pwd();
		}
		try {
			getClient().put(srcFilePath, destPath, monitor, mode.ordinal());
		} catch (SftpException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	/**
	 * 下载文件到{@link OutputStream}中
	 *
	 * @param src 源文件路径，包括文件名
	 * @param out 目标流
	 */
	public void download(String src, OutputStream out) {
		get(src, out);
	}

	/**
	 * 获取远程文件
	 *
	 * @param src 远程文件路径
	 * @param out 目标流
	 * @return this
	 */
	public Sftp get(String src, OutputStream out) {
		try {
			getClient().get(src, out);
		} catch (SftpException e) {
			throw new RuntimeException(e);
		}
		return this;
	}

	@Override
	public void close() {
		JschUtil.close(this.channel);
		this.channel = null;
		JschUtil.close(this.session);
		this.session = null;
	}

	/**
	 * JSch支持的三种文件传输模式
	 */
	public enum Mode {
		/**
		 * 完全覆盖模式，这是JSch的默认文件传输模式
		 */
		OVERWRITE,
		/**
		 * 恢复模式（续传）
		 */
		RESUME,
		/**
		 * 追加模式
		 */
		APPEND
	}
}
