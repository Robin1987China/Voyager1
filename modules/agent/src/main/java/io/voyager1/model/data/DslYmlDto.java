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

import io.voyager1.util.ReflectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.YamlUtil;
import io.voyager1.model.BaseJsonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.common.i18n.I18nMessageUtil;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

/**
 * dsl yml 配置
 *
 * @since 2022/1/15
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DslYmlDto extends BaseJsonModel {

    /**
     * 描述
     */
    private String description;

    /**
     * 运行
     */
    private Run run;

    /**
     * 文件相关配置
     */
    private FileConfig file;
    /**
     * 配置
     */
    private Config config;

    /**
     * 判断是否包含指定流程
     *
     * @param opt 流程名
     * @return true
     */
    public boolean hasRunProcess(String opt) {
        DslYmlDto.Run run = this.getRun();
        if (run == null) {
            return false;
        }
        DslYmlDto.BaseProcess baseProcess = (DslYmlDto.BaseProcess) ReflectUtil.getFieldValue(run, opt);
        return baseProcess != null;
    }

    /**
     * 构建对象
     *
     * @param yml yml 内容
     * @return DslYmlDto
     */
    public static DslYmlDto build(String yml) {
        InputStream inputStream = new ByteArrayInputStream(yml.getBytes());
        return YamlUtil.load(inputStream, DslYmlDto.class);
    }

    /**
     * 运行管理
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class Run extends BaseJsonModel {
        private Start start;
        private Status status;
        private Stop stop;
        private Restart restart;
        private Reload reload;
        /**
         * 文件变动是否执行重新加载
         */
        private Boolean fileChangeReload;
        /**
         * 在指定目录执行
         */
        private String execPath;
    }

    /**
     * 重新加载
     *
     * @see io.voyager1.socket.ConsoleCommandOp
     */
    public static class Reload extends BaseProcess {

    }

    /**
     * 启动流程
     *
     * @see io.voyager1.socket.ConsoleCommandOp
     */
    public static class Start extends BaseProcess {

    }

    /**
     * 获取状态流程
     *
     * @see io.voyager1.socket.ConsoleCommandOp
     */
    public static class Status extends BaseProcess {

    }

    /**
     * 停止流程
     *
     * @see io.voyager1.socket.ConsoleCommandOp
     */
    public static class Stop extends BaseProcess {

    }

    /**
     * 重启流程
     *
     * @see io.voyager1.socket.ConsoleCommandOp
     */
    public static class Restart extends BaseProcess {

    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    public static class BaseProcess extends BaseJsonModel {
        /**
         * 脚本 ID
         */
        private String scriptId;
        /**
         * 执行参数
         */
        private String scriptArgs;
        /**
         * 执行脚本的环境变量
         */
        private Map<String, String> scriptEnv;
    }

    @Data
    public static class FileConfig {
        /**
         * 保留文件备份数量
         */
        private Integer backupCount;

        /**
         * 指定备份文件后缀，如果未指定则备份所有类型文件
         */
        private String[] backupSuffix;

        /**
         * 项目文件备份路径
         */
        private String backupPath;

        /**
         * 是否开启差异备份，默认使用差异备份
         */
        private Boolean diffBackup;
    }

    @Data
    public static class Config {
        /**
         * 是否自动将控制台日志文件备份
         */
        private Boolean autoBackToFile;
    }


    /**
     * 获取 dsl 流程信息
     *
     * @param opt 操作
     * @return 结果
     */
    public static DslYmlDto.BaseProcess tryDslProcess(DslYmlDto build, String opt) {
        return Optional.ofNullable(build)
            .map(DslYmlDto::getRun)
            .map(run -> (DslYmlDto.BaseProcess) ReflectUtil.getFieldValue(run, opt))
            .orElse(null);
    }

    /**
     * 获取 dsl 流程信息
     *
     * @param opt 操作
     * @return 结果
     */
    public DslYmlDto.BaseProcess tryDslProcess(String opt) {
        return tryDslProcess(this, opt);
    }

    /**
     * 获取 dsl 流程信息
     *
     * @param opt 操作
     * @return 结果
     */
    public DslYmlDto.BaseProcess getDslProcess(String opt) {
        DslYmlDto.BaseProcess baseProcess = this.tryDslProcess(opt);
        Assert.notNull(baseProcess, String.format("DSL 未配置运行管理或者未配置 %s 流程", opt));
        return baseProcess;
    }
}
