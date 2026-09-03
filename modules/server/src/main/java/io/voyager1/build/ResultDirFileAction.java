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

package io.voyager1.build;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.StrUtil;
import lombok.Data;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.util.AntPathUtil;
import io.voyager1.util.FileUtils;
import org.springframework.util.Assert;

import java.io.File;
import java.util.List;

/**
 * @since 2023/2/8
 */
@Data
public class ResultDirFileAction {
    /**
     * 配置的产物路径（或者 ant 表达式）
     */
    private String path;

    /**
     * 产物匹配模式
     */
    private Type type;

    /**
     * ant 文件上传模式
     */
    private AntFileUploadMode antFileUploadMode;

    /**
     * ant 使用指定路径下的文件
     */
    private String antSubMatch;

    public String antSubMatch() {
        if ((this.antSubMatch == null || this.antSubMatch.isEmpty())) {
            // 兼容默认数据，未配置
            return "";
        }
        String normalize = FileUtil.normalize(this.antSubMatch);
        //需要包裹成目录结构
        return StrUtil.wrapIfMissing(normalize, "/", "/");
    }

    public ResultDirFileAction(String resultDirFile) {
        // 存在路径 表达式
        if ((resultDirFile != null && resultDirFile.contains(":"))) {
            List<String> resultDirFiles = io.voyager1.util.ConvertUtil.splitTrim(resultDirFile, ":");
            String first = (resultDirFiles == null || resultDirFiles.isEmpty() ? null : resultDirFiles.get(0));
            this.setPath(first);
            this.setType(AntPathUtil.ANT_PATH_MATCHER.isPattern(first) ? Type.ANT_PATH : Type.ORIGINAL);
            if (this.getType() == Type.ANT_PATH) {
                // 文件保留方式
                String antFileUploadModeStr = (1 < resultDirFiles.size() ? resultDirFiles.get(1) : null);
                antFileUploadModeStr = StrUtil.nullToDefault(antFileUploadModeStr, "").toUpperCase();
                AntFileUploadMode fileUploadMode = EnumUtil.fromString(AntFileUploadMode.class, antFileUploadModeStr, AntFileUploadMode.KEEP_DIR);
                this.setAntFileUploadMode(fileUploadMode);
                // ant 使用二级路径
                String antFileUploadPath = (2 < resultDirFiles.size() ? resultDirFiles.get(2) : null);
                this.setAntSubMatch(StrUtil.nullToDefault(antFileUploadPath, ""));
            }
        } else {
            this.setPath(resultDirFile);
            this.setType(AntPathUtil.ANT_PATH_MATCHER.isPattern(resultDirFile) ? Type.ANT_PATH : Type.ORIGINAL);
            if (this.getType() == Type.ANT_PATH) {
                this.setAntFileUploadMode(AntFileUploadMode.KEEP_DIR);
                this.setAntSubMatch("");
            }
        }
    }

    /**
     * ant 模式使用 normalize 方法格式化不规范的路径
     *
     * @see AntPathUtil#antPathMatcher(File, String)
     */
    public void check() {
        if (this.getType() == Type.ORIGINAL) {
            FileUtils.checkSlip(getPath(), e -> new IllegalArgumentException("产物目录不能越级：" + e.getMessage()));
        } else if (this.getType() == Type.ANT_PATH) {
            // ant 模式存在特殊字符，直接判断会发生异常并且判断不到
        }
    }

    /**
     * 解析产物路径
     *
     * @param resultDirFile 产物配置
     * @return ResultDirFileAction
     */
    public static ResultDirFileAction parse(String resultDirFile) {
        Assert.notNull(resultDirFile, "resultDirFile 不能为空");
        return new ResultDirFileAction(resultDirFile);
    }

    public enum AntFileUploadMode {
        /**
         * 保留文件夹层级
         */
        KEEP_DIR,
        /**
         * 将所有文件合并到同一个文件夹
         */
        SAME_DIR,
    }

    public enum Type {
        /**
         * 模糊匹配模式
         */
        ANT_PATH,
        /**
         * 原始目录
         */
        ORIGINAL
    }
}
