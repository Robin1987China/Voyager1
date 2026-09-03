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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 可选值包装，"" {@code io.voyager1.util.Opt} 的常用方法。
 */
public class Opt<T> {

    private final T value;

    private Opt(T value) {
        this.value = value;
    }

    public static <T> Opt<T> ofNullable(T value) {
        return new Opt<>(value);
    }

    public static <T> Opt<T> ofBlankAble(T value) {
        if (value == null) {
            return new Opt<>(null);
        }
        if (value instanceof CharSequence && ((CharSequence) value).length() == 0) {
            return new Opt<>(null);
        }
        return new Opt<>(value);
    }

    public static <T> Opt<T> of(T value) {
        return new Opt<>(value);
    }

    public static <T> Opt<T> ofEmptyAble(T value) {
        if (value == null) {
            return new Opt<>(null);
        }
        if (value instanceof CharSequence && ((CharSequence) value).length() == 0) {
            return new Opt<>(null);
        }
        if (value instanceof java.util.Collection && ((java.util.Collection<?>) value).isEmpty()) {
            return new Opt<>(null);
        }
        if (value instanceof java.util.Map && ((java.util.Map<?, ?>) value).isEmpty()) {
            return new Opt<>(null);
        }
        return new Opt<>(value);
    }

    public static <T> Opt<T> empty() {
        return new Opt<>(null);
    }

    public void ifPresent(Consumer<? super T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }

    public T orElse(T other) {
        return value != null ? value : other;
    }

    public T orElseGet(Supplier<? extends T> supplier) {
        return value != null ? value : supplier.get();
    }

    public <U> Opt<U> map(Function<? super T, ? extends U> mapper) {
        return value == null ? new Opt<>(null) : new Opt<>(mapper.apply(value));
    }

    public <U> Opt<U> flatMap(Function<? super T, ? extends Opt<? extends U>> mapper) {
        if (value == null) {
            return new Opt<>(null);
        }
        Opt<? extends U> result = mapper.apply(value);
        return result == null ? new Opt<>(null) : new Opt<>(result.value);
    }

    public T get() {
        return value;
    }

    public boolean isEmpty() {
        return value == null;
    }

    public boolean isPresent() {
        return value != null;
    }
}
