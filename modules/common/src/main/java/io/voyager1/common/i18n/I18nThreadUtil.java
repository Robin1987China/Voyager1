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

package io.voyager1.common.i18n;

import io.voyager1.util.ThreadUtil;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * @since 2024/6/15
 */
public class I18nThreadUtil {

    /**
     * 线程执行（获取父级线程语言）
     *
     * @param runnable runnable
     */
    public static void execute(Runnable runnable) {
        String language = I18nMessageUtil.tryGetLanguage();
        ThreadUtil.execute(() -> {
            try {
                I18nMessageUtil.setLanguage(language);
                runnable.run();
            } finally {
                I18nMessageUtil.clearLanguage();
            }
        });
    }

    /**
     * 执行有返回值的异步方法<br>
     * Future代表一个异步执行的操作，通过get()方法可以获得操作的结果，如果异步操作还没有完成，则，get()会使当前线程阻塞
     *
     * @param <T>  回调对象类型
     * @param task {@link Callable}
     * @return Future
     */
    public static <T> Future<T> execAsync(Callable<T> task) {
        String language = I18nMessageUtil.tryGetLanguage();
        return ThreadUtil.execAsync(() -> {
            try {
                I18nMessageUtil.setLanguage(language);
                return task.call();
            } finally {
                I18nMessageUtil.clearLanguage();
            }
        });
    }

    /**
     * 执行有返回值的异步方法<br>
     * Future代表一个异步执行的操作，通过get()方法可以获得操作的结果，如果异步操作还没有完成，则，get()会使当前线程阻塞
     *
     * @param runnable 可运行对象
     * @return {@link Future}
     * @since 3.0.5
     */
    public static Future<?> execAsync(Runnable runnable) {
        String language = I18nMessageUtil.tryGetLanguage();
        return ThreadUtil.execAsync(() -> {
            try {
                I18nMessageUtil.setLanguage(language);
                runnable.run();
            } finally {
                I18nMessageUtil.clearLanguage();
            }
        });
    }

}
