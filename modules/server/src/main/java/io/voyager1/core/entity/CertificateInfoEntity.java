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

import io.voyager1.core.jpa.WorkspaceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 证书信息 JPA 实体。
 * <p>
 * 注：DB 列 {@code expirationTime} 为 String(VARCHAR)，模型层为 Long，copyProperties 负责 String&lt;-&gt;Number 转换。
 */
@Entity
@Table(name = "SYS_CERTIFICATE")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificateInfoEntity implements WorkspaceEntity {

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

    @Column(name = "workspaceId", length = 50)
    private String workspaceId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "keyAlias", length = 50)
    private String keyAlias;

    @Column(name = "keyType", length = 50)
    private String keyType;

    @Column(name = "certPassword", length = 50)
    private String certPassword;

    @Column(name = "serialNumberStr", length = 50)
    private String serialNumberStr;

    @Column(name = "issuerDnName", length = 255)
    private String issuerDnName;

    @Column(name = "subjectDnName", length = 255)
    private String subjectDnName;

    @Column(name = "sigAlgOid", length = 255)
    private String sigAlgOid;

    @Column(name = "sigAlgName", length = 255)
    private String sigAlgName;

    @Column(name = "expirationTime", length = 255)
    private String expirationTime;

    @Column(name = "effectiveTime")
    private Long effectiveTime;

    @Column(name = "certVersion")
    private Integer certVersion;

    @Column(name = "fingerprint", length = 50)
    private String fingerprint;
}
