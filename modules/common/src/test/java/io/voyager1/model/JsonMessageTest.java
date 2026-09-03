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

package io.voyager1.model;


import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * ApiResult 序列化与语义契约测试
 *
 * @since 2026/8/3
 */
public class JsonMessageTest {

    @Test
    public void testSuccessAndFail() {
        ApiResult<String> success = new ApiResult<>(200, "ok");
        Assertions.assertTrue(success.success());
        Assertions.assertFalse(success.fail());

        ApiResult<String> fail = new ApiResult<>(405, "error");
        Assertions.assertFalse(fail.success());
        Assertions.assertTrue(fail.fail());
    }

    @Test
    public void testGetData() {
        ApiResult<String> message = new ApiResult<>(200, "ok", "hello");
        Assertions.assertEquals("hello", message.getData());
        Assertions.assertEquals("hello", message.getData(String.class));
    }

    @Test
    public void testLongSerializeToString() {
        ApiResult<Long> message = new ApiResult<>(200, "ok", 1234567890123456789L);
        JSONObject jsonObject = message.toJson();
        // long 类型自动转字符串
        Assertions.assertEquals("1234567890123456789", jsonObject.getString("data"));
    }

    @Test
    public void testToString() {
        ApiResult<String> message = new ApiResult<>(200, "ok", "data");
        String json = message.toString();
        Assertions.assertTrue(json.contains("\"code\":200"));
        Assertions.assertTrue(json.contains("\"msg\":\"ok\""));
        Assertions.assertTrue(json.contains("\"data\":\"data\""));
    }

    @Test
    public void testStaticToJson() {
        JSONObject jsonObject = ApiResult.toJson(200, "msg", 1);
        Assertions.assertEquals(200, jsonObject.getIntValue("code"));
        Assertions.assertEquals("msg", jsonObject.getString("msg"));
        Assertions.assertEquals(1, jsonObject.getIntValue("data"));
    }
}
