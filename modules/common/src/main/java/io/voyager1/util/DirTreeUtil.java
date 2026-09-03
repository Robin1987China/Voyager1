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

import io.voyager1.util.CompareUtil;
import io.voyager1.util.FileUtil;
import com.alibaba.fastjson2.JSONObject;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 目录树
 *
 * @since 2019/7/21
 */
public class DirTreeUtil {

    /**
     * 获取树的json
     *
     * @param path 文件名
     * @return jsonArray
     */
    public static List<JSONObject> getTreeData(String path) {
        File file = FileUtil.file(path);
        return readTree(file, path);
    }

    private static List<JSONObject> readTree(File file, String logFile) {
        File[] files = file.listFiles();
        if (files == null) {
            return null;
        }
        return Arrays.stream(files)
            .sorted((o1, o2) -> CompareUtil.compare(o2.lastModified(), o1.lastModified()))
            .map(file1 -> {
                JSONObject jsonObject = new JSONObject();
                String path = StringUtil.delStartPath(file1, logFile, true);
                jsonObject.put("title", file1.getName());
                jsonObject.put("path", path);
                if (file1.isDirectory()) {
                    List<JSONObject> children = readTree(file1, logFile);
                    jsonObject.put("children", children);
                }
                return jsonObject;
            })
            .collect(Collectors.toList());
    }
}
