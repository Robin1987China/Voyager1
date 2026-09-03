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

import io.voyager1.util.HttpRequest;
import io.voyager1.util.HttpUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.ServerOpenApi;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * @since 2021/12/14
 */
@Tag("external")
public class BuildTriggerTest {

    @Test
    public void test() {
        // 8cf594526db74f0eb79cac6da141c655/219a4009a0a68173d8d643d237f2ca8ad797d41dc5bcfceb83da4f4f1d1dbe933a1
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", "8cf594526db74f0eb79cac6da141c655");
        jsonObject.put("token", "219a4009a0a68173d8d643d237f2ca8ad797d41dc5bcfceb83da4f4f1d1dbe933a1");
        //
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(jsonObject);
        //
        HttpRequest post = HttpUtil.createPost("http://127.0.0.1:2122/" + ServerOpenApi.BUILD_TRIGGER_BUILD_BATCH);
        post.body(jsonArray.toString(), MediaType.APPLICATION_JSON_VALUE);
        String body = post.execute().body();
        System.out.println(body);
    }
}
