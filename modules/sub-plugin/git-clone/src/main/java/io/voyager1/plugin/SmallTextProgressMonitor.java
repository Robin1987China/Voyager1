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

package io.voyager1.plugin;

import org.eclipse.jgit.lib.TextProgressMonitor;

import java.io.Writer;
import java.util.HashSet;
import java.util.Set;

/**
 * 少量的输出，只在进度 x%5=0 的时候输出
 *
 * @see 2022/11/28
 */
public class SmallTextProgressMonitor extends TextProgressMonitor {

    private final Set<Integer> progressRangeList;
    private final int reduceProgressRatio;

    /**
     * @param out                 输出流
     * @param reduceProgressRatio 压缩折叠显示进度比例 范围 1-100
     */
    public SmallTextProgressMonitor(Writer out, int reduceProgressRatio) {
        super(out);
        this.reduceProgressRatio = reduceProgressRatio;
        this.progressRangeList = new HashSet<>((int) Math.floor((float) 100 / reduceProgressRatio));
    }

    @Override
    protected void onUpdate(String taskName, int cmp, int totalWork, int pcnt) {
        int progressRange = (int) Math.floor((float) pcnt / this.reduceProgressRatio);
        if (progressRangeList.add(progressRange)) {
            super.onUpdate(taskName, cmp, totalWork, pcnt);
        }
    }
}
