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

package io.voyager1.service.user;

import io.voyager1.util.CollUtil;
import io.voyager1.util.CompareUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.RandomUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.core.db.Entity;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.ServerConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.controller.user.UserWorkspaceModel;
import io.voyager1.core.entity.UserEntity;
import io.voyager1.core.jpa.JpaBaseService;
import io.voyager1.core.repository.UserRepository;
import io.voyager1.model.data.WorkspaceModel;
import io.voyager1.model.dto.UserLoginDto;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.system.SystemParametersServer;
import io.voyager1.util.JwtUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 2021/12/3
 */
@Service
public class UserService extends JpaBaseService<UserModel, UserEntity> {
    private final SystemParametersServer systemParametersServer;
    private final UserBindWorkspaceService userBindWorkspaceService;
    private final UserRepository userRepository;

    public UserService(SystemParametersServer systemParametersServer,
                       UserBindWorkspaceService userBindWorkspaceService,
                       UserRepository userRepository) {
        this.systemParametersServer = systemParametersServer;
        this.userBindWorkspaceService = userBindWorkspaceService;
        this.userRepository = userRepository;
    }

    @Override
    protected JpaRepository<UserEntity, String> repository() {
        return userRepository;
    }

    @Override
    protected JpaSpecificationExecutor<UserEntity> specExecutor() {
        return userRepository;
    }

    @Override
    protected Class<UserEntity> entityClass() {
        return UserEntity.class;
    }

    @Override
    protected Class<UserModel> modelClass() {
        return UserModel.class;
    }

    /**
     * 是否需要初始化
     *
     * @return true 有系统管理员账号，系统可以正常使用
     */
    public boolean canUse() {
        UserModel userModel = new UserModel();
        userModel.setSystemUser(1);
        return this.exists(userModel);
    }

    @Override
    protected void fillSelectResult(UserModel data) {
        if (data == null) {
            return;
        }
        data.setSalt(null);
        data.setPassword(null);
    }

    /**
     * 生成 随机盐值
     *
     * @return 随机盐值
     */
    public synchronized String generateSalt() {
        while (true) {
            String salt = RandomUtil.randomString(UserModel.SALT_LEN);
            UserModel userModel = new UserModel();
            userModel.setSalt(salt);
            boolean exists = this.exists(userModel);
            if (exists) {
                continue;
            }
            return salt + "-" + System.currentTimeMillis();
        }
    }

    /**
     * 验证用户md5
     *
     * @param userMd5 用户md5
     * @return userModel 用户对象
     */
    public UserModel checkUser(String userMd5) {
        UserModel userModel = new UserModel();
        userModel.setPassword(userMd5);
        return this.queryByBean(userModel);
    }

    /**
     * 查询用户 jwt id
     *
     * @param userModel 用户
     * @return jwt id
     */
    public UserLoginDto getUserJwtId(UserModel userModel) {
        // 判断是否禁用
        Integer status = userModel.getStatus();
        Assert.state(status == null || status == 1, ServerConst.ACCOUNT_LOCKED_TIP);
        String id = userModel.getId();
        String sql = "select password from " + this.getTableName() + " where id=?";
        List<io.voyager1.core.db.Entity> query = this.query(sql, id);
        Entity first = (query == null || query.isEmpty() ? null : query.get(0));
        Assert.notEmpty(first, "没有对应的用户信息");
        String password = (String) first.get("password");
        Assert.hasText(password, "没有对应的用户信息");
        return new UserLoginDto(JwtUtil.builder(userModel, password), password);
    }

    /**
     * 当前系统中的系统管理员的数量
     *
     * @return int
     */
    public long systemUserCount() {
        UserModel userModel = new UserModel();
        userModel.setSystemUser(1);
        return this.count(this.dataBeanToEntity(userModel));
    }

    /**
     * 修改密码
     *
     * @param id     账号ID
     * @param newPwd 新密码
     */
    public void updatePwd(String id, String newPwd) {
        String salt = this.generateSalt();
        UserModel userModel = UserModel.unLock(id);
        //		userModel.setId(id);
        userModel.setSalt(salt);
        userModel.setPassword(DigestUtil.sha1(newPwd + salt));
        this.updateById(userModel);
    }

    /**
     * 用户登录
     *
     * @param name 用户名
     * @param pwd  密码
     * @return 登录
     */
    public UserModel simpleLogin(String name, String pwd) {
        UserModel userModel = this.getByKey(name, false);
        if (userModel == null) {
            return null;
        }
        String obj = DigestUtil.sha1(pwd + userModel.getSalt());
        if (java.util.Objects.equals(obj, userModel.getPassword())) {
            this.fillSelectResult(userModel);
            return userModel;
        }
        return null;
    }

    /**
     * 重置超级管理账号密码
     *
     * @return 新密码
     */
    public String restSuperUserPwd() {
        UserModel userModel = new UserModel();
        userModel.setParent(UserModel.SYSTEM_ADMIN);
        UserModel queryByBean = this.queryByBean(userModel);
        if (queryByBean == null) {
            return null;
        }
        String newPwd = RandomUtil.randomString(UserModel.SALT_LEN);
        this.updatePwd(queryByBean.getId(), DigestUtil.sha1(newPwd));
        return String.format("重置超级管理员账号密码成功, 登录账号为：%s 新密码为：%s", queryByBean.getId(), newPwd);
    }

    /**
     * 是否包含 demo 账号
     *
     * @return true
     */
    public boolean hasDemoUser() {
        UserModel userModel = new UserModel();
        userModel.setId(UserModel.DEMO_USER);
        return this.exists(userModel);
    }

    public List<UserWorkspaceModel> myWorkspace(UserModel user) {
        List<WorkspaceModel> models = userBindWorkspaceService.listUserWorkspaceInfo(user);
        Assert.notEmpty(models, "当前账号没有绑定任何工作空间，请联系管理员处理");
        JSONObject parametersServerConfig = systemParametersServer.getConfig("user-my-workspace-" + user.getId(), JSONObject.class);
        return models.stream()
            .map(workspaceModel -> {
                UserWorkspaceModel userWorkspaceModel = new UserWorkspaceModel();
                userWorkspaceModel.setId(workspaceModel.getId());
                userWorkspaceModel.setName(workspaceModel.getName());
                userWorkspaceModel.setGroup(workspaceModel.getGroup());
                userWorkspaceModel.setOriginalName(workspaceModel.getName());
                userWorkspaceModel.setClusterInfoId(workspaceModel.getClusterInfoId());
                Long createTimeMillis = workspaceModel.getCreateTimeMillis();
                userWorkspaceModel.setSort((int) ((createTimeMillis != null ? createTimeMillis : 0L) / 1000L));
                return userWorkspaceModel;
            })
            .peek(userWorkspaceModel -> {
                if (parametersServerConfig == null) {
                    return;
                }
                UserWorkspaceModel userConfig = parametersServerConfig.getObject(userWorkspaceModel.getId(), UserWorkspaceModel.class);
                if (userConfig == null) {
                    return;
                }
                userWorkspaceModel.setName((userConfig.getName() == null || userConfig.getName().isEmpty() ? userWorkspaceModel.getName() : userConfig.getName()));
                userWorkspaceModel.setSort((userConfig.getSort() != null ? userConfig.getSort() : userWorkspaceModel.getSort()));
            })
            .sorted((o1, o2) -> CompareUtil.compare(o1.getSort(), o2.getSort()))
            .collect(Collectors.toList());
    }
}
