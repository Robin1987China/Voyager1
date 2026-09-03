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

package io.voyager1.common;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.voyager1.Voyager1Application;
import io.voyager1.core.AppType;
import io.voyager1.cron.CronUtils;
import io.voyager1.cron.ICron;
import io.voyager1.event.IAsyncLoad;
import io.voyager1.event.ICacheTask;
import io.voyager1.event.ISystemTask;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.util.*;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 启动 、关闭监听
 *
 * @since 2019/4/7
 */
@Slf4j
@Configuration
public class Voyager1ApplicationEvent implements ApplicationListener<ApplicationEvent>, ApplicationContextAware {

    private final Voyager1Application configBean;

    private static int oldJarsCount = 2;

    public static void setOldJarsCount(int oldJarsCount) {
        Voyager1ApplicationEvent.oldJarsCount = oldJarsCount;
    }

    public Voyager1ApplicationEvent(Voyager1Application configBean) {
        this.configBean = configBean;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // 启动最后的预加载
        if (event instanceof ApplicationReadyEvent) {

        } else if (event instanceof ContextClosedEvent) {
            //
        }
    }


    private void checkPath() {
        String path = ExtConfigBean.getPath();
        String extConfigPath;
        try {
            extConfigPath = ExtConfigBean.getResource().getURL().toString();
        } catch (IOException e) {
            throw Lombok.sneakyThrow(e);
        }
        File file = FileUtil.file(path);
        try {
            FileUtil.mkdir(file);
            file = FileUtil.createTempFile("voyager1", ".temp", file, true);
        } catch (Exception e) {
            log.error(String.format("voyager1创建数据目录失败，目录位置：{}, 请检查当前用户是否有权限访问该目录或者修改配置文件：{} 中的 voyager1.path 属性中确保目录存在读写权限", path, extConfigPath), e);
            throw new io.voyager1.system.Voyager1RuntimeException("数据目录创建失败: " + path, e);
        }
        FileUtil.del(file);
        String tip1 = "当前数据路径";
        String tip2 = "外部配置文件路径";
        log.info("Voyager1[{}] {}：{} {}：{}", Voyager1Manifest.getInstance().getVersion(), tip1, path, tip2, extConfigPath);
    }

    private void install() {
        String installId;
        File file = FileUtil.file(configBean.getDataPath(), Const.INSTALL);
        if (file.exists()) {
            JSONObject jsonObject;
            try {
                jsonObject = JsonFileUtil.readJson(file);
            } catch (FileNotFoundException e) {
                throw Lombok.sneakyThrow(e);
            }
            installId = jsonObject.getString("installId");
            Assert.hasText(installId, "数据错误,安装 ID 不存在");
            log.info("本机安装 ID 为：{}", installId);
        } else {
            JSONObject jsonObject = new JSONObject();
            installId = java.util.UUID.randomUUID().toString().replace("-", "");
            jsonObject.put("installId", installId);
            jsonObject.put("installTime", DateTime.now().toString());
            String value = "请勿删除此文件,删除后关联 id 将失效";
            jsonObject.put("desc", value);
            JsonFileUtil.saveJson(file.getAbsolutePath(), jsonObject);
            log.info("安装成功,本机安装 ID 为：{}", installId);
        }
        Voyager1Manifest.getInstance().setInstallId(installId);
    }

    /**
     * 检查更新包文件状态
     */
    private void checkUpdate() {
        File runFile = Voyager1Manifest.getRunPath().getParentFile();
        String upgrade = FileUtil.file(runFile, Const.UPGRADE).getAbsolutePath();
        JSONObject jsonObject = null;
        try {
            jsonObject = JsonFileUtil.readJson(upgrade);
        } catch (FileNotFoundException ignored) {
        }
        if (jsonObject != null) {
            String beforeJar = jsonObject.getString("beforeJar");
            String newJar = jsonObject.getString("newJar");
            if ((beforeJar != null && !beforeJar.isEmpty())) {
                File beforeJarFile = FileUtil.file(runFile, beforeJar);
                if (beforeJarFile.exists()) {
                    if (this.canMvOldJar(jsonObject, runFile)) {
                        File oldJars = Voyager1Manifest.getOldJarsPath();
                        FileUtil.mkdir(oldJars);
                        FileUtil.move(beforeJarFile, oldJars, true);
                        log.info("备份旧程序包：{}", beforeJar);
                    } else {
                        log.debug("备份旧程序包失败：{},因为新程序包不存在：{}", beforeJar, newJar);
                    }
                } else {
                    log.debug("备份旧程序包失败：{},因为旧程序包不存在", beforeJar);
                }
            }
        }
        clearOldJar();
        // windows 备份日志
        //        if (SystemUtil.getOsInfo().isWindows()) {
        //            boolean logBack = jsonObject.getBooleanValue("logBack");
        //            String oldLogName = jsonObject.getString("oldLogName");
        //            if (logBack && (oldLogName != null && !oldLogName.isEmpty())) {
        //                File scriptFile = Voyager1Manifest.getScriptFile();
        //                File oldLog = FileUtil.file(scriptFile.getParentFile(), oldLogName);
        //                if (oldLog.exists()) {
        //                    File logBackDir = FileUtil.file(scriptFile.getParentFile(), "log");
        //                    FileUtil.move(oldLog, logBackDir, true);
        //                }
        //            }
        //        }
    }

