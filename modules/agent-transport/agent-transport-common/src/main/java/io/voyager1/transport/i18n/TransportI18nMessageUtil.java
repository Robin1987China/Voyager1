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

package io.voyager1.transport.i18n;

import io.voyager1.util.Singleton;

/**
 * @since 2024/6/11
 */
public class TransportI18nMessageUtil {

    /**
     * 获得单例的 TransportServer
     *
     * @return 单例的 TransportServer
     */
    public static String get(String key) {
        return Singleton.get(II18nMessageUtil.class.getName(), TransportI18nMessageUtil::doCreate).get(key);
    }


    /**
     * 根据用户引入的拼音引擎jar，自动创建对应的拼音引擎对象<br>
     * 推荐创建的引擎单例使用，此方法每次调用会返回新的引擎
     *
     * @return {@code EngineFactory}
     */
    private static II18nMessageUtil doCreate() {
        final II18nMessageUtil engine = java.util.ServiceLoader.load(II18nMessageUtil.class).findFirst().orElse(null);
        if (null != engine) {
            return engine;
        }

        throw new RuntimeException("No voyager1 IMessageUtil jar found ! Please add one of it to your project !");
    }
}
