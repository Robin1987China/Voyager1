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
 * Flyway 手动编排（Phase 1）
 * <p>
 * 由于 Spring Boot 的 Flyway 自动装配会在 {@code InitDb} 初始化存储之前过早连库，
 * 这里改为 {@link ILoadEvent}：在 {@code InitDb}（{@code HIGHEST_PRECEDENCE}）创建完表结构之后、
 * 在 {@code DataInitEvent}（{@code HIGHEST_PRECEDENCE + 2}，触发 {@code statusRecover} 等业务初始化）之前执行。
 * <p>
 * 顺序保证：InitDb 用旧表名建表 → Flyway 执行 V2+ 重命名 → 业务层用新表名读写。
 * <p>
 * 采用 {@code baselineOnMigrate}：对已存在的 schema 只做基线标记（版本 1），不重建表，
 * 后续新表/改表通过 {@code db/migration/V2__xxx.sql} 增量演进。
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
        // 介于 InitDb（HIGHEST_PRECEDENCE）与 DataInitEvent（HIGHEST_PRECEDENCE + 2）之间
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) {
        Flyway flyway = Flyway.configure()
            .dataSource(dataSource)
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .baselineDescription("fresh-or-existing-schema")
            .locations("classpath:db/migration")
            .load();
        flyway.migrate();
        log.info("Flyway 迁移完成（baseline 已建立，后续通过 V2+ 增量演进）");
    }
}
