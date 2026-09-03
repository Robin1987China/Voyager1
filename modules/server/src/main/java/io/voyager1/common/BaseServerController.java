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

package io.voyager1.common;

import io.voyager1.util.Cache;
import io.voyager1.util.CacheUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.interceptor.LoginInterceptor;
import io.voyager1.common.interceptor.PermissionInterceptor;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.func.assets.server.MachineNodeServer;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.node.NodeService;
import io.voyager1.util.StringUtil;
import org.springframework.util.Assert;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Voyager1 server 端
 *
 * @since 2019/4/16
 */
public abstract class BaseServerController extends BaseVoyager1Controller {
    private static final ThreadLocal<UserModel> USER_MODEL_THREAD_LOCAL = new ThreadLocal<>();
    public static final Cache<String, String> SHARDING_IDS = CacheUtil.newLRUCache(10, TimeUnit.DAYS.toMillis(1));
    public static final String NODE_ID = "nodeId";

    @Resource
    protected NodeService nodeService;
    @Resource
    protected MachineNodeServer machineNodeServer;

    protected NodeModel getNode() {
        NodeModel nodeModel = tryGetNode();
        Assert.notNull(nodeModel, "节点信息不正确,对应对节点不存在");
        return nodeModel;
    }

    protected NodeModel tryGetNode() {
        HttpServletRequest request = getRequest();
        return (NodeModel) request.getAttribute("node");
    }

    /**
     * 判断是否传入机器 id
     *
     * @param machineId 机器id
     * @param request   请求
     * @param nodeUrl   节点 url
     * @param pars      参数
     * @param <T>       泛型
     * @return data
     */
    protected <T> ApiResult<T> tryRequestMachine(String machineId, HttpServletRequest request, NodeUrl nodeUrl, String... pars) {
        if ((machineId != null && !machineId.isEmpty())) {
            MachineNodeModel model = machineNodeServer.getByKey(machineId);
            Assert.notNull(model, "没有找到对应的机器");
            return NodeForward.request(model, request, nodeUrl, new String[]{}, pars);
        }
        return null;
    }

    /**
     * 判断是否传入机器 id 或者节点id
     *
     * @param machineId 机器id
     * @param request   请求
     * @param nodeUrl   节点 url
     * @param pars      参数
     * @param <T>       泛型
     * @return data
     */
    protected <T> ApiResult<T> tryRequestNode(String machineId, HttpServletRequest request, NodeUrl nodeUrl, String... pars) {
        NodeModel nodeModel = tryGetNode();
        if (nodeModel != null) {
            return NodeForward.request(nodeModel, request, nodeUrl, new String[]{}, pars);
        }
        if ((machineId != null && !machineId.isEmpty())) {
            MachineNodeModel model = machineNodeServer.getByKey(machineId);
            Assert.notNull(model, "没有找到对应的机器");
            return NodeForward.request(model, request, nodeUrl, new String[]{}, pars);
        }
        return null;
    }

    /**
     * 验证 cron 表达式, demo 账号不能开启 cron
     *
     * @param cron cron
     * @return 原样返回
     */
    protected String checkCron(String cron) {
        return StringUtil.checkCron(cron, s -> {
            UserModel user = getUser();
            Assert.state(!user.isDemoUser(), PermissionInterceptor.DEMO_TIP);
            return s;
        });
    }

    /**
     * 为线程设置 用户
     *
     * @param userModel 用户
     */
    public static void resetInfo(UserModel userModel) {
        UserModel userModel1 = USER_MODEL_THREAD_LOCAL.get();
        if (userModel1 != null && userModel == UserModel.EMPTY) {
            // 已经存在，更新为 empty 、跳过
            return;
        }
        USER_MODEL_THREAD_LOCAL.set(userModel);
    }

    protected UserModel getUser() {
        UserModel userByThreadLocal = getUserByThreadLocal();
        Assert.notNull(userByThreadLocal, ServerConst.AUTHORIZE_TIME_OUT_CODE + "");
        return userByThreadLocal;
    }

    /**
     * 从线程 缓存中获取 用户信息
     *
     * @return 用户
     */
    public static UserModel getUserByThreadLocal() {
        return Optional.ofNullable(USER_MODEL_THREAD_LOCAL.get()).orElseGet(BaseServerController::getUserModel);
//		return ;
    }

    public static void removeAll() {
        USER_MODEL_THREAD_LOCAL.remove();
    }

    /**
     * 只清理 是 empty 对象
     */
    public static void removeEmpty() {
        UserModel userModel = USER_MODEL_THREAD_LOCAL.get();
        if (userModel == UserModel.EMPTY) {
            USER_MODEL_THREAD_LOCAL.remove();
        }
    }

    public static UserModel getUserModel() {
        ServletRequestAttributes servletRequestAttributes = tryGetRequestAttributes();
        if (servletRequestAttributes == null) {
            return null;
        }
        return (UserModel) servletRequestAttributes.getAttribute(LoginInterceptor.SESSION_NAME, RequestAttributes.SCOPE_SESSION);
    }

    @Override
    public void uploadSharding(MultipartFile file, String tempPath, String sliceId, Integer totalSlice, Integer nowSlice, String fileSumMd5, String... extNames) throws IOException {
        Assert.state(BaseServerController.SHARDING_IDS.containsKey(sliceId), "不合法的分片id");
        super.uploadSharding(file, tempPath, sliceId, totalSlice, nowSlice, fileSumMd5, extNames);
    }

    @Override
    public File shardingTryMerge(String tempPath, String sliceId, Integer totalSlice, String fileSumMd5) throws IOException {
        Assert.state(BaseServerController.SHARDING_IDS.containsKey(sliceId), "不合法的分片id");
        try {
            return super.shardingTryMerge(tempPath, sliceId, totalSlice, fileSumMd5);
        } finally {
            // 判断-删除分片id
            BaseServerController.SHARDING_IDS.remove(sliceId);
        }
    }
}
