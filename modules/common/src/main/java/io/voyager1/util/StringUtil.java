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

import io.voyager1.util.CollStreamUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.BetweenFormatter;
import io.voyager1.util.FileUtil;
import io.voyager1.util.Tuple;
import io.voyager1.util.MapUtil;
import io.voyager1.util.ClassUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.URLUtil;
import io.voyager1.util.CronPattern;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONValidator;
import io.voyager1.common.i18n.I18nMessageUtil;
import org.springframework.core.env.SimpleCommandLinePropertySource;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * main 方法运行参数工具
 *
 * @see SimpleCommandLinePropertySource
 * @since 2019/4/7
 */
public class StringUtil {

    public static final String GENERAL_STR = "^[a-zA-Z0-9_\\-]+$";

    /**
     * 支持的压缩包格式
     */
    public static final String[] PACKAGE_EXT = new String[]{"tar.bz2", "tar.gz", "tar", "bz2", "zip", "gz"};

    /**
     * 转换 文件内容
     *
     * @param text 字符串，可能为文件协议地址
     * @param def  默认值
     * @return 如果存在文件 则读取文件内容
     */
    public static String convertFileStr(String text, String def) {
        if ((text != null && text.startsWith(URLUtil.FILE_URL_PREFIX))) {
            String path = (text != null && text.startsWith(URLUtil.FILE_URL_PREFIX) ? text.substring(URLUtil.FILE_URL_PREFIX.length()) : text);
            if (FileUtil.isFile(path)) {
                String fileText = FileUtil.readUtf8String(path);
                return (fileText == null || fileText.isEmpty() ? def : fileText);
            }
        }
        return (text == null || text.isEmpty() ? def : text);
    }

