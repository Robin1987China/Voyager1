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

package io.voyager1.model.node;

import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseNodeGroupModel;

/**
 * @since 2021/12/5
 */
@TableName(value = "CI_PROJECT",
    nameKey = "项目信息")
@Data
@EqualsAndHashCode(callSuper = true)
public class ProjectInfoCacheModel extends BaseNodeGroupModel {

    private String projectId;
    /**
     * 项目自动启动
     */
    private Boolean autoStart;

    private String name;

    private String mainClass;
    private String lib;
    /**
     * 授权目录
     */
    private String whitelistDirectory;
    /**
     * 日志目录
     */
    private String logPath;
    /**
     * jvm 参数
     */
    private String jvm;
    /**
     * java main 方法参数
     */
    private String args;
    /**
     * 副本
     */
    private String javaCopyItemList;
    /**
     * WebHooks
     */
    private String token;

    private String runMode;
    /**
     * 节点分发项目，不允许在项目管理中编辑
     */
    private Boolean outGivingProject;
    /**
     * -Djava.ext.dirs=lib -cp conf:run.jar
     * 填写【lib:conf】
     */
    private String javaExtDirsCp;
    /**
     * DSL 内容
     */
    private String dslContent;
    /**
     * 排序
     */
    private Float sortValue;

    /**
     * 触发器 token
     */
    private String triggerToken;

    @Override
    public String dataId() {
        return getProjectId();
    }

    @Override
    public void dataId(String id) {
        setProjectId(id);
    }
}
