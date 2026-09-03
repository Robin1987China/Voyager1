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
import io.voyager1.Voyager1Application;
import io.voyager1.core.AppType;
import io.voyager1.core.api.ApiResult;
import io.voyager1.util.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * 远程的版本信息
 *
 * <pre>
 * {
 * "tag_name": "v2.6.4",
 * "agentUrl": "",
 * "serverUrl": "",
 * "changelog": ""
 * }
 * </pre>
 *
 * @since 2021/9/19
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class RemoteVersion extends io.voyager1.RemoteVersion {

    @Override
    public String toString() {
        return JSONObject.toJSONString(io.voyager1.RemoteVersion.cacheInfo());
    }

    /**
     * 下载
     *
     * @param savePath    下载文件保存路径
     * @param type        类型
     * @param checkRepeat 是否验证重复
     * @return 保存的全路径
     * @throws IOException 异常
     * @see HttpDownloader#requestDownload(String, int)
     */
    public static Tuple download(String savePath, AppType type, boolean checkRepeat) throws IOException {
        io.voyager1.RemoteVersion remoteVersion = io.voyager1.RemoteVersion.loadRemoteInfo();
        Assert.notNull(remoteVersion, "没有可用的新版本升级:-1");
        // 检查是否存在下载地址
        String remoteUrl = type.getRemoteUrl(remoteVersion);
        Assert.hasText(remoteUrl, "存在新版本,下载地址不可用");
        // 下载
        File downloadFileFromUrl;
        try {
            downloadFileFromUrl = HttpUtil.downloadFileFromUrl(remoteUrl, savePath);
        } catch (HttpException httpException) {
            String message = httpException.getMessage();
            if (StrUtil.containsAnyIgnoreCase(message, "Server response error with status code: [403]")) {
                String msg = "可能是下载授权码错误或者对应授权码被禁用以及触发限流机制";
                throw new IllegalStateException(message + " " + msg);
            }
            throw Lombok.sneakyThrow(httpException);
        }
        // 解析压缩包
        File file = Voyager1Manifest.zipFileFind(FileUtil.getAbsolutePath(downloadFileFromUrl), type, savePath);
        // 检查
        ApiResult<Tuple> error = Voyager1Manifest.checkVoyager1Jar(FileUtil.getAbsolutePath(file), type, checkRepeat);
        Assert.state(error.success(), error.getMsg());
        return error.getData();
    }

    /**
     * 下载
     *
     * @param savePath 下载文件保存路径
     * @param type     类型
     * @return 保存的全路径
     * @throws IOException 异常
     */
    public static Tuple download(String savePath, AppType type) throws IOException {
        return download(savePath, type, true);
    }

    /**
     * 升级
     *
     * @param savePath 下载文件保存路径
     * @throws IOException 异常
     */
    public static void upgrade(String savePath) throws IOException {
        upgrade(savePath, null);
    }

    /**
     * 升级
     *
     * @param savePath 下载文件保存路径
     * @param consumer 执行申请前回调
     * @throws IOException 异常
     */
    public static void upgrade(String savePath, Consumer<Tuple> consumer) throws IOException {
        AppType type = Voyager1Manifest.getInstance().getType();
        // 下载
        Tuple data = download(savePath, type);
        File file = data.get(3);
        // 基础检查
        String path = FileUtil.getAbsolutePath(file);
        String version = data.get(0);
        Voyager1Manifest.releaseJar(path, version);
        //
        if (consumer != null) {
            consumer.accept(data);
        }
        Voyager1Application.restart();
    }
}
