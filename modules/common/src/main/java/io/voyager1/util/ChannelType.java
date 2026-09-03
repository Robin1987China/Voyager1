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
 * JSch支持的Channel类型（兼容 io.voyager1.util.ChannelType）。
 */
public enum ChannelType {

	/** Session */
	SESSION("session"),
	/** shell */
	SHELL("shell"),
	/** exec */
	EXEC("exec"),
	/** x11 */
	X11("x11"),
	/** agent forwarding */
	AGENT_FORWARDING("auth-agent@openssh.com"),
	/** direct tcpip */
	DIRECT_TCPIP("direct-tcpip"),
	/** forwarded tcpip */
	FORWARDED_TCPIP("forwarded-tcpip"),
	/** sftp */
	SFTP("sftp"),
	/** subsystem */
	SUBSYSTEM("subsystem");

	/** channel值 */
	private final String value;

	ChannelType(String value) {
		this.value = value;
	}

	/**
	 * 获取值
	 *
	 * @return 值
	 */
	public String getValue() {
		return this.value;
	}
}
