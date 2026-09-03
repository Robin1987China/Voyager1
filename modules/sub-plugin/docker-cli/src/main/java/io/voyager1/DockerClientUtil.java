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

import io.voyager1.util.FileUtil;
import io.voyager1.util.ArrayUtil;
import io.voyager1.util.StrUtil;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.LogContainerCmd;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Frame;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.util.LogRecorder;
import org.springframework.util.Assert;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @since 2022/2/7
 */
public class DockerClientUtil {

    /**
     * 拉取容器日志
     *
     * @param dockerClient 连接
     * @param containerId  容器ID
     * @param charset      字符编码
     * @param consumer     回调
     * @throws InterruptedException 打断异常
     */
    public static void pullLog(DockerClient dockerClient,
                               String containerId,
                               Boolean timestamps,
                               Integer tail,
                               Charset charset,
                               Consumer<String> consumer,
                               Consumer<AutoCloseable> consumerAdapter) throws InterruptedException {
        Assert.state(tail == null || tail > 0, "tail > 0");
        // 获取日志
        LogContainerCmd logContainerCmd = dockerClient.logContainerCmd(containerId);
        if (tail == null) {
            logContainerCmd.withTailAll();
        } else {
            logContainerCmd.withTail(tail);
        }
        ResultCallback.Adapter<Frame> exec = logContainerCmd
            .withTimestamps(timestamps)
            .withStdOut(true)
            .withStdErr(true)
            .withFollowStream(true)
            .exec(new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame object) {
                    byte[] payload = object.getPayload();
                    if (payload == null) {
                        return;
                    }
                    String s = new String(payload, charset);
                    consumer.accept(s);
                }
            });
        Optional.ofNullable(consumerAdapter).ifPresent(consumer1 -> consumer1.accept(exec));
        //
        exec.awaitCompletion();
    }

    /**
     * 将容器文件下载到本地
     *
     * @param dockerClient  容器连接
     * @param containerId   容器ID
     * @param logRecorder   日志记录
     * @param resultFile    结果文件
     * @param resultFileOut 保存目录
     */
    public static void copyArchiveFromContainerCmd(DockerClient dockerClient, String containerId, LogRecorder logRecorder, String resultFile, String resultFileOut) {
        logRecorder.system("download file from : {}", resultFile);
        File tmpDir = FileUtil.getTmpDir();
        File fileArchive = FileUtil.file(tmpDir, "voyager1", "docker-temp-archive", containerId);
        try {
            try (InputStream stream = dockerClient.copyArchiveFromContainerCmd(containerId, resultFile).exec();
                 TarArchiveInputStream tarStream = new TarArchiveInputStream(stream)) {
                TarArchiveEntry tarArchiveEntry;
                while ((tarArchiveEntry = tarStream.getNextEntry()) != null) {
                    if (!tarStream.canReadEntryData(tarArchiveEntry)) {
                        logRecorder.systemWarning("不能读取tarArchiveEntry {}", tarArchiveEntry.getName());
                    }
                    if (tarArchiveEntry.isDirectory()) {
                        continue;
                    }
                    String archiveEntryName = tarArchiveEntry.getName();
                    // 截取第一级目录
                    archiveEntryName = StrUtil.subAfter(archiveEntryName, "/", false);
                    // 可能中包含文件 使用原名称
                    archiveEntryName = (archiveEntryName == null || archiveEntryName.isEmpty() ? tarArchiveEntry.getName() : archiveEntryName);
                    File currentFile = FileUtil.file(fileArchive, archiveEntryName);
                    FileUtil.mkParentDirs(currentFile);
                    FileUtil.writeFromStream(tarStream, currentFile, false);
                }
            } catch (NotFoundException notFoundException) {
                logRecorder.systemWarning("容器中没有找到执行结果文件: {}", notFoundException.getMessage());
            } catch (Exception e) {
                logRecorder.error("无法获取容器执行结果文件", e);
            }
            // github pr 71
            File[] files = fileArchive.listFiles();
            if (files == null) {
                logRecorder.systemWarning("临时结果文件不存在: {}", fileArchive.getAbsolutePath());
                return;
            }
            File resultFileOutFile = FileUtil.file(resultFileOut);
            if (ArrayUtil.length(files) == 1) {
                FileUtil.mkParentDirs(resultFileOutFile);
                FileUtil.move(files[0], resultFileOutFile, true);
            } else {
                FileUtil.mkdir(resultFileOutFile);
                FileUtil.moveContent(fileArchive, resultFileOutFile, true);
            }
        } finally {
            FileUtil.del(fileArchive);
        }
    }

    /**
     * 删除容器
     *
     * @param dockerClient docker 连接
     * @param containerId  容器ID
     */
    public static void removeContainerCmd(DockerClient dockerClient, String containerId) {
        if (containerId == null) {
            return;
        }
        // 清除容器
        dockerClient.removeContainerCmd(containerId)
            .withRemoveVolumes(true)
            .withForce(true)
            .exec();
    }
}
