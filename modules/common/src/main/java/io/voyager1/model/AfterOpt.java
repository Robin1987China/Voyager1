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

package io.voyager1.model;

/**
 * @since 2020/3/21
 */
public enum AfterOpt implements BaseEnum {
	/**
	 * 操作
	 */
	No(0, "不做任何操作"),
	/**
	 * 并发执行项目分发
	 */
	Restart(1, "并发重启"),
	/**
	 * 顺序执行项目分发
	 */
	Order_Must_Restart(2, "完整顺序重启(有重启失败将结束本次)"),
	/**
	 * 顺序执行项目分发
	 */
	Order_Restart(3, "顺序重启(有重启失败将继续)"),
	;
	private final int code;
	private final String desc;

	AfterOpt(int code, String desc) {
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
