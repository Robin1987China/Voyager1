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

package io.voyager1.model.docker;

import io.voyager1.util.PropIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.func.assets.model.MachineDockerModel;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.model.data.WorkspaceModel;

/**
 * @since 2022/2/13
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "INFRA_DOCKER_SWARM",
    nameKey = "docker 集群信息")
public class DockerSwarmInfoMode extends BaseWorkspaceModel {
    /**
     * 集群名称
     */
    private String name;
    /**
     * 集群ID
     */
    private String swarmId;

    /**
     * 集群容器标签
     */
    private String tag;

    @PropIgnore
    private MachineDockerModel machineDocker;

    @PropIgnore
    private WorkspaceModel workspace;
}
