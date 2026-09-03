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

package io.voyager1.core.jpa;

/**
 * 数据服务接口（清洁室定义，取代承继存储框架被外部消费的「类型契约」）。
 * <p>
 * 权限/日志/通信等框架消费者原本以 {@code BaseDbService} 作为类型强转调用 {@code getData}/{@code getByKey}。
 * 迁移到 JPA 后，服务不再继承这些基类，改由本接口统一对外最小契约；旧 {@code BaseDbService} 也实现本接口，
 * 实现「绞杀者」共存。
 *
 * @param <T> 数据模型类型
 */
public interface DataService<T> {

    T getByKey(String id);

    default T getByKey(String id, boolean fill) {
        return getByKey(id);
    }

    default T getByKey(String id, io.voyager1.model.user.UserModel userModel) {
        return getByKey(id);
    }

    default T getData(String nodeId, String dataId) {
        return getByKey(dataId);
    }
}
