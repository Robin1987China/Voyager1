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

package io.voyager1.model.system;

import io.voyager1.model.BaseJsonModel;

/**
 * agent 端自动生成的密码实体
 *
 * @since 2019/4/18
 */
public class AgentAutoUser extends BaseJsonModel {

    private String agentName;
    private String agentPwd;

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getAgentPwd() {
        return agentPwd;
    }

    public void setAgentPwd(String agentPwd) {
        this.agentPwd = agentPwd;
    }
}
