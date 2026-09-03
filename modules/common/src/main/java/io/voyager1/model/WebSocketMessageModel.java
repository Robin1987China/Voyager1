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

import com.alibaba.fastjson2.JSONObject;

/**
 * websocket发送和接收消息Model
 *
 */
public class WebSocketMessageModel extends BaseJsonModel {

    private String command;
    private String nodeId;
    private Object params;
    private Object data;

    public WebSocketMessageModel(String command, String nodeId) {
        this.command = command;
        this.nodeId = nodeId;
        this.data = "";
    }

    public static WebSocketMessageModel getInstance(String message) {
        JSONObject commandObj = JSONObject.parseObject(message);
        String command = commandObj.getString("command");
        String nodeId = commandObj.getString("nodeId");
        WebSocketMessageModel model = new WebSocketMessageModel(command, nodeId);
        model.setParams(commandObj.get("params"));
        model.setData(commandObj.get("data"));

        return model;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Object getData() {
        return data;
    }

    public WebSocketMessageModel setData(Object data) {
        this.data = data;
        return this;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public Object getParams() {
        return params;
    }

    public void setParams(Object params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
