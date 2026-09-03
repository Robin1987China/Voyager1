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

package io.voyager1;

import io.voyager1.util.BetweenFormatter;
import io.voyager1.core.AppTypeBinding;
import io.voyager1.core.AppType;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.startup.CommandExecutor;
import io.voyager1.util.StringUtil;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.ApplicationContext;

/**
 * voyager1 启动类
 *
 * @since 2017/9/14
 */
@SpringBootApplication(
    scanBasePackages = {"io.voyager1"},
    // Phase 1: 旧存储（StorageServiceFactory）在启动阶段才初始化 DataSource，
    // Spring Boot 的 JPA/Flyway 自动装配会过早连接 DataSource 导致启动失败，
    // 故先排除，待手动编排（在 InitDb 之后初始化）再逐步启用。
    exclude = {
        org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration.class
    }
)
@ServletComponentScan(basePackages = {"io.voyager1"})
@Slf4j
@AppTypeBinding(AppType.Server)
public class Voyager1ServerApplication {

    /**
     * 启动执行
     * <p>
     * --rest:ip_config 重置 IP 授权配置
     * <p>
     * --rest:load_init_db 重新加载数据库初始化操作
     * <p>
     * --rest:super_user_pwd 重置超级管理员密码
     * <p>
     * --recover:h2db 当 h2 数据出现奔溃无法启动需要执行恢复逻辑
     * <p>
     * --close:super_user_mfa 关闭超级管理员 mfa
     * <p>
     * --backup-h2 备份数据库
     * <p>
     * --import-h2-sql=/xxxx.sql 导入指定文件 sql
     * <p>
     * --replace-import-h2-sql=/xxxx.sql 替换导入指定文件 sql（会删除掉已经存的数据）
     * <p>
     * --transform-sql 转换 sql 内容(低版本兼容高版本),仅在导入 sql 文件时候生效：--import-h2-sql=/xxxx.sql、--replace-import-h2-sql=/xxxx.sql
     * <p>
     * --h2-migrate-mysql --h2-user=voyager1 --h2-pass=voyager1  将 h2 数据库迁移到 mysql
     * <p>
     * --h2-migrate-postgresql --h2-user=voyager1 --h2-pass=voyager1 将 h2 数据库迁移到 postgresql
     * <p>
     * --h2-migrate-mariadb --h2-user=voyager1 --h2-pass=voyager1 将 h2 数据库迁移到 mariadb
     *
     * @param args 参数
     * @throws Exception 异常
     */
    public static void main(String[] args) throws Exception {
        long time = System.currentTimeMillis();
        //
        SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(Voyager1ServerApplication.class);
        springApplicationBuilder.bannerMode(Banner.Mode.LOG);
        ApplicationContext applicationContext = springApplicationBuilder.run(args);

        // 使用命令执行器处理启动参数
        CommandExecutor commandExecutor = new CommandExecutor(applicationContext, args);
        commandExecutor.execute();

        //
        log.info("启动耗时：{}", StringUtil.formatBetween(System.currentTimeMillis() - time, BetweenFormatter.Level.MILLISECOND));
    }
}
