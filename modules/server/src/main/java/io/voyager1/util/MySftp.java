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

import io.voyager1.util.FileUtil;
import io.voyager1.util.Sftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpProgressMonitor;

import java.io.File;
import java.nio.charset.Charset;

/**
 * @since 2023/3/16
 */
public class MySftp extends Sftp {

    private final ProgressMonitor progressMonitor;

    public interface ProgressMonitor {

        /**
         * 进度回调
         *
         * @param desc 远程目录
         * @param max  文件总大小
         * @param now  当前进程
         */
        void progress(String desc, long max, long now);

        /**
         * 重置
         */
        void rest();
    }

    public MySftp(Session session, Charset charset, long timeOut, ProgressMonitor monitor) {
        super(session, charset, timeOut);
        this.progressMonitor = monitor;
    }

    @Override
    public boolean upload(String destPath, File file) {
        final String[] desc = new String[1];
        final long[] maxLen = new long[1];
        progressMonitor.rest();
        SftpProgressMonitor sftpProgressMonitor = new SftpProgressMonitor() {
            private long totalCount = 0;

            @Override
            public void init(int op, String src, String dest, long max) {
                desc[0] = dest;
                maxLen[0] = max;
            }

            @Override
            public boolean count(long count) {
                totalCount += count;
                progressMonitor.progress(desc[0], maxLen[0], totalCount);
                return true;
            }

            @Override
            public void end() {

            }
        };
        super.put(FileUtil.getAbsolutePath(file), destPath, sftpProgressMonitor, Mode.OVERWRITE);
        return true;
    }
}
