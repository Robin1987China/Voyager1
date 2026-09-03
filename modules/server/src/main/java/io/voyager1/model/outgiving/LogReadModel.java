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

package io.voyager1.model.outgiving;

import io.voyager1.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.util.StringUtil;

import java.util.List;

/**
 * 日志阅读
 *
 * @since 2022/5/15
 */
@TableName(value = "OPS_LOG_FILE",
    nameKey = "日志阅读")
@Data
@EqualsAndHashCode(callSuper = true)
public class LogReadModel extends BaseWorkspaceModel {

    /**
     * 名称
     */
    private String name;
    /**
     * 节点下的项目列表
     *
     * @see Item
     */
    private String nodeProject;
    /**
     * 缓存操作数据
     */
    private String cacheData;

    /**
     * {"op":"showlog","projectId":"python",
     * "search":true,"useProjectId":"python",
     * "useNodeId":"localhost",
     * "beforeCount":0,"afterCount":10,
     * "head":0,"tail":100,"first":"false",
     * "logFile":"/run.log"}
     */
    @Data
    public static class CacheDta {
        /**
         * 日志文件名称
         */
        private String logFile;
        /**
         * 显示关键词，后多少行
         */
        private Integer afterCount;
        /**
         * 显示关键词，前多少行
         */
        private Integer beforeCount;
        private Integer head;
        private Integer tail;
        private Boolean first;
        /**
         * 搜索关键词
         */
        private String keyword;
        /**
         * 使用等节点ID
         */
        private String useProjectId;
        private String useNodeId;
    }


    public List<Item> nodeProjectList() {
        return StringUtil.jsonConvertArray(nodeProject, Item.class);
    }

    /**
     * 判断是否包含某个项目id
     *
     * @param projectId 项目id
     * @return true 包含
     */
    public boolean checkContains(String nodeId, String projectId) {
        return getNodeProject(nodeId, projectId) != null;
    }

    /**
     * 获取节点的项目信息
     *
     * @param nodeId    节点
     * @param projectId 项目
     * @return outGivingNodeProject
     */
    public Item getNodeProject(String nodeId, String projectId) {
        List<Item> thisPs = nodeProjectList();
        if (thisPs == null) {
            return null;
        }
        for (Item outGivingNodeProject1 : thisPs) {
            if ((outGivingNodeProject1.getProjectId() != null && outGivingNodeProject1.getProjectId().equalsIgnoreCase(projectId)) && (outGivingNodeProject1.getNodeId() != null && outGivingNodeProject1.getNodeId().equalsIgnoreCase(nodeId))) {
                return outGivingNodeProject1;
            }
        }
        return null;
    }


    public static class Item extends BaseNodeProject {

    }
}
