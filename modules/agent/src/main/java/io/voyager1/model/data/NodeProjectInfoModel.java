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

import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.common.commander.CommandOpResult;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.RunMode;
import io.voyager1.system.Voyager1RuntimeException;
import org.springframework.util.Assert;

import java.util.List;

/**
 * 项目配置信息实体
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NodeProjectInfoModel extends BaseWorkspaceModel {
    /**
     * 分组
     */
    private String group;
    /**
     * 项目路径
     */
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
     * 日志编码
     */
    private String logCharset;
    /**
     * java 模式运行的 class
     */
    private String mainClass;
    /**
     * jvm 参数
     */
    private String jvm;
    /**
     * java main 方法参数
     */
    private String args;
    /**
     * WebHooks
     */
    private String token;
    /**
     * 项目运行模式
     */
    private RunMode runMode;
    /**
     * 软链的父级项目id
     */
    private String linkId;
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
     * 项目自动启动
     */
    private Boolean autoStart;
    /**
     * dsl yml 内容
     *
     * @see DslYmlDto
     */
    private String dslContent;
    /**
     * dsl 环境变量
     */
    private String dslEnv;
    /**
     * 最后一次执行 reload 结果
     */
    private CommandOpResult lastReloadResult;
    /**
     * 禁用扫描目录
     */
    private Boolean disableScanDir;
    //  ---------------- 中转字段 start
    /**
     * 是否可以重新加载
     */
    private Boolean canReload;
    /**
     * DSL 流程信息统计
     */
    private List<JSONObject> dslProcessInfo;
    /**
     * 实际运行的命令
     */
    private String runCommand;
    //  ---------------- 中转字段 end

    public boolean isDisableScanDir() {
        return disableScanDir != null && disableScanDir;
    }


    public String javaExtDirsCp() {
        return (javaExtDirsCp == null || javaExtDirsCp.isEmpty() ? "" : javaExtDirsCp);
    }

    public boolean outGivingProject() {
        return outGivingProject != null && outGivingProject;
    }

    public String mainClass() {
        return (mainClass == null || mainClass.isEmpty() ? "" : mainClass);
    }

    public String whitelistDirectory() {
        if ((whitelistDirectory == null || whitelistDirectory.isEmpty())) {
            throw new Voyager1RuntimeException("恢复授权数据异常或者没有选择授权目录");
        }
        return whitelistDirectory;
    }

    public String allLib() {
        String directory = this.whitelistDirectory();
        return FileUtil.file(directory, this.getLib()).getAbsolutePath();
    }

    public String logPath() {
        return (this.logPath == null || this.logPath.isEmpty() ? "" : this.logPath);
    }

    /**
     * 默认
     *
     * @return url token
     */
    public String token() {
        // 兼容旧数据
        if ("no".equalsIgnoreCase(this.token)) {
            return "";
        }
        return (token == null || token.isEmpty() ? "" : token);
    }

    /**
     * 获取当前 dsl 配置
     *
     * @return DslYmlDto
     */
    public DslYmlDto dslConfig() {
        String dslContent = this.getDslContent();
        if ((dslContent == null || dslContent.isEmpty())) {
            return null;
        }
        return DslYmlDto.build(dslContent);
    }

    /**
     * 必须存在 dsl 配置
     *
     * @return DslYmlDto
     */
    public DslYmlDto mustDslConfig() {
        DslYmlDto dslYmlDto = this.dslConfig();
        Assert.notNull(dslYmlDto, "未配置 dsl 信息（项目信息错误）");
        return dslYmlDto;
    }

}
