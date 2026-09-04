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
import io.voyager1.util.Validator;
import io.voyager1.util.RandomUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.user.TriggerTokenLogServer;
import io.voyager1.service.user.UserBindWorkspaceService;
import io.voyager1.service.user.UserService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户列表
 *
 */
@RestController
@RequestMapping(value = "/user")
@Feature(cls = ClassFeature.USER)
@SystemPermission
public class UserListController extends BaseServerController {

    private final UserService userService;
    private final UserBindWorkspaceService userBindWorkspaceService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public UserListController(UserService userService,
                              UserBindWorkspaceService userBindWorkspaceService,
                              TriggerTokenLogServer triggerTokenLogServer) {
        this.userService = userService;
        this.userBindWorkspaceService = userBindWorkspaceService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * 查询所有用户
     *
     * @return json
     */
    @RequestMapping(value = "get_user_list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<UserModel>> getUserList(HttpServletRequest request) {
        PageResultDto<UserModel> userModelPageResultDto = userService.listPage(request);
        return new ApiResult<>(200, "", userModelPageResultDto);
    }

    /**
     * 获取所有管理员信息
     * get all admin user list
     *
     * @return json
     */
    @RequestMapping(value = "get_user_list_all", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<UserModel>> getUserListAll() {
        List<UserModel> list = userService.list();
        return ApiResult.success("", list);
    }

    /**
     * 编辑用户
     *
     * @param type 操作类型
     * @return String
     */
    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<JSONObject> addUser(String type) {
        //
        boolean create = java.util.Objects.equals(type, "add");
        UserModel userModel = this.parseUser(create);
        JSONObject result = new JSONObject();
        if (create) {
            String randomPwd = RandomUtil.randomString(UserModel.SALT_LEN);
            String sha1Pwd = DigestUtil.sha1(randomPwd);
            userModel.setSalt(userService.generateSalt());
            userModel.setPassword(DigestUtil.sha1(sha1Pwd + userModel.getSalt()));
            userModel.setSource("voyager1");
            userService.insert(userModel);
            result.put("randomPwd", randomPwd);
        } else {
            UserModel model = userService.getByKey(userModel.getId());
            Assert.notNull(model, "不存在对应的用户");
            boolean systemUser = userModel.checkSystemUser();
            if (!systemUser) {
                Assert.state(!model.isSuperSystemUser(), "不能取消超级管理员的权限");
            }
            if (model.isSuperSystemUser()) {
                Assert.state(userModel.getStatus() == 1, "不能禁用超级管理员");
            }
            UserModel optUser = getUser();
            if (java.util.Objects.equals(model.getId(), optUser.getId())) {
                Assert.state(optUser.isSuperSystemUser(), "不能修改自己的信息");
            }
            userService.updateById(userModel);
            // 删除旧数据
            userBindWorkspaceService.deleteByUserId(userModel.getId());
        }
        return new ApiResult<>(200, "操作成功", result);
    }

    private UserModel parseUser(boolean create) {
        String id = getParameter("id");
        boolean email = Validator.isEmail(id);
        if (email) {
            int length = id.length();
            Assert.state(length <= Const.ID_MAX_LEN && length >= UserModel.USER_NAME_MIN_LEN, String.format("登录名如果为邮箱格式, 长度必须 %s-%s", UserModel.USER_NAME_MIN_LEN, Const.ID_MAX_LEN));
        } else {
            String checkId = id.replace("-", "_");
            Validator.validateGeneral(checkId, UserModel.USER_NAME_MIN_LEN, Const.ID_MAX_LEN, String.format("登录名格式不正确（英文字母 、数字和下划线）, 并且长度必须 %s-%s", UserModel.USER_NAME_MIN_LEN, Const.ID_MAX_LEN));
        }

        Assert.state(!StrUtil.equalsAnyIgnoreCase(id, UserModel.SYSTEM_OCCUPY_NAME.get(), UserModel.SYSTEM_ADMIN), "当前登录名已经被系统占用");

        UserModel userModel = new UserModel();
        UserModel optUser = getUser();
        if (create) {
            // 登录名重复
            boolean exists = userService.exists(new UserModel(id));
            Assert.state(!exists, "登录名已经存在");
            userModel.setParent(optUser.getId());
        }
        userModel.setId(id);
        //
        String name = getParameter("name");
        Assert.hasText(name, "请输入账户昵称");
        int len = name.length();
        Assert.state(len <= 10 && len >= 2, "昵称长度只能是2-10");

        userModel.setName(name);

//        String password = getParameter("password");
//        if (create || (password != null && !password.isEmpty())) {
//            Assert.hasText(password, "密码不能为空");
//            // 修改用户
//            Assert.state(create || optUser.checkSystemUser(), "只有系统管理员才能重置用户密码");
//            userModel.setSalt(userService.generateSalt());
//            userModel.setPassword(DigestUtil.sha1(password + userModel.getSalt()));
//        }

        int systemUser = getParameterInt("systemUser", 0);
        userModel.setSystemUser(systemUser);
        //
        String permissionGroup = getParameter("permissionGroup");
        List<String> permissionGroupList = java.util.Arrays.asList(permissionGroup.split(java.util.regex.Pattern.quote("@")));
        Assert.notEmpty(permissionGroupList, "用户未选择权限组");
        userModel.setPermissionGroup("@" + permissionGroupList.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining("@")) + "@");
        //
        int status = getParameterInt("status", 1);
        Assert.state(status == 0 || status == 1, "选择的用户状态异常");
        userModel.setStatus(status);
        return userModel;
    }

    /**
     * 删除用户
     *
     * @param id 用户id
     * @return String
     */
    @RequestMapping(value = "deleteUser", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> deleteUser(String id) {
        UserModel userName = getUser();
        Assert.state(!java.util.Objects.equals(userName.getId(), id), "不能删除自己");

        UserModel userModel = userService.getByKey(id);
        Assert.notNull(userModel, "非法访问");
        if (userModel.checkSystemUser()) {
            // 如果是系统管理员，判断个数
            Assert.state(userService.systemUserCount() > 1, "系统中的系统管理员账号数量必须存在一个以上");
        }
        Assert.state(!userModel.isSuperSystemUser(), "不能删除超级管理员");
        // 非系统管理员不支持删除演示账号
        Assert.state(!userModel.isRealDemoUser(), "演示账号不支持删除");
        userService.delByKey(id);
        // 删除工作空间
        userBindWorkspaceService.deleteByUserId(id);
        //
        triggerTokenLogServer.delByUserId(id);
        return ApiResult.success("删除成功");
    }

    /**
     * 解锁用户锁定状态
     *
     * @param id id
     * @return json
     */
    @GetMapping(value = "unlock", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> unlock(@ValidatorItem String id) {
        UserModel update = UserModel.unLock(id);
        userService.updateById(update);
        return ApiResult.success("解锁成功");
    }

    /**
     * 重置用户密码
     *
     * @param id id
     * @return json
     */
    @GetMapping(value = "rest-user-pwd", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<JSONObject> restUserPwd(@ValidatorItem String id) {
        UserModel userModel = userService.getByKey(id);
        Assert.notNull(userModel, "账号不存在");
        Assert.state(!userModel.isSuperSystemUser(), "超级管理员不能通过此方式重置密码");
        //不支持重置演示账号
        Assert.state(!userModel.isRealDemoUser(), "演示账号不支持重置密码");
        String randomPwd = RandomUtil.randomString(UserModel.SALT_LEN);
        String sha1Pwd = DigestUtil.sha1(randomPwd);
        userService.updatePwd(id, sha1Pwd);
        //
        JSONObject result = new JSONObject();
        result.put("randomPwd", randomPwd);
        return ApiResult.success("重置成功", result);
    }
}
