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

import io.voyager1.util.ArrayUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.core.db.TableName;
import io.voyager1.model.BaseDbModel;

/**
 * @since 2021/12/4
 */
@TableName(value = "SYS_USER_WORKSPACE",
    nameKey = "用户(权限组)工作空间关系表")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserBindWorkspaceModel extends BaseDbModel {

    /**
     * 权限组ID
     *
     * @see UserPermissionGroupBean#getId()
     * 兼容旧数据
     * @see UserModel#getId()
     */
    private String userId;

    private String workspaceId;

    /**
     * 生产绑定关系表 主键 ID
     *
     * @param userId      用户ID
     * @param workspaceId 工作空间ID
     * @return id
     */
    public static String getId(String userId, String workspaceId) {
        return DigestUtil.sha1(userId + workspaceId);
    }

    @Builder
    public static class PermissionResult {
        /**
         * 结果
         */
        private PermissionResultEnum state;
        /**
         * 不能执行的原因
         */
        private String msg;

        public boolean isSuccess() {
            return state == PermissionResultEnum.SUCCESS;
        }

        public String errorMsg(String... pars) {
            String errorMsg = (msg == null || msg.isEmpty() ? "您没有对应权限" : msg);
            return String.format("%s %s", ArrayUtil.join(pars, " "), errorMsg);
        }
    }

    public enum PermissionResultEnum {
        /**
         * 允许执行
         */
        SUCCESS,
        /**
         * 没有权限
         */
        FAIL,
        /**
         * 当前禁止执行
         */
        MISS_PROHIBIT,
        /**
         * 不在计划允许时间段
         */
        MISS_PERIOD,
    }
}
