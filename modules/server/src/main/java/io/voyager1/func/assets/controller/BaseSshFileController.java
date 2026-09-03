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
import io.voyager1.util.StrUtil;
import io.voyager1.util.URLUtil;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.ExceptionUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.Sftp;
import io.voyager1.util.DigestUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.util.ChannelType;
import io.voyager1.util.JschUtil;
import io.voyager1.util.Sftp;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.validator.ValidatorItem;
import io.voyager1.func.assets.model.MachineSshModel;
import io.voyager1.func.assets.server.MachineSshServer;
import io.voyager1.model.data.AgentWhitelist;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.plugins.JschUtils;
import io.voyager1.service.node.ssh.SshService;
import io.voyager1.system.ServerConfig;
import io.voyager1.util.CommandUtil;
import io.voyager1.util.CompressionFileUtil;
import io.voyager1.util.StringUtil;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.function.BiFunction;
import java.nio.charset.StandardCharsets;

/**
 * @since 2023/2/25
 */
@Slf4j
public abstract class BaseSshFileController extends BaseServerController {

    @Resource
    protected SshService sshService;
    @Resource
    protected MachineSshServer machineSshServer;
    @Resource
    private ServerConfig serverConfig;

    public interface ItemConfig {
        /**
         * 允许编辑的文件后缀
         *
         * @return 文件后缀
         */
        List<String> allowEditSuffix();

        /**
         * 允许管理的文件目录
         *
         * @return 文件目录
         */
        List<String> fileDirs();
    }

    /**
     * 验证数据id 和目录合法性
     *
     * @param id       数据id
     * @param function 回调
     * @param <T>      泛型
     * @return 处理后的数据
     */
    protected abstract <T> T checkConfigPath(String id, BiFunction<MachineSshModel, ItemConfig, T> function);

    /**
     * 验证数据id 和目录合法性
     *
     * @param id              数据id
     * @param allowPathParent 想要验证的目录 （授权）
     * @param nextPath        授权后的二级路径
     * @param function        回调
     * @param <T>             泛型
     * @return 处理后的数据
     */
    protected abstract <T> T checkConfigPathChildren(String id, String allowPathParent, String nextPath, BiFunction<MachineSshModel, ItemConfig, T> function);