    private boolean canMvOldJar(JSONObject jsonObject, File runFile) {
        String newJar = jsonObject.getString("newJar");
        if ((newJar == null || newJar.isEmpty())) {
            return false;
        }
        File newJarFile = FileUtil.file(runFile, newJar);
        return FileUtil.exist(newJarFile);
    }

    private void clearOldJar() {
        File oldJars = Voyager1Manifest.getOldJarsPath();
        List<File> files = FileUtil.loopFiles(oldJars, 1, file -> StrUtil.endWith(file.getName(), FileUtil.JAR_FILE_EXT, true));
        if ((files == null || files.isEmpty())) {
            return;
        }
        // 排序
        files.sort((o1, o2) -> FileUtil.lastModifiedTime(o2).compareTo(FileUtil.lastModifiedTime(o1)));
        // 截取
        int size = (files == null ? 0 : files.size());
        files = files.subList(oldJarsCount, size);
        // 删除文件
        files.forEach(file -> {
            FileUtil.del(file);
            log.debug("删除旧程序包：{}", file.getAbsolutePath());
        });
    }


    @SuppressWarnings("rawtypes")
    private void statLoad() {
        ThreadUtil.execute(() -> {
            // 加载定时器
            Map<String, ICron> cronMap = SpringContextHolder.getApplicationContext().getBeansOfType(ICron.class);
            cronMap.forEach((name, iCron) -> {
                int startCron = iCron.startCron();
                if (startCron > 0) {
                    log.debug("{} 定时任务已经自动启动:{}", name, startCron);
                }
            });
            Map<String, IAsyncLoad> asyncLoadMap = SpringContextHolder.getApplicationContext().getBeansOfType(IAsyncLoad.class);
            asyncLoadMap.forEach((name, asyncLoad) -> asyncLoad.startLoad());
            //
        });
    }

    /**
     * 输出启动成功的 日志
     */
    private void success() {
        AppType type = Voyager1Manifest.getInstance().getType();
        int port = configBean.getPort();
        String address = configBean.getAddress();
        String localhostStr = Opt.ofBlankAble(address).orElseGet(NetUtil::getLocalhostStr);
        String url = String.format("http://%s:%s", localhostStr, port);
        if (type == AppType.Server) {
            log.info("{} 成功启动，可以愉快使用 => {} 【当前地址仅供参考】", type, url);
        } else if (type == AppType.Agent) {
            log.info("{} 启动成功,请前往服务端配置使用,当前节点地址 => {} 【当前地址仅供参考】", type, url);
        }
    }


    private void clearTemp() {
        log.debug("自动清理临时目录");
        File file = configBean.getTempPath();
        /**
         * use 's FileUtil.del method just put file as param not file's path
         * or else,  may be return Accessdenied exception
         */
        try {
            FileUtil.del(file);
        } catch (Exception e) {
            // Try again  jzy 2021-07-31
            log.warn("尝试删除临时文件夹失败，尝试处理只读权限：{}", e.getMessage());
            List<File> files = FileUtil.loopFiles(file);
            long count = files.stream().map(file12 -> file12.setWritable(true)).filter(aBoolean -> aBoolean).count();
            log.warn("临时文件夹累计文件数：{}，处理成功数：{}", (files == null ? 0 : files.size()), count);
            try {
                FileUtil.del(file.toPath());
            } catch (Exception e1) {
                e1.addSuppressed(e);
                boolean causedBy = ExceptionUtil.isCausedBy(e1, AccessDeniedException.class);
                if (causedBy) {
                    log.error("{}{}", "清除临时文件失败,请手动清理：", FileUtil.getAbsolutePath(file), e);
                    return;
                }
                log.error("{}{}", "清除临时文件失败,请检查目录：", FileUtil.getAbsolutePath(file), e);
            }
        }
    }

