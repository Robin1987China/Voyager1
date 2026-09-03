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

import io.voyager1.util.EnumUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.HttpUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import io.voyager1.build.BuildUtil;
import io.voyager1.common.Const;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseEnum;
import io.voyager1.model.BaseGroupModel;
import io.voyager1.model.enums.GitProtocolEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * 仓库地址实体类
 */
@TableName(value = "CI_REPOSITORY",
    nameKey = "仓库信息")
@Data
@EqualsAndHashCode(callSuper = true)
public class RepositoryModel extends BaseGroupModel {
    /**
     * 名称
     */
    private String name;
    /**
     * 仓库地址
     */
    private String gitUrl;
    /**
     * 仓库类型{0: GIT, 1: SVN}
     */
    private Integer repoType;
    /**
     * 拉取代码的协议{0: http, 1: ssh}
     *
     * @see GitProtocolEnum
     */
    private Integer protocol;
    /**
     * 登录用户
     */
    private String userName;
    /**
     * 登录密码
     */
    private String password;
    /**
     * SSH RSA 公钥
     */
    @Deprecated
    private String rsaPub;
    /**
     * SSH RSA 私钥
     */
    private String rsaPrv;
    /**
     * 排序
     */
    private Float sortValue;
    /**
     * 仓库连接超时时间
     */
    private Integer timeout;

    /**
     * 返回协议类型，如果为 null 会尝试识别 http
     *
     * @return 枚举的值（1/0）
     * @see GitProtocolEnum
     */
    public Integer getProtocol() {
        if (protocol != null) {
            return protocol;
        }
        String gitUrl = this.getGitUrl();
        if ((gitUrl == null || gitUrl.isEmpty())) {
            return null;
        }
        if (HttpUtil.isHttps(gitUrl) || HttpUtil.isHttp(gitUrl)) {
            return GitProtocolEnum.HTTP.getCode();
        }
        return null;
    }

    /**
     * 转换为 map
     *
     * @return map
     */
    public Map<String, Object> toMap() {
        //
        Map<String, Object> map = new HashMap<>(10);
        map.put("url", this.getGitUrl());
        map.put("protocol", this.getProtocol());
        Integer protocolCode = this.getProtocol();
        GitProtocolEnum protocol = EnumUtil.likeValueOf(GitProtocolEnum.class, protocolCode);
        if (protocol != null) {
            map.put("protocolStr", protocol.name());
        }
        map.put("username", this.getUserName());
        map.put("password", this.getPassword());
        map.put(Const.WORKSPACE_ID_REQ_HEADER, this.getWorkspaceId());
        map.put("rsaFile", BuildUtil.getRepositoryRsaFile(this));
        map.put("timeout", this.getTimeout());
        return map;
    }

    /**
     * 仓库类型
     */
    @Getter
    public enum RepoType implements BaseEnum {
        /**
         * git
         */
        Git(0, "Git"),
        Svn(1, "Svn"),
        ;
        private final int code;
        private final String desc;

        RepoType(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }

    @Override
    protected boolean hasCreateUser() {
        return true;
    }
}
