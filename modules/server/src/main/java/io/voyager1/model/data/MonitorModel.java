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

import io.voyager1.util.StrUtil;
import io.voyager1.model.BaseJsonModel;
import com.alibaba.fastjson2.JSON;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseEnum;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.util.StringUtil;

import java.util.List;

/**
 * 监控管理实体
 *
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "OPS_MONITOR",
    nameKey = "监控信息")
@Data
public class MonitorModel extends BaseWorkspaceModel {

    private String name;
    /**
     * 监控的项目
     */
    private String projects;
    /**
     * 报警联系人
     */
    private String notifyUser;
    /**
     * 异常后是否自动重启
     */
    private Boolean autoRestart;
    /**
     * 监控周期
     *
     * @see io.voyager1.model.Cycle
     */
    @Deprecated
    private Integer cycle;
    /**
     * 监控定时周期
     */
    private String execCron;
    /**
     * 监控开启状态
     */
    private Boolean status;
    /**
     * 报警状态
     */
    private Boolean alarm;
    /**
     * webhook
     */
    private String webhook;
    /**
     * 使用语言
     */
    private String useLanguage;
    /**
     * 静默时间
     */
    private Integer silenceTime;
    /**
     * 静默单位
     */
    private String silenceUnit;

    public String getExecCron() {
        if (execCron == null) {
            // 兼容旧版本
            if (cycle != null) {
                return String.format("0 0/%s * * * ?", cycle);
            }
        }
        return execCron;
    }

    public boolean autoRestart() {
        return autoRestart != null && autoRestart;
    }

    /**
     * 开启状态
     *
     * @return true 启用
     */
    public boolean status(String autoExecCron) {
        return status != null && status && (autoExecCron != null && !autoExecCron.isEmpty());
    }

    public List<NodeProject> projects() {
        return StringUtil.jsonConvertArray(projects, NodeProject.class);
    }


    public String getProjects() {
        List<NodeProject> projects = projects();
        return projects == null ? null : JSON.toJSONString(projects);
    }

    public void projects(List<NodeProject> projects) {
        if (projects == null) {
            this.projects = null;
        } else {
            this.projects = JSON.toJSONString(projects);
        }
    }

    public List<String> notifyUser() {
        return StringUtil.jsonConvertArray(notifyUser, String.class);
    }

    public String getNotifyUser() {
        List<String> object = notifyUser();
        return object == null ? null : JSON.toJSONString(object);
    }

    public void notifyUser(List<String> notifyUser) {
        if (notifyUser == null) {
            this.notifyUser = null;
        } else {
            this.notifyUser = JSON.toJSONString(notifyUser);
        }
    }

    public boolean checkNodeProject(String nodeId, String projectId) {
        List<NodeProject> projects = projects();
        if (projects == null) {
            return false;
        }
        for (NodeProject project : projects) {
            if (project.getNode().equals(nodeId)) {
                List<String> projects1 = project.getProjects();
                if (projects1 == null) {
                    return false;
                }
                for (String s : projects1) {
                    if (projectId.equals(s)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Getter
    public enum NotifyType implements BaseEnum {
        /**
         * 通知方式
         */
        dingding(0, "钉钉"),
        mail(1, "邮箱"),
        workWx(2, "企业微信"),
        webhook(3, "webhook"),
        ;

        private final int code;
        private final String desc;

        NotifyType(int code, String desc) {
            this.code = code;
            this.desc = desc;
        }
    }

    /**
     * 通知
     */
    public static class Notify extends BaseJsonModel {
        private int style;
        private String value;

        public Notify() {
        }

        public Notify(NotifyType style, String value) {
            this.style = style.getCode();
            this.value = value;
        }

        public int getStyle() {
            return style;
        }

        public void setStyle(int style) {
            this.style = style;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class NodeProject extends BaseJsonModel {
        /**
         * 节点 ID
         */
        private String node;
        /**
         * 被监控的项目ID
         */
        private List<String> projects;

        public String getNode() {
            return node;
        }

        public void setNode(String node) {
            this.node = node;
        }

        public List<String> getProjects() {
            return projects;
        }

        public void setProjects(List<String> projects) {
            this.projects = projects;
        }
    }
}
