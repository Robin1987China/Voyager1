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

import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.model.BaseJsonModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.common.i18n.I18nMessageUtil;
import org.springframework.util.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 节点分发授权
 *
 * @since 2019/4/22
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ServerWhitelist extends BaseJsonModel {

    public static final String ID = "OUTGIVING_WHITELIST";

    /**
     * 不同工作空间的 ID
     *
     * @param workspaceId 工作空间ID
     * @return id
     */
    public static String workspaceId(String workspaceId) {
        return ServerWhitelist.ID + "-" + workspaceId;
    }

    /**
     * 项目的授权
     */
    private List<String> outGiving;

    /**
     * 允许远程下载的 host
     */
    private Set<String> allowRemoteDownloadHost;

    /**
     * 静态目录
     */
    private List<String> staticDir;

    /**
     * 规范化路径
     *
     * @return list
     */
    public List<String> staticDir() {
        if (staticDir == null) {
            return new ArrayList<>();
        }
        return staticDir.stream()
            .map(s -> {
                // 规范化
                File file = FileUtil.file(s);
                String absolutePath = file.getAbsolutePath();
                return FileUtil.normalize(absolutePath);
            })
            .collect(Collectors.toList());
    }

    /**
     * 验证静态目录权限
     */
    public void checkStaticDir(String path) {
        List<String> dir = this.staticDir;
        boolean contains = (dir != null && dir.contains(path));
        Assert.state(contains, "没有当前静态目录权限");
    }

    /**
     * 判断指定 url 是否在授权范围
     *
     * @param url url 地址
     */
    public void checkAllowRemoteDownloadHost(String url) {
        Set<String> allowRemoteDownloadHost = this.getAllowRemoteDownloadHost();
        Assert.state((allowRemoteDownloadHost != null && !allowRemoteDownloadHost.isEmpty()), "还没有配置允许的远程地址");
        List<String> collect = allowRemoteDownloadHost.stream()
            .filter(s -> (url != null && url.startsWith(s)))
            .collect(Collectors.toList());
        Assert.state((collect != null && !collect.isEmpty()), "不允许下载当前地址的文件");
    }
}
