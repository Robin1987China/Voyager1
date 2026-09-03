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


import com.alibaba.fastjson2.JSONObject;
import lombok.Lombok;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

/**
 * 文件工具
 */
public class FileUtils {

    private static JSONObject fileToJson(File file, boolean disableScanDir) {
        JSONObject jsonObject = new JSONObject(6);
        boolean directory = file.isDirectory();
        jsonObject.put("isDirectory", directory);
        if (!directory || !disableScanDir) {
            long sizeFile = FileUtil.size(file);
            jsonObject.put("fileSizeLong", sizeFile);
        }
        jsonObject.put("filename", file.getName());
        long mTime = file.lastModified();
        jsonObject.put("modifyTimeLong", mTime);
        return jsonObject;
    }

    /**
     * 对文件信息解析排序
     *
     * @param files     文件数组
     * @param time      是否安装时间排序
     * @param startPath 开始路径
     * @return 排序后的json
     */
    public static List<JSONObject> parseInfo(File[] files, boolean time, String startPath, boolean disableScanDir) {
        return parseInfo(new java.util.ArrayList<>(java.util.Arrays.asList(files)), time, startPath, disableScanDir);
    }

    /**
     * 对文件信息解析排序
     *
     * @param files     文件数组
     * @param time      是否安装时间排序
     * @param startPath 开始路径
     * @return 排序后的json
     */
    public static List<JSONObject> parseInfo(Collection<File> files, boolean time, String startPath, boolean disableScanDir) {
        if (files == null) {
            return new ArrayList<>();
        }
        return files.stream()
            .map(file -> {
                JSONObject jsonObject = FileUtils.fileToJson(file, disableScanDir);
                //
                if (startPath != null) {
                    String levelName = StringUtil.delStartPath(file, startPath, false);
                    jsonObject.put("levelName", levelName);
                }
                return jsonObject;
            })
            .sorted((jsonObject1, jsonObject2) -> {
                if (time) {
                    return jsonObject2.getLong("modifyTimeLong").compareTo(jsonObject1.getLong("modifyTimeLong"));
                }
                return jsonObject1.getString("filename").compareTo(jsonObject2.getString("filename"));
            }).collect(Collectors.toList());
    }

    /**
     * 读取 日志文件
     *
     * @param logFile 日志文件
     * @param line    开始行数
     * @return data
     */
    public static JSONObject readLogFile(File logFile, int line) {
        JSONObject data = new JSONObject();
        // 读取文件
        //int linesInt = ConvertUtil.toInt(line, 1);
        LimitQueue<String> lines = new LimitQueue<>(1000);
        final int[] readCount = {0};
        FileUtil.readLines(logFile, StandardCharsets.UTF_8, (LineHandler) line1 -> {
            readCount[0]++;
            if (readCount[0] < line) {
                return;
            }
            lines.add(line1);
        });
        // 下次应该获取的行数
        data.put("line", readCount[0] + 1);
        data.put("getLine", line);
        data.put("dataLines", lines);
        return data;
    }

    /**
     * 读取环境变量文件
     *
     * @param baseFile  基础文件夹
     * @param attachEnv 要读取的文件列表
     * @return map
     */
    public static Map<String, String> readEnvFile(File baseFile, String attachEnv) {
        HashMap<String, String> map = new java.util.HashMap<>();
        if ((attachEnv == null || attachEnv.isEmpty())) {
            return map;
        }
        List<String> list2 = io.voyager1.util.ConvertUtil.splitTrim(attachEnv, ",");
        for (String itemEnv : list2) {
            File envFile = FileUtil.file(baseFile, itemEnv);
            if (FileUtil.isFile(envFile)) {
                List<String> list = FileUtil.readLines(envFile, StandardCharsets.UTF_8);
                Map<String, String> envMap = StringUtil.parseEnvStr(list);
                // java.lang.UnsupportedOperationException
                map.putAll(envMap);
            }
        }
        return map;
    }

    /**
     * 判断目录是否有越级问题
     *
     * @param dir      目录
     * @param function 异常
     */
    public static void checkSlip(String dir, Function<Exception, Exception> function) {
        try {
            File tmpDir = FileUtil.getTmpDir();
            FileUtil.checkSlip(tmpDir, FileUtil.file(tmpDir, dir));
        } catch (IllegalArgumentException e) {
            throw Lombok.sneakyThrow(function.apply(e));
        }
    }

    /**
     * 判断目录是否有越级问题
     *
     * @param dir 目录
     */
    public static void checkSlip(String dir) {
        checkSlip(dir, e -> new IllegalArgumentException("目录不能越级：" + e.getMessage()));
    }

    /**
     * 文件追加
     *
     * @param file    被添加的文件
     * @param channel 需要添加的文件通道
     * @throws IOException io
     */
    public static void appendChannel(File file, FileChannel channel) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            try (FileChannel inChannel = fileInputStream.getChannel()) {
                ByteBuffer bb = ByteBuffer.allocate(IoUtil.DEFAULT_MIDDLE_BUFFER_SIZE);
                while (inChannel.read(bb) != NioUtil.EOF) {
                    bb.flip();
                    channel.write(bb);
                    bb.clear();
                }
            }
        }
    }

    /**
     * 使用当前系统的换行符写文件
     *
     * @param context    文件内容
     * @param scriptFile 文件路径
     * @param charset    编码格式
     */
    public static void writeScript(String context, File scriptFile, Charset charset) {
        // 替换换行符
        String replace = context.replace("\n", FileUtil.getLineSeparator());
        FileUtil.writeString(replace, scriptFile, charset);
    }

    /**
     * 安全的方式 move 文件夹内容
     *
     * @param src    源文件夹
     * @param target 目标文件夹
     */
    public static void tempMoveContent(File src, File target) {
        if (FileUtil.isSub(src, target)) {
            // 子目录
            // 将文件内容先复制到临时目录，避免递归出现自己 mv 自己的情况
            File tmpDir = FileUtil.getTmpDir();
            File tempMv = FileUtil.file(tmpDir, "mv", java.util.UUID.randomUUID().toString().replace("-", ""));
            FileUtil.mkdir(tempMv);
            FileUtil.moveContent(src, tempMv, true);
            // 再将临时目录下的文件移动到目标路径
            FileUtil.mkdir(target);
            FileUtil.moveContent(tempMv, target, true);
            //
            FileUtil.del(tempMv);
            // 子目录不需要删除
        } else {
            FileUtil.mkdir(target);
            FileUtil.moveContent(src, target, true);
            // 删除文件夹
            FileUtil.del(src);
        }
    }

    public static String safeFileName(String name1, String extName, String defaultName){
        // 需要考虑文件名中存在非法字符 [\s\\/:\*\?\"<>\|]
        String name = ReUtil.replaceAll(name1, "[\\s\\\\/:\\*\\?\\\"<>\\|]", "");
        if ((name == null || name.isEmpty())) {
            name = defaultName;
        } else if (!(name != null && name.endsWith("." + extName))) {
            name += "." + extName;
        }
        return name;
    }
}