    @Bean
    public MappingJackson2HttpMessageConverter objectMapper() {
        ObjectMapper build = createJackson();
        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter(build);
//        messageConverter.setDefaultCharset(StandardCharsets.UTF_8);
        return messageConverter;
    }


    /**
     * jackson 配置
     *
     * @return mapper
     */
    private ObjectMapper createJackson() {
        Jackson2ObjectMapperBuilder jackson2ObjectMapperBuilder = Jackson2ObjectMapperBuilder.json();
        jackson2ObjectMapperBuilder.simpleDateFormat("yyyyMMddHHmmss");
        ObjectMapper build = jackson2ObjectMapperBuilder.build();
        // 忽略空
        build.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 驼峰转下划线
        //        build.setPropertyNamingStrategy(new PropertyNamingStrategy.SnakeCaseStrategy());
        // long to String
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        build.registerModule(simpleModule);
        //
        build.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//        build.activateDefaultTyping(objectMapper.getPolymorphicTypeValidator(), ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);

        return build;
    }

    /**
     * 异步退出，避免 springboot 锁 synchronized (this.startupShutdownMonitor)
     *
     * @param code 退出码
     * @see AbstractApplicationContext#refresh()
     * @see AbstractApplicationContext#close()
     */
    public static void asyncExit(int code) {
        ThreadUtil.execute(() -> System.exit(code));
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        //
        File file = FileUtil.file(Voyager1Application.getInstance().getDataPath(), Const.REMOTE_VERSION);
        System.setProperty("VOYAGER1_REMOTE_VERSION_CACHE_FILE", file.getAbsolutePath());
        Voyager1Manifest voyager1Manifest = Voyager1Manifest.getInstance();
        System.setProperty("VOYAGER1_IS_DEBUG", String.valueOf(voyager1Manifest.isDebug()));
        System.setProperty("VOYAGER1_TYPE", voyager1Manifest.getType().name());
        System.setProperty("VOYAGER1_VERSION", voyager1Manifest.getVersion());
        // 检查目录权限
        this.checkPath();
        this.install();
        System.setProperty("VOYAGER1_INSTALL_ID", voyager1Manifest.getInstallId());
        // 清空临时目录
        this.clearTemp();
        // 开始加载子模块
        Map<String, ILoadEvent> loadEventMap = applicationContext.getBeansOfType(ILoadEvent.class);
        loadEventMap.values()
            .stream()
            .sorted((o1, o2) -> CompareUtil.compare(o1.getOrder(), o2.getOrder()))
            .forEach(iLoadEvent -> {
                try {
                    iLoadEvent.afterPropertiesSet(applicationContext);
                } catch (Exception e) {
                    throw Lombok.sneakyThrow(e);
                }
            });
        // 检查更新文件
        this.checkUpdate();
        // 开始异常加载
        this.statLoad();
        // 提示成功消息
        this.success();
    }

    @Configuration
    public static class SystemEvent implements ILoadEvent, SmartInitializingSingleton {

        @Override
        public int getOrder() {
            return LOWEST_PRECEDENCE;
        }

        @Override
        public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
            CronUtils.upsert("system_monitor", "0 0 0,12 * * ?", this::executeTask);
            CronUtils.upsert("system_cache", "0 0/10 * * * ?", this::refresh);
        }

        @Override
        public void afterSingletonsInstantiated() {
            // 所有单例 bean 实例化完成后启动执行一次
            try {
                this.executeTask();
                this.refresh();
            } catch (Exception e) {
                log.error("执行系统任务异常", e);
            }
        }

        private void executeTask() {
            Map<String, ISystemTask> taskMap = SpringContextHolder.getBeansOfType(ISystemTask.class);
            Optional.ofNullable(taskMap).ifPresent(map -> map.values().forEach(ISystemTask::executeTask));
        }

        private void refresh() {
            Map<String, ICacheTask> taskMap = SpringContextHolder.getBeansOfType(ICacheTask.class);
            Optional.ofNullable(taskMap).ifPresent(map -> map.values().forEach(ICacheTask::refreshCache));
        }
    }
}
