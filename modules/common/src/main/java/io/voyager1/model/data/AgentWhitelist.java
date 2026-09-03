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

package io.voyager1.model.data;

import io.voyager1.model.BaseModel;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.util.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 授权
 *
 * @since 2019/4/16
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentWhitelist extends BaseModel {
    /**
     * 项目目录授权、日志文件授权
     */
    private List<String> project;
    /**
     * 运行编辑的后缀文件
     */
    private List<String> allowEditSuffix;

    /**
     * 格式化，判断是否与voyager1 数据路径冲突
     *
     * @param list list
     * @return null 是有冲突的
     */
    public static List<String> covertToArray(List<String> list, String errorMsg) {
        return covertToArray(list, -1, errorMsg);
    }

    /**
     * 格式化，判断是否与voyager1 数据路径冲突
     *
     * @param list list
     * @return null 是有冲突的
     */
    public static List<String> covertToArray(List<String> list, int maxLen, String errorMsg) {
        if (list == null) {
            return null;
        }
        return list.stream()
            .map(s -> {
                String val = FileUtil.normalize(s);
                Assert.state(FileUtil.isAbsolutePath(val), "需要配置绝对路径：" + val);
                File file = FileUtil.file(val);
                File parentFile = file.getParentFile();
                Assert.notNull(parentFile, "不能配置根路径：" + val);
                // 判断是否保护voyager1 路径
                Assert.state(!StrUtil.startWith(ExtConfigBean.getPath(), val), errorMsg);
                //
                if (maxLen > 0) {
                    Assert.state(StrUtil.length(val) <= maxLen, String.format("配置路径超过%s长度限制:%s", maxLen, val));
                }
                return val;
            })
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * 转换为字符串
     *
     * @param jsonArray jsonArray
     * @return str
     */
    public static String convertToLine(Collection<String> jsonArray) {
        return String.join("\n", jsonArray);
    }

    /**
     * 判断是否在授权列表中
     *
     * @param list list
     * @param path 对应项
     * @return false 不在列表中
     */
    public static boolean checkPath(List<String> list, String path) {
        if (list == null) {
            return false;
        }
        if ((path == null || path.isEmpty())) {
            return false;
        }
        File file1;
        File file2 = FileUtil.file(path);
        for (String item : list) {
            file1 = FileUtil.file(item);
            if (FileUtil.pathEquals(file1, file2)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将字符串转为 list
     *
     * @param value    字符串
     * @param errorMsg 错误消息
     * @return list
     */
    public static List<String> parseToList(String value, String errorMsg) {
        return parseToList(value, false, errorMsg);
    }

    /**
     * 将字符串转为 list
     *
     * @param value    字符串
     * @param required 是否为必填
     * @param errorMsg 错误消息
     * @return list
     */
    public static List<String> parseToList(String value, boolean required, String errorMsg) {
        if (required) {
            Assert.hasLength(value, errorMsg);
        } else {
            if ((value == null || value.isEmpty())) {
                return null;
            }
        }
        List<String> list = StrSplitter.splitTrim(value, "\n", true);
        Assert.notEmpty(list, errorMsg);
        return list;
    }

    /**
     * 获取文件可以编辑的 文件编码格式
     *
     * @param filename 文件名
     * @return charset 不能编辑情况会抛出异常
     */
    public static Charset checkFileSuffix(List<String> allowEditSuffix, String filename) {
        Assert.notEmpty(allowEditSuffix, "没有配置可允许编辑的后缀");
        Charset charset = AgentWhitelist.parserFileSuffixMap(allowEditSuffix, filename);
        Assert.notNull(charset, "不允许编辑的文件后缀");
        return charset;
    }

    /**
     * 静默判断是否可以编辑对应的文件
     *
     * @param filename 文件名
     * @return true 可以编辑
     */
    public static boolean checkSilentFileSuffix(List<String> allowEditSuffix, String filename) {
        if ((allowEditSuffix == null || allowEditSuffix.isEmpty())) {
            return false;
        }
        Charset charset = AgentWhitelist.parserFileSuffixMap(allowEditSuffix, filename);
        return charset != null;
    }

    /**
     * 根据文件名 和 可以配置列表 获取编码格式
     *
     * @param allowEditSuffix 允许编辑的配置
     * @param filename        文件名
     * @return 没有匹配到 返回 null，没有配置编码格式即使用系统默认编码格式
     */
    private static Charset parserFileSuffixMap(List<String> allowEditSuffix, String filename) {
        Map<String, Charset> map = CollStreamUtil.toMap(allowEditSuffix, s -> {
            List<String> split = java.util.Arrays.asList(s.split(java.util.regex.Pattern.quote("@")));
            return (split == null || split.isEmpty() ? null : split.get(0));
        }, s -> {
            List<String> split = java.util.Arrays.asList(s.split(java.util.regex.Pattern.quote("@")));
            if (split.size() > 1) {
                String last = (split == null || split.isEmpty() ? null : split.get(split.size() - 1));
                return CharsetUtil.charset(last);
            } else {
                return java.nio.charset.Charset.defaultCharset();
            }
        });
        // 可能配置 所有
        Charset charset = map.get("*");
        if (charset != null) {
            return charset;
        }
        Set<Map.Entry<String, Charset>> entries = map.entrySet();
        for (Map.Entry<String, Charset> entry : entries) {
            if (StrUtil.endWithAnyIgnoreCase(filename, entry.getKey(), "." + entry.getKey())) {
                return entry.getValue();
            }
            if (ReUtil.isMatch(entry.getKey(), filename)) {
                // 满足正则条件
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * 检查授权包含关系
     *
     * @param jsonArray 要检查的对象
     * @return null 正常
     */
    public static String findStartsWith(List<String> jsonArray) {
        return findStartsWith(jsonArray, 0);
    }

    /**
     * 检查授权包含关系
     *
     * @param jsonArray 要检查的对象
     * @param start     检查的坐标
     * @return null 正常
     */
    private static String findStartsWith(List<String> jsonArray, int start) {
        if (jsonArray == null) {
            return null;
        }
        String str = jsonArray.get(start);
        int len = jsonArray.size();
        for (int i = 0; i < len; i++) {
            if (i == start) {
                continue;
            }
            String findStr = jsonArray.get(i);
            if (FileUtil.isSub(FileUtil.file(findStr), FileUtil.file(str))) {
                return str;
            }
        }
        if (start < len - 1) {
            return findStartsWith(jsonArray, start + 1);
        }
        return null;
    }
}
