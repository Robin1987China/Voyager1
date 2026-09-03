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

package io.voyager1.startup.commands;

import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.data.SystemIpConfigModel;
import io.voyager1.service.system.SystemParametersServer;
import io.voyager1.startup.StartupCommand;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * 重置IP配置命令
 *
 * @since 2024/12/08
 */
@Slf4j
@Component
public class ResetIpConfigCommand implements StartupCommand {
    @Override
    public String getCommandFlag() {
        return "--rest:ip_config";
    }

    @Override
    public void execute(ApplicationContext applicationContext) {
        SystemParametersServer parametersServer = applicationContext.getBean(SystemParametersServer.class);
        parametersServer.delByKey(SystemIpConfigModel.ID);
        log.info("清除 IP 白名单配置成功");
    }
}
