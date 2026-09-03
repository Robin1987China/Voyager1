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
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 树节点（属性以 Map 存储），"" {@code io.voyager1.util.Tree}。
 *
 * @param <T> ID 类型
 */
public class Tree<T> extends LinkedHashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    public Tree() {
    }

    @SuppressWarnings("unchecked")
    public T getId() {
        return (T) this.get("id");
    }

    public Tree<T> setId(T id) {
        this.put("id", id);
        return this;
    }

    @SuppressWarnings("unchecked")
    public T getParentId() {
        return (T) this.get("parentId");
    }

    public Tree<T> setParentId(T parentId) {
        this.put("parentId", parentId);
        return this;
    }

    public CharSequence getName() {
        return (CharSequence) this.get("name");
    }

    public Tree<T> setName(CharSequence name) {
        this.put("name", name);
        return this;
    }

    public Comparable<?> getWeight() {
        return (Comparable<?>) this.get("weight");
    }

    public Tree<T> setWeight(Comparable<?> weight) {
        this.put("weight", weight);
        return this;
    }

    /**
     * 获取所有子节点。
     *
     * @return 子节点列表
     */
    @SuppressWarnings("unchecked")
    public List<Tree<T>> getChildren() {
        return (List<Tree<T>>) this.get("children");
    }

    /**
     * 设置子节点，覆盖所有原有子节点。
     *
     * @param children 子节点列表
     * @return this
     */
    public Tree<T> setChildren(List<Tree<T>> children) {
        if (children == null) {
            this.remove("children");
        } else {
            this.put("children", children);
        }
        return this;
    }

    /**
     * 增加子节点。
     *
     * @param children 子节点列表
     * @return this
     */
    @SafeVarargs
    public final Tree<T> addChildren(Tree<T>... children) {
        if (children != null && children.length > 0) {
            List<Tree<T>> list = this.getChildren();
            if (list == null) {
                list = new ArrayList<>();
                this.setChildren(list);
            }
            for (Tree<T> child : children) {
                list.add(child);
            }
        }
        return this;
    }

    /**
     * 扩展属性。
     *
     * @param key   键
     * @param value 值
     */
    public void putExtra(String key, Object value) {
        this.put(key, value);
    }
}
