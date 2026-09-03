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

package io.voyager1.controller.system;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.Tree;
import io.voyager1.util.TreeNode;
import io.voyager1.util.TreeUtil;
import io.voyager1.util.MapUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.util.StringUtil;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 配置文件管理
 *
 * @since 2023/1/04
 */
@RestController
@RequestMapping(value = "system/ext-conf")
@Feature(cls = ClassFeature.SYSTEM_EXT_CONFIG)
@SystemPermission
@Slf4j
public class SystemExtConfigController extends BaseServerController {

    /**
     * 获取外部配置文件的 数据
     *
     * @param parentMap 父级 map
     * @return map
     */
    private Map<String, TreeNode<String>> listDir(Map<String, TreeNode<String>> parentMap) {
        File configResourceDir = ExtConfigBean.getConfigResourceDir();
        if (configResourceDir == null) {
            return new java.util.HashMap<>();
        }
        List<File> files = FileUtil.loopFiles(configResourceDir);
        return files.stream()
            .filter(FileUtil::isFile)
            .map(file -> {
                String path = StringUtil.delStartPath(file, configResourceDir.getAbsolutePath(), true);
                return this.buildItemTreeNode(path);
            })
            .peek(node -> {
                String id = node.getId();
                this.buildParent(parentMap, id);
                //
                Map<String, Object> extra = node.getExtra();
                extra.put("defaultConfig", false);
            }).collect(Collectors.toMap(TreeNode::getId, node -> node));
    }

    /**
     * 插件单个 node 对象
     *
     * @param path 路径
     * @return tree node
     */
    private TreeNode<String> buildItemTreeNode(String path) {

        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(path, "/");
        int size = list.size();
        String parentId = size > 1 ? CollUtil.join(list.subList(0, size - 1), "/") : "/";

        // 使用可变 Map，后续 listDir/classPathList 会向 extra 追加 defaultConfig/hasDefault 等字段
        Map<String, Object> extra = new HashMap<>();
        extra.put("isLeaf", true);
        return new TreeNode<>(path, parentId, (list == null || list.isEmpty() ? null : list.get(list.size() - 1)), 0).setExtra(extra);
    }

    @GetMapping(value = "list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<Tree<String>> list() throws Exception {
        Map<String, TreeNode<String>> parentMap = new LinkedHashMap<>(10);
        // root 节点
        parentMap.put("/", new TreeNode<>("/", null, "根路径", 0));
        Map<String, TreeNode<String>> listDir = this.listDir(parentMap);
        //
        PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = pathMatchingResourcePatternResolver.getResources("classpath*:/config_default/**");
        List<TreeNode<String>> classPathList = Arrays.stream(resources)
            .filter(resource -> {
                if (resource.isFile()) {
                    // 本地运行可能出现文件夹的 item
                    try {
                        if (resource.getFile().isDirectory()) {
                            return false;
                        }
                    } catch (IOException e) {
                        throw Lombok.sneakyThrow(e);
                    }
                }
                return true;
            })
            .map(resource -> {
                try {
                    String path = resource.getURL().getPath();
                    if ((path != null && path.endsWith("/"))) {
                        // 目录
                        return null;
                    }
                    String itemPath = StrUtil.subAfter(path, "/config_default/", false);
                    //log.debug("测试：{} {}", path, itemPath);
                    return this.buildItemTreeNode(itemPath);
                } catch (IOException e) {
                    throw Lombok.sneakyThrow(e);
                }
            })
            .filter(Objects::nonNull)
            .peek(node -> {
                String id = node.getId();
                this.buildParent(parentMap, id);
                //
                node.setName(String.format("%s [默认]", node.getName()));
                Map<String, Object> extra = node.getExtra();
                extra.put("defaultConfig", true);
                extra.put("hasDefault", true);
            })
            // 过滤 dir 已经存在的
            .filter(node -> {
                TreeNode<String> treeNode = listDir.get(node.getId());
                if (treeNode != null) {
                    Map<String, Object> extra = treeNode.getExtra();
                    extra.put("hasDefault", true);
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());

        List<TreeNode<String>> allList = new ArrayList<>();
        allList.addAll(parentMap.values());
        allList.addAll(classPathList);
        allList.addAll(listDir.values());
        // 过滤主配置文件
        allList = allList.stream().peek(node -> {
            Map<String, Object> extra = node.getExtra();
            if (extra == null) {
                return;
            }
            extra.put("disabled", java.util.Objects.equals(node.getId(), Const.FILE_NAME));
        }).collect(Collectors.toList());
        Tree<String> stringTree = TreeUtil.buildSingle(allList, "/");
        stringTree.setName("/");

        return ApiResult.success("", stringTree);
    }

    @GetMapping(value = "get-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<Object> getItem(@ValidatorItem String name) {
        InputStream resourceInputStream = ExtConfigBean.getConfigResourceInputStream(name);
        String s = IoUtil.readUtf8(resourceInputStream);
        return ApiResult.success("", s);
    }

    @PostMapping(value = "save-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> saveItem(@ValidatorItem String name, @ValidatorItem String content) {
        File configResourceFile = ExtConfigBean.getConfigResourceFile(name);
        Assert.notNull(configResourceFile, "不能编辑对应的配置文件");
        FileUtil.writeUtf8String(content, configResourceFile);
        return ApiResult.success("修改成功");
    }

    @GetMapping(value = "get-default-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<Object> getDefaultItem(@ValidatorItem String name) {
        InputStream resourceInputStream = ExtConfigBean.getDefaultConfigResourceInputStream(name);
        String s = IoUtil.readUtf8(resourceInputStream);
        return ApiResult.success("", s);
    }

    @GetMapping(value = "add-item", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> addItem(@ValidatorItem String name) {
        boolean existConfigResource = ExtConfigBean.existConfigResource(name);
        Assert.state(!existConfigResource, "对应的配置文件已经存在啦");
        File resourceFile = ExtConfigBean.getConfigResourceFile(name);
        Assert.notNull(resourceFile, "当前环境不能创建配置文件");
        FileUtil.touch(resourceFile);
        return ApiResult.success("创建成功");
    }

    private void buildParent(Map<String, TreeNode<String>> parentMap, String path) {
        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(path, "/");
        for (int i = 0; i < list.size() - 1; i++) {
            String name = list.get(i);
            String pathId = CollUtil.join(list.subList(0, i + 1), "/");
            String parentId = i > 0 ? CollUtil.join(list.subList(0, i), "/") : "/";
            parentMap.computeIfAbsent(pathId, s -> new TreeNode<>(s, parentId, name, 0));
        }
    }
}
