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

package io.voyager1.func.user.dto;

import io.voyager1.util.StrUtil;
import lombok.Data;
import io.voyager1.common.i18n.I18nMessageUtil;
import org.springframework.util.Assert;

/**
 * @since 2024/4/20
 */
@Data
public class UserNotificationDto {
    /**
     * 是否开启公告
     */
    private Boolean enabled;
    /**
     * 是否可以关闭
     */
    private Boolean closable;
    /**
     * 公告级别
     */
    private Level level;
    /**
     * 公告标题
     */
    private String title;
    /**
     * 公告内容
     */
    private String content;

    public enum Level {
        info, warning, error
    }

    public void verify() {
        if (this.enabled != null && this.enabled) {
            Assert.state(!StrUtil.isAllBlank(this.title, this.content), "请配置公告标题或者内容");
        }
    }
}
