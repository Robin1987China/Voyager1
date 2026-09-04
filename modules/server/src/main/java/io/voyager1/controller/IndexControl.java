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

package io.voyager1.controller;
import io.voyager1.util.URLUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.ReUtil;

import io.voyager1.util.Cache;
import io.voyager1.util.Base64;
import io.voyager1.util.CollUtil;
import io.voyager1.util.FileTypeUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.ResourceUtil;
import io.voyager1.util.RegexPool;
import io.voyager1.util.Validator;
import io.voyager1.util.Cache;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.util.ContentType;
import io.voyager1.util.SystemUtil;
import io.voyager1.core.api.ApiResult;
import io.voyager1.plugin.IPlugin;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.common.ServerConst;
import io.voyager1.common.UrlRedirectUtil;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.interceptor.NotLogin;
import io.voyager1.configuration.NodeConfig;
import io.voyager1.configuration.WebConfig;
import io.voyager1.db.DbExtConfig;
import io.voyager1.func.user.controller.UserNotificationController;
import io.voyager1.func.user.dto.UserNotificationDto;
import io.voyager1.model.user.UserModel;
import io.voyager1.permission.SystemPermission;
import io.voyager1.plugin.PluginFactory;
import io.voyager1.service.system.SystemParametersServer;
import io.voyager1.service.user.UserService;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.system.ServerConfig;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;

/**
 * 首页
 *
 */
@RestController
@RequestMapping(value = "/")
@Slf4j
public class IndexControl extends BaseServerController {

    private final UserService userService;
    private final SystemParametersServer systemParametersServer;
    private final WebConfig webConfig;
    private final NodeConfig nodeConfig;
    private final DbExtConfig dbExtConfig;

    public IndexControl(UserService userService,
                        SystemParametersServer systemParametersServer,
                        ServerConfig serverConfig,
                        DbExtConfig dbExtConfig) {
        this.userService = userService;
        this.systemParametersServer = systemParametersServer;
        this.webConfig = serverConfig.getWeb();
        this.nodeConfig = serverConfig.getNode();
        this.dbExtConfig = dbExtConfig;
    }


    /**
     * 加载首页
     *
     * @api {get} / 加载首页 服务端前端页面
     * @apiGroup index
     * @apiSuccess {String} BODY HTML
     */
    @GetMapping(value = {"index", "", "/", "index.html"}, produces = MediaType.TEXT_HTML_VALUE)
    @NotLogin
    public void index(HttpServletResponse response, HttpServletRequest request) {
        this.toIndex(response, request, "");
    }

    @GetMapping(value = "oauth2-{provide}", produces = MediaType.TEXT_HTML_VALUE)
    @NotLogin
    public void oauth2(HttpServletResponse response, HttpServletRequest request, @PathVariable String provide) {
        this.toIndex(response, request, provide);
    }

