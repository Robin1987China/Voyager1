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

package io.voyager1.service.h2db;

import io.voyager1.ApplicationStartTest;
import io.voyager1.service.dblog.BackupInfoService;
import org.junit.jupiter.api.Test;

import jakarta.annotation.Resource;

/**
 * @since 2022/2/9
 */
public class H2BackupTest extends ApplicationStartTest {

    @Resource
    protected BackupInfoService backupInfoService;

    @Test
    public void testBackup() {
        backupInfoService.executeTask();
    }

    @Test
    public void testAuto() {
        backupInfoService.autoBackup();
    }
}
