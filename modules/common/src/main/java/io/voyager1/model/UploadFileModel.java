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

package io.voyager1.model;

import io.voyager1.util.FileUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class UploadFileModel extends BaseModel {
    private long size = 0;
    private long completeSize = 0;
    private String savePath;
    private String version;

    public void save(byte[] data) {
        this.completeSize += data.length;
        File file = new File(this.getFilePath());
        FileUtil.mkParentDirs(file);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file, true)) {
            fileOutputStream.write(data);
            fileOutputStream.flush();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public String getFilePath() {
        return savePath + "/" + getName();
    }

    public void remove() {
        FileUtil.del(this.getFilePath());
    }
}