    private void toIndex(HttpServletResponse response, HttpServletRequest request, String oauth2Provide) {
        InputStream inputStream = ResourceUtil.getStream("classpath:/dist/index.html");
        String html = IoUtil.read(inputStream, StandardCharsets.UTF_8);
        //<div id="voyager1CommonJs"></div>
        String path = ExtConfigBean.getPath();
        File file = FileUtil.file(String.format("%s/script/common.js", path));
        if (file.exists()) {
            String jsCommonContext = FileUtil.readString(file, StandardCharsets.UTF_8);
            // <div id="voyager1CommonJs"><!--Don't delete this line, place for public JS --></div>
            String[] commonJsTemps = new String[]{"<div id=\"voyager1CommonJs\"><!--Don't delete this line, place for public JS --></div>", "<div id=\"voyager1CommonJs\"></div>"};
            for (String item : commonJsTemps) {
                html = html.replace(item, jsCommonContext);
            }
        }
        String language = I18nMessageUtil.tryGetSystemLanguage();
        // <routerBase>
        String proxyPath = UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH);
        html = html.replace("<routerBase>", proxyPath);
        //
        html = html.replace("<link rel=\"icon\" href=\"favicon.ico\">", "<link rel=\"icon\" href=\"" + proxyPath + "favicon.ico\">");
        // <apiTimeOut>
        int webApiTimeout = webConfig.getApiTimeout();
        html = html.replace("<apiTimeout>", String.valueOf(TimeUnit.SECONDS.toMillis(webApiTimeout)));
        html = html.replace("<uploadFileSliceSize>", String.valueOf(nodeConfig.getUploadFileSliceSize()));
        html = html.replace("<uploadFileConcurrent>", String.valueOf(nodeConfig.getUploadFileConcurrent()));
        html = html.replace("<oauth2Provide>", oauth2Provide);
        html = html.replace("<transportEncryption>", webConfig.getTransportEncryption());
        html = html.replace("<voyager1DefaultLocale>", language);
        // 修改网页标题
        String title = ReUtil.get("<title>.*?</title>", html, 0);
        if ((title != null && !title.isEmpty())) {
            html = html.replace(title, "<title>" + webConfig.getName() + "</title>");
        }
        JakartaServletUtil.write(response, html, ContentType.TEXT_HTML.getValue());
    }

    /**
     * logo 图片
     *
     * @api {get} logo_image logo 图片
     * @apiGroup index
     * @apiSuccess {Object} BODY image
     */
    @RequestMapping(value = "logo-image", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @NotLogin
    public ApiResult<String> logoImage(HttpServletResponse response) {
        String logoFile = webConfig.getLogoFile();
        String imageSrc = this.loadImageSrc(response, logoFile, "classpath:/logo/voyager1.png", "jpg", "png", "gif");
        return ApiResult.success("", imageSrc);
    }

    /**
     * logo 图片
     *
     * @api {get} logo_image logo 图片
     * @apiGroup index
     * @apiSuccess {Object} BODY image
     */
    @RequestMapping(value = "favicon.ico", method = RequestMethod.GET, produces = MediaType.IMAGE_PNG_VALUE)
    @NotLogin
    public void favicon(HttpServletResponse response) throws IOException {
        String iconFile = webConfig.getIconFile();
        this.loadImage(response, iconFile, "classpath:/logo/favicon.ico", "ico", "png");
    }

    private void loadImage(HttpServletResponse response, String imgFile, String defaultResource, String... suffix) throws IOException {
        if ((imgFile != null && !imgFile.isEmpty())) {
            if (Validator.isMatchRegex(RegexPool.URL_HTTP, imgFile)) {
                // 重定向
                response.sendRedirect(imgFile);
                return;
            }
            File file = FileUtil.file(imgFile);
            if (FileUtil.isFile(file)) {
                String type = FileTypeUtil.getType(file);
                String extName = FileUtil.extName(file);
                if (StrUtil.equalsAnyIgnoreCase(type, suffix) || StrUtil.equalsAnyIgnoreCase(extName, suffix)) {
                    JakartaServletUtil.write(response, file);
                    return;
                }
            }
        }
        // favicon ico
        InputStream inputStream = ResourceUtil.getStream(defaultResource);
        JakartaServletUtil.write(response, inputStream, MediaType.IMAGE_PNG_VALUE);
    }

    private String loadImageSrc(HttpServletResponse response, String imgFile, String defaultResource, String... suffix) {
        if ((imgFile != null && !imgFile.isEmpty())) {
            if (Validator.isMatchRegex(RegexPool.URL_HTTP, imgFile)) {
                // 重定向
                return imgFile;
            }
            File file = FileUtil.file(imgFile);
            if (FileUtil.isFile(file)) {
                String type = FileTypeUtil.getType(file);
                String extName = FileUtil.extName(file);
                if (StrUtil.equalsAnyIgnoreCase(type, suffix) || StrUtil.equalsAnyIgnoreCase(extName, suffix)) {
                    JakartaServletUtil.write(response, file);
                    String encode = Base64.encode(file);
                    String mimeType = FileUtil.getMimeType(file.toPath());
                    return URLUtil.getDataUriBase64(mimeType, encode);
                }
            }
        }
        // favicon ico
        InputStream inputStream = ResourceUtil.getStream(defaultResource);
        String encode = Base64.encode(inputStream);
        return URLUtil.getDataUriBase64(MediaType.IMAGE_PNG_VALUE, encode);
    }


    /**
     * @return json
     * <p>
     * check if need to init system
     * @api {get} check-system 检查是否需要初始化系统
     * @apiGroup index
     * @apiUse defResultJson
     * @apiSuccess {String} routerBase 二级地址
     * @apiSuccess {String} name 系统名称
     * @apiSuccess {String} subTitle 主页面副标题
     * @apiSuccess {String} loginTitle 登录也标题
     * @apiSuccess {String} disabledGuide 是否禁用引导
     * @apiSuccess (222) {Object}  data 系统还没有超级管理员需要初始化
     */
    @NotLogin
    @RequestMapping(value = ServerConst.CHECK_SYSTEM, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<JSONObject> checkSystem(HttpServletRequest request) {
        JSONObject data = new JSONObject();
        data.put("routerBase", UrlRedirectUtil.getHeaderProxyPath(request, ServerConst.PROXY_PATH));
        //
        data.put("name", webConfig.getName());
        data.put("subTitle", webConfig.getSubTitle());
        data.put("loginTitle", webConfig.getLoginTitle());
        data.put("disabledGuide", webConfig.isDisabledGuide());
        //data.put("disabledCaptcha", webConfig.isDisabledCaptcha());
        data.put("notificationPlacement", webConfig.getNotificationPlacement());
        data.put("installId", Voyager1Manifest.getInstance().getInstallId());
        data.put("version", Voyager1Manifest.getInstance().getVersion());
        // 用于判断是否属于容器部署
        boolean inDocker = (SystemUtil.get("VOYAGER1_PKG") != null && !SystemUtil.get("VOYAGER1_PKG").isEmpty());
        List<String> extendPlugins = new ArrayList<>();
        if (inDocker) {
            extendPlugins.add("inDocker");
        }
        //extendPlugins.add("db-" + dbExtConfig.getMode().name().toLowerCase());
        // 验证 git 仓库信息
        try {
            IPlugin plugin = PluginFactory.getPlugin("git-clone");
            Map<String, Object> map = new HashMap<>(0);
            boolean systemGit = (boolean) plugin.execute("systemGit", map);
            if (systemGit) {
                extendPlugins.add("system-git");
            }
        } catch (Exception e) {
            log.warn("检查 git 客户端异常", e);
        }
        data.put("extendPlugins", extendPlugins);
        if (userService.canUse()) {
            return new ApiResult<>(200, "", data);
        }
        return new ApiResult<>(222, "需要初始化系统", data);
    }


    /**
     * @return json
     * @api {post} menus_data.json 获取系统菜单相关数据
     * @apiGroup index
     * @apiUse loginUser
     * @apiParam {String} nodeId 节点ID
     * @apiSuccess {JSON}  data 菜单相关字段
     */
    @RequestMapping(value = "menus_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<List<Object>> menusData(HttpServletRequest request) {
        UserModel userModel = getUserModel();
        String workspaceId = nodeService.getCheckUserWorkspace(request);
        JSONObject config = systemParametersServer.getConfigDefNewInstance(String.format("menus_config_%s", workspaceId), JSONObject.class);
        String language = I18nMessageUtil.tryGetNormalLanguage();
        // 菜单
        InputStream inputStream = ResourceUtil.getStream("classpath:/menus/" + language + "/index.json");
        JSONArray showArray = config.getJSONArray("serverMenuKeys");


        String json = IoUtil.read(inputStream, StandardCharsets.UTF_8);
        JSONArray jsonArray = JSONArray.parseArray(json);
        List<Object> collect1 = jsonArray.stream().filter(o -> {
            JSONObject jsonObject = (JSONObject) o;
            if (!testMenus(jsonObject, userModel, showArray, request)) {
                return false;
            }
            JSONArray childs = jsonObject.getJSONArray("childs");
            if (childs != null) {
                List<Object> collect = childs.stream().filter(o1 -> {
                    JSONObject jsonObject1 = (JSONObject) o1;
                    return testMenus(jsonObject1, userModel, showArray, request);
                }).collect(Collectors.toList());
                if (collect.isEmpty()) {
                    return false;
                }
                jsonObject.put("childs", collect);
            }
            return true;
        }).collect(Collectors.toList());
        Assert.notEmpty(jsonArray, "没有任何菜单,请联系管理员");
        return ApiResult.success("", collect1);
    }

    /**
     * @return json
     * @api {post} menus_data.json 获取系统菜单相关数据
     * @apiGroup index
     * @apiUse loginUser
     * @apiParam {String} nodeId 节点ID
     * @apiSuccess {JSON}  data 菜单相关字段
     */
    @RequestMapping(value = "system_menus_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @SystemPermission
    public ApiResult<List<Object>> systemMenusData(HttpServletRequest request) {
        UserModel userModel = getUserModel();
        String language = I18nMessageUtil.tryGetNormalLanguage();
        // 菜单
        InputStream inputStream = ResourceUtil.getStream("classpath:/menus/" + language + "/system.json");
        String json = IoUtil.read(inputStream, StandardCharsets.UTF_8);
        JSONArray jsonArray = JSONArray.parseArray(json);
        List<Object> collect1 = jsonArray.stream().filter(o -> {
            JSONObject jsonObject = (JSONObject) o;
            if (!testMenus(jsonObject, userModel, null, request)) {
                return false;
            }
            JSONArray childs = jsonObject.getJSONArray("childs");
            if (childs != null) {
                List<Object> collect = childs.stream().filter(o1 -> {
                    JSONObject jsonObject1 = (JSONObject) o1;
                    return testMenus(jsonObject1, userModel, null, request);
                }).collect(Collectors.toList());
                if (collect.isEmpty()) {
                    return false;
                }
                jsonObject.put("childs", collect);
            }
            return true;
        }).collect(Collectors.toList());
        Assert.notEmpty(jsonArray, "没有任何菜单,请联系管理员");
        return ApiResult.success("", collect1);
    }

    private boolean testMenus(JSONObject jsonObject, UserModel userModel, JSONArray showArray, HttpServletRequest request) {
        String storageMode = jsonObject.getString("storageMode");
        if ((storageMode != null && !storageMode.isEmpty())) {
            if (!java.util.Objects.equals(dbExtConfig.getMode().name(), storageMode)) {
                return false;
            }
        }
        String role = jsonObject.getString("role");
        if (java.util.Objects.equals(role, UserModel.SYSTEM_ADMIN) && !userModel.isSuperSystemUser()) {
            // 超级理员权限
            return false;
        }
        // 判断菜单显示
        if ((showArray != null && !showArray.isEmpty()) && !userModel.isSuperSystemUser()) {
            String id = jsonObject.getString("id");
            if (!(showArray != null && showArray.contains(id))) {
                boolean present = showArray.stream().anyMatch(o -> {
                    String str = String.valueOf(o);
                    return (str != null && str.startsWith(id + ":")) || (str != null && str.endsWith(":" + id));
                });
                if (!present) {
                    return false;
                }
            }
        }
        // 系统管理员权限
        boolean system = java.util.Objects.equals(role, "system");
        if (system) {
            return userModel.checkSystemUser();
        }
        return true;
    }

    /**
     * 生成分片id
     *
     * @return json
     */
    @GetMapping(value = "generate-sharding-id", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<String> generateShardingId() {
        Cache<String, String> shardingIds = BaseServerController.SHARDING_IDS;
        int size = shardingIds.size();
        Assert.state(size <= 100, "分片id最大同时使用 100 个");
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        shardingIds.put(uuid, uuid);
        return ApiResult.success(uuid, uuid);
    }

    /**
     * 获取通知
     *
     * @return json
     */
    @GetMapping(value = "system-notification", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResult<UserNotificationDto> getNotification() {
        UserNotificationDto notificationDto = systemParametersServer.getConfigDefNewInstance(UserNotificationController.KEY, UserNotificationDto.class);
        return ApiResult.success("", notificationDto);
    }
}
