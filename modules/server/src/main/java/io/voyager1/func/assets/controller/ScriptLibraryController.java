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

package io.voyager1.func.assets.controller;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.Validator;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.forward.NodeForward;
import io.voyager1.common.forward.NodeUrl;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.func.assets.model.ScriptLibraryModel;
import io.voyager1.func.assets.server.ScriptLibraryServer;
import io.voyager1.model.PageResultDto;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

/**
 * @since 2024/6/1
 */
@RestController
@RequestMapping(value = "/system/assets/script-library")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_GLOBAL_SCRIPT)
@SystemPermission
@Slf4j
public class ScriptLibraryController extends BaseServerController {

    private final ScriptLibraryServer scriptLibraryServer;

    public ScriptLibraryController(ScriptLibraryServer scriptLibraryServer) {
        this.scriptLibraryServer = scriptLibraryServer;
    }

    @PostMapping(value = "list-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<ScriptLibraryModel>> listJson(HttpServletRequest request) {
        PageResultDto<ScriptLibraryModel> pageResultDto = scriptLibraryServer.listPage(request);
        return ApiResult.success("", pageResultDto);
    }

    @PostMapping(value = "edit", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> save(HttpServletRequest request) {
        ScriptLibraryModel scriptLibraryModel = JakartaServletUtil.toBean(request, ScriptLibraryModel.class, true);
        String tag = scriptLibraryModel.getTag();
        Assert.hasText(tag, "标记不能为空");
        Validator.validateGeneral(tag, 4, 20, "标记只能包含字母、数字、下划线");
        Assert.hasText(scriptLibraryModel.getScript(), "脚本不能为空");
        //
        String id = scriptLibraryModel.getId();
        Assert.state(!scriptLibraryServer.existsByTag(tag, id), "标记已存在");
        String oldIds = "";
        String version = StrUtil.sub(DigestUtil.md5(scriptLibraryModel.getScript()), 0, 6);
        if ((id != null && !id.isEmpty())) {
            ScriptLibraryModel libraryModel = scriptLibraryServer.getByKey(id);
            Assert.notNull(libraryModel, "数据不存在");
            Assert.state(java.util.Objects.equals(libraryModel.getTag(), tag), "脚本标记不能修改");
            oldIds = libraryModel.getMachineIds();
            if (java.util.Objects.equals(libraryModel.getScript(), scriptLibraryModel.getScript())) {
                // 内容没有变化不
                scriptLibraryModel.setVersion(null);
            } else {
                // 自动生成版本号
                String libraryModelVersion = libraryModel.getVersion();
                List<String> list = io.voyager1.util.ConvertUtil.splitTrim(libraryModelVersion, "#");
                int nextIncVersion = ConvertUtil.toInt(list.get(0), -2) + 1;
                scriptLibraryModel.setVersion(String.format("%s#%s", nextIncVersion, version));
            }
            scriptLibraryServer.updateById(scriptLibraryModel);
            if (scriptLibraryModel.getVersion() == null) {
                scriptLibraryModel.setVersion(libraryModel.getVersion());
            }
        } else {
            scriptLibraryModel.setVersion(String.format("1#%s", version));
            scriptLibraryServer.insert(scriptLibraryModel);
        }
        // 同步到机器节点
        this.syncMachineNodeScript(scriptLibraryModel, oldIds, request);
        return ApiResult.success("操作成功");
    }

    private void syncMachineNodeScript(ScriptLibraryModel scriptModel, String oldMachineIds, HttpServletRequest request) {
        List<String> oldNodeIds = io.voyager1.util.ConvertUtil.splitTrim(oldMachineIds, ",");
        List<String> newNodeIds = StrUtil.splitTrim(scriptModel.getMachineIds(), ",");
        if (newNodeIds == null) {
            newNodeIds = new java.util.ArrayList<>();
        }
        Collection<String> delNode = CollUtil.subtract(oldNodeIds, newNodeIds);
        // 删除
        this.syncDelMachineNodeScriptLibrary(scriptModel.getTag(), delNode);
        // 更新
        for (String machineId : newNodeIds) {
            MachineNodeModel byKey = machineNodeServer.getByKey(machineId);
            Assert.notNull(byKey, "没有找到对应的节点");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", scriptModel.getTag());
            jsonObject.put("description", scriptModel.getDescription());
            jsonObject.put("tag", scriptModel.getTag());
            jsonObject.put("script", scriptModel.getScript());
            jsonObject.put("version", scriptModel.getVersion());
            ApiResult<String> jsonMessage = NodeForward.request(byKey, NodeUrl.SCRIPT_LIBRARY_SAVE, jsonObject);
            String message = String.format("处理 %s 节点同步脚本库失败 %s", byKey.getName(), jsonMessage.getMsg());
            Assert.state(jsonMessage.success(), message);
        }
    }

    private void syncDelMachineNodeScriptLibrary(String tag, Collection<String> delNode) {
        for (String machineId : delNode) {
            MachineNodeModel byKey = machineNodeServer.getByKey(machineId);
            if (byKey == null) {
                // 机器可能被删除了
                // 避免无法删除脚本库的清空
                continue;
            }
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", tag);
            ApiResult<String> request = NodeForward.request(byKey, NodeUrl.SCRIPT_LIBRARY_DEL, jsonObject);
            String message = String.format("处理 %s 节点删除脚本库失败 %s", byKey.getName(), request.getMsg());
            Assert.state(request.getCode() == 200, message);
        }
    }

    @RequestMapping(value = "del", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> del(String id, HttpServletRequest request) {
        ScriptLibraryModel server = scriptLibraryServer.getByKey(id);
        if (server != null) {
            // 删除节点中的脚本
            String nodeIds = server.getMachineIds();
            List<String> delNode = io.voyager1.util.ConvertUtil.splitTrim(nodeIds, ",");
            this.syncDelMachineNodeScriptLibrary(server.getTag(), delNode);
            scriptLibraryServer.delByKey(id);
        }
        return ApiResult.success("删除成功");
    }
}
