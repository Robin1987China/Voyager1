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

package io.voyager1;

import io.voyager1.common.Const;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.core.AppType;
import io.voyager1.core.AppTypeBinding;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.system.Voyager1RuntimeException;
import io.voyager1.util.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Voyager1
 */
@Slf4j
@Configuration
@Getter
public class Voyager1Application implements DisposableBean, InitializingBean {
    /**
     * 程序端口
     */
    @Value("${server.port}")
    private int port;

    @Value("${server.address:}")
    private String address;
    /**
     * 数据目录缓存大小
     */
    private long dataSizeCache;


    private static volatile Voyager1Application voyager1Application;

    private static final Map<String, ExecutorService> LINK_EXECUTOR_SERVICE = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 单利模式
     *
     * @return config
     */
    public static Voyager1Application getInstance() {
        if (voyager1Application == null) {
            synchronized (Voyager1Application.class) {
                if (voyager1Application == null) {
                    voyager1Application = SpringContextHolder.getBean(Voyager1Application.class);
                }
            }
        }
        return voyager1Application;
    }

    /**
     * 获取项目运行数据存储文件夹路径
     *
     * @return 文件夹路径
     */
    public String getDataPath() {
        String dataPath = FileUtil.normalize(ExtConfigBean.getPath() + "/" + Const.DATA);
        FileUtil.mkdir(dataPath);
        return dataPath;
    }

//    /**
//     * 执行脚本
//     *
//     * @param inputStream 脚本内容
//     * @param function    回调分发
//     * @param <T>         值类型
//     * @return 返回值
//     */
//    public <T> T execScript(InputStream inputStream, Function<File, T> function) {
//        String sshExecTemplate = IoUtil.readUtf8(inputStream);
//        return this.execScript(sshExecTemplate, function);
//    }

    /**
     * 执行脚本
     *
     * @param context  脚本内容
     * @param function 回调分发
     * @param <T>      值类型
     * @return 返回值
     */
    public <T> T execScript(String context, Function<File, T> function) {
        String dataPath = this.getDataPath();
        File scriptFile = FileUtil.file(dataPath, Const.SCRIPT_RUN_CACHE_DIRECTORY, String.format("%s.%s", java.util.UUID.randomUUID().toString().replace("-", ""), CommandUtil.SUFFIX));
        FileUtils.writeScript(context, scriptFile, ExtConfigBean.getConsoleLogCharset());
        try {
            return function.apply(scriptFile);
        } finally {
            FileUtil.del(scriptFile);
        }
    }

    /**
     * 获取临时文件存储路径
     *
     * @return file
     */
    public File getTempPath() {
        File file = new File(this.getDataPath());
        file = FileUtil.file(file, "temp");
        FileUtil.mkdir(file);
        return file;
    }

    /**
     * 数据目录大小
     *
     * @return byte
     */
    public long dataSize() {
        String dataPath = getDataPath();
        long size = FileUtil.size(FileUtil.file(dataPath));
        dataSizeCache = size;
        return size;
    }

    /**
     * 获取脚本模板路径
     *
     * @return file
     */
    public File getScriptPath() {
        return FileUtil.file(this.getDataPath(), Const.SCRIPT_DIRECTORY);
    }

    /**
     * 获取当前程序的类型
     *
     * @return Agent 或者 Server
     */
    public static AppType getAppType() {
        Map<String, Object> beansWithAnnotation = SpringContextHolder.getApplicationContext().getBeansWithAnnotation(AppTypeBinding.class);
        Class<?> voyager1AppClass = Optional.of(beansWithAnnotation)
            .map(map -> CollUtil.getFirst(map.values()))
            .map(Object::getClass)
            .orElseThrow(() -> new RuntimeException("没有找到 Voyager1 类型配置"));
        AppTypeBinding voyager1AppType = voyager1AppClass.getAnnotation(AppTypeBinding.class);
        return voyager1AppType.value();
    }

    public static Class<?> getAppClass() {
        Map<String, Object> beansWithAnnotation = SpringContextHolder.getApplicationContext().getBeansWithAnnotation(SpringBootApplication.class);
        return Optional.of(beansWithAnnotation)
            .map(map -> CollUtil.getFirst(map.values()))
            .map(Object::getClass)
            .orElseThrow(() -> new RuntimeException("没有找到运行的主类"));
    }

    /**
     * 重启自身
     * 分发会延迟2秒执行正式升级 重启命令
     *
     * @see Voyager1Manifest#releaseJar
     */
    public static void restart() {
        File runFile = Voyager1Manifest.getRunPath();
        File runPath = runFile.getParentFile();
        if (!runPath.isDirectory()) {
            throw new Voyager1RuntimeException(runPath.getAbsolutePath() + " error");
        }
        OsInfo osInfo = SystemUtil.getOsInfo();
        if (osInfo.isWindows()) {
            // 需要重新变更 stdout_log 文件来保证进程不被占用
            String format = String.format("stdout_%s.log", System.currentTimeMillis());
            FileUtil.writeString(format, FileUtil.file(runPath, "run.log"), StandardCharsets.UTF_8);
        }
        File scriptFile = Voyager1Manifest.getScriptFile();
        ThreadUtil.execute(() -> {
            // Waiting for method caller,For example, the interface response
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(2));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            try {
                String command = CommandUtil.generateCommand(scriptFile, "restart upgrade");
                File parentFile = scriptFile.getParentFile();
                if (osInfo.isWindows()) {
                    //String result = CommandUtil.execSystemCommand(command, scriptFile.getParentFile());
                    //log.debug("windows restart {}", result);
                    CommandUtil.asyncExeLocalCommand("start /b" + command, parentFile);
                } else {
                    String voyager1Service = SystemUtil.get("VOYAGER1_SERVICE");
                    if ((voyager1Service == null || voyager1Service.isEmpty())) {
                        CommandUtil.asyncExeLocalCommand(command, parentFile);
                    } else {
                        // 使用了服务
                        CommandUtil.asyncExeLocalCommand("systemctl restart " + voyager1Service, parentFile, null, true);
                    }
                }
            } catch (Exception e) {
                log.error("重启自身异常", e);
            }
        });
    }

    public static ScheduledExecutorService getScheduledExecutorService() {
        return (ScheduledExecutorService) LINK_EXECUTOR_SERVICE.computeIfAbsent("voyager1-system-task",
            s -> Executors.newScheduledThreadPool(4,
                r -> new Thread(r, "voyager1-system-task")));
    }

    /**
     * 注册线程池
     *
     * @param name            线程池名
     * @param executorService 线程池
     */
    public static void register(String name, ExecutorService executorService) {
        LINK_EXECUTOR_SERVICE.put(name, executorService);
    }

    /**
     * 关闭全局线程池
     */
    public static void shutdownGlobalThreadPool() {
        LINK_EXECUTOR_SERVICE.forEach((s, executorService) -> {
            if (!executorService.isShutdown()) {
                log.debug("关闭 {} 线程池", s);
                executorService.shutdownNow();
            }
        });
    }

    @Override
    public void destroy() throws Exception {
        AppType appType = getAppType();
        log.info("Voyager1 {} disposable", appType);
        shutdownGlobalThreadPool();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        register("Global", GlobalThreadPool.getExecutor());
    }
}
