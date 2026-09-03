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

import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.model.user.UserBindWorkspaceModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.user.UserBindWorkspaceService;
import io.voyager1.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.util.List;

/**
 * 用户管理
 *
 * @since 2018/9/28
 */
@RestController
@RequestMapping(value = "/user")
@Slf4j
public class UserInfoController extends BaseServerController {

    private final UserService userService;
    private final UserBindWorkspaceService userBindWorkspaceService;

    public UserInfoController(UserService userService,
                              UserBindWorkspaceService userBindWorkspaceService) {
        this.userService = userService;
        this.userBindWorkspaceService = userBindWorkspaceService;
    }

    /**
     * 修改密码
     *
     * @param oldPwd 旧密码
     * @param newPwd 新密码
     * @return json
     */
    @RequestMapping(value = "updatePwd", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> updatePwd(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "密码不能为空") String oldPwd,
                                          @ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "密码不能为空") String newPwd,
                                          HttpSession session) {
        Assert.state(!java.util.Objects.equals(oldPwd, newPwd), "新旧密码一致");
        UserModel userName = getUser();
        Assert.state(!userName.isDemoUser(), "当前账户为演示账号，不支持修改密码");

        UserModel userModel = userService.simpleLogin(userName.getId(), oldPwd);
        Assert.notNull(userModel, "旧密码不正确！");
        Assert.state((userModel.getPwdErrorCount() != null ? userModel.getPwdErrorCount() : 0) <= 0, "当前账号被锁定中，不能修改密码");

        userService.updatePwd(userName.getId(), newPwd);
        // 如果修改成功，则销毁会话
        session.invalidate();
        return ApiResult.success("修改密码成功！");
    }

    /**
     * 查询用户工作空间
     *
     * @return json
     */
    @GetMapping(value = "workspace_list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<UserBindWorkspaceModel>> workspaceList(@ValidatorItem String userId) {
        List<UserBindWorkspaceModel> workspaceModels = userBindWorkspaceService.listUserWorkspace(userId);
        return ApiResult.success("", workspaceModels);
    }
}
