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

package io.voyager1.controller.build;

import io.voyager1.util.FileUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.ServerConst;
import io.voyager1.common.ServerOpenApi;
import io.voyager1.common.UrlRedirectUtil;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.dblog.BuildInfoService;
import io.voyager1.service.user.TriggerTokenLogServer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * new trigger controller for build
 *
 * @since 2021-08-23
 */
@RestController
@Feature(cls = ClassFeature.BUILD)
public class BuildInfoTriggerController extends BaseServerController {

    private final BuildInfoService buildInfoService;
    private final TriggerTokenLogServer triggerTokenLogServer;

    public BuildInfoTriggerController(BuildInfoService buildInfoService,
                                      TriggerTokenLogServer triggerTokenLogServer) {
        this.buildInfoService = buildInfoService;
        this.triggerTokenLogServer = triggerTokenLogServer;
    }

    /**
     * get a trigger url
     *
     * @param id id
     * @return json
     */
    @RequestMapping(value = "/build/trigger/url", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Map<String, String>> getTriggerUrl(String id, String rest, HttpServletRequest request) {
        BuildInfoModel item = buildInfoService.getByKey(id, request);
        UserModel user = getUser();
        BuildInfoModel updateInfo;
        if ((item.getTriggerToken() == null || item.getTriggerToken().isEmpty()) || (rest != null && !rest.isEmpty())) {
            updateInfo = new BuildInfoModel();
            updateInfo.setId(id);
            updateInfo.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), buildInfoService.typeName(),
                item.getId(), user.getId()));
            buildInfoService.updateById(updateInfo);
        } else {
            updateInfo = item;
        }
        Map<String, String> map = this.getBuildToken(updateInfo, request);
        String string = "重置成功";
        return ApiResult.success((rest == null || rest.isEmpty()) ? "ok" : string, map);
    }

    private Map<String, String> getBuildToken(BuildInfoModel item, HttpServletRequest request) {
        String contextPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        String url = ServerOpenApi.BUILD_TRIGGER_BUILD2.
            replace("{id}", item.getId()).
            replace("{token}", item.getTriggerToken());
        String triggerBuildUrl = String.format("/%s/%s", contextPath, url);
        Map<String, String> map = new HashMap<>(10);
        map.put("triggerBuildUrl", FileUtil.normalize(triggerBuildUrl));
        String batchTriggerBuildUrl = String.format("/%s/%s", contextPath, ServerOpenApi.BUILD_TRIGGER_BUILD_BATCH);
        map.put("batchTriggerBuildUrl", FileUtil.normalize(batchTriggerBuildUrl));
        //
        String batchBuildStatusUrl = String.format("/%s/%s", contextPath, ServerOpenApi.BUILD_TRIGGER_STATUS);
        map.put("batchBuildStatusUrl", FileUtil.normalize(batchBuildStatusUrl));
        String buildLogUrl = String.format("/%s/%s", contextPath, ServerOpenApi.BUILD_TRIGGER_LOG);
        map.put("buildLogUrl", FileUtil.normalize(buildLogUrl));
        map.put("id", item.getId());
        map.put("token", item.getTriggerToken());
        return map;
    }


//    /**
//     * reset new trigger url
//     *
//     * @param id id
//     * @return json
//     */
//    @RequestMapping(value = "/build/trigger/rest", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
//    @Feature(method = MethodFeature.EDIT)
//    public String triggerRest(String id) {
//        BuildInfoModel item = buildInfoService.getByKey(id, getRequest());
//        UserModel user = getUser();
//        BuildInfoModel updateInfo = new BuildInfoModel();
//        updateInfo.setId(id);
//        // new trigger url
//        updateInfo.setTriggerToken(triggerTokenLogServer.restToken(item.getTriggerToken(), buildInfoService.typeName(),
//            item.getId(), user.getId()));
//        buildInfoService.update(updateInfo);
//        Map<String, String> map = this.getBuildToken(updateInfo);
//        return ApiResult.success( "重置成功", map);
//    }
}
