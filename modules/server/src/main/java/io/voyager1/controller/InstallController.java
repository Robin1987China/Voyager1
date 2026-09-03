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

package io.voyager1.controller;

import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.interceptor.LoginInterceptor;
import io.voyager1.common.interceptor.NotLogin;
import io.voyager1.common.validator.ValidatorConfig;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.controller.user.UserWorkspaceModel;
import io.voyager1.model.dto.UserLoginDto;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.user.UserBindWorkspaceService;
import io.voyager1.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 初始化程序
 *
 * @since 2019/2/22
 */
@RestController
@Slf4j
public class InstallController extends BaseServerController {


    private final UserService userService;
    private final UserBindWorkspaceService userBindWorkspaceService;

    public InstallController(UserService userService,
                             UserBindWorkspaceService userBindWorkspaceService) {
        this.userService = userService;
        this.userBindWorkspaceService = userBindWorkspaceService;
    }

    /**
     * 初始化提交
     *
     * @param userName 系统管理员登录名
     * @param userPwd  系统管理员的登录密码
     * @return json
     * @api {post} install_submit.json 初始化提交
     * @apiGroup index
     * @apiUse defResultJson
     * @apiParam {String} userName 系统管理员登录名
     * @apiParam {String} userPwd 设置的登录密码 sha1 后传入
     * @apiSuccess {JSON}  data.tokenData token 相关信息
     */
    @PostMapping(value = "install_submit.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    public ApiResult<JSONObject> installSubmit(
        @ValidatorConfig(value = {
            @ValidatorItem(value = ValidatorRule.NOT_EMPTY, msg = "登录名不能为空"),
            @ValidatorItem(value = ValidatorRule.NOT_BLANK, range = UserModel.USER_NAME_MIN_LEN + ":" + Const.ID_MAX_LEN, msg = "登录名长度范围 3-50"),
            @ValidatorItem(value = ValidatorRule.WORD, msg = "登录名不能包含汉字并且不能包含特殊字符")
        }) String userName,
        @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "密码不能为空") String userPwd) {
        //
        Assert.state(!userService.canUse(), "系统已经初始化过啦，请勿重复初始化");

        boolean systemOccupyUserName = StrUtil.equalsAnyIgnoreCase(userName, UserModel.DEMO_USER, Const.SYSTEM_ID, UserModel.SYSTEM_ADMIN);
        Assert.state(!systemOccupyUserName, "当前登录名已经被系统占用啦");
        // 创建用户
        UserModel userModel = new UserModel();
        userModel.setName(UserModel.SYSTEM_OCCUPY_NAME.get());
        userModel.setId(userName);
        userModel.setSalt(userService.generateSalt());
        userModel.setPassword(DigestUtil.sha1(userPwd + userModel.getSalt()));
        userModel.setSystemUser(1);
        userModel.setParent(UserModel.SYSTEM_ADMIN);
        try {
            BaseServerController.resetInfo(userModel);
            userService.insert(userModel);
        } catch (Exception e) {
            log.error("初始化用户失败", e);
            return new ApiResult<>(400, "初始化失败:" + e.getMessage());
        }
        //自动登录
        setSessionAttribute(LoginInterceptor.SESSION_NAME, userModel);
        UserLoginDto userLoginDto = userService.getUserJwtId(userModel);
        List<UserWorkspaceModel> bindWorkspaceModels = userService.myWorkspace(userModel);
        userLoginDto.setBindWorkspaceModels(bindWorkspaceModels);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("tokenData", userLoginDto);
        return ApiResult.success("初始化成功", jsonObject);
    }

}
