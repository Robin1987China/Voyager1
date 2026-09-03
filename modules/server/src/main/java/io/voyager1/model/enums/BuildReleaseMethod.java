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

package io.voyager1.model.enums;

import io.voyager1.model.BaseEnum;

/**
 * @since 2021/8/27
 */
public enum BuildReleaseMethod implements BaseEnum {
	/**
	 * 发布
	 */
	No(0, "不发布"),
	Outgiving(1, "节点分发"),
	Project(2, "项目"),
	Ssh(3, "SSH"),
	LocalCommand(4, "本地命令行"),
	DockerImage(5, "Docker镜像"),
	;
	private final int code;
	private final String desc;

	BuildReleaseMethod(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	@Override
	public int getCode() {
		return code;
	}

	@Override
	public String getDesc() {
		return desc;
	}
}
