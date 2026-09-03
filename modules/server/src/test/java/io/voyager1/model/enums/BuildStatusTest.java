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

package io.voyager1.model.enums;

import io.voyager1.model.BaseEnum;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 构建状态机枚举契约测试
 *
 * @since 2026/8/3
 */
public class BuildStatusTest {

    @Test
    public void testEnumValues() {
        Assertions.assertEquals(11, BuildStatus.values().length);
        Assertions.assertEquals(0, BuildStatus.No.getCode());
        Assertions.assertEquals(1, BuildStatus.Ing.getCode());
        Assertions.assertEquals(9, BuildStatus.WaitExec.getCode());
        Assertions.assertEquals(10, BuildStatus.AbnormalShutdown.getCode());
    }

    @Test
    public void testProgressFlag() {
        Assertions.assertTrue(BuildStatus.Ing.isProgress());
        Assertions.assertTrue(BuildStatus.PubIng.isProgress());
        Assertions.assertTrue(BuildStatus.WaitExec.isProgress());
        Assertions.assertFalse(BuildStatus.No.isProgress());
        Assertions.assertFalse(BuildStatus.Success.isProgress());
        Assertions.assertFalse(BuildStatus.Error.isProgress());
        Assertions.assertFalse(BuildStatus.Cancel.isProgress());
    }

    @Test
    public void testGetEnumByCode() {
        Assertions.assertEquals(BuildStatus.Ing, BaseEnum.getEnum(BuildStatus.class, 1));
        Assertions.assertEquals(BuildStatus.Success, BaseEnum.getEnum(BuildStatus.class, 2));
        Assertions.assertEquals(BuildStatus.AbnormalShutdown, BaseEnum.getEnum(BuildStatus.class, 10));
        Assertions.assertNull(BaseEnum.getEnum(BuildStatus.class, 999));
    }

    @Test
    public void testCodeUniqueness() {
        long distinctCount = java.util.Arrays.stream(BuildStatus.values())
            .map(BuildStatus::getCode)
            .distinct()
            .count();
        Assertions.assertEquals(BuildStatus.values().length, distinctCount);
    }
}
