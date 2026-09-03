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

package io.voyager1.socket;

import lombok.Getter;
import io.voyager1.func.assets.server.MachineDockerServer;
import io.voyager1.func.assets.server.MachineSshServer;
import io.voyager1.service.docker.DockerInfoService;
import io.voyager1.service.node.ProjectInfoCacheService;
import io.voyager1.service.node.script.NodeScriptServer;
import io.voyager1.service.node.ssh.SshService;
import io.voyager1.service.script.ScriptServer;
import io.voyager1.socket.handler.*;

/**
 * @since 2019/8/9
 */
@Getter
public enum HandlerType {
    /**
     * 脚本模板
     */
    nodeScript(NodeScriptHandler.class, NodeScriptServer.class),
    /**
     * 系统日志
     */
    systemLog(SystemLogHandler.class, null),
    /**
     * 插件端日志
     */
    agentLog(AgentLogHandler.class, null),
    /**
     * 项目控制台和首页监控
     */
    console(ConsoleHandler.class, ProjectInfoCacheService.class),
    /**
     * ssh
     */
    ssh(SshHandler.class, SshService.class, MachineSshServer.class, "machineSshId"),
    /**
     * 节点升级
     */
    nodeUpdate(NodeUpdateHandler.class, null),
    /**
     * 服务端 脚本模版
     */
    script(ServerScriptHandler.class, ScriptServer.class),
    /**
     * 容器 log
     */
    dockerLog(DockerLogHandler.class, DockerInfoService.class, MachineDockerServer.class, "machineDockerId"),
    /**
     * 容器 终端
     */
    docker(DockerCliHandler.class, DockerInfoService.class, MachineDockerServer.class, "machineDockerId"),
    freeScript(FreeScriptHandler.class, null),
    ;
    final Class<?> handlerClass;

    final Class<? extends io.voyager1.core.jpa.DataService<?>> serviceClass;
    final Class<? extends io.voyager1.core.jpa.DataService<?>> assetsServiceClass;
    /**
     * 资产关联字段
     */
    final String assetsLinkDataId;

    HandlerType(Class<?> handlerClass,
                Class<? extends io.voyager1.core.jpa.DataService<?>> serviceClass) {
        this.handlerClass = handlerClass;
        this.serviceClass = serviceClass;
        this.assetsServiceClass = null;
        this.assetsLinkDataId = null;
    }

    HandlerType(Class<?> handlerClass,
                Class<? extends io.voyager1.core.jpa.DataService<?>> serviceClass,
                Class<? extends io.voyager1.core.jpa.DataService<?>> assetsServiceClass,
                String assetsLinkDataId) {
        this.handlerClass = handlerClass;
        this.serviceClass = serviceClass;
        this.assetsServiceClass = assetsServiceClass;
        this.assetsLinkDataId = assetsLinkDataId;
    }
}
