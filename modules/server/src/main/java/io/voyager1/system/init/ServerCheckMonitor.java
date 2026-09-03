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

package io.voyager1.system.init;

import io.voyager1.event.ISystemTask;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.ILoadEvent;
import io.voyager1.common.RemoteVersion;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.cron.CronUtils;
import io.voyager1.func.assets.AssetsExecutorPoolService;
import io.voyager1.model.data.NodeModel;
import io.voyager1.script.BaseRunScript;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.node.script.NodeScriptExecuteLogServer;
import io.voyager1.service.node.script.NodeScriptServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.util.Collection;
import java.util.List;

/**
 * 检查监控数据状态
 *
 * @since 2019/7/14
 */
@Configuration
@Slf4j
public class ServerCheckMonitor implements ILoadEvent, ISystemTask {

    private final NodeService nodeService;
    private final NodeScriptServer nodeScriptServer;
    private final NodeScriptExecuteLogServer nodeScriptExecuteLogServer;
    private final AssetsExecutorPoolService assetsExecutorPoolService;

    public ServerCheckMonitor(NodeService nodeService,
                              NodeScriptServer nodeScriptServer,
                              NodeScriptExecuteLogServer nodeScriptExecuteLogServer,
                              AssetsExecutorPoolService assetsExecutorPoolService) {
        this.nodeService = nodeService;
        this.nodeScriptServer = nodeScriptServer;
        this.nodeScriptExecuteLogServer = nodeScriptExecuteLogServer;
        this.assetsExecutorPoolService = assetsExecutorPoolService;
    }

    /**
     * 同步 节点的脚本模版日志
     *
     * @param nodeModel 节点
     */
    private void pullScriptLogItem(NodeModel nodeModel) {
        try {
            //NodeScriptExecuteLogServer nodeScriptExecuteLogServer = SpringContextHolder.getBean(NodeScriptExecuteLogServer.class);
            Collection<String> strings = nodeScriptExecuteLogServer.syncExecuteNodeInc(nodeModel);
            if (strings == null) {
                return;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ids", strings);
            ApiResult<Object> jsonMessage = NodeForward.requestBody(nodeModel, NodeUrl.SCRIPT_DEL_EXEC_LOG, jsonObject);
            if (!jsonMessage.success()) {
                log.error("删除脚本模版执行数据错误:{}", jsonMessage);
            }
        } catch (Exception e) {
            log.error("同步脚本异常", e);
        }
    }


    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        // 拉取 脚本模版日志
        CronUtils.upsert("pull_script_log", "0 0/1 * * * ?", () -> {
            //NodeService nodeService = SpringContextHolder.getBean(NodeService.class);
            //NodeScriptServer nodeScriptServer = SpringContextHolder.getBean(NodeScriptServer.class);
            List<String> nodeIds = nodeScriptServer.hasScriptNode();
            if (nodeIds == null) {
                return;
            }
            for (String nodeId : nodeIds) {
                NodeModel nodeModel = nodeService.getByKey(nodeId);
                if (nodeModel == null) {
                    continue;
                }
                assetsExecutorPoolService.execute(() -> this.pullScriptLogItem(nodeModel));
            }
        });
    }

    @Override
    public void executeTask() {
        // 尝试获取版本更新信息
        RemoteVersion.loadRemoteInfo();
        // 清空脚本缓存
        BaseRunScript.clearRunScript();
    }
}
