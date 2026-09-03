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

package io.voyager1.db;

import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * 数据库配置
 */
@Configuration
@ConfigurationProperties(prefix = "voyager1.db")
@Data
public class DbExtConfig implements InitializingBean {

    /**
     * 默认的账号或者密码
     */
    public static final String DEFAULT_USER_OR_AUTHORIZATION = "voyager1";

    /**
     * SQL backup default directory name
     * 数据库备份默认目录名称
     */
    public static final String BACKUP_DIRECTORY_NAME = "backup";
    /**
     * 备份 SQL 文件 后缀
     */
    public static final String SQL_FILE_SUFFIX = ".sql";

    /**
     * 日志记录最大条数
     */
    private Integer logStorageCount = 10000;
    /**
     * 数据库默认
     */
    private Mode mode = Mode.H2;
    /**
     * 数据库 url
     */
    private String url;
    /**
     * 数据库账号、默认为 voyager1
     */
    private String userName;

    /**
     * 数据库密码、默认为 voyager1
     */
    private String userPwd;

    /**
     * 缓存大小
     * <p>
     * <a href="http://www.h2database.com/html/features.html#cache_settings">http://www.h2database.com/html/features.html#cache_settings</a>
     */
    private DataSize cacheSize = DataSize.ofMegabytes(10);

    /**
     * 自动全量备份数据库间隔天数 小于等于 0，不自动备份
     */
    private Integer autoBackupIntervalDay = 1;

    /**
     * 自动备份保留天数 小于等于 0，不自动删除自动备份数据
     */
    private int autoBackupReserveDay = 5;

    private int maxActive = 100;

    private int initialSize = 10;

    private int maxWait = 10;

    private int minIdle = 1;
    /**
     * 是否显示 SQL（对应 showSql 配置项）
     */
    private Boolean showSql = false;

    public String userName() {
        return this.userName == null || this.userName.isEmpty() ? DbExtConfig.DEFAULT_USER_OR_AUTHORIZATION : this.userName;
    }

    public String userPwd() {
        return this.userPwd == null || this.userPwd.isEmpty() ? DbExtConfig.DEFAULT_USER_OR_AUTHORIZATION : this.userPwd;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 存储工厂已移除（Flyway/直连 DataSource），无 mode 全局状态需要同步
    }

    public enum Mode {
        /**
         * h2
         */
        H2,
        /**
         * mysql
         */
        MYSQL,
        /**
         * postgresql
         */
        POSTGRESQL,
        /**
         * Mariadb
         */
        MARIADB
    }
}
