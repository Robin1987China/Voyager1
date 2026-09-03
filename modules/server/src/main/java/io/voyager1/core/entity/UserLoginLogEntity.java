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
 * 用户登录日志 JPA 实体。
 */
@Entity
@Table(name = "SYS_USER_LOGIN_LOG")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginLogEntity {

    @Id
    @Column(name = "id", length = 50)
    private String id;

    @Column(name = "createTimeMillis")
    private Long createTimeMillis;

    @Column(name = "modifyTimeMillis")
    private Long modifyTimeMillis;

    @Column(name = "modifyUser", length = 50)
    private String modifyUser;

    @Column(name = "ip", length = 80)
    private String ip;

    @Column(name = "userAgent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "success")
    private Integer success;

    @Column(name = "operateCode")
    private Integer operateCode;

    @Column(name = "username", length = 50)
    private String username;
}
