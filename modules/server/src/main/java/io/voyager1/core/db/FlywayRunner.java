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

import io.voyager1.common.ILoadEvent;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Flyway 手动编排（schema 唯一权威）
 * <p>
 * 应用未启用 Spring Boot 的 Flyway 自动装配（{@code FlywayAutoConfiguration} 被排除），
 * 这里通过 {@link ILoadEvent} 在 Bean 加载完成后、业务初始化（{@code DataInitEvent}，{@code HIGHEST_PRECEDENCE + 2}）
 * 之前执行 {@code db/migration/V1__init.sql}，确保业务读取数据时表结构已就绪。
 * <p>
 * 未发布项目采用「单基线」策略：只保留一个 {@code V1__init.sql}（全量建表 + 索引，表名/列名均为 Voyager1 新命名）。
 * 后续 schema 演进一律新增 {@code V2__xxx}、{@code V3__xxx} ...，不再修改已应用的 V1。
 */
@Component
public class FlywayRunner implements ILoadEvent {

    private static final Logger log = LoggerFactory.getLogger(FlywayRunner.class);

    private final DataSource dataSource;

    public FlywayRunner(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public int getOrder() {
        // 早于 DataInitEvent（HIGHEST_PRECEDENCE + 2），保证先建表、后做业务初始化
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load();
        flyway.migrate();
        log.info("Flyway 迁移完成（V1 基线 + 增量迁移，当前已应用至最新版本）");
    }
}
