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

package io.voyager1.func.cert.model;

import io.voyager1.util.PropIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;


@EqualsAndHashCode(callSuper = true)
@TableName(value = "SYS_CERTIFICATE",
    nameKey = "证书信息表")
@Data
@NoArgsConstructor
public class CertificateInfoModel extends BaseWorkspaceModel {
    /**
     * 证书类型
     */
    private String keyType;
    private String keyAlias;
    /**
     * 指纹
     */
    private String fingerprint;
    /**
     * 证书密码
     */
    private String certPassword;
    /**
     * 证书序列号
     */
    private String serialNumberStr;
    /**
     * 颁发者 DN 名称
     */
    private String issuerDnName;
    /**
     * 主题 DN 名称
     */
    private String subjectDnName;
    /**
     * 版本号
     */
    private Integer certVersion;
    /**
     *
     */
    private String sigAlgOid;
    /**
     * 算法名
     */
    private String sigAlgName;

    /**
     * 证书到期时间
     */
    private Long expirationTime;
    /**
     * 证书生效日期
     */
    private Long effectiveTime;

    private String description;

    /**
     * 文件是否存在
     */
    @PropIgnore
    private Boolean fileExists;

    @Override
    protected boolean hasCreateUser() {
        return true;
    }
}
