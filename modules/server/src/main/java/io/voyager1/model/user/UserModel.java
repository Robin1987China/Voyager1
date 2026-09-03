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

package io.voyager1.model.user;

import io.voyager1.util.DateUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseUserModifyDbModel;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 用户实体
 *
 * @since 2019/1/16
 */
@EqualsAndHashCode(callSuper = true)
@TableName(value = "SYS_USER",
    nameKey = "用户账号")
@Data
@NoArgsConstructor
public class UserModel extends BaseUserModifyDbModel {
    /**
     * 系统管理员
     */
    public final static String SYSTEM_ADMIN = "sys";
    /**
     * demo 演示账号、系统预设
     */
    public final static String DEMO_USER = "demo";
    /**
     *
     */
    public static final UserModel EMPTY = new UserModel(UserModel.SYSTEM_ADMIN);
    /**
     * 系统占用名
     */
    public static final Supplier<String> SYSTEM_OCCUPY_NAME = () -> "系统管理员";
    /**
     * 用户名限制
     */
    public static final int USER_NAME_MIN_LEN = 3;
    /**
     * 盐值长度
     */
    public static int SALT_LEN = 8;
    /**
     * 昵称
     */
    private String name;
    /**
     * 密码
     */
    private String password;
    /**
     * 密码盐值
     */
    private String salt;
    /**
     * 创建此用户的人
     */
    private String parent;
    /**
     * 系统管理员
     */
    private Integer systemUser;
    /**
     * 连续登录失败次数
     */
    private Integer pwdErrorCount;
    /**
     * 最后失败时间
     */
    private Long lastPwdErrorTime;
    /**
     * 账号被锁定的时长
     */
    private Long lockTime;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 钉钉
     */
    private String dingDing;
    /**
     * 企业微信
     */
    private String workWx;
    /**
     * 状态 0 禁用  null、1 启用
     */
    private Integer status;

    /**
     * 权限组
     */
    private String permissionGroup;
    /**
     * 账号来源
     */
    private String source;

    public UserModel(String id) {
        this.setId(id);
    }

    public void setSystemUser(Integer systemUser) {
        this.systemUser = (systemUser != null ? systemUser : 0) == 1 ? systemUser : 0;
    }

    /**
     * 解锁
     */
    public static UserModel unLock(String id) {
        UserModel newModel = new UserModel(id);
        newModel.setPwdErrorCount(0);
        newModel.setLockTime(0L);
        newModel.setLastPwdErrorTime(0L);
        return newModel;
    }

    /**
     * 剩余解锁时间
     *
     * @return 0是未锁定
     */
    public long overLockTime(int alwaysLoginError) {
        if (alwaysLoginError <= 0) {
            return 0;
        }
        // 不限制演示账号的登录
        if (isDemoUser()) {
            return 0;
        }
        // 最后一次失败时间
        Long lastTime = getLastPwdErrorTime();
        if (lastTime == null || lastTime <= 0) {
            return 0;
        }
        // 当前锁定时间
        Long lockTime = getLockTime();
        if (lockTime == null || lockTime <= 0) {
            return 0;
        }
        // 解锁时间
        lastTime += lockTime;
        long nowTime = DateUtil.currentSeconds();
        // 剩余解锁时间
        lastTime -= nowTime;
        if (lastTime > 0) {
            return lastTime;
        }
        return 0;
    }

    /**
     * 登录失败，重新计算锁定时间
     *
     * @param alwaysLoginError 运行登录失败次数
     * @return 返回的信息需要更新到数据库
     */
    public UserModel errorLock(int alwaysLoginError) {
        // 未开启锁定功能
        if (alwaysLoginError <= 0) {
            return null;
        }
        UserModel newModel = new UserModel(this.getId());
        newModel.setPwdErrorCount((this.getPwdErrorCount() != null ? this.getPwdErrorCount() : 0) + 1);
        int count = newModel.getPwdErrorCount();
        // 记录错误时间
        newModel.setLastPwdErrorTime(DateUtil.currentSeconds());
        if (count < alwaysLoginError) {
            // 还未达到锁定条件
            return newModel;
        }
        int level = count / alwaysLoginError;
        switch (level) {
            case 1:
                // 在错误倍数 为1 锁定 30分钟
                newModel.setLockTime(TimeUnit.MINUTES.toSeconds(30));
                break;
            case 2:
                // 在错误倍数 为2 锁定 1小时
                newModel.setLockTime(TimeUnit.HOURS.toSeconds(1));
                break;
            default:
                // 其他情况 10小时
                newModel.setLockTime(TimeUnit.HOURS.toSeconds(10));
                break;
        }
        return newModel;
    }

    public boolean checkSystemUser() {
        return systemUser != null && systemUser == 1;
    }

    /**
     * 是否为超级管理员
     *
     * @return true 是
     */
    public boolean isSuperSystemUser() {
        return java.util.Objects.equals(getParent(), SYSTEM_ADMIN);
    }

    /**
     * demo 登录名默认为系统演示账号
     *
     * @return true
     */
    public boolean isDemoUser() {
        // demo 账号 和他创建的账号都是 demo
        return isRealDemoUser() || UserModel.DEMO_USER.equals(getParent());
    }

    /**
     * demo 登录名默认为系统演示账号
     *
     * @return true
     */
    public boolean isRealDemoUser() {
        return UserModel.DEMO_USER.equals(getId());
    }
}
