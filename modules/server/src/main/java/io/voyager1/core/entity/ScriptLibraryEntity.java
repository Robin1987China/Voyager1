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
 * 脚本库 JPA 实体。
 */
@Entity
@Table(name = "OPS_SCRIPT_LIBRARY")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScriptLibraryEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "modifyUser", length = 50)
    private String modifyUser;

    @Column(name = "createUser", length = 50)
    private String createUser;

    @Column(name = "tag", length = 50)
    private String tag;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "script", columnDefinition = "TEXT")
    private String script;

    @Column(name = "machineIds", columnDefinition = "TEXT")
    private String machineIds;

    @Column(name = "version", length = 50)
    private String version;
}
