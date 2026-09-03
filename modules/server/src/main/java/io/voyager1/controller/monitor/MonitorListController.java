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

package io.voyager1.controller.monitor;

import io.voyager1.util.CollUtil;
import io.voyager1.util.RegexPool;
import io.voyager1.util.Validator;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONArray;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.MonitorModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.dblog.DbMonitorNotifyLogService;
import io.voyager1.service.monitor.MonitorService;
import io.voyager1.service.node.ProjectInfoCacheService;
import io.voyager1.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 监控列表
 *
 * @since 2019/6/15
 */
@RestController
@RequestMapping(value = "/monitor")
@Feature(cls = ClassFeature.MONITOR)
public class MonitorListController extends BaseServerController {

    private final MonitorService monitorService;
    private final DbMonitorNotifyLogService dbMonitorNotifyLogService;
    private final UserService userService;
    private final ProjectInfoCacheService projectInfoCacheService;

    public MonitorListController(MonitorService monitorService,
                                 DbMonitorNotifyLogService dbMonitorNotifyLogService,
                                 UserService userService,
                                 ProjectInfoCacheService projectInfoCacheService) {
        this.monitorService = monitorService;
        this.dbMonitorNotifyLogService = dbMonitorNotifyLogService;
        this.userService = userService;
        this.projectInfoCacheService = projectInfoCacheService;
    }

    /**
     * 展示监控列表
     *
     * @return json
     */
    @RequestMapping(value = "getMonitorList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<MonitorModel>> getMonitorList(HttpServletRequest request) {
        PageResultDto<MonitorModel> pageResultDto = monitorService.listPage(request);
        return ApiResult.success("", pageResultDto);
    }

    /**
     * 删除列表
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "deleteMonitor", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> deleteMonitor(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "删除失败") String id, HttpServletRequest request) {
        //

        int delByKey = monitorService.delByKey(id, request);
        if (delByKey > 0) {
            // 删除日志
            dbMonitorNotifyLogService.delByMonitorId(id);
        }
        return ApiResult.success("删除成功");
    }


    /**
     * 增加或修改监控
     *
     * @param id         id
     * @param name       name
     * @param notifyUser user
     * @return json
     */
    @RequestMapping(value = "updateMonitor", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> updateMonitor(String id,
                                              @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "监控名称不能为空") String name,
                                              @ValidatorItem(msg = "请配置监控周期") String execCron,
                                              String notifyUser, String webhook,
                                              String useLanguage,
                                              Integer silenceTime,
                                              String silenceUnit,
                                              HttpServletRequest request) {
        String status = getParameter("status");
        String autoRestart = getParameter("autoRestart");

        JSONArray jsonArray = JSONArray.parseArray(notifyUser);
//		List<String> notifyUsers = jsonArray.toJavaList(String.class);
        List<String> notifyUserList = jsonArray.toJavaList(String.class);
        if ((notifyUserList != null && !notifyUserList.isEmpty())) {
            for (String userId : notifyUserList) {
                Assert.state(userService.exists(new UserModel(userId)), "没有对应的用户：" + userId);
            }
        }
        String projects = getParameter("projects");
        JSONArray projectsArray = JSONArray.parseArray(projects);
        List<MonitorModel.NodeProject> nodeProjects = projectsArray.toJavaList(MonitorModel.NodeProject.class);
        Assert.notEmpty(nodeProjects, "请至少选择一个节点");
        for (MonitorModel.NodeProject nodeProject : nodeProjects) {
            Assert.notEmpty(nodeProject.getProjects(), "请至少选择一个项目");
            for (String project : nodeProject.getProjects()) {
                boolean exists = projectInfoCacheService.exists(nodeProject.getNode(), project);
                Assert.state(exists, "没有对应的项目：" + project);
            }
        }
        //
        // 设置参数
        if ((webhook != null && !webhook.isEmpty())) {
            Validator.validateMatchRegex(RegexPool.URL_HTTP, webhook, "WebHooks 地址不合法");
        }
        Assert.state((notifyUserList != null && !notifyUserList.isEmpty()) || (webhook != null && !webhook.isEmpty()), "请选择一位报警联系人或者填写webhook");

        boolean start = "on".equalsIgnoreCase(status);
        MonitorModel monitorModel = monitorService.getByKey(id);
        if (monitorModel == null) {
            monitorModel = new MonitorModel();
        }
        monitorModel.setAutoRestart("on".equalsIgnoreCase(autoRestart));
        monitorModel.setExecCron(this.checkCron(execCron));
        monitorModel.projects(nodeProjects);
        monitorModel.setStatus(start);
        monitorModel.setWebhook(webhook);
        monitorModel.setUseLanguage(useLanguage);
        monitorModel.notifyUser(notifyUserList);
        monitorModel.setName(name);
        Integer silenceTime1 = (silenceTime != null ? silenceTime : 0);
        Assert.state(silenceTime1 >= 0, "沉默时间不能小于 0");
        monitorModel.setSilenceTime(silenceTime1);
        TimeUnit timeUnit = EnumUtil.fromString(TimeUnit.class, silenceUnit, TimeUnit.MINUTES);
        monitorModel.setSilenceUnit(timeUnit.name());

        if ((id == null || id.isEmpty())) {
            //添加监控
            monitorService.insert(monitorModel);
            return ApiResult.success("添加成功");
        }

        monitorService.updateById(monitorModel, request);
        return ApiResult.success("修改成功");
    }
}
