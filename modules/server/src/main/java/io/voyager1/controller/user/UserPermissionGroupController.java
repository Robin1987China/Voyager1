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

package io.voyager1.controller.user;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.DateUtil;
import io.voyager1.util.Tuple;
import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.user.UserPermissionGroupBean;
import io.voyager1.oauth2.BaseOauth2Config;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.system.SystemParametersServer;
import io.voyager1.service.user.UserBindWorkspaceService;
import io.voyager1.service.user.UserPermissionGroupServer;
import io.voyager1.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @since 2022/8/3
 */
@RestController
@RequestMapping(value = "/user-permission-group")
@Feature(cls = ClassFeature.USER_PERMISSION_GROUP)
@SystemPermission
public class UserPermissionGroupController extends BaseServerController {

    private final UserPermissionGroupServer userPermissionGroupServer;
    private final UserBindWorkspaceService userBindWorkspaceService;
    private final UserService userService;
    private final SystemParametersServer systemParametersServer;

    public UserPermissionGroupController(UserPermissionGroupServer userPermissionGroupServer,
                                         UserBindWorkspaceService userBindWorkspaceService,
                                         UserService userService,
                                         SystemParametersServer systemParametersServer) {
        this.userPermissionGroupServer = userPermissionGroupServer;
        this.userBindWorkspaceService = userBindWorkspaceService;
        this.userService = userService;
        this.systemParametersServer = systemParametersServer;
    }

    /**
     * 分页查询权限组
     *
     * @return json
     */
    @RequestMapping(value = "get-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<UserPermissionGroupBean>> getUserList(HttpServletRequest request) {
        PageResultDto<UserPermissionGroupBean> userModelPageResultDto = userPermissionGroupServer.listPage(request);
        return new ApiResult<>(200, "", userModelPageResultDto);
    }

    /**
     * 查询所有权限组
     *
     * @return json
     */
    @GetMapping(value = "get-list-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<UserPermissionGroupBean>> getListAll() {
        List<UserPermissionGroupBean> list = userPermissionGroupServer.list();
        return new ApiResult<>(200, "", list);
    }

    /**
     * 编辑权限组
     *
     * @return String
     */
    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> edit(String id,
                                     @ValidatorItem String name,
                                     String description,
                                     String prohibitExecute,
                                     String allowExecute,
                                     @ValidatorItem String workspace) {
        UserPermissionGroupBean userPermissionGroupBean = new UserPermissionGroupBean();
        userPermissionGroupBean.setName(name);
        userPermissionGroupBean.setDescription(description);
        //
        userPermissionGroupBean.setProhibitExecute(this.resolveProhibitExecute(prohibitExecute));
        userPermissionGroupBean.setAllowExecute(this.resolveAllowExecute(allowExecute));
        if ((id == null || id.isEmpty())) {
            userPermissionGroupServer.insert(userPermissionGroupBean);
        } else {
            UserPermissionGroupBean permissionGroupBean = userPermissionGroupServer.getByKey(id);
            Assert.notNull(permissionGroupBean, "数据不存在");
            userPermissionGroupBean.setId(id);
            userPermissionGroupServer.updateById(userPermissionGroupBean);
        }
        //
        JSONArray jsonArray = JSONArray.parseArray(workspace);
        List<String> workspaceList = jsonArray.toJavaList(String.class);
        userBindWorkspaceService.updateUserWorkspace(userPermissionGroupBean.getId(), workspaceList);
        return new ApiResult<>(200, "操作成功");
    }

    private String resolveAllowExecute(String allowExecute) {
        if ((allowExecute == null || allowExecute.isEmpty())) {
            return "";
        }
        JSONArray jsonArray = JSONArray.parseArray(allowExecute);
        return JSON.toJSONString(jsonArray.stream().map(o -> {
            JSONObject jsonObject = (JSONObject) o;
            String startTime = jsonObject.getString("startTime");
            String endTime = jsonObject.getString("endTime");
            if (((startTime == null || startTime.isEmpty()) || (endTime == null || endTime.isEmpty()))) {
                return null;
            }
            JSONArray week = jsonObject.getJSONArray("week");
            if ((week == null || week.isEmpty())) {
                return null;
            }
            int[] weeks = week.stream().mapToInt(value -> {
                int week1 = ConvertUtil.toInt(value, 0);
                Assert.state(week1 >= 1 && week1 <= 7, "选择的周几不正确");
                return week1;
            }).toArray();
            //
            JSONObject result = new JSONObject();
            result.put("week", weeks);
            result.put("startTime", DateUtil.parseTimeToday(startTime).toString("HH:mm:ss"));
            result.put("endTime", DateUtil.parseTimeToday(endTime).toString("HH:mm:ss"));
            return result;
        }).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    private String resolveProhibitExecute(String prohibitExecute) {
        if ((prohibitExecute == null || prohibitExecute.isEmpty())) {
            return "";
        }
        JSONArray jsonArray = JSONArray.parseArray(prohibitExecute);
        return JSON.toJSONString(jsonArray.stream().map(o -> {
            JSONObject jsonObject = (JSONObject) o;
            String startTime = jsonObject.getString("startTime");
            String endTime = jsonObject.getString("endTime");
            if (((startTime == null || startTime.isEmpty()) || (endTime == null || endTime.isEmpty()))) {
                return null;
            }
            JSONObject result = new JSONObject();
            result.put("startTime", DateUtil.parse(startTime).toString("yyyy-MM-dd HH:mm:ss"));
            result.put("endTime", DateUtil.parse(endTime).toString("yyyy-MM-dd HH:mm:ss"));
            result.put("reason", jsonObject.getString("reason"));
            return result;
        }).filter(Objects::nonNull).collect(Collectors.toList()));
    }

    /**
     * 删除
     *
     * @param id 权限组
     * @return String
     */
    @GetMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> delete(String id) {
        UserPermissionGroupBean groupBean = userPermissionGroupServer.getByKey(id);
        Assert.notNull(groupBean, "数据不存在");
        // 判断是否绑定用户
        Entity entity = Entity.create();
        entity.set("permissionGroup", "like %@" + id + "@%");
        long count = userService.count(entity);
        Assert.state(count == 0, "当前权限组还绑定用户,不能直接删除（需要提前解绑或者删除关联数据后才能删除）");
        // 判断是否被 oauth2 绑定
        for (Map.Entry<String, Tuple> entry : BaseOauth2Config.DB_KEYS.entrySet()) {
            Tuple value = entry.getValue();
            String dbKey = value.get(0);
            BaseOauth2Config baseOauth2Config = systemParametersServer.getConfigDefNewInstance(dbKey, value.get(1));
            String permissionGroup = baseOauth2Config.getPermissionGroup();
            List<String> permissionGroupList = io.voyager1.util.StrUtil.splitTrim(permissionGroup, "@");
            Assert.state(!CollUtil.contains(permissionGroupList, groupBean.getId()), String.format("当前权限组被 oauth2[%s] 绑定，不能直接删除（需要提前解绑或者删除关联数据后才能删除）", baseOauth2Config.provide()));
        }
        //
        userPermissionGroupServer.delByKey(id);
        // 删除工作空间
        userBindWorkspaceService.deleteByUserId(id);
        return ApiResult.success("删除成功");
    }
}
