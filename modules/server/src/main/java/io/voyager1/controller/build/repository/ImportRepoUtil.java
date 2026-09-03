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

package io.voyager1.controller.build.repository;

import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.PatternPool;
import io.voyager1.util.StrUtil;
import org.springframework.data.domain.Pageable;
import io.voyager1.util.HttpRequest;
import io.voyager1.util.HttpResponse;
import io.voyager1.util.HttpUtil;
import io.voyager1.util.Method;
import io.voyager1.util.JSONArray;
import io.voyager1.util.JSONObject;
import io.voyager1.util.JSONUtil;
import io.voyager1.util.YamlUtil;
import lombok.Lombok;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.system.ExtConfigBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 */
@Slf4j
@UtilityClass
public class ImportRepoUtil {

    private static final String IMPORT_REPO_PROVIDER_DIR = "/import-repo-provider/";

    public Map<String, Map<String, Object>> getProviderList() {
        Resource[] configResources = ExtConfigBean.getDefaultConfigResources("import-repo-provider/*.yml");
        Map<String, Map<String, Object>> map = resourceToMap(configResources);
        Resource[] diyConfigResources = ExtConfigBean.getConfigResources("import-repo-provider/*.yml");
        map.putAll(resourceToMap(diyConfigResources));
        return map;
    }

    private Map<String, Map<String, Object>> resourceToMap(Resource[] configResources) {
        if (configResources == null) {
            return new HashMap<>(1);
        }
        return Arrays.stream(configResources)
            .map(resource -> {
                String filename = resource.getFilename();
                String mainName = FileUtil.mainName(filename);

                try (InputStream inputStream = resource.getInputStream()) {
                    ImportRepoProviderConfig providerConfig = YamlUtil.load(inputStream, ImportRepoProviderConfig.class);
                    Map<String, Object> map = new HashMap<>();
                    map.put("name", mainName);
                    map.put("baseUrl", providerConfig.getBaseUrl());
                    // 是否支持查询
                    map.put("query", providerConfig.getRepoListParam().values().stream().anyMatch(s -> s.contains("${query}")));
                    return map;
                } catch (Exception e) {
                    throw Lombok.sneakyThrow(e);
                }
            })
            .collect(Collectors.toMap(map -> (String) map.get("name"), map -> map));
    }

    @SneakyThrows
    public ImportRepoProviderConfig getProviderConfig(String platform) {
        try (InputStream inputStream = ExtConfigBean.getConfigResourceInputStream(IMPORT_REPO_PROVIDER_DIR + platform + ".yml")) {
            return YamlUtil.load(inputStream, ImportRepoProviderConfig.class);
        }
    }

    private void setCommonParams(String platform, HttpRequest request, String token) {
        ImportRepoProviderConfig provider = getProviderConfig(platform);
        String callToken = provider.getAuthValue().replace("${token}", token);
        if (provider.getAuthType() == 1) {
            request.header(provider.getAuthKey(), callToken);
        } else if (provider.getAuthType() == 2) {
            request.form(provider.getAuthKey(), callToken);
        } else if (provider.getAuthType() == 3) {
            request.body(JSONUtil.createObj().set(provider.getAuthKey(), callToken).toString());
        }

        Map<String, String> extraParams = provider.getExtraParams();
        if ((extraParams != null && !extraParams.isEmpty())) {
            if (provider.getExtraParamsType() == 1) {
                extraParams.forEach(request::header);
            } else if (provider.getExtraParamsType() == 2) {
                extraParams.forEach(request::form);
            } else if (provider.getExtraParamsType() == 3) {
                request.body(JSONUtil.toJsonStr(extraParams));
            }
        }
    }

