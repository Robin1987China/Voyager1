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

package io.voyager1.controller.system;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.CharsetUtil;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.Voyager1Application;
import io.voyager1.common.BaseAgentController;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.system.ExtConfigBean;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 系统配置
 *
 * @since 2019/08/08
 */
@RestController
@RequestMapping(value = "system")
@Slf4j
public class SystemConfigController extends BaseAgentController {

 @RequestMapping(value = "getConfig.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
 public ApiResult<JSONObject> config() throws IOException {
 Resource resource = ExtConfigBean.getResource();
 String content = IoUtil.read(resource.getInputStream(), StandardCharsets.UTF_8);
 JSONObject json = new JSONObject();
 json.put("content", content);
 json.put("file", FileUtil.getAbsolutePath(resource.getFile()));
 return ApiResult.success("", json);
 }

 @RequestMapping(value = "save_config.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
 public ApiResult<String> saveConfig(@ValidatorItem(msg = "内容不能为空") String content, String restart) throws IOException {
 try {
 YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();
 // 
 ByteArrayResource resource = new ByteArrayResource(content.replace("\t", " ").getBytes(StandardCharsets.UTF_8));
 yamlPropertySourceLoader.load("test", resource);
 } catch (Exception e) {
 log.warn("内容格式错误，请检查修正", e);
 return new ApiResult<>(500, "内容格式错误，请检查修正:" + e.getMessage());
 }
 Resource resource = ExtConfigBean.getResource();
 Assert.state(resource.isFile(), "当前环境下不支持在线修改配置文件");
 FileUtil.writeString(content, resource.getFile(), StandardCharsets.UTF_8);

 if (ConvertUtil.toBool(restart, false)) {
 // 重启
 Voyager1Application.restart();
 return ApiResult.success(Const.UPGRADE_MSG.get());
 }
 return ApiResult.success("修改成功");
 }
}
