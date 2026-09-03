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

package io.voyager1.core;

import io.voyager1.RemoteVersion;
import lombok.Getter;

import java.util.function.Function;

/**
 * 应用类型（服务端 / 插件端）。
 * <p>
 * 取代承继的 {@code io.voyager1.Type}：语义更明确（AppType 而非泛泛的 Type），并移入 {@code io.voyager1.core} 命名空间。
 */
@Getter
public enum AppType {
    /**
     * 插件端
     */
    Agent("io.voyager1.Voyager1AgentApplication", remoteVersion -> {
        String token = readRemoteToken();
        if (!token.isEmpty()) {
            RemoteVersion.RemoteVersionAuth auth = remoteVersion.getAuth();
            if (auth != null && auth.getAgentUrl() != null && !auth.getAgentUrl().isEmpty()) {
                return auth.getAgentUrl().replace("{token}", token);
            }
        }
        return remoteVersion.getAgentUrl();
    }, "VOYAGER1_AGENT_APPLICATION"),

    /**
     * 中心服务端
     */
    Server("io.voyager1.Voyager1ServerApplication", remoteVersion -> {
        String token = readRemoteToken();
        if (!token.isEmpty()) {
            RemoteVersion.RemoteVersionAuth auth = remoteVersion.getAuth();
            if (auth != null && auth.getServerUrl() != null && !auth.getServerUrl().isEmpty()) {
                return auth.getServerUrl().replace("{token}", token);
            }
        }
        return remoteVersion.getServerUrl();
    }, "VOYAGER1_SERVER_APPLICATION"),
    ;

    private final String applicationClass;
    private final Function<RemoteVersion, String> remoteUrl;
    private final String tag;

    AppType(String applicationClass, Function<RemoteVersion, String> remoteUrl, String tag) {
        this.applicationClass = applicationClass;
        this.remoteUrl = remoteUrl;
        this.tag = tag;
    }

    public String getRemoteUrl(RemoteVersion remoteVersion) {
        return remoteUrl.apply(remoteVersion);
    }

    private static String readRemoteToken() {
        String env = System.getenv("VOYAGER1_REMOTE_VERSION_AUTH");
        if (env != null) {
            return env;
        }
        String property = System.getProperty("VOYAGER1_REMOTE_VERSION_AUTH");
        return property != null ? property : "";
    }
}
