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

import io.voyager1.util.DigestUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.model.data.NodeModel;
import org.springframework.util.Assert;

/**
 * 节点 数据
 *
 * @since 2021/12/05
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class BaseNodeModel extends BaseWorkspaceModel {

    /**
     * 节点Id
     *
     * @see NodeModel
     */
    private String nodeId;
    /**
     * 节点名称
     */
    private String nodeName;
    /**
     * 工作空间名称
     */
    private String workspaceName;

    @Override
    public String toString() {
        return super.toString();
    }

    public String fullId() {
        String workspaceId = this.getWorkspaceId();

        String nodeId = this.getNodeId();

        String dataId = this.dataId();

        return BaseNodeModel.fullId(workspaceId, nodeId, dataId);
    }

    public static String fullId(String workspaceId, String nodeId, String dataId) {

        Assert.hasText(workspaceId, "workspaceId");

        Assert.hasText(workspaceId, "nodeId");

        Assert.hasText(workspaceId, "dataId");
        return DigestUtil.sha1(workspaceId + nodeId + dataId);
    }

    /**
     * 获取数据ID
     *
     * @return 数据ID
     */
    public abstract String dataId();

    /**
     * 设置数据ID
     *
     * @param id 数据ID
     */
    public abstract void dataId(String id);
}
