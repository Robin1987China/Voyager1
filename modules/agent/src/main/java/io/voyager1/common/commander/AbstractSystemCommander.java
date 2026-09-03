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

import io.voyager1.util.CommandUtil;

import java.io.File;

/**
 * 系统监控命令
 *
 * @since 2019/4/16
 */
public abstract class AbstractSystemCommander implements SystemCommander {


    /**
     * 清空文件内容
     *
     * @param file 文件
     * @return 执行结果
     */
    public abstract String emptyLogFile(File file);

//    /**
//     * 查询服务状态
//     *
//     * @param serviceName 服务名称
//     * @return true 运行中
//     */
//    public abstract boolean getServiceStatus(String serviceName);
//
//    /**
//     * 启动服务
//     *
//     * @param serviceName 服务名称
//     * @return 结果
//     */
//    public abstract String startService(String serviceName);
//
//    /**
//     * 关闭服务
//     *
//     * @param serviceName 服务名称
//     * @return 结果
//     */
//    public abstract String stopService(String serviceName);

    /**
     * 构建kill 命令
     *
     * @param pid 进程编号
     * @return 结束进程命令
     */
    public abstract String buildKill(int pid);

    /**
     * kill
     *
     * @param pid 进程编号
     */
    public String kill(File file, int pid) {
        String kill = buildKill(pid);
        return CommandUtil.execSystemCommand(kill, file);
    }
}
