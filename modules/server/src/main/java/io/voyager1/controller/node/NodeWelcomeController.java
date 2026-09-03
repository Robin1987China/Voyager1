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

package io.voyager1.controller.node;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import io.voyager1.util.DateTime;
import io.voyager1.util.DateUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.NodeConfig;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.func.assets.model.MachineNodeStatLogModel;
import io.voyager1.func.assets.server.MachineNodeStatLogServer;
import io.voyager1.model.BaseMachineModel;
import io.voyager1.model.data.NodeModel;
import io.voyager1.permission.SystemPermission;
import io.voyager1.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

/**
 * 节点统计信息
 *
 */
@RestController
@RequestMapping(value = "/node")
public class NodeWelcomeController extends BaseServerController {

    private final MachineNodeStatLogServer machineNodeStatLogServer;
    private final NodeConfig nodeConfig;

    public NodeWelcomeController(MachineNodeStatLogServer machineNodeStatLogServer,
                                 ServerConfig serverConfig) {
        this.machineNodeStatLogServer = machineNodeStatLogServer;
        this.nodeConfig = serverConfig.getNode();
    }

    @PostMapping(value = "node_monitor_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<MachineNodeStatLogModel>> nodeMonitorJson(String machineId) {
        NodeModel node = tryGetNode();
        List<MachineNodeStatLogModel> list = this.getList(node, machineId);
        return ApiResult.success("", list);
    }

    private List<MachineNodeStatLogModel> getList(NodeModel node, String machineId) {
        String useMachineId = Optional.ofNullable(node).map(BaseMachineModel::getMachineId).orElse(machineId);
        String startDateStr = getParameter("startTime");
        String endDateStr = getParameter("endTime");
        if (((startDateStr == null || startDateStr.isEmpty()) || (endDateStr == null || endDateStr.isEmpty()))) {
            return machineNodeStatLogServer.listByMachineId(useMachineId, 500);
        }
        //  处理时间
        DateTime startDate = DateUtil.parse(startDateStr);
        long startTime = startDate.getTime();
        DateTime endDate = DateUtil.parse(endDateStr);
        if (startDate.equals(endDate)) {
            // 时间相等
            endDate = DateUtil.endOfDay(endDate);
        }
        long endTime = endDate.getTime();
        // 开启了节点信息采集
        Pageable pageObj = PageRequest.of(0, 5000, Sort.by(Sort.Order.desc("monitorTime")));
        return machineNodeStatLogServer.listByMachineIdAndTimeRange(useMachineId, startTime, endTime, 5000);
    }

    @RequestMapping(value = "processList", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<JSONObject>> getProcessList(HttpServletRequest request, String machineId) {
        NodeModel node = tryGetNode();
        if (node != null) {
            return NodeForward.request(node, request, NodeUrl.ProcessList);
        }
        MachineNodeModel model = machineNodeServer.getByKey(machineId);
        Assert.notNull(model, "没有找到对应的机器");
        return NodeForward.request(model, request, NodeUrl.ProcessList);
    }

    @RequestMapping(value = "kill.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    public ApiResult<String> kill(HttpServletRequest request, String machineId) {
        NodeModel node = tryGetNode();
        if (node != null) {
            return NodeForward.request(node, request, NodeUrl.Kill);
        }
        MachineNodeModel model = machineNodeServer.getByKey(machineId);
        Assert.notNull(model, "没有找到对应的机器");
        return NodeForward.request(model, request, NodeUrl.Kill);
    }

    @GetMapping(value = "machine-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> machineInfo(String machineId) {
        NodeModel nodeModel = tryGetNode();
        String useMachineId = Optional.ofNullable(nodeModel).map(BaseMachineModel::getMachineId).orElse(machineId);
        MachineNodeModel model = machineNodeServer.getByKey(useMachineId);
        Assert.notNull(model, "没有找到对应的机器");
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("data", model);
        jsonObject.put("heartSecond", nodeConfig.getHeartSecond());
        return ApiResult.success("", jsonObject);
    }

    @GetMapping(value = "disk-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<JSONObject>> diskInfo(HttpServletRequest request, String machineId) {
        NodeModel node = tryGetNode();
        if (node != null) {
            return NodeForward.request(node, request, NodeUrl.DiskInfo);
        }
        MachineNodeModel model = machineNodeServer.getByKey(machineId);
        Assert.notNull(model, "没有找到对应的机器");
        return NodeForward.request(model, request, NodeUrl.DiskInfo);
    }

    @GetMapping(value = "hw-disk-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<JSONObject>> hwDiskInfo(HttpServletRequest request, String machineId) {
        NodeModel node = tryGetNode();
        if (node != null) {
            return NodeForward.request(node, request, NodeUrl.HwDiskInfo);
        }
        MachineNodeModel model = machineNodeServer.getByKey(machineId);
        Assert.notNull(model, "没有找到对应的机器");
        return NodeForward.request(model, request, NodeUrl.HwDiskInfo);
    }

    @GetMapping(value = "network-interfaces", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<JSONObject>> networkInterfaces(HttpServletRequest request, String machineId) {
        NodeModel node = tryGetNode();
        if (node != null) {
            return NodeForward.request(node, request, NodeUrl.NetworkInterfaces);
        }
        MachineNodeModel model = machineNodeServer.getByKey(machineId);
        Assert.notNull(model, "没有找到对应的机器");
        return NodeForward.request(model, request, NodeUrl.NetworkInterfaces);
    }
}
