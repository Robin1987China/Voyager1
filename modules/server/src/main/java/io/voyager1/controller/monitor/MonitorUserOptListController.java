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

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.MonitorUserOptModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.monitor.MonitorUserOptService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 监控用户操作
 *
 * @since 2020/08/06
 */
@RestController
@RequestMapping(value = "/monitor_user_opt")
@Feature(cls = ClassFeature.OPT_MONITOR)
public class MonitorUserOptListController extends BaseServerController {

    private final MonitorUserOptService monitorUserOptService;

    public MonitorUserOptListController(MonitorUserOptService monitorUserOptService) {
        this.monitorUserOptService = monitorUserOptService;
    }


    @RequestMapping(value = "list_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<MonitorUserOptModel>> getMonitorList(HttpServletRequest request) {
        PageResultDto<MonitorUserOptModel> pageResultDto = monitorUserOptService.listPage(request);
        return ApiResult.success("", pageResultDto);
    }

    /**
     * 操作监控类型列表
     *
     * @return json
     */
    @RequestMapping(value = "type_data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONObject> getOperateTypeList() {
        JSONObject jsonObject = new JSONObject();
        //
        List<JSONObject> classFeatureList = Arrays.stream(ClassFeature.values())
            .filter(classFeature -> classFeature != ClassFeature.NULL)
            .map(classFeature -> {
                JSONObject jsonObject1 = new JSONObject();
                String value = I18nMessageUtil.get(classFeature.getName().get());
                jsonObject1.put("title", value);
                jsonObject1.put("value", classFeature.name());
                return jsonObject1;
            })
            .collect(Collectors.toList());
        jsonObject.put("classFeature", classFeatureList);
        //
        List<JSONObject> methodFeatureList = Arrays.stream(MethodFeature.values())
            .filter(methodFeature -> methodFeature != MethodFeature.NULL && methodFeature != MethodFeature.LIST)
            .map(classFeature -> {
                JSONObject jsonObject1 = new JSONObject();
                String value = I18nMessageUtil.get(classFeature.getName().get());
                jsonObject1.put("title", value);
                jsonObject1.put("value", classFeature.name());
                return jsonObject1;
            })
            .collect(Collectors.toList());
        jsonObject.put("methodFeature", methodFeatureList);

        return ApiResult.success("", jsonObject);
    }

    /**
     * 删除列表
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "delete", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> deleteMonitor(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "删除失败") String id, HttpServletRequest request) {
        //
        monitorUserOptService.delByKey(id, request);
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
    @RequestMapping(value = "update", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> updateMonitor(String id,
                                              @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "监控名称不能为空") String name,
                                              String notifyUser,
                                              String monitorUser,
                                              String monitorOpt,
                                              String monitorFeature) {

        String status = getParameter("status");

        JSONArray jsonArray = JSONArray.parseArray(notifyUser);
        List<String> notifyUsers = jsonArray.toJavaList(String.class)
            .stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        Assert.notEmpty(notifyUsers, "请选择报警联系人");


        JSONArray monitorUserArray = JSONArray.parseArray(monitorUser);
        List<String> monitorUserArrays = monitorUserArray.toJavaList(String.class)
            .stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        Assert.notEmpty(monitorUserArrays, "请选择监控人员");


        JSONArray monitorOptArray = JSONArray.parseArray(monitorOpt);
        List<MethodFeature> monitorOptArrays = monitorOptArray
            .stream()
            .map(o -> EnumUtil.fromString(MethodFeature.class, String.valueOf(o), null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        Assert.notEmpty(monitorOptArrays, "请选择监控的操作");

        JSONArray monitorFeatureArray = JSONArray.parseArray(monitorFeature);
        List<ClassFeature> monitorFeatureArrays = monitorFeatureArray
            .stream()
            .map(o -> EnumUtil.fromString(ClassFeature.class, String.valueOf(o), null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        Assert.notEmpty(monitorFeatureArrays, "请选择监控的功能");


        boolean start = "on".equalsIgnoreCase(status);
        MonitorUserOptModel monitorModel = monitorUserOptService.getByKey(id);
        if (monitorModel == null) {
            monitorModel = new MonitorUserOptModel();
        }
        monitorModel.monitorUser(monitorUserArrays);
        monitorModel.setStatus(start);
        monitorModel.monitorOpt(monitorOptArrays);
        monitorModel.monitorFeature(monitorFeatureArrays);
        monitorModel.notifyUser(notifyUsers);
        monitorModel.setName(name);

        if ((id == null || id.isEmpty())) {
            //添加监控
            monitorUserOptService.insert(monitorModel);
            return ApiResult.success("添加成功");
        }
        monitorUserOptService.updateById(monitorModel);
        return ApiResult.success("修改成功");
    }

    /**
     * 开启或关闭监控
     *
     * @param id     id
     * @param status 状态
     * @return json
     */
    @RequestMapping(value = "changeStatus", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> changeStatus(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "参数错误id不能为空") String id,
                                             String status) {
        MonitorUserOptModel monitorModel = monitorUserOptService.getByKey(id);
        Assert.notNull(monitorModel, "不存在监控项啦");

        boolean bStatus = ConvertUtil.toBool(status, false);
        monitorModel.setStatus(bStatus);
        monitorUserOptService.updateById(monitorModel);
        return ApiResult.success("修改成功");
    }


}
