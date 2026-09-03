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

import io.voyager1.util.PropIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import io.voyager1.core.db.TableName;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.model.BaseMachineModel;

/**
 * 节点实体
 *
 * @see MachineNodeModel
 * @since 2019/4/16
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "INFRA_NODE",
    nameKey = "节点信息")
@Data
@NoArgsConstructor
public class NodeModel extends BaseMachineModel {

    @Deprecated
    private String url;
    @Deprecated
    private String loginName;
    @Deprecated
    private String loginPwd;
    private String name;

    /**
     * 节点协议
     */
    @Deprecated
    private String protocol;
    /**
     * 开启状态，如果关闭状态就暂停使用节点 1 启用
     */
    private Integer openStatus;
    /**
     * 节点超时时间
     */
    @Deprecated
    private Integer timeOut;
    /**
     * 绑定的sshId
     */
    private String sshId;

    /**
     * http 代理
     */
    @Deprecated
    private String httpProxy;
    /**
     * https 代理 类型
     */
    @Deprecated
    private String httpProxyType;
    /**
     * 排序
     */
    private Float sortValue;

    @PropIgnore
    private MachineNodeModel machineNodeData;

    @PropIgnore
    private WorkspaceModel workspace;
    /**
     * voyager1 项目数
     */
    private Integer voyager1ProjectCount;
    /**
     * voyager1 脚本数据
     */
    private Integer voyager1ScriptCount;

    public boolean isOpenStatus() {
        return openStatus != null && openStatus == 1;
    }

    public NodeModel(String id) {
        this.setId(id);
    }

    public NodeModel(String id, String workspaceId) {
        this.setId(id);
        this.setWorkspaceId(workspaceId);
    }
}
