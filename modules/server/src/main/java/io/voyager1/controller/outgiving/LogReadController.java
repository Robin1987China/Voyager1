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

package io.voyager1.controller.outgiving;

import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.outgiving.LogReadModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.outgiving.LogReadServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 日志阅读
 *
 * @since 2022/5/15
 */
@RestController
@RequestMapping(value = "/log-read")
@Feature(cls = ClassFeature.LOG_READ)
public class LogReadController extends BaseServerController {

    private final LogReadServer logReadServer;

    public LogReadController(LogReadServer logReadServer) {
        this.logReadServer = logReadServer;
    }

    /**
     * 日志阅读列表
     *
     * @return json
     */
    @PostMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<LogReadModel>> list(HttpServletRequest request) {
        PageResultDto<LogReadModel> pageResultDto = logReadServer.listPage(request);
        return ApiResult.success("", pageResultDto);
    }

    /**
     * 删除日志阅读信息
     *
     * @param id 分发id
     * @return json
     */
    @RequestMapping(value = "del.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> del(String id, HttpServletRequest request) {
        int byKey = logReadServer.delByKey(id, request);
        return ApiResult.success("操作成功");
    }

    /**
     * 编辑日志阅读信息
     * <p>
     * {"projectList":[{"nodeId":"localhost","projectId":"test-jar"}],"name":"11"}
     *
     * @param jsonObject 参数
     * @return msg
     */
    @RequestMapping(value = "save.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> save(@RequestBody JSONObject jsonObject, HttpServletRequest request) {
        Assert.notNull(jsonObject, "请传入参数");
        String id = jsonObject.getString("id");
        String name = jsonObject.getString("name");
        Assert.hasText(name, "请填写名称");
        JSONArray projectListArray = jsonObject.getJSONArray("projectList");
        Assert.notEmpty(projectListArray, "请选择节点和项目");
        List<LogReadModel.Item> projectList = projectListArray.toJavaList(LogReadModel.Item.class);
        projectList = projectList.stream()
            .filter(item -> StrUtil.isAllNotEmpty(item.getNodeId(), item.getProjectId()))
            .collect(Collectors.toList());
        Assert.notEmpty(projectList, "请选择节点和项目");
        LogReadModel logReadModel = new LogReadModel();
        logReadModel.setId(id);
        logReadModel.setName(name);
        logReadModel.setNodeProject(JSONArray.toJSONString(projectList));
        //
        if ((id == null || id.isEmpty())) {
            logReadServer.insert(logReadModel);
        } else {
            logReadServer.updateById(logReadModel, request);
        }
        return ApiResult.success("修改成功");
    }

    /**
     * 更新缓存
     * <p>
     * {"op":"showlog","projectId":"python",
     * "search":true,"useProjectId":"python",
     * "useNodeId":"localhost",
     * "beforeCount":0,"afterCount":10,
     * "head":0,"tail":100,"first":"false",
     * "logFile":"/run.log"}
     *
     * @param jsonObject 参数
     * @return msg
     */
    @RequestMapping(value = "update-cache.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> updateCache(@RequestBody JSONObject jsonObject, HttpServletRequest request) {
        Assert.notNull(jsonObject, "请传入参数");
        String id = jsonObject.getString("id");
        Assert.hasText(id, "请传入参数");
        LogReadModel.CacheDta cacheDta = jsonObject.toJavaObject(LogReadModel.CacheDta.class);

        LogReadModel logReadModel = new LogReadModel();
        logReadModel.setId(id);
        logReadModel.setCacheData(JSONArray.toJSONString(cacheDta));
        logReadServer.updateById(logReadModel, request);
        return ApiResult.success("修改成功");
    }
}
