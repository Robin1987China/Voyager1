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

package io.voyager1.build;

import io.voyager1.ApplicationStartTest;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.enums.BuildStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 构建执行服务状态检查逻辑测试（Spring 上下文）
 *
 * @since 2026/8/3
 */
public class BuildExecuteServiceCheckStatusTest extends ApplicationStartTest {

    @Autowired
    private BuildExecuteService buildExecuteService;

    @Test
    public void testCheckStatusNullModel() {
        Assertions.assertNotNull(buildExecuteService.checkStatus(null));
    }

    @Test
    public void testCheckStatusNullStatus() {
        BuildInfoModel model = new BuildInfoModel();
        model.setStatus(null);
        Assertions.assertNull(buildExecuteService.checkStatus(model));
    }

    @Test
    public void testCheckStatusProgress() {
        BuildInfoModel model = new BuildInfoModel();
        model.setName("demo-build");
        model.setStatus(BuildStatus.Ing.getCode());
        String message = buildExecuteService.checkStatus(model);
        Assertions.assertNotNull(message);
        Assertions.assertTrue(message.contains("demo-build"));
    }

    @Test
    public void testCheckStatusFinished() {
        BuildInfoModel model = new BuildInfoModel();
        model.setStatus(BuildStatus.Success.getCode());
        Assertions.assertNull(buildExecuteService.checkStatus(model));
    }
}
