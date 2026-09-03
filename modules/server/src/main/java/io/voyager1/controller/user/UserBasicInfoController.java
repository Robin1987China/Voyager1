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
import org.springframework.data.domain.Sort;

import io.voyager1.util.TimedCache;
import io.voyager1.util.CollStreamUtil;
import io.voyager1.util.RegexPool;
import io.voyager1.util.Validator;
import io.voyager1.util.RandomUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.func.system.model.ClusterInfoModel;
import io.voyager1.func.system.service.ClusterInfoService;
import io.voyager1.func.user.model.UserLoginLogModel;
import io.voyager1.func.user.server.UserLoginLogServer;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.MailAccountModel;
import io.voyager1.model.log.BuildHistoryLog;
import io.voyager1.model.log.UserOperateLogV1;
import io.voyager1.model.user.UserModel;
import io.voyager1.monitor.EmailUtil;
import io.voyager1.service.dblog.DbBuildHistoryLogService;
import io.voyager1.service.dblog.DbUserOperateLogService;
import io.voyager1.service.system.SystemParametersServer;
import io.voyager1.service.user.UserBindWorkspaceService;
import io.voyager1.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @since 2019/8/10
 */
@RestController
@RequestMapping(value = "/user")
@Slf4j
public class UserBasicInfoController extends BaseServerController {

    private static final TimedCache<String, Integer> CACHE = new TimedCache<>(TimeUnit.MINUTES.toMillis(30));

    private final SystemParametersServer systemParametersServer;
    private final UserBindWorkspaceService userBindWorkspaceService;
    private final UserService userService;
    private final UserLoginLogServer userLoginLogServer;
    private final DbUserOperateLogService dbUserOperateLogService;
    private final ClusterInfoService clusterInfoService;
    private final DbBuildHistoryLogService dbBuildHistoryLogService;

    public UserBasicInfoController(SystemParametersServer systemParametersServer,
                                   UserBindWorkspaceService userBindWorkspaceService,
                                   UserService userService,
                                   UserLoginLogServer userLoginLogServer,
                                   DbUserOperateLogService dbUserOperateLogService,
                                   ClusterInfoService clusterInfoService,
                                   DbBuildHistoryLogService dbBuildHistoryLogService) {
        this.systemParametersServer = systemParametersServer;
        this.userBindWorkspaceService = userBindWorkspaceService;
        this.userService = userService;
        this.userLoginLogServer = userLoginLogServer;
        this.dbUserOperateLogService = dbUserOperateLogService;
        this.clusterInfoService = clusterInfoService;
        this.dbBuildHistoryLogService = dbBuildHistoryLogService;
    }


