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

package io.voyager1.common.i18n;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.ComparatorChain;
import io.voyager1.util.FuncComparator;
import io.voyager1.util.Tuple;
import io.voyager1.util.CharsetUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.util.Header;
import io.voyager1.util.SystemUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.nio.charset.StandardCharsets;

/**
 * 国际化转换工具类
 *
 **/
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class I18nMessageUtil {
    /**
     * 线程中的语言
     */
    private static final ThreadLocal<String> LANGUAGE = new ThreadLocal<>();
    /**
     * 语言获取方式
     */
    private static final List<Supplier<String>> LANGUAGE_OBTAIN = new ArrayList<>();

    static {
        // 线程变量获取
        LANGUAGE_OBTAIN.add(LANGUAGE::get);
        // http 请求获取
        LANGUAGE_OBTAIN.add(I18nMessageUtil::getLanguageByRequest);
        // Voyager1 配置获取
        LANGUAGE_OBTAIN.add(() -> SystemUtil.get("VOYAGER1_LANG"));
        // 系统语言
        LANGUAGE_OBTAIN.add(() -> {
            Locale locale = Locale.getDefault();
            String country = locale.getCountry();
            if (java.util.Objects.equals("zh", country)) {
                // 中国
                return "zh-CN";
            }
            TimeZone timeZone = TimeZone.getDefault();
            String id = timeZone.getID();
            if (StrUtil.equalsAny(id, "Asia/Chongqing", "Asia/Shanghai")) {
                return "zh-CN";
            }
            if (StrUtil.equalsAny(id, "Asia/Hong_Kong")) {
                return "zh-HK";
            }
            if (StrUtil.equalsAny(id, "Asia/Taipei")) {
                return "zh-TW";
            }
            return "en-US";
        });
    }

    /**
     * 尝试获取语言(系统语言)
     *
     * @return 语言
     */
    public static String tryGetSystemLanguage() {
        String language = null;
        for (int i = 2; i < LANGUAGE_OBTAIN.size(); i++) {
            language = LANGUAGE_OBTAIN.get(i).get();
            if (language != null) {
                break;
            }
        }
        return language;
    }

    /**
     * 设置语言
     *
     * @param language 语言
     */
    public static void setLanguage(String language) {
        LANGUAGE.set(language);
    }

    /**
     * 清除语言
     */
    public static void clearLanguage() {
        LANGUAGE.remove();
    }

    /**
     * 获取语言 通过 http 请求
     * <p>
     * Accept-Language
     * 在Web应用程序中，HTTP规范规定了浏览器会在请求中携带Accept-Language头，用来指示用户浏览器设定的语言顺序，如：
     * <p>
     * Accept-Language: zh-CN,zh;q=0.8,en;q=0.2
     * <p>
     * 上述HTTP请求头表示优先选择简体中文，其次选择中文，最后选择英文。q表示权重，解析后我们可获得一个根据优先级排序的语言列表，把它转换为Java的Locale，即获得了用户的Locale。大多数框架通常只返回权重最高的Locale。
     *
     * @return 语言
     */
    public static String getLanguageByRequest() {
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes != null) {
            HttpServletRequest request = servletRequestAttributes.getRequest();
            String header = JakartaServletUtil.getHeader(request, Header.ACCEPT_LANGUAGE.getValue(), StandardCharsets.UTF_8);
            return headerAcceptLanguageBest(header);
        }
        return null;
    }

    public static String headerAcceptLanguageBest(String header) {
        List<String> languageTags = io.voyager1.util.ConvertUtil.splitTrim(header, ",");
        return languageTags.stream()
            .map(tag -> {
                String[] parts = tag.trim().split(";");

                float quality = 1.0f; // Default quality is 1.0

                if (parts.length > 0) {
                    // The first part is the language code
                    String locale = parts[0];

                    if (parts.length > 1) {
                        // If there's a second part, it's the quality factor
                        String qPart = parts[1].trim();
                        if (qPart.startsWith("q=")) {
                            try {
                                quality = Float.parseFloat(qPart.substring(2));
                            } catch (NumberFormatException e) {
                                // Ignore if parsing fails
                            }
                        }
                    }
                    return new Tuple(locale, quality);
                }
                return null;
            })
            .filter(Objects::nonNull)
            .max((o1, o2) -> {
                FuncComparator<Tuple> funcComparator = new FuncComparator<>(true, objects -> objects.get(1));
                FuncComparator<Tuple> funcComparator2 = new FuncComparator<>(true, objects -> objects.get(0));
                return ComparatorChain.of(funcComparator, funcComparator2).compare(o1, o2);
            })
            .map((Function<Tuple, String>) objects -> objects.get(0))
            .orElse(null);
    }

    /**
     * 语言格式化
     *
     * @param language 语言
     * @return 语言
     */
    private static String normalLanguage(String language) {
        language = language != null ? language.toLowerCase() : "";
        language = language.replace("_", "-");
        switch (language) {
            case "en-us":
            case "en":
                return "en-US";
            case "zh-tw":
                return "zh-TW";
            case "zh-hk":
                return "zh-HK";
            case "zh-cn":
            case "zh":
            default:
                return "zh-CN";
        }
    }

    /**
     * 尝试获取语言
     *
     * @return 语言
     */
    public static String tryGetNormalLanguage() {
        return normalLanguage(tryGetLanguage());
    }

    /**
     * 尝试获取语言
     *
     * @return 语言
     */
    public static String tryGetLanguage() {
        String language = null;
        for (Supplier<String> supplier : LANGUAGE_OBTAIN) {
            language = supplier.get();
            if (language != null) {
                break;
            }
        }
        return language;
    }

    /**
     * 根据key信息获取对应语言的内容
     *
     * @param key 消息key值
     * @return msg
     */
    public static String get(String key) {
        if ((key == null || key.isEmpty())) {
            return "";
        }
        String language = tryGetLanguage();
        language = normalLanguage(language);
        Locale locale;
        switch (language) {
            case "zh-CN":
                locale = Locale.CHINA;
                break;
            case "zh-TW":
                locale = Locale.TAIWAN;
                break;
            case "zh-HK":
                locale = getZhHkInstance();
                break;
            case "en-US":
                locale = Locale.US;
                break;
            default:
                locale = Locale.CHINA;
                log.warn("Unknown language:{}", language);
                break;
        }
        return get(key, locale);
    }

    private static String get(String key, Locale language) {
        return get(key, new String[0], language);
    }

    private static String get(String key, Object[] params, Locale language) {
        return getInstance().getMessage(key, params, language);
    }

    private static MessageSource getInstance() {
        return Lazy.MESSAGE_SOURCE;
    }

    private static Locale getZhHkInstance() {
        return LazyZhHk.LOCALE;
    }

    /**
     * 使用懒加载方式实例化messageSource国际化工具
     */
    private static class Lazy {
        private static final MessageSource MESSAGE_SOURCE = SpringContextHolder.getBean(MessageSource.class);
    }

    private static class LazyZhHk {
        private static final Locale LOCALE = new Locale("zh", "HK");
    }

}
