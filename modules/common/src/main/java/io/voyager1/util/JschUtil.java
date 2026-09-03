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

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Jsch工具类（兼容 io.voyager1.util.JschUtil）。
 */
public class JschUtil {

	/**
	 * 创建一个SSH会话
	 *
	 * @param jsch    {@link JSch}
	 * @param sshHost 主机
	 * @param sshPort 端口
	 * @param sshUser 用户名，如果为null，默认root
	 * @return {@link Session}
	 */
	public static Session createSession(JSch jsch, String sshHost, int sshPort, String sshUser) {
		if (sshHost == null || sshHost.isEmpty()) {
			throw new IllegalArgumentException("SSH Host must be not empty!");
		}
		if (sshPort <= 0) {
			throw new IllegalArgumentException("SSH port must be > 0");
		}
		// 默认root用户
		if (sshUser == null || sshUser.isEmpty()) {
			sshUser = "root";
		}
		if (jsch == null) {
			jsch = new JSch();
		}
		try {
			Session session = jsch.getSession(sshUser, sshHost, sshPort);
			// 设置第一次登录的时候提示，可选值：(ask | yes | no)
			session.setConfig("StrictHostKeyChecking", "no");
			// 设置登录认证方式，跳过Kerberos身份验证
			session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
			return session;
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 新建一个新的SSH会话，此方法并不打开会话（既不调用connect方法）
	 *
	 * @param sshHost 主机
	 * @param sshPort 端口
	 * @param sshUser 用户名
	 * @param sshPass 密码
	 * @return SSH会话
	 */
	public static Session createSession(String sshHost, int sshPort, String sshUser, String sshPass) {
		final Session session = createSession(new JSch(), sshHost, sshPort, sshUser);
		if (sshPass != null && !sshPass.isEmpty()) {
			session.setPassword(sshPass);
		}
		return session;
	}

	/**
	 * 新建一个新的SSH会话，此方法并不打开会话（既不调用connect方法）
	 *
	 * @param sshHost        主机
	 * @param sshPort        端口
	 * @param sshUser        用户名
	 * @param privateKeyPath 私钥的路径
	 * @param passphrase     私钥文件的密码，可以为null
	 * @return SSH会话
	 */
	public static Session createSession(String sshHost, int sshPort, String sshUser, String privateKeyPath, byte[] passphrase) {
		if (privateKeyPath == null || privateKeyPath.isEmpty()) {
			throw new IllegalArgumentException("PrivateKey Path must be not empty!");
		}
		final JSch jsch = new JSch();
		try {
			jsch.addIdentity(privateKeyPath, passphrase);
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
		return createSession(jsch, sshHost, sshPort, sshUser);
	}

	/**
	 * 打开一个新的SSH会话
	 *
	 * @param sshHost 主机
	 * @param sshPort 端口
	 * @param sshUser 用户名
	 * @param sshPass 密码
	 * @return SSH会话
	 */
	public static Session openSession(String sshHost, int sshPort, String sshUser, String sshPass) {
		return openSession(sshHost, sshPort, sshUser, sshPass, 0);
	}

	/**
	 * 打开一个新的SSH会话
	 *
	 * @param sshHost 主机
	 * @param sshPort 端口
	 * @param sshUser 用户名
	 * @param sshPass 密码
	 * @param timeout Socket连接超时时长，单位毫秒
	 * @return SSH会话
	 */
	public static Session openSession(String sshHost, int sshPort, String sshUser, String sshPass, int timeout) {
		final Session session = createSession(sshHost, sshPort, sshUser, sshPass);
		try {
			session.connect(timeout);
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
		return session;
	}

	/**
	 * 打开Channel连接
	 *
	 * @param session     Session会话
	 * @param channelType 通道类型，可以是shell或sftp等，见{@link ChannelType}
	 * @return {@link Channel}
	 */
	public static Channel openChannel(Session session, ChannelType channelType) {
		return openChannel(session, channelType, 0);
	}

	/**
	 * 打开Channel连接
	 *
	 * @param session     Session会话
	 * @param channelType 通道类型，可以是shell或sftp等，见{@link ChannelType}
	 * @param timeout     连接超时时长，单位毫秒
	 * @return {@link Channel}
	 */
	public static Channel openChannel(Session session, ChannelType channelType, int timeout) {
		final Channel channel = createChannel(session, channelType);
		try {
			channel.connect(Math.max(timeout, 0));
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
		return channel;
	}

	/**
	 * 创建Channel连接
	 *
	 * @param session     Session会话
	 * @param channelType 通道类型，可以是shell或sftp等，见{@link ChannelType}
	 * @return {@link Channel}
	 */
	public static Channel createChannel(Session session, ChannelType channelType) {
		try {
			if (!session.isConnected()) {
				session.connect();
			}
			return session.openChannel(channelType.getValue());
		} catch (JSchException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * 关闭SSH连接会话
	 *
	 * @param session SSH会话
	 */
	public static void close(Session session) {
		if (session != null && session.isConnected()) {
			session.disconnect();
		}
	}

	/**
	 * 关闭会话通道
	 *
	 * @param channel 会话通道
	 */
	public static void close(Channel channel) {
		if (channel != null && channel.isConnected()) {
			channel.disconnect();
		}
	}
}
