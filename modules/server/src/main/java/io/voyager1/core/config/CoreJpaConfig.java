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

import jakarta.persistence.EntityManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 新持久层 JPA 配置（Phase 1）
 * <p>
 * 手动编排 EntityManagerFactory + TransactionManager（因 JPA 自动装配已排除）。
 * 关键点：显式指定方言 + 关闭 JDBC 元数据探测，避免 EMF 在 InitDb 初始化存储之前过早连库。
 */
@Configuration
@EnableJpaRepositories(basePackages = "io.voyager1.core.repository")
public class CoreJpaConfig {

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("io.voyager1.core.entity");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Properties props = new Properties();
        // 显式方言（默认 H2，Phase 3 再按 mode 动态选择）
        props.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        // 不自动建表/校验（schema 由 Flyway/旧 InitDb 管理）
        props.setProperty("hibernate.hbm2ddl.auto", "none");
        // 避免 SessionFactory 构建阶段探测 JDBC 元数据（防过早连库）
        props.setProperty("hibernate.temp.use_jdbc_metadata_defaults", "false");
        // 列名保持与旧 CSV 建表一致（camelCase 原样，不做下划线转换）
        props.setProperty("hibernate.physical_naming_strategy", "org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl");
        em.setJpaProperties(props);
        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
