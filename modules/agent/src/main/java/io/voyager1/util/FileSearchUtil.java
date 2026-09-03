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

import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.Tuple;
import io.voyager1.util.ReUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.common.i18n.I18nMessageUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 文件搜索工具
 *
 * @since 2022/5/15
 */
public class FileSearchUtil {

    /**
     * @param file             文件
     * @param charset          编码格式
     * @param searchKey        搜索关键词
     * @param cacheBeforeCount 关键词前多少行
     * @param afterCount       关键词后多少行
     * @param head             搜索文件头多少行
     * @param tailCount        文件后多少行
     * @param first            是否从头开始读取
     * @param consumer         回调
     * @return 结果描述
     * @throws IOException io
     */
    public static String searchList(File file, Charset charset,
                                    String searchKey,
                                    int cacheBeforeCount, int afterCount,
                                    int head, int tailCount,
                                    boolean first, Consumer<Tuple> consumer) throws IOException {

        int[] calculate = FileSearchUtil.calculate(head, tailCount, first);
        Collection<Tuple> strings;
        if (calculate.length == 1) {
            strings = FileSearchUtil.readLastLine(file, charset, calculate[0]);
        } else {
            strings = FileSearchUtil.readRangeLine(file, charset, calculate);
        }
        int showLine = searchList(strings, searchKey, cacheBeforeCount, afterCount, consumer);
        return String.format("在 %s 行中搜索到并显示 %s 行", (strings == null ? 0 : strings.size()), showLine);
    }

    private static int searchList(Collection<Tuple> strings, String searchKey, int beforeCount, int afterCount, Consumer<Tuple> consumer) {
        AtomicInteger hitIndex = new AtomicInteger(0);
        LimitQueue<Tuple> beforeQueue = new LimitQueue<>(beforeCount);
        List<Integer> cacheLineNum = new LinkedList<>();
        strings.forEach(tuple -> {
            String s = tuple.get(1);
            Integer index = tuple.get(0);
            // System.out.println(s);
            if ((searchKey == null || searchKey.isEmpty()) || (s != null && s.toLowerCase().contains(searchKey.toLowerCase())) || ReUtil.isMatch(searchKey, s)) {
                // 先输出之前的
                for (Tuple before : beforeQueue) {
                    checkEchoCache(cacheLineNum, before, consumer);
                }
                checkEchoCache(cacheLineNum, tuple, consumer);
                hitIndex.set(index);
            }
            // 是否需要输出后面的内容
            int i = hitIndex.get();
            if (i > 0 && index > i && index <= i + afterCount) {
                checkEchoCache(cacheLineNum, tuple, consumer);
            }
            if (beforeCount > 0) {
                //
                beforeQueue.offerFirst(tuple);
            }
        });
        return (cacheLineNum == null ? 0 : cacheLineNum.size());
    }

    private static void checkEchoCache(List<Integer> cacheLineNum, Tuple tuple, Consumer<Tuple> consumer) {
        int index = tuple.get(0);
        if (cacheLineNum.contains(index)) {
            return;
        }
        consumer.accept(tuple);
        cacheLineNum.add(index);
    }

    public static Collection<Tuple> readLastLine(File file, Charset charset, int line) throws IOException {
        BufferedReader reader = FileUtil.getReader(file, charset);
        LineNumberReader lineNumberReader = new LineNumberReader(reader);
        LimitQueue<Tuple> limitQueue = new LimitQueue<>(line);
        while (true) {
            String readLine = lineNumberReader.readLine();
            if (readLine == null) {
                break;
            }
            limitQueue.add(new Tuple(lineNumberReader.getLineNumber(), readLine));
        }
        return limitQueue;
    }

    public static Collection<Tuple> readRangeLine(File file, Charset charset, int[] range) throws IOException {
        BufferedReader reader = FileUtil.getReader(file, charset);
        LineNumberReader lineNumberReader = new LineNumberReader(reader);
        List<Tuple> list = new LinkedList<>();
        while (true) {
            String readLine = lineNumberReader.readLine();
            if (readLine == null) {
                break;
            }
            int lineNumber = lineNumberReader.getLineNumber();
            if (lineNumber >= range[0] && lineNumber <= range[1]) {
                list.add(new Tuple(lineNumber, readLine));
            }
            if (lineNumber > range[1]) {
                break;
            }
        }
        return list;
    }

    /**
     * 计算读取文件行数相关
     *
     * @param head     从文件头开始读取
     * @param tailLine 读最后几乎
     * @param first    是否从头开始读取
     * @return int
     */
    public static int[] calculate(int head, int tailLine, boolean first) {
        if (head > 0) {
            return first ? new int[]{Math.min(tailLine, head), head} : new int[]{Math.max(head - tailLine, 1), head};
        }
        return first ? new int[]{tailLine, Integer.MAX_VALUE} : new int[]{tailLine};
    }

}
