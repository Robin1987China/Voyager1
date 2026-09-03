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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * @since 23/12/25 025
 */
@Data
@ConfigurationProperties("voyager1.node")
public class NodeConfig {
    /**
     * 检查节点心跳间隔时间,最小值 5 秒
     */
    private int heartSecond = 30;

    public int getHeartSecond() {
        return Math.max(this.heartSecond, 5);
    }

    /**
     * 上传文件的超时时间 单位秒,最短5秒中
     */
    private int uploadFileTimeout = 300;

    /**
     * 节点文件分片上传大小，单位 M
     */
    private int uploadFileSliceSize = 1;

    /**
     * 节点文件分片上传并发数,最小1 最大 服务端 CPU 核心数
     */
    private int uploadFileConcurrent = 2;
    /**
     * web socket 消息最大长度
     */
    private DataSize webSocketMessageSizeLimit = DataSize.ofMegabytes(5);

    public int getUploadFileTimeout() {
        return Math.max(this.uploadFileTimeout, 5);
    }

    public int getUploadFileSliceSize() {
        return Math.max(this.uploadFileSliceSize, 1);
    }

    public void setUploadFileConcurrent(int uploadFileConcurrent) {
        this.uploadFileConcurrent = Math.min(Math.max(uploadFileConcurrent, 1), Runtime.getRuntime().availableProcessors());
    }

    /**
     * 节点统计日志保留天数，如果小于等于 0 不自动删除
     */
    private int statLogKeepDays = 3;
}
