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

package io.voyager1.model.data;

import io.voyager1.util.PropIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.db.DbExtConfig;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseUserModifyDbModel;

/**
 * @since 2021-11-18
 * Backup info with H2 database
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "BACKUP_INFO",
    nameKey = "数据备份", modes = DbExtConfig.Mode.H2)
@Data
public class BackupInfoModel extends BaseUserModifyDbModel {

    /**
     * 备份名称
     */
    private String name;
    /**
     * 文件地址，绝对路径
     */
    private String filePath;
    /**
     * 备份类型{0: 全量, 1: 部分, 2: 导入, 3 自动}
     */
    private Integer backupType;
    /**
     * 文件大小
     */
    private Long fileSize;
    /**
     * SHA1SUM
     */
    private String sha1Sum;

    /**
     * 状态{0: 默认; 1: 成功; 2: 失败}
     */
    private Integer status;

    /**
     * 服务端版本
     */
    private String version;

    /**
     * 打包时间
     */
    private Long baleTimeStamp;
    /**
     * 文件是否存在
     */
    @PropIgnore
    private Boolean fileExist;

}
