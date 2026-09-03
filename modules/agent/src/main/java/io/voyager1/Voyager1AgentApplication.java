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

package io.voyager1;

import io.voyager1.util.BetweenFormatter;
import io.voyager1.util.ArrayUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.core.AppTypeBinding;
import io.voyager1.core.AppType;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.system.AgentStartInit;
import io.voyager1.util.StringUtil;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * voyager1 启动类
 *
 * @since 2017/9/14.
 */
@SpringBootApplication(scanBasePackages = {"io.voyager1"})
@ServletComponentScan(basePackages = {"io.voyager1"})
@Slf4j
@AppTypeBinding(AppType.Agent)
public class Voyager1AgentApplication {

    /**
     * 启动执行
     *
     * @param args 参数
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        long time = System.currentTimeMillis();
        SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(Voyager1AgentApplication.class);
        springApplicationBuilder.bannerMode(Banner.Mode.LOG);
        springApplicationBuilder.run(args);
        // 自动向服务端推送
        autoPushToServer(args);
        log.info("启动耗时：{}", StringUtil.formatBetween(System.currentTimeMillis() - time, BetweenFormatter.Level.MILLISECOND));
    }

    /**
     * 自动推送 插件端信息到服务端
     *
     * @param args 参数
     */
    private static void autoPushToServer(String[] args) {
        int i = ArrayUtil.indexOf(args, ServerOpenApi.PUSH_NODE_KEY);
        if (i == ArrayUtil.INDEX_NOT_FOUND) {
            return;
        }
        String arg = ArrayUtil.get(args, i + 1);
        if ((arg == null || arg.isEmpty())) {
            log.error("未找到自动推送至服务器的 URL");
            return;
        }
        try {
            AgentStartInit autoRegSeverNode = SpringContextHolder.getBean(AgentStartInit.class);
            autoRegSeverNode.autoPushToServer(arg);
        } catch (Exception e) {
            log.error("向服务端推送注册失败 {}", arg, e);
        }
    }

}
