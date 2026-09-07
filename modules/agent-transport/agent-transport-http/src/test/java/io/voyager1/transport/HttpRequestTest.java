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

package io.voyager1.transport;

import com.sun.net.httpserver.HttpServer;
import io.voyager1.util.BytesResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link HttpRequest} 表单编码测试：普通表单走 form-urlencoded，含文件内容走 multipart/form-data。
 *
 * @since 2026/9/7
 */
public class HttpRequestTest {

    private static class Captured {
        String contentType;
        byte[] body;
    }

    private Captured send(Map<String, Object> form) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        AtomicReference<Captured> ref = new AtomicReference<>();
        server.createContext("/", exchange -> {
            Captured c = new Captured();
            c.contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            c.body = exchange.getRequestBody().readAllBytes();
            ref.set(c);
            exchange.sendResponseHeaders(200, 0);
            exchange.close();
        });
        server.start();
        try {
            HttpRequest request = HttpRequest.of("http://127.0.0.1:" + server.getAddress().getPort() + "/");
            request.form(form);
            request.execute();
        } finally {
            server.stop(0);
        }
        return ref.get();
    }

    @Test
    public void testPlainFormIsUrlEncoded() throws Exception {
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("id", "proj1");
        form.put("name", "项目");
        Captured c = send(form);

        Assertions.assertTrue(c.contentType != null && c.contentType.startsWith("application/x-www-form-urlencoded"),
            "普通表单 Content-Type 应为 form-urlencoded, 实际: " + c.contentType);
        String body = new String(c.body, StandardCharsets.UTF_8);
        Assertions.assertTrue(body.contains("id=proj1"), "表单体应包含 id=proj1: " + body);
    }

    @Test
    public void testFileFormIsMultipart() throws Exception {
        byte[] fileBytes = "jar-bytes-content".getBytes(StandardCharsets.UTF_8);
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("sliceId", "abc123");
        form.put("totalSlice", 1);
        form.put("file", new BytesResource(fileBytes, "app.jar.0"));
        Captured c = send(form);

        Assertions.assertTrue(c.contentType != null && c.contentType.startsWith("multipart/form-data; boundary="),
            "含文件表单 Content-Type 应为 multipart/form-data, 实际: " + c.contentType);
        String body = new String(c.body, StandardCharsets.ISO_8859_1);
        Assertions.assertTrue(body.contains("name=\"file\""), "multipart 应包含 file 字段: " + body);
        Assertions.assertTrue(body.contains("filename=\"app.jar.0\""), "multipart 应保留文件名 app.jar.0: " + body);
        Assertions.assertTrue(body.contains("jar-bytes-content"), "multipart 应包含文件字节内容: " + body);
        Assertions.assertTrue(body.contains("sliceId"), "multipart 应包含普通文本字段: " + body);
    }
}
