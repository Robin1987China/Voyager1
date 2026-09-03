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

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * 元组，用于承载多个不同类型的值，"" 的 Tuple。
 */
public class Tuple implements Iterable<Object>, Serializable {

    private static final long serialVersionUID = 1L;

    private final Object[] members;

    public Tuple(Object... members) {
        this.members = members;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(int index) {
        return (T) members[index];
    }

    public Object[] getMembers() {
        return this.members;
    }

    public int size() {
        return this.members.length;
    }

    public List<Object> toList() {
        return Arrays.asList(members);
    }

    @Override
    public Iterator<Object> iterator() {
        return Arrays.asList(members).iterator();
    }

    @Override
    public String toString() {
        return Arrays.toString(members);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.deepHashCode(members);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Tuple other = (Tuple) obj;
        return Arrays.deepEquals(members, other.members);
    }
}
