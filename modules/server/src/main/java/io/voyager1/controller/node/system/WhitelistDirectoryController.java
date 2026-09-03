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

package io.voyager1.controller.node.system;

import io.voyager1.util.ReflectUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.model.data.AgentWhitelist;
import io.voyager1.model.data.NodeModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.system.WhitelistDirectoryService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 授权目录
 *
 * @since 2019/2/28
 */
@RestController
@RequestMapping(value = "/node/system")
@Feature(cls = ClassFeature.NODE_CONFIG_WHITELIST)
public class WhitelistDirectoryController extends BaseServerController {

    private final WhitelistDirectoryService whitelistDirectoryService;

    public WhitelistDirectoryController(WhitelistDirectoryService whitelistDirectoryService) {
        this.whitelistDirectoryService = whitelistDirectoryService;
    }


    /**
     * get whiteList data
     * 授权数据接口
     *
     * @return json
     */
    @RequestMapping(value = "white-list", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<Map<String, String>> whiteList(String machineId) {
        NodeModel nodeModel = tryGetNode();
        AgentWhitelist agentWhitelist;
        if (nodeModel != null) {
            agentWhitelist = whitelistDirectoryService.getData(nodeModel);
        } else {
            MachineNodeModel machineNodeModel = machineNodeServer.getByKey(machineId);
            agentWhitelist = whitelistDirectoryService.getData(machineNodeModel);
        }
        Map<String, String> map = new HashMap<>(8);
        if (agentWhitelist != null) {
            /**
             * put key and value into map
             * 赋值给 map 对象返回
             */
            Field[] fields = ReflectUtil.getFields(AgentWhitelist.class, field -> Collection.class.isAssignableFrom(field.getType()) || String.class.isAssignableFrom(field.getType()));
            for (Field field : fields) {
                Object fieldValue = ReflectUtil.getFieldValue(agentWhitelist, field);
                if (fieldValue instanceof Collection) {
                    Collection<String> collection = (Collection<String>) fieldValue;
                    map.put(field.getName(), AgentWhitelist.convertToLine(collection));
                } else if (fieldValue instanceof String) {
                    map.put(field.getName(), (String) fieldValue);
                }
            }
        }
        return ApiResult.success("", map);
    }


    /**
     * 保存接口
     *
     * @return json
     */
    @RequestMapping(value = "whitelistDirectory_submit", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> whitelistDirectorySubmit(HttpServletRequest request, String machineId) {
        ApiResult<String> objectJsonMessage = this.tryRequestNode(machineId, request, NodeUrl.WhitelistDirectory_Submit);
        Assert.notNull(objectJsonMessage, "请选择节点");
        return objectJsonMessage;
    }
}
