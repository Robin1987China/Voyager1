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

package io.voyager1.configuration;

import io.voyager1.util.DateTime;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IdUtil;
import io.voyager1.util.ObjectUtil;
import lombok.Data;
import io.voyager1.Voyager1Application;
import io.voyager1.common.ILoadEvent;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.util.BaseFileTailWatcher;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.util.Optional;

/**
 * 插件端配置信息
 *
 * @since 2022/12/16
 */

@Configuration
@ConfigurationProperties("voyager1")
@Data
@EnableConfigurationProperties({ProjectConfig.class, ProjectLogConfig.class, SystemConfig.class, AgentAuthorize.class, MonitorConfig.class, MonitorConfig.NetworkConfig.class})
public class AgentConfig implements ILoadEvent, InitializingBean {

    private final Voyager1Application voyager1Application;

    public AgentConfig(Voyager1Application voyager1Application) {
        this.voyager1Application = voyager1Application;
    }

    /**
     * 授权配置
     */
    private AgentAuthorize authorize;

    /**
     * 项目配置
     */
    private ProjectConfig project;
    /**
     * 系统配置参数
     */
    private SystemConfig system;
    /**
     * 监控配置
     */
    private MonitorConfig monitor;

    /**
     * 数据目录
     */
    private String path;

    /**
     * 初始读取日志文件行号
     */
    private Integer initReadLine = 10;

    public AgentAuthorize getAuthorize() {
        return Optional.ofNullable(this.authorize).orElseGet(() -> {
            this.authorize = new AgentAuthorize();
            return this.authorize;
        });
    }

    public ProjectConfig getProject() {
        return Optional.ofNullable(this.project).orElseGet(() -> {
            this.project = new ProjectConfig();
            return this.project;
        });
    }

    public SystemConfig getSystem() {
        return Optional.ofNullable(this.system).orElseGet(() -> {
            this.system = new SystemConfig();
            return this.system;
        });
    }

    /**
     * 获取临时文件存储路径，并添加一个随机字符串
     *
     * @return 文件夹
     */
    public String getTempPathName() {
        File file = getTempPath();
        // 生成随机的一个文件夹、避免同一个节点分发同一个文件，mv 失败
        return FileUtil.getAbsolutePath(FileUtil.file(file, java.util.UUID.randomUUID().toString().replace("-", "")));
    }

    /**
     * 获取临时文件存储路径
     *
     * @return 文件夹
     */
    public String getFixedTempPathName() {
        File file = getTempPath();
        return FileUtil.getAbsolutePath(file);
    }


    /**
     * 获取临时文件存储路径
     *
     * @return file
     */
    public File getTempPath() {
        File file = voyager1Application.getTempPath();
        file = FileUtil.file(file, DateTime.now().toDateStr());
        FileUtil.mkdir(file);
        return file;
    }


    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        // 登录名不能为空
        this.getAuthorize().init(voyager1Application);
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        int initReadLine = (this.initReadLine != null ? this.initReadLine : 10);
        BaseFileTailWatcher.setInitReadLine(initReadLine);
        ExtConfigBean.setPath(path);
    }
}
