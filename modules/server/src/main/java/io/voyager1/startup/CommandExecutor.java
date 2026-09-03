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

package io.voyager1.startup;

import io.voyager1.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;

/**
 * 命令执行器
 *
 * @since 2024/12/08
 */
@Slf4j
public class CommandExecutor {
    private final ApplicationContext applicationContext;
    private final String[] args;

    public CommandExecutor(ApplicationContext applicationContext, String[] args) {
        this.applicationContext = applicationContext;
        this.args = args;
    }

    public void execute() {
        for (StartupCommand command : applicationContext.getBeansOfType(StartupCommand.class).values()) {
            String commandFlag = command.getCommandFlag();
            if (ArrayUtil.containsIgnoreCase(args, commandFlag)) {
                try {
                    command.execute(applicationContext);
                    log.info("Successfully executed command: {}", commandFlag);
                } catch (Exception e) {
                    log.error("Failed to execute command: {}", commandFlag, e);
                }
            }
        }
    }
}
