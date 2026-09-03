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

package i8n.api;
import io.voyager1.util.HttpStatus;
import io.voyager1.util.ContentType;
import io.voyager1.util.Header;
import io.voyager1.util.HttpUtil;
import io.voyager1.util.HttpRequest;

import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.ResourceUtil;
import io.voyager1.util.MapUtil;
import io.voyager1.util.UrlBuilder;
import io.voyager1.util.RandomUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.UrlBuilder;
import io.voyager1.util.ScriptUtil;
import io.voyager1.util.SystemUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import lombok.Lombok;
import io.voyager1.util.StringUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @since 2024/6/12
 */
@Tag("external")
public class BaiduBceRpcTexttransTest {

    @Test
    public void testTranslate() {
        ArrayList<String> strings = new java.util.ArrayList<>(java.util.Arrays.asList("请输入正确的验证码", "请传入 body 参数", "开始准备项目重启：{} {}"));
        JSONObject jsonObject = this.doTranslate(strings);
        System.out.println(jsonObject);
    }

    private boolean checkHasI18nKey(JSONObject jsonObject) {
        Set<String> keyed = jsonObject.keySet();
        for (String s : keyed) {
            if ((s != null && s.startsWith("i18n."))) {
                // 提前失败 或者翻译失败
                //System.err.println("翻译失败或者提取失败," + s + "=" + jsonObject.get(s));
                return true;
            }
        }
        return false;
    }

    public JSONObject doTranslate(Collection<String> words) {
        while (true) {
            JSONObject jsonObject = this.doTranslate2(words);
            if (checkHasI18nKey(jsonObject)) {
                System.err.println("翻译失败或者提取失败,自动重试," + jsonObject);
            } else {
                return jsonObject;
            }
        }
    }


    private JSONObject doTranslate2(Collection<String> words) {
        String token = this.getToken();
        UrlBuilder urlBuilder = UrlBuilder.of("https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/chat/completions");
        urlBuilder.addQuery("access_token", token);

        HttpRequest httpRequest = HttpUtil.createPost(urlBuilder.build());
        httpRequest.header(Header.CONTENT_TYPE, ContentType.JSON.getValue());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("model", "moonshot-v1-8k");
        jsonObject.put("temperature", 0.3);
        JSONObject message = new JSONObject();
        message.put("role", "user");
        //
        InputStream inputStream = ResourceUtil.getStream("baidubce_translate.txt");
        String string = IoUtil.readUtf8(inputStream);
        //
        JSONObject from = new JSONObject();
        for (String value : words) {
            String key;
            do {
                key = String.format("i18n.%s", RandomUtil.randomStringUpper(6));
            } while (from.containsKey(key));
            from.put(key, value);
        }
        string = String.format(string, java.util.Map.of("REQUEST_STR", from.toString()));
        //System.out.println(string);
        message.put("content", string);
        jsonObject.put("messages", new java.util.ArrayList<>(java.util.Arrays.asList(message)));
        //
        httpRequest.body(jsonObject.toString());
        String result = httpRequest.thenFunction(httpResponse -> {
            String body = httpResponse.body();
            JSONObject jsonObject1 = JSONObject.parseObject(body);
            if (jsonObject1.getIntValue("error_code") != 0) {
                Assertions.fail(jsonObject1.getString("error_msg"));
            }
            return jsonObject1.getString("result");
        });
        String patternString = "(?s)```json\\s*([^`]*?)\\s*```";
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(result);
        //
        JSONObject jsonObject1 = null;
        while (matcher.find()) {
            System.out.println(result);
            String jsonContent = matcher.group(1);
            JSONValidator.Type type = StringUtil.validatorJson(jsonContent);
            if (type == JSONValidator.Type.Object) {
                jsonObject1 = JSONObject.parseObject(jsonContent);
                if (!this.checkHasI18nKey(jsonObject1)) {
                    return jsonObject1;
                }
            } else {
                try {
                    String eval = (String) ScriptUtil.getJavaScriptEngine().eval(jsonContent);
                    type = StringUtil.validatorJson(eval);
                    if (type == JSONValidator.Type.Object) {
                        jsonObject1 = JSONObject.parseObject(eval);
                        if (!this.checkHasI18nKey(jsonObject1)) {
                            return jsonObject1;
                        }
                    }
                } catch (Exception e) {
                    throw Lombok.sneakyThrow(e);
                }
            }
        }
        Assertions.assertNotNull(jsonObject1, "翻译失败或者提取失败");
        return jsonObject1;
    }