    /**
     * get user basic info
     * 获取管理员基本信息接口
     *
     * @return json
     */
    @RequestMapping(value = "user-basic-info", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Map<String, Object>> getUserBasicInfo() {
        UserModel userModel = getUser();
        userModel = userService.getByKey(userModel.getId(), false);
        // return basic info
        Map<String, Object> map = new HashMap<>(10);
        map.put("id", userModel.getId());
        map.put("name", userModel.getName());
        map.put("systemUser", userModel.checkSystemUser());
        map.put("superSystemUser", userModel.isSuperSystemUser());
        map.put("demoUser", userModel.isDemoUser());
        map.put("email", userModel.getEmail());
        map.put("dingDing", userModel.getDingDing());
        map.put("workWx", userModel.getWorkWx());
        map.put("md5Token", userModel.getPassword());
        return ApiResult.success("", map);
    }

    @RequestMapping(value = "save_basicInfo.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> saveBasicInfo(String email,
                                              String dingDing, String workWx, String code,
                                              @ValidatorItem(value = ValidatorRule.NOT_BLANK, range = "2:10", msg = "昵称长度只能是2-10") String name) {
        UserModel user = getUser();
        UserModel userModel = userService.getByKey(user.getId());
        UserModel updateModel = new UserModel(user.getId());
        // 判断是否一样
        if ((email != null && !email.isEmpty()) && !java.util.Objects.equals(email, userModel.getEmail())) {
            Validator.validateEmail(email, "邮箱格式不正确");
            Integer cacheCode = CACHE.get(email);
            if (cacheCode == null || !Objects.equals(cacheCode.toString(), code)) {
                return new ApiResult<>(405, "请输入正确验证码");
            }
            updateModel.setEmail(email);
        }

        updateModel.setName(name);
        //
        if ((dingDing != null && !dingDing.isEmpty()) && !Validator.isUrl(dingDing)) {
            Validator.validateMatchRegex(RegexPool.URL_HTTP, dingDing, "请输入正确钉钉地址");
        }
        updateModel.setDingDing(dingDing);
        if ((workWx != null && !workWx.isEmpty())) {
            Validator.validateMatchRegex(RegexPool.URL_HTTP, workWx, "请输入正确企业微信地址");
        }
        updateModel.setWorkWx(workWx);
        userService.updateById(updateModel);
        return ApiResult.success("修改成功");
    }

    /**
     * 发送邮箱验证
     *
     * @param email 邮箱
     * @return msg
     */
    @RequestMapping(value = "sendCode.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> sendCode(@ValidatorItem(value = ValidatorRule.EMAIL, msg = "邮箱格式不正确") String email) {
        MailAccountModel config = systemParametersServer.getConfig(MailAccountModel.ID, MailAccountModel.class);
        Assert.notNull(config, "管理员还没有配置系统邮箱,请联系管理配置发件信息");
        int randomInt = RandomUtil.randomInt(1000, 9999);
        try {
            String title = "Voyager1 验证码";
            EmailUtil.send(email, title, String.format("验证码是：%s", randomInt));
        } catch (Exception e) {
            log.error("发送失败", e);
            return new ApiResult<>(500, "发送邮件失败：" + e.getMessage());
        }
        CACHE.put(email, randomInt);
        return ApiResult.success("发送成功");
    }

    /**
     * 查询用户自己的工作空间
     *
     * @return msg
     */
    @GetMapping(value = "my-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<UserWorkspaceModel>> myWorkspace() {
        UserModel user = getUser();
        List<UserWorkspaceModel> userWorkspaceModels = userService.myWorkspace(user);
        return ApiResult.success("", userWorkspaceModels);
    }

    /**
     * 保存用户自己的工作空间
     *
     * @return msg
     */
    @PostMapping(value = "save-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> saveWorkspace(@RequestBody List<UserWorkspaceModel> workspaceModels) {
        Assert.notEmpty(workspaceModels, "没有选择任何工作空间");
        List<UserWorkspaceModel> collect = workspaceModels.stream()
            .filter(workspaceModel -> (workspaceModel.getId() != null && !workspaceModel.getId().isEmpty()))
            .peek(userWorkspaceModel -> userWorkspaceModel.setOriginalName(null))
            .collect(Collectors.toList());
        UserModel user = getUser();
        Map<String, UserWorkspaceModel> map = CollStreamUtil.toMap(collect, UserWorkspaceModel::getId, workspaceModel -> workspaceModel);
        String name = "user-my-workspace-" + user.getId();
        systemParametersServer.upsert(name, map, "用户自定义工作空间");
        return ApiResult.success("保存成功");
    }

    /**
     * 登录日志列表
     *
     * @return json
     */
    @RequestMapping(value = "list-login-log-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<PageResultDto<UserLoginLogModel>> listLoginLogData(HttpServletRequest request) {
        UserModel user = getUser();
        PageResultDto<UserLoginLogModel> pageResult = userLoginLogServer.listPageByUserId(request, user.getId());
        return ApiResult.success("", pageResult);
    }

    /**
     * 操作日志
     *
     * @return json
     */
    @RequestMapping(value = "list-operate-log-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<PageResultDto<UserOperateLogV1>> listOperateLogData(HttpServletRequest request) {
        UserModel user = getUser();
        PageResultDto<UserOperateLogV1> pageResult = dbUserOperateLogService.listPageByUserId(request, user.getId());
        return ApiResult.success("", pageResult);
    }

    @RequestMapping(value = "recent-log-data", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> recentData(HttpServletRequest request) {
        UserModel user = getUser();
        JSONObject jsonObject = new JSONObject();
        {
            Entity entity = Entity.create();
            entity.set("userId", user.getId());
            List<UserOperateLogV1> operateLog = dbUserOperateLogService.queryList(entity, 10, Sort.by(Sort.Order.desc("createTimeMillis")));
            jsonObject.put("operateLog", operateLog);
        }
        {
            List<UserLoginLogModel> loginLog = userLoginLogServer.listByModifyUser(user.getId(), 10);

            jsonObject.put("loginLog", loginLog);
        }
        {
            String workspaceId = dbBuildHistoryLogService.getCheckUserWorkspace(request);
            Entity entity = Entity.create();
            entity.set("workspaceId", workspaceId);
            entity.set("modifyUser", user.getId());
            List<BuildHistoryLog> loginLog = dbBuildHistoryLogService.queryList(entity, 10, Sort.by(Sort.Order.desc("createTimeMillis")));
            jsonObject.put("buildLog", loginLog);
        }
        return ApiResult.success("", jsonObject);
    }

    /**
     * 查询集群列表
     *
     * @return json
     */
    @GetMapping(value = "cluster-list")
    public ApiResult<JSONObject> clusterList() {
        List<ClusterInfoModel> list = clusterInfoService.list();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("list", list);
        jsonObject.put("currentId", Voyager1Manifest.getInstance().getInstallId());
        return ApiResult.success("", jsonObject);
    }
}
