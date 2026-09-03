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

import java.util.Map;
import java.util.Objects;

/**
 * 树节点，"" {@code io.voyager1.util.TreeNode}。
 *
 * @param <T> ID 类型
 */
public class TreeNode<T> {

    private T id;
    private T parentId;
    private CharSequence name;
    private Comparable<?> weight = 0;
    private Map<String, Object> extra;

    public TreeNode() {
    }

    /**
     * 构造。
     *
     * @param id       ID
     * @param parentId 父节点 ID
     * @param name     名称
     * @param weight   权重
     */
    public TreeNode(T id, T parentId, String name, Comparable<?> weight) {
        this.id = id;
        this.parentId = parentId;
        this.name = name;
        if (weight != null) {
            this.weight = weight;
        }
    }

    public T getId() {
        return id;
    }

    public TreeNode<T> setId(T id) {
        this.id = id;
        return this;
    }

    public T getParentId() {
        return this.parentId;
    }

    public TreeNode<T> setParentId(T parentId) {
        this.parentId = parentId;
        return this;
    }

    public CharSequence getName() {
        return name;
    }

    public TreeNode<T> setName(CharSequence name) {
        this.name = name;
        return this;
    }

    public Comparable<?> getWeight() {
        return weight;
    }

    public TreeNode<T> setWeight(Comparable<?> weight) {
        this.weight = weight;
        return this;
    }

    /**
     * 获取扩展字段。
     *
     * @return 扩展字段 Map
     */
    public Map<String, Object> getExtra() {
        return extra;
    }

    /**
     * 设置扩展字段。
     *
     * @param extra 扩展字段
     * @return this
     */
    public TreeNode<T> setExtra(Map<String, Object> extra) {
        this.extra = extra;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TreeNode<?> that = (TreeNode<?>) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
