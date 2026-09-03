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

package io.voyager1.core.api;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 行为契约（golden）测试：冻结 API 响应体的对外契约（{@code {code, msg, data}} + Long 转字符串）。
 */
public class ApiResultGoldenTest {

    private Map<String, Object> sampleData() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "node-1");
        map.put("id", 123456789012345L); // Long，应序列化为字符串
        map.put("ok", true);
        return map;
    }

    @Test
    public void testSuccessShape() {
        String json = new ApiResult<>(200, "ok", sampleData()).toString();
        assertEquals(
            "{\"code\":200,\"data\":{\"name\":\"node-1\",\"id\":\"123456789012345\",\"ok\":true},\"msg\":\"ok\"}",
            json);
    }

    @Test
    public void testFailShape() {
        String json = ApiResult.getString(405, "参数错误");
        assertEquals("{\"code\":405,\"msg\":\"参数错误\"}", json);
    }

    @Test
    public void testStaticFactoryShape() {
        String json = ApiResult.success("ok", sampleData()).toString();
        assertEquals(
            "{\"code\":200,\"data\":{\"name\":\"node-1\",\"id\":\"123456789012345\",\"ok\":true},\"msg\":\"ok\"}",
            json);
    }

    @Test
    public void testLongSerializedAsString() {
        String json = ApiResult.success("ok", sampleData()).toString();
        assertTrue(json.contains("\"id\":\"123456789012345\""), "Long 应序列化为字符串，实际: " + json);
    }

    @Test
    public void testSuccessFailFlags() {
        ApiResult<Object> ok = ApiResult.success("ok");
        ApiResult<Object> err = ApiResult.fail("err");
        assertTrue(ok.success());
        assertFalse(ok.fail());
        assertTrue(err.fail());
        assertFalse(err.success());
    }
}
