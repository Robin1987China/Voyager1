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
import io.voyager1.util.SecureUtil;
import io.voyager1.util.HexUtil;

import io.voyager1.util.CollUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.URLEncodeUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.util.HttpRequest;
import io.voyager1.util.HttpResponse;
import io.voyager1.util.HttpUtil;
import io.voyager1.util.Method;
import io.voyager1.util.SystemUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.*;

/**
 * <a href="https://www.volcengine.com/docs/4640/65067">https://www.volcengine.com/docs/4640/65067</a>
 *
 * @since 2024/6/11
 */
@Tag("external")
public class VolcTranslateApiTest {

    private final String region;
    private final String service;
    private final String schema;
    private final String host;
    private final String path;
    private final String ak;
    private final String sk;


    private VolcTranslateApiTest(String region, String service, String schema, String host, String path, String ak, String sk) {
        this.region = region;
        this.service = service;
        this.host = host;
        this.schema = schema;
        this.path = path;
        this.ak = ak;
        this.sk = sk;
    }

    public VolcTranslateApiTest() {
        String volcSk = (System.getenv("VOYAGER1_TRANSLATE_VOLC_SK") != null ? System.getenv("VOYAGER1_TRANSLATE_VOLC_SK") : (System.getProperty("VOYAGER1_TRANSLATE_VOLC_SK") != null ? System.getProperty("VOYAGER1_TRANSLATE_VOLC_SK") : ""));
        String volcAk = (System.getenv("VOYAGER1_TRANSLATE_VOLC_AK") != null ? System.getenv("VOYAGER1_TRANSLATE_VOLC_AK") : (System.getProperty("VOYAGER1_TRANSLATE_VOLC_AK") != null ? System.getProperty("VOYAGER1_TRANSLATE_VOLC_AK") : ""));
        Assertions.assertNotEquals("请配置火山翻译 SecretAccessKey[VOYAGER1_TRANSLATE_VOLC_SK]", volcSk, "");
        Assertions.assertNotEquals("请配置火山翻译 AccessKeyID[VOYAGER1_TRANSLATE_VOLC_AK]", volcAk, "");

        this.region = "cn-north-1";
        this.service = "translate";
        this.host = "translate.volcengineapi.com";
        this.schema = "https";
        this.path = "/";
        this.ak = volcAk;
        this.sk = volcSk;
    }


    @Test
    public void test2() throws Exception {
        VolcTranslateApiTest translateApi = new VolcTranslateApiTest();
        JSONArray translateText = translateApi.translate("zh", "en", new java.util.ArrayList<>(java.util.Arrays.asList("你好", "世界")));
        System.out.println(translateText);
    }

    public JSONArray translate(String source, String target, List<String> textList) throws Exception {


        String action = "TranslateText";
        String version = "2020-06-01";

        HashMap<String, String> queryMap = new HashMap<>(0);

        HashMap<String, Object> query2Map = new HashMap<>(3);
        query2Map.put("SourceLanguage", source);
        query2Map.put("TargetLanguage", target);
        query2Map.put("TextList", textList);

        String jsonStr = JSONObject.toJSONString(query2Map);
        String request = this.doRequest("POST", queryMap, jsonStr.getBytes(), action, version);
        JSONObject jsonObject = JSONObject.parse(request);
        Object error = jsonObject.getByPath("ResponseMetadata.Error");
        if (error != null) {
            throw new IllegalStateException("翻译异常：" + error);
        }
        return jsonObject.getJSONArray("TranslationList");
    }

    private String doRequest(String method, Map<String, String> queryList, byte[] body, String action, String version) throws Exception {
        if (body == null) {
            body = new byte[0];
        }
        String xContentSha256 = DigestUtil.sha256().digestHex(body);

        DateTime dateTime = DateTime.now().setTimeZone(TimeZone.getTimeZone("GMT"));
        String xDate = dateTime.toString("yyyyMMdd'T'HHmmss'Z'");

        String shortXDate = dateTime.toString("yyyyMMdd");

        String contentType = "application/json";

        String signHeader = "host;x-date;x-content-sha256;content-type";


        SortedMap<String, String> realQueryList = new TreeMap<>(queryList);
        realQueryList.put("Action", action);
        realQueryList.put("Version", version);
        StringBuilder querySB = new StringBuilder();
        for (String key : realQueryList.keySet()) {
            querySB.append(signStringEncoder(key)).append("=").append(signStringEncoder(realQueryList.get(key))).append("&");
        }
        querySB.deleteCharAt(querySB.length() - 1);

        String canonicalStringBuilder = method + "\n" + path + "\n" + querySB + "\n" +
            "host:" + host + "\n" +
            "x-date:" + xDate + "\n" +
            "x-content-sha256:" + xContentSha256 + "\n" +
            "content-type:" + contentType + "\n" +
            "\n" +
            signHeader + "\n" +
            xContentSha256;

        //System.out.println(canonicalStringBuilder);

        String hashcanonicalString = DigestUtil.sha256().digestHex(canonicalStringBuilder.getBytes());
        String credentialScope = shortXDate + "/" + region + "/" + service + "/request";
        String signString = "HMAC-SHA256" + "\n" + xDate + "\n" + credentialScope + "\n" + hashcanonicalString;

        byte[] signKey = genSigningSecretKeyV4(sk, shortXDate, region, service);
        String signature = HexUtil.encodeHexStr(hmacSHA256(signKey, signString));

        Method method1 = Method.valueOf(method);
        HttpRequest request = HttpUtil.createRequest(method1, schema + "://" + host + path + "?" + querySB);


        request.header("Host", host);
        request.header("X-Date", xDate);
        request.header("X-Content-Sha256", xContentSha256);
        request.header("Content-Type", contentType);
        request.header("Authorization", "HMAC-SHA256" +
            " Credential=" + ak + "/" + credentialScope +
            ", SignedHeaders=" + signHeader +
            ", Signature=" + signature);
        if (!Objects.equals(method, "GET")) {
            request.body(body);
        }
        return request.thenFunction(HttpResponse::body);
    }

    private String signStringEncoder(String source) {
        return URLEncodeUtil.encodeQuery(source);
    }

    public static byte[] hmacSHA256(byte[] key, String content) throws Exception {
        return SecureUtil.hmacSha256(key).digest(content);
    }

    private byte[] genSigningSecretKeyV4(String secretKey, String date, String region, String service) throws Exception {
        byte[] kDate = hmacSHA256((secretKey).getBytes(), date);
        byte[] kRegion = hmacSHA256(kDate, region);
        byte[] kService = hmacSHA256(kRegion, service);
        return hmacSHA256(kService, "request");
    }
}
