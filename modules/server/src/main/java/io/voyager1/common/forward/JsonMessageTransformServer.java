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

package io.voyager1.common.forward;

import io.voyager1.util.ExceptionUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.exception.AgentException;
import io.voyager1.transport.INodeInfo;
import io.voyager1.transport.TransformServer;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;

/**
 * json 消息转换
 *
 * @since 2022/12/24
 */
@Slf4j
public class JsonMessageTransformServer implements TransformServer {

    @Override
    public <T> T transform(String data, TypeReference<T> tTypeReference) {
        return NodeForward.toJsonMessage(data, tTypeReference);
    }

    @Override
    public <T> T transformOnlyData(String data, Class<T> tClass) {
        ApiResult<T> transform = this.transform(data, new TypeReference<ApiResult<T>>() {
        });
        return transform.getData(tClass);
    }

    @Override
    public Exception transformException(Exception exception, INodeInfo nodeModel) {
        if (exception instanceof NullPointerException) {
            log.error("{}节点,程序空指针异常", nodeModel.name(), exception);
            return new AgentException(nodeModel.name() + "节点异常,空指针");
        }
        String message = exception.getMessage();
        log.error("node [{}] connect failed...message: [{}]", nodeModel.name(), message);
        List<Throwable> throwableList = ExceptionUtil.getThrowableList(exception);
        for (Throwable throwable : throwableList) {
            if (throwable instanceof ConnectException || throwable instanceof SocketTimeoutException) {
                return new AgentException(nodeModel.name() + "节点网络连接异常或超时,请优先检查插件端运行状态再检查 IP 地址、" +
                    "端口号是否配置正确,防火墙规则," +
                    "云服务器的安全组配置等网络相关问题排查定位。" + message);
            }
            if (throwable instanceof UnknownHostException) {
                return new AgentException(nodeModel.name() + "无法访问节点网络(未知的名称或服务),请检查主机名或者 DNS 是否可用。" + message);
            }
            if (throwable instanceof NoRouteToHostException) {
                return new AgentException(nodeModel.name() + "节点通讯失败,远程地址和端口时发生错误的信号。通常，由于中间的防火墙或中间路由器已关闭，无法访问远程主机。" + message);
            }
            if (throwable instanceof IOException && (message != null && message.toLowerCase().contains("Error writing to server".toLowerCase()))) {
                return new AgentException(nodeModel.name() + "节点通讯失败,请优先检查限制上传大小配置是否合理,或者网络连接是否被代理终端、防火墙终端等。" + message);
            }
        }
        return new AgentException(nodeModel.name() + "节点异常：" + message);
    }
}
