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

package io.voyager1.model.data;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.PropIgnore;
import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import io.voyager1.core.db.TableName;
import io.voyager1.func.assets.controller.BaseSshFileController;
import io.voyager1.func.assets.model.MachineSshModel;
import io.voyager1.model.BaseGroupModel;
import io.voyager1.util.StringUtil;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ssh 信息
 *
 * @since 2019/8/9
 */
@TableName(value = "INFRA_SSH",
    nameKey = "SSH 信息")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class SshModel extends BaseGroupModel implements BaseSshFileController.ItemConfig {

    private String name;
    @Deprecated
    private String host;
    @Deprecated
    private Integer port;
    @Deprecated
    private String user;
    @Deprecated
    private String password;
    /**
     * 编码格式
     */
    @Deprecated
    private String charset;

    /**
     * ssh 私钥
     */
    @Deprecated
    private String privateKey;
    @Deprecated
    private String connectType;
    /**
     * 文件目录
     */
    private String fileDirs;
    /**
     * 不允许执行的命令
     */
    private String notAllowedCommand;
    /**
     * 允许编辑的后缀文件
     */
    private String allowEditSuffix;
    /**
     * 节点超时时间
     */
    @Deprecated
    private Integer timeout;

    /**
     * ssh id
     */
    private String machineSshId;

    @PropIgnore
    private MachineSshModel machineSsh;

    @PropIgnore
    private NodeModel linkNode;

    @PropIgnore
    private WorkspaceModel workspace;

    public SshModel(String id) {
        this.setId(id);
    }


    @Override
    public List<String> fileDirs() {
        List<String> strings = StringUtil.jsonConvertArray(this.fileDirs, String.class);
        return Optional.ofNullable(strings)
                .map(strings1 -> strings1.stream()
                        .map(s -> FileUtil.normalize("/" + s + "/"))
                        .collect(Collectors.toList()))
                .orElse(null);
    }

    public void fileDirs(List<String> fileDirs) {
        if (fileDirs != null) {
            for (int i = fileDirs.size() - 1; i >= 0; i--) {
                String s = fileDirs.get(i);
                fileDirs.set(i, FileUtil.normalize(s));
            }
            this.fileDirs = JSONArray.toJSONString(fileDirs);
        } else {
            this.fileDirs = "";
        }
    }


    @Override
    public List<String> allowEditSuffix() {
        return StringUtil.jsonConvertArray(this.allowEditSuffix, String.class);
    }

    public void allowEditSuffix(List<String> allowEditSuffix) {
        if (allowEditSuffix == null) {
            this.allowEditSuffix = null;
        } else {
            this.allowEditSuffix = JSONArray.toJSONString(allowEditSuffix);
        }
    }

    /**
     * 检查是否包含禁止命令
     *
     * @param sshItem   实体
     * @param inputItem 输入的命令
     * @return false 存在禁止输入的命令
     */
    public static boolean checkInputItem(SshModel sshItem, String inputItem) {
        // 检查禁止执行的命令
        String notAllowedCommand = (sshItem.getNotAllowedCommand() == null || sshItem.getNotAllowedCommand().isEmpty() ? "" : sshItem.getNotAllowedCommand()).toLowerCase();
        if ((notAllowedCommand == null || notAllowedCommand.isEmpty())) {
            return true;
        }
        List<String> split = io.voyager1.util.ConvertUtil.splitTrim(notAllowedCommand, ",");
        inputItem = inputItem.toLowerCase();
        List<String> commands = io.voyager1.util.ConvertUtil.splitTrim(inputItem, StrUtil.CR);
        commands.addAll(java.util.Arrays.asList(inputItem.split(java.util.regex.Pattern.quote("&"))));
        for (String s : split) {
            //
            boolean anyMatch = commands.stream().anyMatch(item -> StrUtil.startWithAny(item, s + " ", ("&" + s + " "), " " + s + " "));
            if (anyMatch) {
                return false;
            }
            //
            anyMatch = commands.stream().anyMatch(item -> java.util.Objects.equals(item, s));
            if (anyMatch) {
                return false;
            }
        }
        return true;
    }
}
