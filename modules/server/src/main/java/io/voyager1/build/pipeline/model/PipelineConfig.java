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

package io.voyager1.build.pipeline.model;

import lombok.Data;
import io.voyager1.model.AfterOpt;
import io.voyager1.model.data.BuildInfoModel;

import java.util.List;
import java.util.Map;

/**
 * @since 2024/4/7
 */
@Data
public class PipelineConfig {

    private Map<String, Repository> repositories;

    private List<IStage> stages;

    public interface IStage {
        StageType getStageType();
    }

    public enum StageType {
        EXEC,
        PUBLISH
    }

    @Data
    public static class Publish implements IStage {
        /**
         * 阶段类型
         */
        private StageType stageType;
        /**
         * 执行描述
         */
        private String desc;
        /**
         * 执行的脚本
         */
        private String commands;
    }

    @Data
    public static class BaseStage implements IStage {
        /**
         * 阶段类型
         */
        private StageType stageType;
        /**
         * 执行的目录
         * <p>
         * 仓库的标记
         *
         * @see PipelineConfig#getRepositories()
         */
        private String repoTag;
    }

    @Data
    public static class PublishByProject {

        private String nodeId;

        private String projectId;

        private String projectSecondaryDirectory;

        /**
         * 保存项目文件前先关闭
         */
        private Boolean projectUploadCloseFirst;

        /**
         * 分发后的操作
         * 仅在项目发布类型生效
         *
         * @see AfterOpt
         * @see BuildInfoModel#getExtraData()
         */
        private int afterOpt;
    }

    @Data
    public static class ExecCommand implements IStage {
        /**
         * 阶段类型
         */
        private StageType stageType;
        /**
         * 执行描述
         */
        private String desc;
        /**
         * 执行的脚本
         */
        private String commands;
        /**
         * 环境变量
         */
        private Map<String, String> env;

        /**
         * 脚本执行超时时间
         */
        private Integer timeout;

        /**
         * 产物
         */
        private List<ArtifactItem> artifacts;
    }

    @Data
    private static class ArtifactItem {
        private String id;

        private List<String> path;
    }

    @Data
    public static class Repository {
        /**
         * 仓库ID
         */
        private String repositoryId;
        /**
         * 分支
         */
        private String branchName;
        /**
         * 标签
         */
        private String branchTagName;
        /**
         * 克隆深度
         */
        private Integer cloneDepth;
        /**
         * 工作目录
         */
        private String workPath;
    }
}
