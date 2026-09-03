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

package io.voyager1.build;

import lombok.Builder;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.data.RepositoryModel;
import io.voyager1.model.user.UserModel;

import java.util.Map;

/**
 * @since 2022/1/26
 */
@Builder
public class TaskData {

    protected final BuildInfoModel buildInfoModel;
    protected final RepositoryModel repositoryModel;
    protected final UserModel userModel;
    /**
     * 延迟执行的时间（单位秒）
     */
    protected final Integer delay;
    /**
     * 触发类型
     * 0: "手动",
     * 1: "触发器",
     * 2: "定时",
     */
    protected final int triggerBuildType;
    /**
     * 构建备注
     */
    protected String buildRemark;
    /**
     * 环境变量
     * 工作空间环境变量
     */
    protected EnvironmentMapBuilder environmentMapBuilder;

    /**
     * 仓库代码最后一次变动信息（ID，git 为 commit hash, svn 最后的版本号）
     */
    protected String repositoryLastCommitId;
    protected String repositoryLastCommitMsg;
    /**
     * 是否差异构建
     */
    protected Boolean checkRepositoryDiff;
    /**
     * 产物文件大小
     */
    protected Long resultFileSize;

    protected Map<String, Object> dockerParameter;

    protected String buildContainerId;
}
