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
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Tolerate;
import io.voyager1.build.BuildExtraModule;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseGroupModel;
import io.voyager1.model.enums.BuildStatus;
import io.voyager1.model.log.BuildHistoryLog;
import io.voyager1.util.StringUtil;

/**
 * new BuildModel class, for replace old BuildModel
 *
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "CI_BUILD",
    nameKey = "构建信息")
@Data
@Builder
public class BuildInfoModel extends BaseGroupModel {

    /**
     * 仓库 ID
     */
    private String repositoryId;
    /**
     * 名称
     */
    private String name;
    /**
     * 构建 ID
     *
     * @see BuildHistoryLog#getBuildNumberId()
     */
    private Integer buildId;
    /**
     * 分支
     */
    private String branchName;
    /**
     * 标签
     */
    private String branchTagName;
    /**
     * 构建命令
     */
    private String script;
    /**
     * 构建产物目录
     */
    private String resultDirFile;
    /**
     * 发布方法{0: 不发布, 1: 节点分发, 2: 分发项目, 3: SSH}
     */
    private Integer releaseMethod;
    /**
     * 发布方法执行数据关联字段
     */
    private String releaseMethodDataId;
    /**
     * 状态
     *
     * @see BuildStatus
     */
    private Integer status;
    /**
     * 状态消息
     */
    private String statusMsg;
    /**
     * 触发器token
     */
    private String triggerToken;
    /**
     * 额外信息，JSON 字符串格式
     *
     * @see BuildExtraModule
     */
    private String extraData;
    /**
     * 构建 webhook
     */
    private String webhook;
    /**
     * 定时构建表达式
     */
    private String autoBuildCron;
    /**
     * 源码目录是否存在
     */
    @PropIgnore
    private Boolean sourceDirExist;
    /**
     * 产物文件是否存在
     */
    @PropIgnore
    private Boolean resultHasFile;
    /**
     * 构建方式 0 本地构建 1 docker 构建
     */
    private Integer buildMode;
    /**
     * 仓库代码最后一次变动信息（ID，git 为 commit hash, svn 最后的版本号）
     */
    private String repositoryLastCommitId;
    /**
     * 排序
     */
    private Float sortValue;
    /**
     * 构建环境变量
     */
    private String buildEnvParameter;
    /**
     * 别名码
     */
    private String aliasCode;
    /**
     * 产物保留天数
     */
    private Integer resultKeepDay;

    @Tolerate
    public BuildInfoModel() {
    }

    /**
     * 获取构建的扩展数据
     *
     * @return extraData
     */
    public BuildExtraModule extraData() {
        BuildExtraModule buildExtraModule = StringUtil.jsonConvert(this.getExtraData(), BuildExtraModule.class);
        if (buildExtraModule != null) {
            if (this.releaseMethodDataId != null) {
                // 兼容数据迁移
                buildExtraModule.setReleaseMethodDataId(this.releaseMethodDataId);
            }
        }
        return buildExtraModule;
    }

    public static String getBuildIdStr(int buildId) {
        return String.format("#%s", buildId);
    }

    @Override
    protected boolean hasCreateUser() {
        return true;
    }
}
