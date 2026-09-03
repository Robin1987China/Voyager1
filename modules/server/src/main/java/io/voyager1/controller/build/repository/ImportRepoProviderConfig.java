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

import lombok.Data;

import java.util.Map;

/**
 * 仓库提供商配置
 *
 */
@Data
public class ImportRepoProviderConfig {
    private String baseUrl;
    /**
     * 鉴权方式 1:header 2:from 3:body
     */
    private Integer authType;
    /**
     * 鉴权key 例如：Authorization
     */
    private String authKey;
    /**
     * 鉴权值 例如：Bearer ${token}
     */
    private String authValue;
    /**
     * 扩展参数
     */
    private Map<String, String> extraParams;
    /**
     * 扩展参数类型 1:header 2:from 3:body
     */
    private Integer extraParamsType;
    /**
     * 获取用户信息的请求方式
     */
    private String currentUserMethod;
    /**
     * 获取用户信息的请求地址
     */
    private String currentUserUrl;
    /**
     * 获取用户名 path
     */
    private String userNamePath;
    /**
     * 获取仓库列表的请求方式
     */
    private String repoListMethod;
    /**
     * 获取仓库列表的请求地址
     */
    private String repoListUrl;
    /**
     * 获取仓库列表的请求参数
     */
    private Map<String, String> repoListParam;
    /**
     * 获取仓库列表数组 path
     */
    private String repoListPath;
    /**
     * 仓库信息 转换 path
     */
    private Map<String, String> repoConvertPath;
    /**
     * 获取仓库总数 X-Total
     */
    private String repoTotalHeader;

}
