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

import io.voyager1.configuration.NodeConfig;
import io.voyager1.func.assets.server.MachineNodeServer;
import io.voyager1.service.system.SystemParametersServer;
import io.voyager1.socket.handler.*;
import io.voyager1.system.ServerConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * socket 配置
 *
 */
@Configuration
@EnableWebSocket
public class ServerWebSocketConfig implements WebSocketConfigurer {
    private final ServerWebSocketInterceptor serverWebSocketInterceptor;
    private final SystemParametersServer systemParametersServer;
    private final NodeConfig nodeConfig;
    private final MachineNodeServer machineNodeServer;

    public ServerWebSocketConfig(ServerWebSocketInterceptor serverWebSocketInterceptor,
                                 SystemParametersServer systemParametersServer,
                                 ServerConfig serverConfig,
                                 MachineNodeServer machineNodeServer) {
        this.serverWebSocketInterceptor = serverWebSocketInterceptor;
        this.systemParametersServer = systemParametersServer;
        this.nodeConfig = serverConfig.getNode();
        this.machineNodeServer = machineNodeServer;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 控制台
        registry.addHandler(new ConsoleHandler(), "/socket/console")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // 节点脚本模板
        registry.addHandler(new NodeScriptHandler(), "/socket/node/script_run")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // 系统日志
        registry.addHandler(new SystemLogHandler(), "/socket/system_log")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // 插件端日志
        registry.addHandler(new AgentLogHandler(), "/socket/agent_log")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // ssh
        registry.addHandler(new SshHandler(), "/socket/ssh")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // 节点升级
        registry.addHandler(new NodeUpdateHandler(machineNodeServer, systemParametersServer, nodeConfig), "/socket/node_update")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // 脚本模板
        registry.addHandler(new ServerScriptHandler(), "/socket/script_run")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // docker log
        registry.addHandler(new DockerLogHandler(), "/socket/docker_log")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // docker cli
        registry.addHandler(new DockerCliHandler(), "/socket/docker_cli")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
        // free script
        registry.addHandler(new FreeScriptHandler(), "/socket/free_script")
            .addInterceptors(serverWebSocketInterceptor).setAllowedOrigins("*");
    }
}
