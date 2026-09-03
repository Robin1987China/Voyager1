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

import io.voyager1.util.HttpRequest;
import io.voyager1.util.HttpUtil;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.model.data.MonitorModel;
import org.springframework.http.MediaType;

/**
 * 钉钉工具
 *
 */
public class WebHookUtil implements INotify {

    /**
     * 发送钉钉群自定义机器人消息
     *
     * @param notify  通知对象
     * @param title   描述标签
     * @param context 消息内容
     */
    @Override
    public void send(MonitorModel.Notify notify, String title, String context) {
        JSONObject text = new JSONObject();
        JSONObject param = new JSONObject();
        //消息内容
        text.put("content", title + "\n" + context);
        param.put("msgtype", "text");
        param.put("text", text);
        HttpRequest request = HttpUtil.
            createPost(notify.getValue()).
            contentType(MediaType.APPLICATION_JSON_VALUE).
            body(param.toJSONString());
        request.execute();
    }
}
