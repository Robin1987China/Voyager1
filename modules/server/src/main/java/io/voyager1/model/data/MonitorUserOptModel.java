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

import com.alibaba.fastjson2.JSON;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseWorkspaceModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.util.StringUtil;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 监控用户操作实体
 *
 */
@TableName(value = "OPS_MONITOR_NOTIFY",
    nameKey = "监控用户操作")
public class MonitorUserOptModel extends BaseWorkspaceModel {
    /**
     *
     */
    private String name;
    /**
     * 监控的人员
     */
    private String monitorUser;
    /**
     * 监控的功能
     *
     * @see ClassFeature
     */
    private String monitorFeature;
    /**
     * 监控的操作
     *
     * @see MethodFeature
     */
    private String monitorOpt;
    /**
     * 报警联系人
     */
    private String notifyUser;

    /**
     * 监控开启状态
     */
    private Boolean status;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setMonitorUser(String monitorUser) {
        this.monitorUser = monitorUser;
    }

    public String getMonitorFeature() {
        List<ClassFeature> object = monitorFeature();
        return object == null ? null : JSON.toJSONString(object);
    }

    public List<ClassFeature> monitorFeature() {
        return StringUtil.jsonConvertArray(monitorFeature, ClassFeature.class);
    }

    public void setMonitorFeature(String monitorFeature) {
        this.monitorFeature = monitorFeature;
    }

    public void monitorFeature(List<ClassFeature> monitorFeature) {
        if (monitorFeature == null) {
            this.monitorFeature = null;
        } else {
            this.monitorFeature = JSON.toJSONString(monitorFeature.stream().map(Enum::name).collect(Collectors.toList()));
        }
    }

    public String getMonitorOpt() {
        List<MethodFeature> object = monitorOpt();
        return object == null ? null : JSON.toJSONString(object);
    }


    public List<MethodFeature> monitorOpt() {
        return StringUtil.jsonConvertArray(monitorOpt, MethodFeature.class);
    }

    public void setMonitorOpt(String monitorOpt) {
        this.monitorOpt = monitorOpt;
    }

    public void monitorOpt(List<MethodFeature> monitorOpt) {
        if (monitorOpt == null) {
            this.monitorOpt = null;
        } else {
            this.monitorOpt = JSON.toJSONString(monitorOpt.stream().map(Enum::name).collect(Collectors.toList()));
        }
    }

    public void setNotifyUser(String notifyUser) {
        this.notifyUser = notifyUser;
    }

    public Boolean getStatus() {
        return status;
    }

    public String getMonitorUser() {
        List<String> object = monitorUser();
        return object == null ? null : JSON.toJSONString(object);
    }

    public String getNotifyUser() {
        List<String> object = notifyUser();
        return object == null ? null : JSON.toJSONString(object);

    }

    public List<String> monitorUser() {
        return StringUtil.jsonConvertArray(monitorUser, String.class);
    }

    public void monitorUser(List<String> monitorUser) {
        if (monitorUser == null) {
            this.monitorUser = null;
        } else {
            this.monitorUser = JSON.toJSONString(monitorUser);
        }
    }

    public List<String> notifyUser() {
        return StringUtil.jsonConvertArray(notifyUser, String.class);
    }

    public void notifyUser(List<String> notifyUser) {
        if (notifyUser == null) {
            this.notifyUser = null;
        } else {
            this.notifyUser = JSON.toJSONString(notifyUser);
        }
    }


    public boolean isStatus() {
        return status != null && status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }
}
