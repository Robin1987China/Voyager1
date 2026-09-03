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

package io.voyager1.build;

import io.voyager1.util.ExecutorBuilder;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.BuildExtConfig;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

@Service
@Slf4j
public class BuildExecutorPoolService {
    /**
     * 构建线程池
     */
    private volatile ThreadPoolExecutor threadPoolExecutor;
    private final BuildExtConfig buildExtConfig;

    public BuildExecutorPoolService(BuildExtConfig buildExtConfig) {
        this.buildExtConfig = buildExtConfig;
    }

    public ThreadPoolExecutor getThreadPoolExecutor() {
        this.initPool();
        return threadPoolExecutor;
    }

    public void execute(Runnable command) {
        this.initPool();
        threadPoolExecutor.execute(command);
    }

    /**
     * 创建构建线程池
     */
    private void initPool() {
        if (threadPoolExecutor == null) {
            synchronized (BuildExecutorPoolService.class) {
                if (threadPoolExecutor == null) {
                    ExecutorBuilder executorBuilder = ExecutorBuilder.create();
                    int poolSize = buildExtConfig.getPoolSize();
                    if (poolSize > 0) {
                        executorBuilder.setCorePoolSize(poolSize).setMaxPoolSize(poolSize);
                    }
                    executorBuilder.useArrayBlockingQueue(Math.max(buildExtConfig.getPoolWaitQueue(), 1));
                    executorBuilder.setHandler(new ThreadPoolExecutor.DiscardPolicy() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                            if (r instanceof BuildExecuteManage) {
                                // 取消任务
                                BuildExecuteManage buildExecuteManage = (BuildExecuteManage) r;
                                buildExecuteManage.rejectedExecution();
                            } else {
                                log.warn("构建线程池拒绝了未知任务：{}", r.getClass());
                            }
                        }
                    });
                    threadPoolExecutor = executorBuilder.build();
                    Voyager1Application.register("build", threadPoolExecutor);
                }
            }
        }
    }
}
