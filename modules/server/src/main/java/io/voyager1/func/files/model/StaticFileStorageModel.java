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

import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseUserModifyDbModel;

/**
 * @since 23/12/28 028
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "STORAGE_STATIC_FILE",
    nameKey = "静态文件管理")
@Data
@NoArgsConstructor
public class StaticFileStorageModel extends BaseUserModifyDbModel implements IFileStorage {

    /**
     * 文件名
     */
    private String name;

    /**
     * 只保留 100 字符
     *
     * @param name 名称
     */
    public void setName(String name) {
        this.name = (name == null ? null : (name.length() <= 100 ? name : name.substring(0, 100)));
    }

    /**
     * 文件大小
     */
    private Long size;
    /**
     * 文件路径
     */
    private String absolutePath;
    private String parentAbsolutePath;
    /**
     * 要组索引不字段不能太长
     * [42000][1071] Specified key was too long; max key length is 3072 bytes
     */
    private String staticDir;
    private Integer level;
    /**
     * 文件修改时间
     */
    private Long lastModified;
    /**
     * 文件扩展名
     */
    private String extName;
    /**
     * 文件状态
     * 0 不存在
     * 1 存在
     */
    private Integer status;
    /**
     * 文件类型
     * 0 文件夹
     * 1 文件
     */
    private Integer type;
    /**
     * 扫描任务id
     */
    private Long scanTaskId;
    /**
     * 描述
     */
    private String description;
    /**
     * 触发器 token
     */
    private String triggerToken;

    @Override
    protected boolean hasCreateUser() {
        return false;
    }

    public int type() {
        return (this.getType() != null ? this.getType() : 0);
    }
}