    public JSONObject getRepoList(String platform, String query, Pageable page, String token, String username, String baseUrl) {
        baseUrl = StrUtil.blankToDefault(baseUrl, getProviderConfig(platform).getBaseUrl());
        ImportRepoProviderConfig provider = getProviderConfig(platform);
        HttpRequest request = HttpUtil.createRequest(Method.valueOf(provider.getRepoListMethod()), baseUrl + provider.getRepoListUrl());
        setCommonParams(platform, request, token);
        query = StrUtil.blankToDefault(query, "");
        String finalQuery = query;
        provider.getRepoListParam().forEach((k, v) -> {
            if ("${query}".equals(v)) {
                v = v.replace("${query}", finalQuery);
            }
            if ("${page}".equals(v)) {
                v = v.replace("${page}", String.valueOf(page.getPageNumber() + 1));
            }
            if ("${pageSize}".equals(v)) {
                v = v.replace("${pageSize}", String.valueOf(page.getPageSize()));
            }
            request.form(k, v);
        });
        String body;
        int total;
        request.getUrl();
        log.debug(String.format("url: %s headers: %s form: %s", request.getUrl(), request.headers(), request.form()));
        try (HttpResponse execute = request.execute()) {
            body = execute.body();
            int status = execute.getStatus();
            Map<String, List<String>> headers = execute.headers();
            String totalHeader = execute.header(provider.getRepoTotalHeader());
            int totalCount = page.getPageSize() * (page.getPageNumber() + 1);
            if ("Link".equals(provider.getRepoTotalHeader()) && (totalHeader != null && !totalHeader.isBlank())) {
                // github 特殊处理
                Pattern pattern = PatternPool.get("page=(\\d+)&per_page=(\\d+)>; rel=\"last\"");
                Matcher matcher = pattern.matcher(totalHeader);
                if (matcher.find()) {
                    int linkPage = Integer.parseInt(matcher.group(2));
                    int linkPerPage = Integer.parseInt(matcher.group(1));
                    total = linkPage * linkPerPage;
                } else {
                    total = totalCount;
                }
            } else {
                total = (totalHeader != null && !totalHeader.isBlank()) ? Integer.parseInt(totalHeader) : totalCount;
            }
            log.debug(String.format("status: %s body: %s headers: %s", status, body, headers));
            Assert.state(execute.isOk(), String.format("请求失败: status: %s body: %s headers: %s", status, body, headers));
        }
        JSONArray jsonArray = JSONUtil.parse(body).getByPath(provider.getRepoListPath(), JSONArray.class);
        List<JSONObject> data = jsonArray.stream().map(o -> {
            JSONObject obj = (JSONObject) o;
            JSONObject entries = new JSONObject();
            provider.getRepoConvertPath().forEach((k, v) -> {
                if ((v != null && v.startsWith("$ "))) {
                    String[] expression = v.split(" ");
                    String value = obj.getStr(expression[1]);
                    // 对比方式
                    String compare = expression[2];
                    // 对比值
                    String compareValue = expression[3];
                    switch (compare) {
                        case "==":
                            entries.set(k, value.equals(compareValue));
                            break;
                        case "!=":
                            entries.set(k, !value.equals(compareValue));
                            break;
                        default:
                            throw new IllegalStateException("表达式目前仅支持 == 和 != 比较");
                    }
                } else {
                    entries.set(k, obj.get(v));
                }
            });
            entries.set("username", username);
            return entries;
        }).collect(Collectors.toList());
        return JSONUtil.createObj().set("data", data).set("total", total);
    }

    public String getCurrentUserName(String platform, String token, String baseUrl) {
        baseUrl = StrUtil.blankToDefault(baseUrl, getProviderConfig(platform).getBaseUrl());
        Assert.state((baseUrl != null && !baseUrl.isBlank()), String.format("请填写 %s 的 地址", platform));
        ImportRepoProviderConfig provider = getProviderConfig(platform);
        HttpRequest request = HttpUtil.createRequest(Method.valueOf(provider.getCurrentUserMethod()), baseUrl + provider.getCurrentUserUrl());
        setCommonParams(platform, request, token);
        String body;
        log.debug("url: {} headers: {} form: {}", request.getUrl(), request.headers(), request.form());
        try (HttpResponse execute = request.execute()) {
            body = execute.body();
            int status = execute.getStatus();
            Map<String, List<String>> headers = execute.headers();
            log.debug("status: {} body: {} headers: {}", status, body, headers);
            Assert.state(execute.isOk(), String.format("请求失败: status: %s body: %s headers: %s", status, body, headers));
        }
        return JSONUtil.parse(body).getByPath(provider.getUserNamePath(), String.class);
    }

}