    private String getToken() {
        File file = new File("");
        String absolutePath = FileUtil.getAbsolutePath(file);
        File tokenCache = FileUtil.file(absolutePath, ".baidubce.token");
        if (tokenCache.exists()) {
            JSONObject cacheData = JSONObject.parseObject(FileUtil.readUtf8String(tokenCache));
            int expiresIn = cacheData.getIntValue("expires_in");
            if (System.currentTimeMillis() / 1000L < expiresIn) {
                //System.out.println("token 缓存有效，直接使用");
                return cacheData.getString("access_token");
            }
        }
        JSONObject cacheData = this.doTokenByApi(tokenCache);
        return cacheData.getString("access_token");
    }

    /**
     * <a href="https://cloud.baidu.com/doc/WENXINWORKSHOP/s/7lpch74jm">https://cloud.baidu.com/doc/WENXINWORKSHOP/s/7lpch74jm</a>
     *
     * @return token
     */
    private JSONObject doTokenByApi(File file) {
        String bceCi = (System.getenv("VOYAGER1_TRANSLATE_BAIDUBCE_CI") != null ? System.getenv("VOYAGER1_TRANSLATE_BAIDUBCE_CI") : (System.getProperty("VOYAGER1_TRANSLATE_BAIDUBCE_CI") != null ? System.getProperty("VOYAGER1_TRANSLATE_BAIDUBCE_CI") : ""));
        String bceCs = (System.getenv("VOYAGER1_TRANSLATE_BAIDUBCE_CS") != null ? System.getenv("VOYAGER1_TRANSLATE_BAIDUBCE_CS") : (System.getProperty("VOYAGER1_TRANSLATE_BAIDUBCE_CS") != null ? System.getProperty("VOYAGER1_TRANSLATE_BAIDUBCE_CS") : ""));
        Assertions.assertNotEquals(bceCi, "", "请配置百度千帆大模型 client_id[VOYAGER1_TRANSLATE_BAIDUBCE_CI]");
        Assertions.assertNotEquals(bceCs, "", "请配置百度千帆大模型 client_secret[VOYAGER1_TRANSLATE_BAIDUBCE_CS]");

        HttpRequest httpRequest = HttpUtil.createPost("https://aip.baidubce.com/oauth/2.0/token");
        httpRequest.form("grant_type", "client_credentials")
            .form("client_id", bceCi)
            .form("client_secret", bceCs);
        httpRequest.header(Header.CONTENT_TYPE, ContentType.JSON.getValue());
        httpRequest.header(Header.ACCEPT, ContentType.JSON.getValue());
        JSONObject json = httpRequest.thenFunction(httpResponse -> {
            int status = httpResponse.getStatus();
            String body = httpResponse.body();
            Assertions.assertEquals(HttpStatus.HTTP_OK, status, "token 生成异常," + body);
            return JSONObject.parse(body);
        });
        String token = json.getString("access_token");
        int expiresIn = json.getIntValue("expires_in");
        System.out.println("获取最新的 token");
        JSONObject cacheData = new JSONObject();
        cacheData.put("access_token", token);
        cacheData.put("expires_in", expiresIn + System.currentTimeMillis() / 1000L);
        //
        FileUtil.writeUtf8String(cacheData.toString(), file);
        return cacheData;
    }

    @Test
    public void doToken() {
        System.out.println(this.getToken());
    }
}
