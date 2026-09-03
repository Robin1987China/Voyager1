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

package io.voyager1.func.assets;

import io.voyager1.util.ExecutorBuilder;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.AssetsConfig;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

@Service
@Slf4j
public class AssetsExecutorPoolService {
    /**
     * 监控线程池
     */
    private volatile ThreadPoolExecutor threadPoolExecutor;

    private final AssetsConfig assetsConfig;

    public AssetsExecutorPoolService(AssetsConfig assetsConfig) {
        this.assetsConfig = assetsConfig;
    }

    public void execute(Runnable command) {
        this.createPool();
        threadPoolExecutor.execute(command);
    }

    private void createPool() {
        if (threadPoolExecutor == null) {
            synchronized (AssetsExecutorPoolService.class) {
                if (threadPoolExecutor == null) {
                    ExecutorBuilder executorBuilder = ExecutorBuilder.create();
                    int poolSize = assetsConfig.getMonitorPoolSize();
                    if (poolSize <= 0) {
                        // 获取 CPU 核心数
                        poolSize = Runtime.getRuntime().availableProcessors();
                    }
                    executorBuilder.setCorePoolSize(poolSize).setMaxPoolSize(poolSize);
                    executorBuilder.useArrayBlockingQueue(Math.max(assetsConfig.getMonitorPoolWaitQueue(), 1));
                    executorBuilder.setHandler(new ThreadPoolExecutor.DiscardPolicy() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                            log.warn("资产监控线程池拒绝了任务：{}", r.getClass());
                        }
                    });
                    threadPoolExecutor = executorBuilder.build();
                    Voyager1Application.register("assets-monitor", threadPoolExecutor);
                }
            }
        }
    }
}
