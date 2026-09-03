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

package io.voyager1.func.openapi.controller;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.NetUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.interceptor.NotLogin;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.model.data.WorkspaceModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.system.WorkspaceService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 节点管理
 *
 * @since 2019/8/5
 */
@RestController
@Slf4j
public class NodeInfoController extends BaseServerController {

    private static final Map<String, JSONObject> CACHE_RECEIVE_PUSH = new HashMap<>();

    private final NodeService nodeService;
    private final WorkspaceService workspaceService;

    public NodeInfoController(NodeService nodeService,
                              WorkspaceService workspaceService) {
        this.nodeService = nodeService;
        this.workspaceService = workspaceService;
    }

    /**
     * 接收节点推送的信息
     * <p>
     * --auto-push-to-server http://127.0.0.1:3000/api/node/receive_push?token=462a47b8fba8da1f824370bb9fcdc01aa1a0fe20&workspaceId=DEFAULT
     *
     * @return json
     */
    @RequestMapping(value = ServerOpenApi.RECEIVE_PUSH, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    public ApiResult<JSONObject> receivePush(@ValidatorItem(msg = "凭证不能为空") String token,
                                                @ValidatorItem(msg = "通信 IP 不能为空") String ips,
                                                @ValidatorItem(msg = "登录名不能为空") String loginName,
                                                @ValidatorItem(msg = "密码不能为空") String loginPwd,
                                                @ValidatorItem(msg = "工作空间ID不能为空") String workspaceId,
                                                @ValidatorItem(value = ValidatorRule.NUMBERS, msg = "端口错误") int port,
                                                String ping) {
        Assert.state(java.util.Objects.equals(token, Voyager1Manifest.getInstance().randomIdSign()), "token error");
        boolean exists = workspaceService.exists(new WorkspaceModel(workspaceId));
        Assert.state(exists, "workspaceId error");
        String sha1Id = DigestUtil.sha1(ips);
        //
        List<String> ipsList = java.util.Arrays.asList(ips.split(","));
        String clientIp = getClientIP();
        if (!ipsList.contains(clientIp)) {
            ipsList.add(clientIp);
        }
        List<String> canUseIps = ipsList.stream()
            .filter(s -> this.testIpPort(s, ping, port))
            .collect(Collectors.toList());
        List<MachineNodeModel> canUseNode = canUseIps.stream().map(s -> {
            MachineNodeModel model = this.createMachineNodeModel(s, loginName, loginPwd, port);
            try {
                machineNodeServer.testNode(model);
            } catch (Exception e) {
                log.warn("测试结果：{} {}", model.getVoyager1Url(), e.getMessage());
                return null;
            }
            return model;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        // 只返回能通的 IP
        canUseIps = canUseNode.stream().map(MachineNodeModel::getName).collect(Collectors.toList());
        // 标记为系统操作
        BaseServerController.resetInfo(UserModel.EMPTY);
        //
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("allIp", ipsList);
        jsonObject.put("canUseIp", canUseIps);
        jsonObject.put("port", port);
        jsonObject.put("id", sha1Id);
        jsonObject.put("canUseNode", canUseNode);
        //
        for (MachineNodeModel nodeModel : canUseNode) {
            MachineNodeModel existsMachine = machineNodeServer.getByUrl(nodeModel.getVoyager1Url());
            if (existsMachine != null) {
                if (nodeService.existsNode2(workspaceId, existsMachine.getId())) {
                    // 存在
                    jsonObject.put("type", "exists");
                } else {
                    // 自动同步
                    jsonObject.put("type", "success");
                    machineNodeServer.insertNode(existsMachine, workspaceId);
                }
                break;
            }
        }
        if (!jsonObject.containsKey("type")) {
            int size1 = (canUseNode == null ? 0 : canUseNode.size());
            if (size1 == 1) {
                // 只有一个 ip 可以使用,添加插件端
                BaseServerController.resetInfo(UserModel.EMPTY);
                MachineNodeModel first = (canUseNode == null || canUseNode.isEmpty() ? null : canUseNode.get(0));
                machineNodeServer.insertAndNode(first, workspaceId);
                jsonObject.put("type", "success");
            } else {
                jsonObject.put("type", size1 == 0 ? "canUseIpEmpty" : "multiIp");
            }
        }
        CACHE_RECEIVE_PUSH.put(sha1Id, jsonObject);
        return ApiResult.success("done", jsonObject);
    }

    /**
     * 查询所有缓存
     *
     * @return list
     */
    public static Collection<JSONObject> listReceiveCache(String removeId) {
        if ((removeId != null && !removeId.isEmpty())) {
            CACHE_RECEIVE_PUSH.remove(removeId);
        }
        return CACHE_RECEIVE_PUSH.values();
    }

    public static JSONObject getReceiveCache(String id) {
        return CACHE_RECEIVE_PUSH.get(id);
    }

    /**
     * 尝试 ping
     *
     * @param ip   ip 地址
     * @param ping ping 时间
     * @return true
     */
    private boolean testIpPort(String ip, String ping, int port) {
        int pingTime = ConvertUtil.toInt(ping, 5);
        if (pingTime <= 0) {
            return true;
        }
        boolean pinged = NetUtil.ping(ip, pingTime * 1000);
        //
        return pinged || this.testIpCanPort(ip, pingTime, port);
    }

    private boolean testIpCanPort(String ip, int timeout, int port) {
        InetSocketAddress address = NetUtil.createAddress(ip, port);
        return NetUtil.isOpen(address, (int) TimeUnit.SECONDS.toMillis(timeout));
    }

    private MachineNodeModel createMachineNodeModel(String ip, String loginName, String loginPwd, int port) {
        MachineNodeModel machineNodeModel = new MachineNodeModel();
        machineNodeModel.setName(ip);
        machineNodeModel.setStatus(1);
        machineNodeModel.setVoyager1Username(loginName);
        machineNodeModel.setVoyager1Password(loginPwd);
        machineNodeModel.setVoyager1Url(ip + ":" + port);
        machineNodeModel.setVoyager1Protocol("http");
        return machineNodeModel;
    }
}
