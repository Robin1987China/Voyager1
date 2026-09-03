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

package io.voyager1.controller.ssh;

import io.voyager1.util.CollUtil;
import io.voyager1.util.Tree;
import io.voyager1.util.TreeNode;
import io.voyager1.util.TreeUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.core.api.ApiResult;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.common.validator.ValidatorRule;
import io.voyager1.func.assets.server.MachineSshServer;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.data.SshModel;
import io.voyager1.model.enums.BuildReleaseMethod;
import io.voyager1.model.log.SshTerminalExecuteLog;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.dblog.SshTerminalExecuteLogService;
import io.voyager1.service.node.ssh.SshService;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 2019/8/9
 */
@RestController
@RequestMapping(value = "node/ssh")
@Feature(cls = ClassFeature.SSH)
@Slf4j
public class SshController extends BaseServerController {

    private final SshService sshService;
    private final SshTerminalExecuteLogService sshTerminalExecuteLogService;
    private final BuildInfoService buildInfoService;
    private final MachineSshServer machineSshServer;

    public SshController(SshService sshService,
                         SshTerminalExecuteLogService sshTerminalExecuteLogService,
                         BuildInfoService buildInfoService,
                         MachineSshServer machineSshServer) {
        this.sshService = sshService;
        this.sshTerminalExecuteLogService = sshTerminalExecuteLogService;
        this.buildInfoService = buildInfoService;
        this.machineSshServer = machineSshServer;
    }


    @PostMapping(value = "list_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<SshModel>> listData(HttpServletRequest request) {
        PageResultDto<SshModel> pageResultDto = sshService.listPage(request);
        pageResultDto.each(sshModel -> {
            sshModel.setMachineSsh(machineSshServer.getByKey(sshModel.getMachineSshId()));
            List<NodeModel> nodeBySshId = nodeService.getNodeBySshId(sshModel.getId());
            sshModel.setLinkNode((nodeBySshId == null || nodeBySshId.isEmpty() ? null : nodeBySshId.get(0)));
        });
        return new ApiResult<>(200, "", pageResultDto);
    }

    @GetMapping(value = "list_data_all.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<SshModel>> listDataAll(HttpServletRequest request) {
        List<SshModel> list = sshService.listByWorkspace(request);
        return new ApiResult<>(200, "", list);
    }

    @GetMapping(value = "list-tree", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<Tree<String>> listTree(HttpServletRequest request) {
        List<SshModel> list = sshService.listByWorkspace(request);
        Map<String, TreeNode<String>> groupNode = new HashMap<>(4);
        List<TreeNode<String>> treeNodes = list.stream()
            .map(sshModel -> {
                String group = sshModel.getGroup();
                String groupId = DigestUtil.sha1((group == null || group.isEmpty() ? "" : group));
                String groupId2 = String.format("g_%s", groupId);
                groupNode.computeIfAbsent(groupId, s -> new TreeNode<>(groupId2, "/", group, sshModel.getName()));
                //
                TreeNode<String> treeNode = new TreeNode<>(sshModel.getId(), groupId2, sshModel.getName(), sshModel.getName());
                Map<String, Object> extra = new HashMap<>();
                extra.put("fileDirs", sshModel.getFileDirs());
                extra.put("isLeaf", true);
                treeNode.setExtra(extra);
                return treeNode;
            })
            .collect(Collectors.toList());
        //
        treeNodes.addAll(groupNode.values());
        Tree<String> tree = TreeUtil.buildSingle(treeNodes, "/");
        return new ApiResult<>(200, "", tree);
    }

    @GetMapping(value = "get-item.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<SshModel> getItem(@ValidatorItem String id, HttpServletRequest request) {
        SshModel byKey = sshService.getByKey(id, request);
        Assert.notNull(byKey, "对应的 ssh 不存在");
        return new ApiResult<>(200, "", byKey);
    }

    /**
     * 查询所有的分组
     *
     * @return list
     */
    @GetMapping(value = "list-group-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<String>> listGroupAll(HttpServletRequest request) {
        List<String> listGroup = sshService.listGroup(request);
        return ApiResult.success("", listGroup);
    }

    /**
     * 编辑
     *
     * @param name    名称
     * @param group   分组名
     * @param request 请求对象
     * @param id      ID
     * @return json
     */
    @PostMapping(value = "save.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> save(@ValidatorItem(value = ValidatorRule.NOT_BLANK, msg = "参数错误ssh名称不能为空") String name,
                                     String id,
                                     String group,
                                     HttpServletRequest request) {
        SshModel sshModel = new SshModel();
        sshModel.setName(name);
        sshModel.setGroup(group);
        sshModel.setId(id);
        sshService.updateById(sshModel, request);
        return ApiResult.success("操作成功");
    }


    @PostMapping(value = "del.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<Object> del(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id, HttpServletRequest request) {
        boolean checkSsh = buildInfoService.checkReleaseMethodByLike(id, request, BuildReleaseMethod.Ssh);
        Assert.state(!checkSsh, "当前ssh存在构建项，不能直接删除（需要提前解绑或者删除关联数据后才能删除）");
        // 判断是否绑定节点
        List<NodeModel> nodeBySshId = nodeService.getNodeBySshId(id);
        Assert.state((nodeBySshId == null || nodeBySshId.isEmpty()), "当前ssh被节点绑定，不能直接删除（需要提前解绑或者删除关联数据后才能删除）");

        sshService.delByKey(id, request);
        //
        sshTerminalExecuteLogService.delBySshId(id);
        return ApiResult.success("操作成功");
    }

    @PostMapping(value = "del-fore", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    @SystemPermission
    public ApiResult<Object> delFore(@ValidatorItem(value = ValidatorRule.NOT_BLANK) String id) {
        boolean checkSsh = buildInfoService.checkReleaseMethodByLike(id, BuildReleaseMethod.Ssh);
        Assert.state(!checkSsh, "当前ssh存在构建项，不能直接删除（需要提前解绑或者删除关联数据后才能删除）");
        // 判断是否绑定节点
        List<NodeModel> nodeBySshId = nodeService.getNodeBySshId(id);
        Assert.state((nodeBySshId == null || nodeBySshId.isEmpty()), "当前ssh被节点绑定，不能直接删除（需要提前解绑或者删除关联数据后才能删除）");

        sshService.delByKey(id);
        //
        sshTerminalExecuteLogService.delBySshId(id);
        return ApiResult.success("操作成功");
    }

    /**
     * 执行记录
     *
     * @return json
     */
    @PostMapping(value = "log_list_data.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(cls = ClassFeature.SSH_TERMINAL_LOG, method = MethodFeature.LIST)
    public ApiResult<PageResultDto<SshTerminalExecuteLog>> logListData(HttpServletRequest request) {
        PageResultDto<SshTerminalExecuteLog> pageResult = sshTerminalExecuteLogService.listPage(request);
        return ApiResult.success("获取成功", pageResult);
    }

    /**
     * 同步到指定工作空间
     *
     * @param ids           节点ID
     * @param toWorkspaceId 分配到到工作空间ID
     * @return msg
     */
    @GetMapping(value = "sync-to-workspace", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    @SystemPermission()
    public ApiResult<Object> syncToWorkspace(@ValidatorItem String ids,
                                                @ValidatorItem String toWorkspaceId,
                                                HttpServletRequest request) {
        String nowWorkspaceId = nodeService.getCheckUserWorkspace(request);
        //
        sshService.checkUserWorkspace(toWorkspaceId);
        sshService.syncToWorkspace(ids, nowWorkspaceId, toWorkspaceId);
        return ApiResult.success("操作成功");
    }
}
