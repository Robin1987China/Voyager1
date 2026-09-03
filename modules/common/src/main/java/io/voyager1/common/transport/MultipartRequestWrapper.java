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

package io.voyager1.common.transport;

import io.voyager1.util.ArrayUtil;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.encrypt.Encryptor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @since 2023/3/13
 */
@Slf4j
public class MultipartRequestWrapper extends StandardMultipartHttpServletRequest {

    private final Map<String, String[]> parameterMap;

    public MultipartRequestWrapper(HttpServletRequest request, Encryptor encryptor) {
        super(request);
        Map<String, String[]> parameterMap = super.getParameterMap();
        Map<String, String[]> decryptMap = new HashMap<>();
        try {
            for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                String key = entry.getKey();
                String[] value = entry.getValue();
                for (int i = 0; i < value.length; i++) {
                    value[i] = encryptor.decrypt(value[i]);
                }
                decryptMap.put(encryptor.decrypt(key), value);
            }
        } catch (Exception e) {
            log.error("解密失败", e);
        }
        this.parameterMap = decryptMap;
        // 处理文件名
        MultiValueMap<String, MultipartFile> multipartFiles = super.getMultipartFiles();
        try {
            MultiValueMap<String, MultipartFile> files = new LinkedMultiValueMap<>(multipartFiles.size());
            for (String key : multipartFiles.keySet()) {
                files.put(encryptor.decrypt(key), multipartFiles.remove(key));
            }
            setMultipartFiles(files);
        } catch (Exception e) {
            log.error("解密失败", e);
        }
    }


    @Override
    public Map<String, String[]> getParameterMap() {
        return parameterMap;
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameterMap.get(name);
        return ArrayUtil.get(values, 0);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }
}
