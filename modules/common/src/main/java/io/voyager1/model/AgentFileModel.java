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

package io.voyager1.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AgentFileModel extends BaseModel {

    /**
     * 保存Agent文件
     */
    public static final String ID = "AGENT_FILE";
    /**
     * 最新插件端包的文件名
     */
    public static final String ZIP_NAME = "agent.zip";
    /**
     * 默认空版本信息
     */
    public static final AgentFileModel EMPTY = new AgentFileModel();
    /**
     * 文件大小
     */
    private Long size;
    /**
     * 保存路径
     */
    private String savePath;
    /**
     * 版本号
     */
    private String version;
    /**
     * jar 打包时间
     */
    private String timeStamp;

    @Override
    public String toString() {
        return super.toString();
    }
}
