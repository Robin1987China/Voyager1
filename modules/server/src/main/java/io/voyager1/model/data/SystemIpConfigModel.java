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

package io.voyager1.model.data;

import io.voyager1.model.BaseJsonModel;

/**
 * @since 2021/4/18
 */
public class SystemIpConfigModel extends BaseJsonModel {

	public static final String ID = "IP_CONFIG";

	/**
	 * ip 授权  允许访问
	 */
	private String allowed;

	/**
	 * 禁止
	 */
	private String prohibited;

	public String getAllowed() {
		return allowed;
	}

	public void setAllowed(String allowed) {
		this.allowed = allowed;
	}

	public String getProhibited() {
		return prohibited;
	}

	public void setProhibited(String prohibited) {
		this.prohibited = prohibited;
	}
}
