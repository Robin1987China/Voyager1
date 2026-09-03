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

package io.voyager1.service.cloud.provider;

import io.voyager1.cloud.ICloudProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 云厂商实现注册表（按 vendor 路由）
 *
 * @since 2026/8/12
 */
@Component
public class CloudProviderRegistry {

    private final Map<String, ICloudProvider> providers;

    public CloudProviderRegistry(List<ICloudProvider> providerList) {
        this.providers = providerList.stream().collect(Collectors.toMap(ICloudProvider::vendor, Function.identity()));
    }

    /**
     * 按厂商获取实现
     *
     * @param vendor 云厂商标识
     * @return 实现
     */
    public ICloudProvider get(String vendor) {
        ICloudProvider provider = this.providers.get(vendor);
        Assert.notNull(provider, "不支持的云厂商：" + vendor);
        return provider;
    }

    /**
     * 是否已接入该厂商
     *
     * @param vendor 云厂商标识
     * @return true 已接入
     */
    public boolean contains(String vendor) {
        return this.providers.containsKey(vendor);
    }
}
