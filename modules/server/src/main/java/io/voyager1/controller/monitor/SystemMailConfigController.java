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

package io.voyager1.controller.monitor;

import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSON;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.data.MailAccountModel;
import io.voyager1.monitor.EmailUtil;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.system.SystemParametersServer;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 监控邮箱配置
 *
 * @since 2019/7/16
 */
@RestController
@RequestMapping(value = "system")
@Feature(cls = ClassFeature.SYSTEM_EMAIL)
@SystemPermission
public class SystemMailConfigController extends BaseServerController {

    private final SystemParametersServer systemParametersServer;

    public SystemMailConfigController(SystemParametersServer systemParametersServer) {
        this.systemParametersServer = systemParametersServer;
    }

    /**
     * load mail config data
     * 加载邮件配置
     *
     * @return json
     */
    @PostMapping(value = "mail-config-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<MailAccountModel> mailConfigData() {
        MailAccountModel item = systemParametersServer.getConfig(MailAccountModel.ID, MailAccountModel.class);
        if (item != null) {
            item.setPass(null);
        }
        return ApiResult.success("", item);
    }

    @PostMapping(value = "mailConfig_save.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Object> listData(MailAccountModel mailAccountModel) throws Exception {
        Assert.notNull(mailAccountModel, "请填写信息,并检查是否填写合法");
        Assert.hasText(mailAccountModel.getHost(), "请填写host");
        Assert.hasText(mailAccountModel.getUser(), "请填写user");
        Assert.hasText(mailAccountModel.getFrom(), "请填写from");
        // 验证是否正确
        MailAccountModel item = systemParametersServer.getConfig(MailAccountModel.ID, MailAccountModel.class);
        if (item != null) {
            mailAccountModel.setPass((mailAccountModel.getPass() == null || mailAccountModel.getPass().isEmpty() ? item.getPass() : mailAccountModel.getPass()));
        } else {
            Assert.hasText(mailAccountModel.getPass(), "请填写pass");
        }
        IPlugin plugin = PluginFactory.getPlugin("email");
        Object json = JSON.toJSON(mailAccountModel);
        Map<String, Object> map = new HashMap<>(1);
        map.put("data", json);
        boolean checkInfo = plugin.execute("checkInfo", map, Boolean.class);
        Assert.state(checkInfo, "验证邮箱信息失败，请检查配置的邮箱信息。端口号、授权码等。");
        systemParametersServer.upsert(MailAccountModel.ID, mailAccountModel, MailAccountModel.ID);
        //
        EmailUtil.refreshConfig();
        return ApiResult.success("保存成功");
    }
}
