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

package io.voyager1.func.system.controller;

import io.voyager1.util.BeanUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.model.BaseIdModel;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.user.TriggerTokenLogBean;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.permission.SystemPermission;
import io.voyager1.service.ITriggerToken;
import io.voyager1.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @since 24/1/17 017
 */
@RestController
@RequestMapping(value = "system/trigger")
@Feature(cls = ClassFeature.SYSTEM_CACHE)
@SystemPermission
public class TriggerTokenController {

    private final TriggerTokenLogServer triggerTokenLogServer;

    public TriggerTokenController(TriggerTokenLogServer triggerTokenLogServer) {
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    @GetMapping(value = "all-type", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<JSONObject>> allType() {
        List<JSONObject> jsonObjects = triggerTokenLogServer.allType();
        return ApiResult.success("", jsonObjects);
    }

    @GetMapping(value = "delete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<String> delete(String id) {
        triggerTokenLogServer.delete(id);
        return ApiResult.success("删除成功");
    }

    /**
     * 分页列表
     *
     * @return json
     */
    @PostMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<PageResultDto<TriggerTokenLogBean>> list(HttpServletRequest request) {
        PageResultDto<TriggerTokenLogBean> listPage = triggerTokenLogServer.listPage(request);
        listPage.each(triggerTokenLogBean -> {
            String type = triggerTokenLogBean.getType();
            ITriggerToken byType = triggerTokenLogServer.getByType(type);
            if (byType == null) {
                triggerTokenLogBean.setDataName("ERROR:类型不存在" + type);
            } else {
                BaseIdModel byKey = byType.getByKey(triggerTokenLogBean.getDataId());
                if (byKey == null) {
                    triggerTokenLogBean.setDataName("ERROR:关联数据丢失");
                } else {
                    Object name = BeanUtil.getProperty(byKey, "name");
                    if (name == null) {
                        triggerTokenLogBean.setDataName("ERROR:关联数据名称不存在");
                    } else {
                        triggerTokenLogBean.setDataName(name.toString());
                    }
                }
            }
        });
        return ApiResult.success("", listPage);
    }
}
