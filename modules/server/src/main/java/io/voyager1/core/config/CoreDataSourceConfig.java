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

package io.voyager1.core.config;

import io.voyager1.core.db.LazyDataSource;
import io.voyager1.db.DbExtConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * 新持久层 DataSource 配置（Phase 1）
 * <p>
 * 直接创建 DataSource（H2 模式使用与旧实现一致的路径与连接串），供 Spring Data JPA / Flyway 使用。
 * 通过 {@link LazyDataSource} 延迟创建，避免与 InitDb 初始化顺序耦合。
 */
@Configuration
public class CoreDataSourceConfig {

    @Bean
    public DataSource dataSource(DbExtConfig dbExtConfig) {
        return new LazyDataSource(dbExtConfig);
    }
}
