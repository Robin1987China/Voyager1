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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * 拷贝选项 {@code io.voyager1.util.CopyOptions}。
 */
public class CopyOptions {

    private boolean ignoreNullValue = false;
    private boolean ignoreError = true;
    private Set<String> ignoreProperties = new HashSet<>();
    private UnaryOperator<String> fieldNameEditor = null;

    public static CopyOptions create() {
        return new CopyOptions();
    }

    public boolean isIgnoreNullValue() {
        return ignoreNullValue;
    }

    public CopyOptions setIgnoreNullValue(boolean ignoreNullValue) {
        this.ignoreNullValue = ignoreNullValue;
        return this;
    }

    public CopyOptions ignoreNullValue() {
        return setIgnoreNullValue(true);
    }

    public boolean isIgnoreError() {
        return ignoreError;
    }

    public CopyOptions setIgnoreError(boolean ignoreError) {
        this.ignoreError = ignoreError;
        return this;
    }

    public Set<String> getIgnoreProperties() {
        return ignoreProperties;
    }

    public CopyOptions setIgnoreProperties(Collection<String> ignoreProperties) {
        this.ignoreProperties = new HashSet<>(ignoreProperties);
        return this;
    }

    public CopyOptions setIgnoreProperties(String... ignoreProperties) {
        this.ignoreProperties = new HashSet<>(Arrays.asList(ignoreProperties));
        return this;
    }

    public UnaryOperator<String> getFieldNameEditor() {
        return fieldNameEditor;
    }

    public CopyOptions setFieldNameEditor(UnaryOperator<String> fieldNameEditor) {
        this.fieldNameEditor = fieldNameEditor;
        return this;
    }
}
