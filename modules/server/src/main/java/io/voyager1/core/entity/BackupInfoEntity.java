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

package io.voyager1.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 数据备份 JPA 实体。
 */
@Entity
@Table(name = "BACKUP_INFO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BackupInfoEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "filePath", length = 200)
    private String filePath;

    @Column(name = "backupType")
    private Integer backupType;

    @Column(name = "fileSize")
    private Long fileSize;

    @Column(name = "sha1Sum", length = 50)
    private String sha1Sum;

    @Column(name = "status")
    private Integer status;

    @Column(name = "baleTimeStamp")
    private Long baleTimeStamp;

    @Column(name = "version", length = 255)
    private String version;

    @Column(name = "modifyUser", length = 50)
    private String modifyUser;
}
