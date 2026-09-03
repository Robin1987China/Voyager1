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

package io.voyager1.socket.handler;

import io.voyager1.util.ArrayUtil;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.socket.BaseProxyHandler;
import io.voyager1.socket.ConsoleCommandOp;
import io.voyager1.transport.IProxyWebSocket;

import java.io.IOException;
import java.util.Map;

/**
 * 控制台消息处理器
 *
 * @since 2019/4/19
 */
@Feature(cls = ClassFeature.PROJECT_CONSOLE, method = MethodFeature.EXECUTE)
public class ConsoleHandler extends BaseProxyHandler {

    public ConsoleHandler() {
        super(NodeUrl.TopSocket);
    }

    @Override
    protected Object[] getParameters(Map<String, Object> attributes) {
        return new Object[]{"projectId", attributes.get("projectId")};
    }

    @Override
    protected String handleTextMessage(Map<String, Object> attributes,
                                       IProxyWebSocket proxySession,
                                       JSONObject json,
                                       ConsoleCommandOp consoleCommandOp) throws IOException {
        //ProjectInfoCacheModel dataItem = (ProjectInfoCacheModel) attributes.get("dataItem");
//		UserModel userModel = (UserModel) attributes.get("userInfo");
//		if (RunMode.Dsl.name().equals(dataItem.getRunMode()) && userModel.isDemoUser()) {
//			if (consoleCommandOp == ConsoleCommandOp.stop || consoleCommandOp == ConsoleCommandOp.start || consoleCommandOp == ConsoleCommandOp.restart) {
//				return PermissionInterceptor.DEMO_TIP;
//			}
//		}
        ConsoleCommandOp[] commandOps = new ConsoleCommandOp[]{ConsoleCommandOp.heart, ConsoleCommandOp.showlog};
        if (!ArrayUtil.contains(commandOps, consoleCommandOp)) {
            super.logOpt(this.getClass(), attributes, json);
        }
        proxySession.send(json.toString());
        return null;
    }
}