    @RequestMapping(value = "download", method = RequestMethod.GET)
    @Feature(method = MethodFeature.DOWNLOAD)
    public void download(@ValidatorItem String id,
                         @ValidatorItem String allowPathParent,
                         @ValidatorItem String nextPath,
                         @ValidatorItem String name,
                         HttpServletResponse response) throws IOException {
        MachineSshModel machineSshModel = this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel1, itemConfig) -> machineSshModel1);
        if (machineSshModel == null) {
            JakartaServletUtil.write(response, "ssh error 或者 没有配置此文件夹", MediaType.TEXT_HTML_VALUE);
            return;
        }
        try {
            this.downloadFile(machineSshModel, allowPathParent, nextPath, name, response);
        } catch (SftpException e) {
            log.error("下载失败", e);
            JakartaServletUtil.write(response, "download error", MediaType.TEXT_HTML_VALUE);
        }
    }

    /**
     * 根据 id 获取 fileDirs 目录集合
     *
     * @param id ssh id
     * @return json
     * @since for dev 3.x
     */
    @RequestMapping(value = "root_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONArray> rootFileList(@ValidatorItem String id) {
        //
        return this.checkConfigPath(id, (machineSshModel, itemConfig) -> {
            JSONArray listDir = listRootDir(machineSshModel, itemConfig.fileDirs());
            return ApiResult.success("", listDir);
        });
    }


    @RequestMapping(value = "list_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<JSONArray> listData(@ValidatorItem String id,
                                            @ValidatorItem String allowPathParent,
                                            @ValidatorItem String nextPath) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            try {
                JSONArray listDir = listDir(machineSshModel, allowPathParent, nextPath, itemConfig);
                return ApiResult.success("", listDir);
            } catch (SftpException e) {
                throw Lombok.sneakyThrow(e);
            }
        });
    }

    @RequestMapping(value = "read_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<String> readFileData(@ValidatorItem String id,
                                             @ValidatorItem String allowPathParent,
                                             @ValidatorItem String nextPath,
                                             @ValidatorItem String name) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            //
            List<String> allowEditSuffix = itemConfig.allowEditSuffix();
            Charset charset = AgentWhitelist.checkFileSuffix(allowEditSuffix, name);
            //
            String content = this.readFile(machineSshModel, allowPathParent, nextPath, name, charset);
            return ApiResult.success("", content);
        });
    }

    @RequestMapping(value = "update_file_data.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> updateFileData(@ValidatorItem String id,
                                               @ValidatorItem String allowPathParent,
                                               @ValidatorItem String nextPath,
                                               @ValidatorItem String name,
                                               @ValidatorItem String content) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            List<String> allowEditSuffix = itemConfig.allowEditSuffix();
            Charset charset = AgentWhitelist.checkFileSuffix(allowEditSuffix, name);
            // 缓存到本地
            File file = FileUtil.file(serverConfig.getUserTempPath(), machineSshModel.getId(), allowPathParent, nextPath, name);
            try {
                FileUtil.writeString(content, file, charset);
                // 上传
                this.syncFile(machineSshModel, allowPathParent, nextPath, name, file);
            } finally {
                //
                FileUtil.del(file);
            }
            //
            return ApiResult.success("修改成功");
        });
    }

    /**
     * 读取文件
     *
     * @param machineSshModel ssh
     * @param allowPathParent 路径
     * @param nextPath        二级路径
     * @param name            文件
     * @param charset         编码格式
     */
    private String readFile(MachineSshModel machineSshModel, String allowPathParent, String nextPath, String name, Charset charset) {
        Sftp sftp = null;
        try {
            Session session = sshService.getSessionByModel(machineSshModel);
            sftp = new Sftp(session, machineSshModel.charset(), machineSshModel.timeout());
            String normalize = FileUtil.normalize(allowPathParent + "/" + nextPath + "/" + name);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            sftp.download(normalize, byteArrayOutputStream);
            byte[] bytes = byteArrayOutputStream.toByteArray();
            return new String(bytes, charset);
        } finally {
            IoUtil.close(sftp);
        }
    }

    /**
     * 上传文件
     *
     * @param machineSshModel ssh
     * @param allowPathParent 路径
     * @param nextPath        二级路径
     * @param name            文件
     * @param file            同步上传文件
     */
    private void syncFile(MachineSshModel machineSshModel,
                          String allowPathParent,
                          String nextPath,
                          String name,
                          File file) {
        Sftp sftp = null;
        try {
            Session session = sshService.getSessionByModel(machineSshModel);
            sftp = new Sftp(session, machineSshModel.charset(), machineSshModel.timeout());
            String normalize = FileUtil.normalize(allowPathParent + "/" + nextPath + "/" + name);
            sftp.upload(normalize, file);
        } finally {
            IoUtil.close(sftp);
        }
    }

    /**
     * 下载文件
     *
     * @param machineSshModel ssh
     * @param allowPathParent 路径
     * @param name            文件
     * @param response        响应
     * @throws IOException   io
     * @throws SftpException sftp
     */
    private void downloadFile(MachineSshModel machineSshModel, String allowPathParent, String nextPath, String name, HttpServletResponse response) throws IOException, SftpException {
        final String charset = response.getCharacterEncoding() != null ? response.getCharacterEncoding() : StandardCharsets.UTF_8.name();
        String fileName = FileUtil.getName(name);
        response.setHeader("Content-Disposition", String.format("attachment;filename=%s", URLUtil.encode(fileName, Charset.forName(charset))));
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = sshService.getSessionByModel(machineSshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            String normalize = FileUtil.normalize(allowPathParent + "/" + nextPath + "/" + name);
            channel.get(normalize, response.getOutputStream());
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
        }
    }

    /**
     * 查询文件夹下所有文件
     *
     * @param sshModel        ssh
     * @param allowPathParent 允许的路径
     * @param nextPath        下 N 级的文件夹
     * @return array
     * @throws SftpException sftp
     */
    @SuppressWarnings("unchecked")
    private JSONArray listDir(MachineSshModel sshModel, String allowPathParent, String nextPath, ItemConfig itemConfig) throws SftpException {
        Session session = null;
        ChannelSftp channel = null;
        List<String> allowEditSuffix = itemConfig.allowEditSuffix();
        try {
            session = sshService.getSessionByModel(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);

            String children2 = (nextPath == null || nextPath.isEmpty() ? "/" : nextPath);
            String allPath = String.format("%s/%s", allowPathParent, children2);
            allPath = FileUtil.normalize(allPath);
            JSONArray jsonArray = new JSONArray();
            Vector<ChannelSftp.LsEntry> vector;
            try {
                vector = channel.ls(allPath);
            } catch (Exception e) {
                log.warn("获取文件夹失败", e);
                Throwable causedBy = ExceptionUtil.getCausedBy(e, SftpException.class);
                if (causedBy != null) {
                    throw new IllegalStateException("查询文件夹 SFTP 失败," + causedBy.getMessage());
                }
                throw new IllegalStateException("查询文件夹失败," + e.getMessage());
            }
            for (ChannelSftp.LsEntry lsEntry : vector) {
                String filename = lsEntry.getFilename();
                if (".".equals(filename) || StrUtil.DOUBLE_DOT.equals(filename)) {
                    continue;
                }
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", filename);
                jsonObject.put("id", DigestUtil.sha1(allPath + "/" + filename));
                SftpATTRS attrs = lsEntry.getAttrs();
                int mTime = attrs.getMTime();
                //String format = DateUtil.format(DateUtil.date(mTime * 1000L), "yyyy-MM-dd HH:mm");
                jsonObject.put("modifyTime", mTime * 1000L);
                if (attrs.isDir()) {
                    jsonObject.put("dir", true);
                } else {
                    long fileSize = attrs.getSize();
                    jsonObject.put("size", fileSize);
                    // 允许编辑
                    jsonObject.put("textFileEdit", AgentWhitelist.checkSilentFileSuffix(allowEditSuffix, filename));
                }
                String longname = lsEntry.getLongname();
                jsonObject.put("longname", longname);
                jsonObject.put("link", attrs.isLink());
                jsonObject.put("extended", attrs.getExtended());
                jsonObject.put("permissions", attrs.getPermissionsString());
                jsonObject.put("allowPathParent", allowPathParent);
                //
                jsonObject.put("nextPath", FileUtil.normalize(children2));
//                jsonObject.put("absolutePath", FileUtil.normalize(String.format("%s/%s", nextPath, filename)));
                jsonArray.add(jsonObject);
            }
            return jsonArray;
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
        }
    }

    /**
     * 列出目前，判断是否存在
     *
     * @param sshModel 数据信息
     * @param list     目录
     * @return Array
     */
    private JSONArray listRootDir(MachineSshModel sshModel, List<String> list) {
        JSONArray jsonArray = new JSONArray();
        if ((list == null || list.isEmpty())) {
            return jsonArray;
        }
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = sshService.getSessionByModel(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            for (String allowPathParent : list) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", DigestUtil.sha1(allowPathParent));
                jsonObject.put("allowPathParent", allowPathParent);
                try {
                    channel.ls(allowPathParent);
                } catch (SftpException e) {
                    // 标记文件夹不存在
                    jsonObject.put("error", true);
                }
                jsonArray.add(jsonObject);
            }
            return jsonArray;
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
        }
    }


    @RequestMapping(value = "delete.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.DEL)
    public ApiResult<String> delete(@ValidatorItem String id,
                                       @ValidatorItem String allowPathParent,
                                       @ValidatorItem String nextPath,
                                       String name) {
        // name 可能为空，为空情况是删除目录
        String name2 = (name == null || name.isEmpty() ? "" : name);
        Assert.state(!java.util.Objects.equals(name2, "/"), "不能删除根目录");
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            Session session = null;
            Sftp sftp = null;
            try {
                //
                String normalize = FileUtil.normalize(allowPathParent + "/" + nextPath + "/" + name2);
                Assert.state(!java.util.Objects.equals(normalize, "/"), "不能删除根目录");
                session = sshService.getSessionByModel(machineSshModel);
                sftp = new Sftp(session, machineSshModel.charset(), machineSshModel.timeout());
                // 尝试删除
                boolean dirOrFile = this.tryDelDirOrFile(sftp, normalize);
                if (dirOrFile) {
                    String parent = FileUtil.getParent(normalize, 1);
                    return ApiResult.success("删除成功", parent);
                }
                return ApiResult.success("删除成功");
            } catch (Exception e) {
                log.error("ssh删除文件异常", e);
                return new ApiResult<>(400, "删除失败:" + e.getMessage());
            } finally {
                IoUtil.close(sftp);
                JschUtil.close(session);
            }
        });
    }

    @RequestMapping(value = "rename.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> rename(@ValidatorItem String id,
                                       @ValidatorItem String allowPathParent,
                                       @ValidatorItem String nextPath,
                                       @ValidatorItem String name,
                                       @ValidatorItem String newname) {

        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            Session session = null;
            ChannelSftp channel = null;

            try {
                session = sshService.getSessionByModel(machineSshModel);
                channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
                String oldPath = FileUtil.normalize(allowPathParent + "/" + nextPath + "/" + name);
                String newPath = FileUtil.normalize(allowPathParent + "/" + nextPath + "/" + newname);
                channel.rename(oldPath, newPath);
            } catch (Exception e) {
                log.error("ssh重命名失败异常", e);
                return new ApiResult<>(400, "重命名失败:" + e.getMessage());
            } finally {
                JschUtil.close(channel);
                JschUtil.close(session);
            }
            return ApiResult.success("操作成功");
        });
    }

    /**
     * 删除文件 或者 文件夹
     *
     * @param sftp ftp
     * @param path 路径
     * @return true 删除的是 文件夹
     */
    private boolean tryDelDirOrFile(Sftp sftp, String path) {
        try {
            // 先尝试删除文件夹
            sftp.delDir(path);
            return true;
        } catch (Exception e) {
            // 删除文件
            sftp.delFile(path);
        }
        return false;
    }

    /**
     * 上传分片
     *
     * @param file       文件对象
     * @param sliceId    分片id
     * @param totalSlice 总分片
     * @param nowSlice   当前分片
     * @param fileSumMd5 文件 md5
     * @return json
     */
    @PostMapping(value = "upload-sharding", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD, log = false)
    public ApiResult<String> uploadSharding(MultipartFile file,
                                               String sliceId,
                                               Integer totalSlice,
                                               Integer nowSlice,
                                               String fileSumMd5,
                                               @ValidatorItem String id,
                                               @ValidatorItem String allowPathParent,
                                               @ValidatorItem String nextPath) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            String remotePath = FileUtil.normalize(allowPathParent + "/" + nextPath);
            Session session = null;
            ChannelSftp channel = null;
            try {
                session = sshService.getSessionByModel(machineSshModel);
                channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
                channel.cd(remotePath);
                String originalFilename = file.getOriginalFilename();
                // xxxx.txt.1
                originalFilename = StrUtil.subBefore(originalFilename, ".", true);
                if (nowSlice == 0) {
                    channel.put(file.getInputStream(), originalFilename, ChannelSftp.OVERWRITE);
                } else {
                    channel.put(file.getInputStream(), originalFilename, ChannelSftp.APPEND);
                }
            } catch (Exception e) {
                log.error("ssh上传文件异常", e);
                return new ApiResult<>(400, "上传失败:" + e.getMessage());
            } finally {
                JschUtil.close(channel);
                JschUtil.close(session);
            }
            return ApiResult.success("上传成功");
        });
    }


    @RequestMapping(value = "upload", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public ApiResult<String> upload(@ValidatorItem String id,
                                       @ValidatorItem String allowPathParent,
                                       @ValidatorItem String nextPath,
                                       String unzip,
                                       MultipartFile file) {
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            String remotePath = FileUtil.normalize(allowPathParent + "/" + nextPath);
            Session session = null;
            ChannelSftp channel = null;

            try {
                session = sshService.getSessionByModel(machineSshModel);
                channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
                // 保存路径
                File tempPath = serverConfig.getUserTempPath();
                File savePath = FileUtil.file(tempPath, "ssh", machineSshModel.getId());
                FileUtil.mkdir(savePath);
                String originalFilename = file.getOriginalFilename();
                File filePath = FileUtil.file(savePath, originalFilename);
                //
                if (ConvertUtil.toBool(unzip, false)) {
                    String extName = FileUtil.extName(originalFilename);
                    Assert.state(StrUtil.containsAnyIgnoreCase(extName, StringUtil.PACKAGE_EXT), "不支持的文件类型：" + extName);
                    file.transferTo(filePath);
                    // 解压
                    File tempUnzipPath = FileUtil.file(savePath, java.util.UUID.randomUUID().toString().replace("-", ""));
                    try {
                        FileUtil.mkdir(tempUnzipPath);
                        CompressionFileUtil.unCompress(filePath, tempUnzipPath);
                        // 同步上传文件
                        sshService.uploadDir(machineSshModel, remotePath, tempUnzipPath);
                    } finally {
                        // 删除临时文件
                        CommandUtil.systemFastDel(filePath);
                        CommandUtil.systemFastDel(tempUnzipPath);
                    }
                } else {
                    file.transferTo(filePath);
                    channel.cd(remotePath);
                    try (FileInputStream src = new FileInputStream(filePath)) {
                        channel.put(src, filePath.getName());
                    }
                }

            } catch (Exception e) {
                log.error("ssh上传文件异常", e);
                return new ApiResult<>(400, "上传失败:" + e.getMessage());
            } finally {
                JschUtil.close(channel);
                JschUtil.close(session);
            }
            return ApiResult.success("操作成功");
        });
    }

    /**
     * @return json
     * @api {post} new_file_folder.json ssh 中创建文件夹/文件
     * @apiGroup ssh
     * @apiUse defResultJson
     * @apiParam {String} id ssh id
     * @apiParam {String} path ssh 选择到目录
     * @apiParam {String} name 文件名
     * @apiParam {String} unFolder true/1 为文件夹，false/0 为文件
     * @apiSuccess {JSON}  data
     */
    @RequestMapping(value = "new_file_folder.json", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.UPLOAD)
    public ApiResult<String> newFileFolder(String id,
                                              @ValidatorItem String allowPathParent,
                                              @ValidatorItem String nextPath,
                                              @ValidatorItem String name, String unFolder) {
        Assert.state(!(name != null && name.contains("/")), "文件名不能包含/");
        return this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel, itemConfig) -> {
            //
            Session session = null;
            try {
                session = sshService.getSessionByModel(machineSshModel);
                String remotePath = FileUtil.normalize(allowPathParent + "/" + nextPath + "/" + name);
                Charset charset = machineSshModel.charset();
                int timeout = machineSshModel.timeout();
                try (Sftp sftp = new Sftp(session, charset, timeout)) {
                    if (sftp.exist(remotePath)) {
                        return new ApiResult<>(400, "文件夹或者文件已存在");
                    }
                    StringBuilder command = new StringBuilder();
                    if (ConvertUtil.toBool(unFolder, false)) {
                        // 文件
                        command.append("touch ").append(remotePath);
                    } else {
                        // 目录
                        command.append("mkdir ").append(remotePath);
                        try {
                            if (sftp.mkdir(remotePath)) {
                                // 创建成功
                                return ApiResult.success("操作成功");
                            }
                        } catch (Exception e) {
                            log.error("ssh创建文件夹异常", e);
                            return new ApiResult<>(500, "创建文件夹失败（文件夹名可能已经存在啦）:" + e.getMessage());
                        }
                    }
                    List<String> result = new ArrayList<>();
                    JschUtils.execCallbackLine(session, charset, timeout, command.toString(), "", result::add);

                    return ApiResult.success("操作成功:" + String.join("\n", result));
                } catch (IOException e) {
                    throw Lombok.sneakyThrow(e);
                }
            } finally {
                JschUtil.close(session);
            }
        });
    }

    /**
     * 修改文件权限
     *
     * @param id
     * @param allowPathParent
     * @param nextPath
     * @param fileName
     * @param permissionValue
     * @return
     */
    @RequestMapping(value = "change_file_permission.json", produces = MediaType.APPLICATION_JSON_VALUE, method = RequestMethod.POST)
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<JSONArray> changeFilePermissions(@ValidatorItem String id,
                                                         @ValidatorItem String allowPathParent,
                                                         @ValidatorItem String nextPath,
                                                         @ValidatorItem String fileName,
                                                         @ValidatorItem String permissionValue) {
        MachineSshModel machineSshModel = this.checkConfigPathChildren(id, allowPathParent, nextPath, (machineSshModel1, itemConfig) -> machineSshModel1);
        if (machineSshModel == null) {
            return new ApiResult<>(400, "ssh error 或者 没有配置此文件夹");
        }
        Session session = sshService.getSessionByModel(machineSshModel);
        Charset charset = machineSshModel.charset();
        int timeout = machineSshModel.timeout();
        String remotePath = FileUtil.normalize(allowPathParent + "/" + nextPath + "/" + fileName);
        try (Sftp sftp = new Sftp(session, charset, timeout)) {
            ChannelSftp client = sftp.getClient();
            //
            int permissions = Integer.parseInt(permissionValue, 8);
            client.chmod(permissions, remotePath);
        } catch (SftpException e) {
            log.error("ssh修改文件权限异常...: {} {}", remotePath, permissionValue, e);
            return new ApiResult<>(400, "操作失败 " + e.getMessage());
        }
        return ApiResult.success("操作成功");
    }
}
