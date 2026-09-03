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

package io.voyager1.func.files.model;

import io.voyager1.util.PropIgnore;
import io.voyager1.util.DateField;
import io.voyager1.util.DateTime;
import io.voyager1.util.DateUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;

/**
 * @since 2023/3/16
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "STORAGE_FILE",
    nameKey = "文件管理中心")
@Data
@NoArgsConstructor
public class FileStorageModel extends BaseWorkspaceModel implements IFileStorage {

    @Override
    public void setId(String id) {
        // 文件 md5
        super.setId(id);
    }

    /**
     * 文件名
     */
    private String name;

    public void setName(String name) {
        this.name = (name == null ? null : (name.length() <= 240 ? name : name.substring(0, 240)));
    }

    /**
     * 文件大小
     */
    private Long size;
    /**
     * 文件描述
     */
    private String description;
    /**
     * 文件来源 0 上传 1 构建 2 下载 3 证书
     */
    private Integer source;
    /**
     * 文件有效期（毫秒）
     */
    private Long validUntil;
    /**
     * 文件路径
     */
    private String path;
    /**
     * 文件扩展名
     */
    private String extName;
    /**
     * 只有下载的时候才使用本字段
     * <p>
     * 0 下载中 1 下载完成 2 下载异常
     */
    private Integer status;
    /**
     * 进度描述
     */
    private String progressDesc;
    /**
     * 文件是否存在
     */
    @PropIgnore
    private Boolean exists;
    /**
     * 触发器 token
     */
    private String triggerToken;

    /**
     * 别名码
     */
    private String aliasCode;

    /**
     * 设置保留天数的过期时间
     *
     * @param keepDay   保留天数
     * @param startTime 文件开始的时间
     */
    public void validUntil(Integer keepDay, Long startTime) {
        int keepDayInt = (keepDay != null ? keepDay : 3650);
        keepDayInt = Math.max(keepDayInt, 1);
        DateTime dateTime = new DateTime((startTime != null ? startTime : System.currentTimeMillis())).offset(DateField.DAY_OF_YEAR, keepDayInt);
        this.setValidUntil(DateUtil.endOfDay(dateTime).getTime());
    }

    @Override
    protected boolean hasCreateUser() {
        return true;
    }
}
