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

import lombok.extern.slf4j.Slf4j;
import io.voyager1.db.DbExtConfig;
import io.voyager1.plugin.PluginFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;

/**
 * Test application start, then you can use such as service instance to test your methods
 */
@SpringBootTest(classes = {Voyager1ServerApplication.class, PluginFactory.class})
@AutoConfigureMockMvc
@EnableAutoConfiguration
@ContextConfiguration(initializers = PluginFactory.class)
@Slf4j
public class ApplicationStartTest {

    static {
        // 测试数据路径隔离到系统临时目录，避免污染用户主目录（${user.home}/voyager1）
        System.setProperty("VOYAGER1_DEV_PATH", System.getProperty("java.io.tmpdir") + "/voyager1-test-data");
    }

    @Autowired
    protected MockMvc mockMvc;

    @Resource
    protected DbExtConfig dbExtConfig;


    @Test
    public void testApplicationStart() {
        log.info("Voyager1 Server Application started.....");
    }

    @Test
    public void testServerExtConfigBean() {
        //  ServerExtConfigBean serverExtConfigBean = SpringContextHolder.getBean(ServerExtConfigBean.class);
        //System.out.println(serverExtConfigBean);
    }

}
