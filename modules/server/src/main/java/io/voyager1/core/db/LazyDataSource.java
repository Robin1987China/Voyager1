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

import io.voyager1.Voyager1Application;
import io.voyager1.db.DbExtConfig;
import io.voyager1.system.ExtConfigBean;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.jdbc.datasource.AbstractDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * 惰性 DataSource（清洁室实现，取代承继存储框架 {@code StorageServiceFactory} 持有的 DataSource）。
 * <p>
 * 底层 DataSource 在首次 {@link #getConnection()} 时才创建，从而避免与 {@code InitDb} 的初始化顺序耦合。
 * H2 模式：使用与旧实现一致的路径（{@code ExtConfigBean.getPath()/db/Server}）与连接串；
 * 非 H2 模式：使用配置的 {@code voyager1.db.url}。
 */
public class LazyDataSource extends AbstractDataSource {

    private final DbExtConfig dbExtConfig;

    private volatile javax.sql.DataSource delegate;

    public LazyDataSource(DbExtConfig dbExtConfig) {
        this.dbExtConfig = dbExtConfig;
    }

    private javax.sql.DataSource getDelegate() {
        if (delegate == null) {
            synchronized (this) {
                if (delegate == null) {
                    delegate = createDirectDataSource();
                }
            }
        }
        return delegate;
    }

    private javax.sql.DataSource createDirectDataSource() {
        String url = dbExtConfig.getUrl();
        boolean h2Mode = dbExtConfig.getMode() == DbExtConfig.Mode.H2 && (url == null || url.isEmpty());
        if (h2Mode) {
            File dbDir = new File(ExtConfigBean.getPath(), "db");
            dbDir.mkdirs();
            File dbFile = new File(dbDir, Voyager1Application.getAppType().name());
            url = String.format("jdbc:h2:%s;CACHE_SIZE=%d;MODE=MYSQL;LOCK_TIMEOUT=10000;NON_KEYWORDS=USER,VALUE",
                dbFile.getAbsolutePath(), dbExtConfig.getCacheSize().toKilobytes());
        }
        org.springframework.util.Assert.hasText(url, "未配置数据库 url（非 H2 模式需要 voyager1.db.url）");
        DataSourceBuilder<?> builder = DataSourceBuilder.create()
            .url(url)
            .username(dbExtConfig.userName())
            .password(dbExtConfig.userPwd());
        if (h2Mode) {
            builder.driverClassName("org.h2.Driver");
        }
        return builder.build();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return getDelegate().getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return getDelegate().getConnection(username, password);
    }
}
