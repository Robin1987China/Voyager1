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

package io.voyager1.model.log;

import io.voyager1.util.PropIgnore;
import io.voyager1.util.Opt;
import io.voyager1.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.build.BuildExtraModule;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.enums.BuildReleaseMethod;
import io.voyager1.model.enums.BuildStatus;

import java.util.Map;

/**
 * 构建历史记录
 *
 * @see BuildExtraModule
 * @since 2019/7/17
 **/
@EqualsAndHashCode(callSuper = true)
@TableName(value = "CI_BUILD_LOG",
    nameKey = "构建历史", parents = BuildInfoModel.class)
@Data
public class BuildHistoryLog extends BaseWorkspaceModel {
    /**
     * 发布方式
     *
     * @see BuildReleaseMethod
     * @see BuildInfoModel#getReleaseMethod()
     */
    private Integer releaseMethod;

    /**
     * 构建产物目录
     */
    private String resultDirFile;

    /**
     * 触发构建类型 触发类型{0，手动，1 触发器,2 自动触发,3 手动回滚}
     */
    private Integer triggerBuildType;

    /**
     * 关联的构建id
     *
     * @see BuildInfoModel#getId()
     */
    private String buildDataId;
    /**
     * 构建名称
     */
    private String buildName;
    /**
     * 构建编号
     *
     * @see BuildInfoModel#getBuildId()
     */
    private Integer buildNumberId;
    /**
     * 来自的构建编号，回滚时存在
     */
    private Integer fromBuildNumberId;
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
     * 开始时间
     */
    private Long startTime;
    /**
     * 结束时间
     */
    private Long endTime;
    /**
     * 构建备注
     */
    private String buildRemark;
    /**
     * 构建其他信息
     *
     * @see BuildExtraModule
     */
    private String extraData;
    /**
     * 是否存在构建产物
     */
    @PropIgnore
    private Boolean hasFile;
    /**
     * 是否存在日志
     */
    @PropIgnore
    private Boolean hasLog;
    /**
     * 构建环境变量缓存
     */
    private String buildEnvCache;
    /**
     * 产物文件大小
     */
    private Long resultFileSize;

    /**
     * 构建日志文件大小
     */
    private Long buildLogFileSize;

    /**
     * 仓库代码最后一次变动信息（ID，git 为 commit hash, svn 最后的版本号）
     */
    private String repositoryLastCommitId;

    /**
     * 仓库代码最后一次变动信息
     */
    private String repositoryLastCommitMsg;

    public void setBuildRemark(String buildRemark) {
        this.buildRemark = (buildRemark == null ? null : (buildRemark.length() <= 240 ? buildRemark : buildRemark.substring(0, 240)));
    }

    public EnvironmentMapBuilder toEnvironmentMapBuilder() {
        String buildEnvCache = this.getBuildEnvCache();
        JSONObject jsonObject = Opt.ofBlankAble(buildEnvCache).map(JSONObject::parseObject).orElse(new JSONObject());
        Map<String, EnvironmentMapBuilder.Item> map = jsonObject.to(new TypeReference<Map<String, EnvironmentMapBuilder.Item>>() {
        });
        return EnvironmentMapBuilder.builder(map);
    }

//	public void fillLogValue(BuildExtraModule buildExtraModule) {
//		//
//		this.setAfterOpt((buildExtraModule.getAfterOpt() != null ? buildExtraModule.getAfterOpt() : AfterOpt.No.getCode()));
//		this.setReleaseMethod(buildExtraModule.getReleaseMethod());
//		this.setReleaseCommand(buildExtraModule.getReleaseCommand());
//		this.setReleasePath(buildExtraModule.getReleasePath());
//		this.setReleaseMethodDataId(buildExtraModule.getReleaseMethodDataId());
//		this.setClearOld(buildExtraModule.isClearOld());
//		this.setResultDirFile(buildExtraModule.getResultDirFile());
//		this.setDiffSync(buildExtraModule.isDiffSync());
//	}
}
