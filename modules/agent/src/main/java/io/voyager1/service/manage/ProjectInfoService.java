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

package io.voyager1.service.manage;

import io.voyager1.util.FileUtil;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.common.AgentConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.AgentConfig;
import io.voyager1.configuration.ProjectLogConfig;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.RunMode;
import io.voyager1.model.data.NodeProjectInfoModel;
import io.voyager1.service.BaseWorkspaceOptService;
import io.voyager1.service.system.AgentWorkspaceEnvVarService;
import io.voyager1.system.ExtConfigBean;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * 项目管理
 *
 */
@Service
public class ProjectInfoService extends BaseWorkspaceOptService<NodeProjectInfoModel> {

    private final AgentWorkspaceEnvVarService agentWorkspaceEnvVarService;
    private final ProjectLogConfig projectLogConfig;

    public ProjectInfoService(AgentWorkspaceEnvVarService agentWorkspaceEnvVarService, AgentConfig agentConfig) {
        super(AgentConst.PROJECT);
        this.agentWorkspaceEnvVarService = agentWorkspaceEnvVarService;
        this.projectLogConfig = agentConfig.getProject().getLog();
    }

    @Override
    public void updateItem(NodeProjectInfoModel data) {
        super.updateItem(data);
    }

    /**
     * 获取原始项目信息
     *
     * @param nodeProjectInfoModel 项目信息
     * @return model
     */
    public NodeProjectInfoModel resolveModel(NodeProjectInfoModel nodeProjectInfoModel) {
        RunMode runMode = nodeProjectInfoModel.getRunMode();
        if (runMode != RunMode.Link) {
            return nodeProjectInfoModel;
        }
        NodeProjectInfoModel item = this.getItem(nodeProjectInfoModel.getLinkId());
        Assert.notNull(item, "被软链的项目已经不存在啦，" + nodeProjectInfoModel.getLinkId());
        return item;
    }

    /**
     * 解析lib路径
     *
     * @param nodeProjectInfoModel 项目
     * @return 项目的 lib 路径（文件路径）
     */
    public String resolveLibPath(NodeProjectInfoModel nodeProjectInfoModel) {
        RunMode runMode = nodeProjectInfoModel.getRunMode();
        if (runMode == RunMode.Link) {
            NodeProjectInfoModel item = this.getItem(nodeProjectInfoModel.getLinkId());
            Assert.notNull(item, "软链项目已经不存在啦");
            return item.allLib();
        }
        return nodeProjectInfoModel.allLib();
    }

    /**
     * 解析lib路径
     *
     * @param nodeProjectInfoModel 项目
     * @return 项目的 lib 路径（文件路径）
     */
    public File resolveLibFile(NodeProjectInfoModel nodeProjectInfoModel) {
        String path = this.resolveLibPath(nodeProjectInfoModel);
        return FileUtil.file(path);
    }

    /**
     * 解析项目的日志路径
     *
     * @param nodeProjectInfoModel 项目
     * @param originalModel        原始项目
     * @return path
     */
    private File resolveLogFile(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        String id = nodeProjectInfoModel.getId();
        File logPath = this.resolveLogPath(nodeProjectInfoModel, originalModel);
        return FileUtil.file(logPath, id + ".log");
    }

    /**
     * 解析项目的日志路径
     *
     * @param nodeProjectInfoModel 项目
     * @param originalModel        原始项目
     * @return path
     */
    private File resolveLogPath(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        String id = nodeProjectInfoModel.getId();
        Assert.hasText(id, "没有项目id");
        String loggedPath = originalModel.logPath();
        if ((loggedPath != null && !loggedPath.isEmpty())) {
            return FileUtil.file(loggedPath, id);
        }
        String path = ExtConfigBean.getPath();
        return FileUtil.file(path, "project-log", id);
    }

    /**
     * 解析项目的日志路径
     *
     * @param nodeProjectInfoModel 项目
     * @param originalModel        原始项目
     * @return path
     */
    public String resolveAbsoluteLog(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        File file = this.resolveAbsoluteLogFile(nodeProjectInfoModel, originalModel);
        return FileUtil.getAbsolutePath(file);
    }

    /**
     * 解析项目的日志路径
     *
     * @param nodeProjectInfoModel 项目
     * @return path
     */
    public File resolveAbsoluteLogFile(NodeProjectInfoModel nodeProjectInfoModel) {
        NodeProjectInfoModel infoModel = this.resolveModel(nodeProjectInfoModel);
        return this.resolveLogFile(nodeProjectInfoModel, infoModel);
    }

    /**
     * 解析项目的日志路径
     *
     * @param nodeProjectInfoModel 项目
     * @param originalModel        原始项目
     * @return path
     */
    public File resolveAbsoluteLogFile(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        File file = this.resolveLogFile(nodeProjectInfoModel, originalModel);
        // auto create dir
        FileUtil.touch(file);
        return file;
    }

    /**
     * 解析项目的日志编码格式
     *
     * @param nodeProjectInfoModel 项目
     * @param originalModel        原始项目
     * @return path
     */
    public Charset resolveLogCharset(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        Charset defaultCharset = projectLogConfig.getFileCharset();
        String logCharset = originalModel.getLogCharset();
        return CharsetUtil.parse(logCharset, defaultCharset);
    }

    /**
     * 解析日志备份路径
     *
     * @param nodeProjectInfoModel 项目
     * @return file
     */
    public File resolveLogBack(NodeProjectInfoModel nodeProjectInfoModel) {
        NodeProjectInfoModel infoModel = this.resolveModel(nodeProjectInfoModel);
        return this.resolveLogBack(nodeProjectInfoModel, infoModel);
    }

    /**
     * 解析日志备份路径
     *
     * @param nodeProjectInfoModel 项目
     * @param originalModel        原始项目
     * @return file
     */
    public File resolveLogBack(NodeProjectInfoModel nodeProjectInfoModel, NodeProjectInfoModel originalModel) {
        File logPath = this.resolveLogPath(nodeProjectInfoModel, originalModel);
        return FileUtil.file(logPath, "back");
    }

    /**
     * 获取环境变量
     *
     * @param workspaceId 工作空间ID
     * @return map
     */
    public Map<String, String> getEnv(String workspaceId) {
        EnvironmentMapBuilder env = agentWorkspaceEnvVarService.getEnv(workspaceId);
        return env.environment();
    }
}