    /**
     * 删除文件开始的路径
     *
     * @param file     要删除的文件
     * @param baseFile 开始的路径
     * @param inName   是否返回文件名
     * @return /test/a.txt /test/  a.txt
     */
    public static String delStartPath(File file, File baseFile, boolean inName) {
        FileUtil.checkSlip(baseFile, file);
        //
        String newWhitePath;
        if (inName) {
            newWhitePath = FileUtil.getAbsolutePath(file.getAbsolutePath());
        } else {
            newWhitePath = FileUtil.getAbsolutePath(file.getParentFile());
        }
        String itemAbsPath = FileUtil.getAbsolutePath(baseFile);
        itemAbsPath = FileUtil.normalize(itemAbsPath);
        newWhitePath = FileUtil.normalize(newWhitePath);
        String path = (newWhitePath != null && newWhitePath.startsWith(itemAbsPath) ? newWhitePath.substring(itemAbsPath.length()) : newWhitePath);
        //newWhitePath.substring(newWhitePath.indexOf(itemAbsPath) + itemAbsPath.length());
        path = FileUtil.normalize(path);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    /**
     * 删除文件开始的路径
     *
     * @param file      要删除的文件
     * @param startPath 开始的路径
     * @param inName    是否返回文件名
     * @return /test/a.txt /test/  a.txt
     */
    public static String delStartPath(File file, String startPath, boolean inName) {
        return delStartPath(file, FileUtil.file(startPath), inName);
    }

//    /**
//     * 指定时间的下一个刻度
//     *
//     * @return String
//     */
//    public static String getNextScaleTime(String time, Long millis) {
//        DateTime dateTime = DateUtil.parse(time);
//        if (millis == null) {
//            millis = 30 * 1000L;
//        }
//        DateTime newTime = dateTime.offsetNew(DateField.SECOND, (int) (millis / 1000));
//        return DateUtil.formatTime(newTime);
//    }

    /**
     * json 字符串转 bean，兼容普通json和字符串包裹情况
     *
     * @param jsonStr json 字符串
     * @param cls     要转为bean的类
     * @param <T>     泛型
     * @return data
     */
    public static <T> T jsonConvert(String jsonStr, Class<T> cls) {
        if ((jsonStr == null || jsonStr.isEmpty())) {
            return null;
        }
        if (ClassUtil.isPrimitiveWrapper(cls)) {
            return ConvertUtil.convert(cls, jsonStr);
        }
        try {
            return JSON.parseObject(jsonStr, cls);
        } catch (Exception e) {
            return JSON.parseObject(JSON.parse(jsonStr).toString(), cls);
        }
    }

    /**
     * json 字符串转 bean，兼容普通json和字符串包裹情况
     *
     * @param jsonStr json 字符串
     * @param cls     要转为bean的类
     * @param <T>     泛型
     * @return data
     */
    public static <T> List<T> jsonConvertArray(String jsonStr, Class<T> cls) {
        try {
            if ((jsonStr == null || jsonStr.isEmpty())) {
                return null;
            }
            return JSON.parseArray(jsonStr, cls);
        } catch (Exception e) {
            Object parse = JSON.parse(jsonStr);
            return JSON.parseArray(parse.toString(), cls);
        }
    }

    /**
     * 根据 map 替换 字符串变量
     *
     * @param str 字符串
     * @param evn map
     * @return 替换后
     */
    public static String formatStrByMap(String str, Map<String, String> evn) {
        String replace = str;
        Set<Map.Entry<String, String>> entries = evn.entrySet();
        for (Map.Entry<String, String> entry : entries) {
            replace = replace.replace(String.format("${%s}", entry.getKey()), entry.getValue());
            replace = replace.replace(String.format("$%s", entry.getKey()), entry.getValue());
        }
        return replace;
    }

    /**
     * 验证 json 类型
     *
     * @param json json 字符串
     * @return type
     */
    public static JSONValidator.Type validatorJson(String json) {
        try {
            JSONValidator from = JSONValidator.from(json);
            return Optional.of(from).map(JSONValidator::getType).orElse(null);
        } catch (JSONException jsonException) {
            return null;
        } catch (Exception e) {
            // ArrayIndexOutOfBoundsException
            return null;
        }
    }

    /**
     * 验证 cron 表达式, demo 账号不能开启 cron
     *
     * @param cron cron
     * @return 原样返回
     */
    public static String checkCron(String cron, Function<String, String> function) {
        String newCron = cron;
        if ((newCron != null && !newCron.isEmpty())) {
            if ((newCron != null && newCron.startsWith("!"))) {
                // 不用验证
                return newCron;
            }
            newCron = function.apply(newCron);
            try {
                new CronPattern(newCron);
            } catch (Exception e) {
                throw new IllegalArgumentException("cron 表达式格式不正确");
            }
        }
        return (newCron != null ? newCron : "");
    }

    /**
     * 转换 cron 表达式,格式化
     *
     * @param cron cron
     * @return 转化后的
     */
    public static String parseCron(String cron) {
        if ((cron != null && cron.startsWith("!"))) {
            // 存在前缀，直接返回空串
            return null;
        }
        return cron;
    }

    public static Map<String, String> parseEnvStr(String envStr) {
        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(envStr, "\n");
        return parseEnvStr(list);
    }

    public static Map<String, String> parseEnvStr(List<String> envStrList) {
        if (envStrList == null) {
            return new java.util.HashMap<>();
        }
        List<Tuple> collect = envStrList.stream()
            .map(StrUtil::trim)
            .filter(s -> !(s == null || s.isEmpty()) && !(s != null && s.startsWith("#")))
            .map(s -> {
                List<String> list1 = io.voyager1.util.ConvertUtil.splitTrim(s, "=");
                if ((list1 == null ? 0 : list1.size()) != 2) {
                    return null;
                }
                return new Tuple(list1.get(0), list1.get(1));
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        return CollStreamUtil.toMap(collect, objects -> objects.get(0), objects -> objects.get(1));
    }

    /**
     * 时长格式化（支持国际化）
     *
     * @param betweenMs 间隔时长
     * @param level     单位
     * @return 格式化后的字符串
     */
    public static String formatBetween(long betweenMs, BetweenFormatter.Level level) {
        return formatBetween(betweenMs, level, 0);
    }

    /**
     * 时长格式化（支持国际化）
     *
     * @param betweenMs     间隔时长
     * @param level         单位
     * @param levelMaxCount 最大单位数量
     * @return 格式化后的字符串
     */
    public static String formatBetween(long betweenMs, BetweenFormatter.Level level, int levelMaxCount) {
        BetweenFormatter betweenFormatter = new BetweenFormatter(betweenMs, level, levelMaxCount);
        betweenFormatter.setSeparator(",");
        betweenFormatter.setLevelFormatter(level1 -> {
            switch (level1) {
                case MILLISECOND:
                    return "毫秒";
                case SECOND:
                    return "秒";
                case MINUTE:
                    return "分钟";
                case DAY:
                    return "天";
                case HOUR:
                    return "小时";
                default:
                    return level1.name();
            }
        });
        return betweenFormatter.format();
    }

}
