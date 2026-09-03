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

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * 校验器 {@code io.voyager1.util.Validator}。
 */
public class Validator {

    public enum Type {
        GENERAL, EMAIL, URL, IPV4, MOBILE, CHINESE, NUMBER, WORD
    }

    private final Predicate<Object> predicate;
    private final String errorMsg;

    private Validator(Predicate<Object> predicate, String errorMsg) {
        this.predicate = predicate;
        this.errorMsg = errorMsg;
    }

    public static Validator of(Predicate<Object> predicate) {
        return new Validator(predicate, null);
    }

    public static Validator from(Type type, String errorMsg) {
        switch (type) {
            case EMAIL:
                return new Validator(v -> v != null && isEmail(v.toString()), errorMsg);
            case URL:
                return new Validator(v -> v != null && isUrl(v.toString()), errorMsg);
            case IPV4:
                return new Validator(v -> v != null && isIpv(v.toString()), errorMsg);
            case MOBILE:
                return new Validator(v -> v != null && isMobile(v.toString()), errorMsg);
            case CHINESE:
                return new Validator(v -> v != null && isChinese(v.toString()), errorMsg);
            case NUMBER:
                return new Validator(v -> v != null && isNumber(v.toString()), errorMsg);
            case GENERAL:
            default:
                return new Validator(v -> v != null && isGeneral(v.toString()), errorMsg);
        }
    }

    public boolean test(Object value) {
        return predicate.test(value);
    }

    public void validate(Object value) throws IllegalArgumentException {
        if (!predicate.test(value)) {
            throw new IllegalArgumentException(errorMsg == null ? "校验失败" : errorMsg);
        }
    }

    public boolean validateMatchRegex(CharSequence pattern, CharSequence content) {
        return isMatchRegex(pattern, content);
    }

    public static void validateMatchRegex(CharSequence pattern, CharSequence content, String errorMsg) {
        if (!isMatchRegex(pattern, content)) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    public static void validateGeneral(CharSequence value, String errorMsg) {
        if (!isGeneral(value)) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    public static void validateGeneral(CharSequence value, int min, int max, String errorMsg) {
        if (!validateGeneral(value, min, max)) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    public static boolean validateGeneral(CharSequence value, int min, int max) {
        if (value == null) {
            return false;
        }
        return isGeneral(value.toString()) && value.length() >= min && value.length() <= max;
    }

    public static boolean isGeneral(CharSequence value) {
        return value != null && Pattern.compile("^\\w+$").matcher(value).matches();
    }

    public static boolean isGeneral(CharSequence value, int min) {
        return isGeneral(value) && value.length() >= min;
    }

    public static boolean isGeneral(CharSequence value, int min, int max) {
        return isGeneral(value) && value.length() >= min && value.length() <= max;
    }

    public static boolean isMatchRegex(CharSequence pattern, CharSequence content) {
        return content != null && Pattern.compile(pattern.toString()).matcher(content).matches();
    }

    public static boolean isIpv(CharSequence value) {
        return value != null && Pattern.compile(RegexPool.IPV4).matcher(value).matches();
    }

    public static boolean isIpv4(CharSequence value) {
        return value != null && Pattern.compile(RegexPool.IPV4).matcher(value).matches();
    }

    public static boolean isIpv6(CharSequence value) {
        return value != null && Pattern.compile(RegexPool.IPV6).matcher(value).matches();
    }

    public static boolean isUrl(CharSequence value) {
        return value != null && Pattern.compile(RegexPool.URL).matcher(value).matches();
    }

    public static boolean isEmail(CharSequence value) {
        return value != null && Pattern.compile(RegexPool.EMAIL).matcher(value).matches();
    }

    public static boolean isChinese(CharSequence value) {
        return value != null && Pattern.compile(RegexPool.CHINESE).matcher(value).matches();
    }

    public static boolean isWord(CharSequence value) {
        return value != null && Pattern.compile(RegexPool.WORD).matcher(value).matches();
    }

    public static boolean isNumber(CharSequence value) {
        return value != null && Pattern.compile(RegexPool.NUMBERS).matcher(value).matches();
    }

    public static boolean isMobile(CharSequence value) {
        return value != null && Pattern.compile(RegexPool.MOBILE).matcher(value).matches();
    }

    public static boolean isEmpty(Object value) {
        return value == null || value.toString().isEmpty();
    }

    public static boolean isNotEmpty(Object value) {
        return !isEmpty(value);
    }

    public static void validateUrl(CharSequence value) throws IllegalArgumentException {
        if (!isUrl(value)) {
            throw new IllegalArgumentException("URL 格式不正确");
        }
    }

    public static void validateUrl(CharSequence value, String errorMsg) throws IllegalArgumentException {
        if (!isUrl(value)) {
            throw new IllegalArgumentException(errorMsg);
        }
    }

    public static void validateEmail(CharSequence value) throws IllegalArgumentException {
        if (!isEmail(value)) {
            throw new IllegalArgumentException("邮箱格式不正确");
        }
    }

    public static void validateEmail(CharSequence value, String errorMsg) throws IllegalArgumentException {
        if (!isEmail(value)) {
            throw new IllegalArgumentException(errorMsg);
        }
    }
}
