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

package io.voyager1.common.commander;

import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.socket.ConsoleCommandOp;

/**
 * @since 23/12/29 029
 */
public interface ProjectCommander {

    /**
     * 生成可以执行的命令
     *
     * @param nodeProjectInfoModel 项目
     * @return null 是条件不足
     */
    String buildRunCommand(NodeProjectInfoModel nodeProjectInfoModel);

    /**
     * 执行 webhooks 通知
     *
     * @param nodeProjectInfoModel 项目信息
     * @param type                 类型
     * @param other                其他参数
     */
    void asyncWebHooks(NodeProjectInfoModel nodeProjectInfoModel, String type, Object... other);


    /**
     * 清空日志信息
     *
     * @param nodeProjectInfoModel 项目
     * @return 结果
     */
    String backLog(NodeProjectInfoModel nodeProjectInfoModel);


    /**
     * 获取进程占用的主要端口
     *
     * @param pid 进程id
     * @return 端口
     */
    String getMainPort(Integer pid);


    /**
     * 是否正在运行
     *
     * @param nodeProjectInfoModel 项目
     * @return true 正在运行
     */
    boolean isRun(NodeProjectInfoModel nodeProjectInfoModel);


    /**
     * 执行shell命令
     *
     * @param consoleCommandOp     执行的操作
     * @param nodeProjectInfoModel 项目信息
     * @return 执行结果
     */
    CommandOpResult execCommand(ConsoleCommandOp consoleCommandOp, NodeProjectInfoModel nodeProjectInfoModel);
}
