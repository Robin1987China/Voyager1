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

package io.voyager1.func.assets.controller;

import io.voyager1.util.CollUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.func.assets.model.MachineSshModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.util.FileUtils;
import io.voyager1.util.StringUtil;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.BiFunction;

/**
 * @since 2023/2/27
 */
@RestController
@RequestMapping(value = "/system/assets/ssh-file")
@Feature(cls = ClassFeature.SSH_FILE)
@Slf4j
@SystemPermission
public class MachineSshFileController extends BaseSshFileController {
    @Override
    protected <T> T checkConfigPath(String id, BiFunction<MachineSshModel, ItemConfig, T> function) {
        MachineSshModel machineSshModel = machineSshServer.getByKey(id, false);
        Assert.notNull(machineSshModel, "没有对应的ssh");
        return function.apply(machineSshModel, new ItemConfig() {
            @Override
            public List<String> allowEditSuffix() {
                return StringUtil.jsonConvertArray(machineSshModel.getAllowEditSuffix(), String.class);
            }

            @Override
            public List<String> fileDirs() {
                return new java.util.ArrayList<>(java.util.Arrays.asList("/"));
            }
        });
    }

    @Override
    protected <T> T checkConfigPathChildren(String id, String path, String children, BiFunction<MachineSshModel, ItemConfig, T> function) {
        FileUtils.checkSlip(path);
        if (children != null && !children.isEmpty()) FileUtils.checkSlip(children);
        //
        MachineSshModel machineSshModel = machineSshServer.getByKey(id, false);
        return function.apply(machineSshModel, new ItemConfig() {
            @Override
            public List<String> allowEditSuffix() {
                return StringUtil.jsonConvertArray(machineSshModel.getAllowEditSuffix(), String.class);
            }

            @Override
            public List<String> fileDirs() {
                return new java.util.ArrayList<>(java.util.Arrays.asList("/"));
            }
        });
    }
}
