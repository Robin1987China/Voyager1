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

package io.voyager1.controller.ssh;

import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.Opt;
import io.voyager1.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.func.assets.controller.BaseSshFileController;
import io.voyager1.func.assets.model.MachineSshModel;
import io.voyager1.model.data.SshModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.util.FileUtils;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.BiFunction;

/**
 * ssh 文件管理
 *
 * @since 2019/8/10
 */
@RestController
@RequestMapping("node/ssh")
@Feature(cls = ClassFeature.SSH_FILE)
@Slf4j
public class SshFileController extends BaseSshFileController {


    @Override
    protected <T> T checkConfigPath(String id, BiFunction<MachineSshModel, ItemConfig, T> function) {
        SshModel sshModel = sshService.getByKey(id);
        Assert.notNull(sshModel, "没有对应的ssh");
        MachineSshModel machineSshModel = machineSshServer.getByKey(sshModel.getMachineSshId(), false);
        return function.apply(machineSshModel, sshModel);
    }

    @Override
    protected <T> T checkConfigPathChildren(String id, String path, String children, BiFunction<MachineSshModel, ItemConfig, T> function) {
        FileUtils.checkSlip(path);
        if (children != null && !children.isEmpty()) FileUtils.checkSlip(children);

        SshModel sshModel = sshService.getByKey(id);
        Assert.notNull(sshModel, "没有对应的ssh");
        List<String> fileDirs = sshModel.fileDirs();
        String normalize = FileUtil.normalize("/" + path + "/");
        //
        Assert.state((fileDirs != null && fileDirs.contains(normalize)), "不能操作当前目录");
        MachineSshModel machineSshModel = machineSshServer.getByKey(sshModel.getMachineSshId(), false);
        return function.apply(machineSshModel, sshModel);
    }
}
