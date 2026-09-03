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

package io.voyager1.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 树构建工具类，"" {@code io.voyager1.util.TreeUtil}。
 */
public class TreeUtil {

    private TreeUtil() {
    }

    /**
     * 构建单 root 节点树。生成一个以指定 rootId 为 ID 的根节点，再逐级挂载子节点。
     *
     * @param list   源数据集合
     * @param rootId 最顶层父 id 值
     * @param <T>    ID 类型
     * @return {@link Tree}
     */
    public static <T> Tree<T> buildSingle(List<TreeNode<T>> list, T rootId) {
        List<TreeNode<T>> nodes = (list != null ? list : Collections.emptyList());

        // parentId -> 子节点列表
        Map<T, List<TreeNode<T>>> childrenMap = new LinkedHashMap<>();
        TreeNode<T> explicitRoot = null;
        for (TreeNode<T> node : nodes) {
            if (node == null) {
                continue;
            }
            if (Objects.equals(node.getId(), rootId)) {
                explicitRoot = node;
            }
            childrenMap.computeIfAbsent(node.getParentId(), k -> new ArrayList<>()).add(node);
        }

        // 按权重排序（稳定排序）
        for (List<TreeNode<T>> children : childrenMap.values()) {
            children.sort((a, b) -> compareWeight(a.getWeight(), b.getWeight()));
        }

        Tree<T> root = new Tree<>();
        root.setId(rootId);
        if (explicitRoot != null) {
            copyNode(explicitRoot, root);
        }
        buildChildren(root, rootId, childrenMap);
        return root;
    }

    private static <T> void buildChildren(Tree<T> parent, T parentId, Map<T, List<TreeNode<T>>> childrenMap) {
        List<TreeNode<T>> children = childrenMap.get(parentId);
        if (children == null || children.isEmpty()) {
            parent.setChildren(new ArrayList<>());
            return;
        }
        List<Tree<T>> treeChildren = new ArrayList<>(children.size());
        for (TreeNode<T> child : children) {
            Tree<T> tree = new Tree<>();
            copyNode(child, tree);
            buildChildren(tree, child.getId(), childrenMap);
            treeChildren.add(tree);
        }
        parent.setChildren(treeChildren);
    }

    private static <T> void copyNode(TreeNode<T> node, Tree<T> tree) {
        tree.setId(node.getId());
        tree.setParentId(node.getParentId());
        tree.setWeight(node.getWeight());
        tree.setName(node.getName());
        Map<String, Object> extra = node.getExtra();
        if (extra != null && !extra.isEmpty()) {
            extra.forEach(tree::putExtra);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compareWeight(Comparable a, Comparable b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        return a.compareTo(b);
    }
}
