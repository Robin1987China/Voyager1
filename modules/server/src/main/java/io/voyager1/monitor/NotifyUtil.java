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

package io.voyager1.monitor;


import io.voyager1.model.BaseEnum;
import io.voyager1.model.data.MonitorModel;

import java.util.Map;
import java.util.Objects;

/**
 * 通知util
 *
 * @since 2019/7/13
 */
public class NotifyUtil {

	private static final Map<MonitorModel.NotifyType, INotify> NOTIFY_MAP = new java.util.concurrent.ConcurrentHashMap<>();

	static {
		NOTIFY_MAP.put(MonitorModel.NotifyType.dingding, new WebHookUtil());
		NOTIFY_MAP.put(MonitorModel.NotifyType.mail, new EmailUtil());
		NOTIFY_MAP.put(MonitorModel.NotifyType.workWx, new WebHookUtil());
	}

	/**
	 * 发送报警消息
	 *
	 * @param notify  通知方式
	 * @param title   描述
	 * @param context 内容
	 */
	public static void send(MonitorModel.Notify notify, String title, String context) throws Exception {
		int style = notify.getStyle();
		MonitorModel.NotifyType notifyType = BaseEnum.getEnum(MonitorModel.NotifyType.class, style);
		Objects.requireNonNull(notifyType);
		//
		INotify iNotify = NOTIFY_MAP.get(notifyType);
		Objects.requireNonNull(iNotify);
		iNotify.send(notify, title, context);
	}

}
