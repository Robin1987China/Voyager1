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

package io.voyager1.core.db;

import io.voyager1.db.DbExtConfig;
import java.lang.annotation.*;

/**
 * 数据库表名
 *
 * @since 2021/8/13
 */
@Documented
@Target({ElementType.TYPE})
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface TableName {

    /**
     * 表名
     *
     * @return tableName
     */
    String value();

    /**
     * 表描述
     *
     * @return 描述
     */
    String nameKey();

    /**
     * 数据库默认
     *
     * @return 默认所有模式
     */
    DbExtConfig.Mode[] modes() default {};

    /**
     * 父级
     *
     * @return class
     */
    Class<?> parents() default Void.class;

    /**
     * 绑定关系
     * <p>
     * 1 严格模式，需要手动删除
     * 2 删除工作空间时自动删除
     * 3 父级数据为空时可以自动删除
     *
     * @return 数据绑定关系
     */
    int workspaceBind() default 1;
}
