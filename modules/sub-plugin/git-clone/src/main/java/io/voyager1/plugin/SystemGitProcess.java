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

package io.voyager1.plugin;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.VersionComparator;
import io.voyager1.util.FileUtil;
import io.voyager1.util.Tuple;
import io.voyager1.util.StrUtil;
import io.voyager1.util.URLUtil;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.util.CommandUtil;
import org.eclipse.jgit.lib.Constants;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @since 2023/4/10
 */
@Slf4j
public class SystemGitProcess extends AbstractGitProcess {

    protected SystemGitProcess(IWorkspaceEnvPlugin workspaceEnvPlugin, Map<String, Object> parameter) {
        super(workspaceEnvPlugin, parameter);
        PrintWriter printWriter = (PrintWriter) parameter.get("logWriter");
        if (printWriter != null) {
            printWriter.println();
            printWriter.println("use system git");
            printWriter.flush();
        }
    }
//    ssh-agent bash -c 'ssh-add <path-to-private-key>; git clone git@<host>:<username>/<repo-name>.git'
    // ssh-agent bash -c 'ssh-add /path/to/private_key && ssh -o StrictHostKeyChecking=yes git@github.com && git clone git@github.com:user/repo.git'

    /**
     * 获取仓库地址 需要拼接账号密码
     *
     * @return url
     * @throws MalformedURLException url 错误
     */
    private String getCovertUrl() throws MalformedURLException {
        String url = (String) parameter.get("url");
        String username = (String) parameter.getOrDefault("username", "");
        String password = (String) parameter.getOrDefault("password", "");
        int protocol = (int) parameter.getOrDefault("protocol", 0);
        if (protocol == 0) {
            if ((url != null && url.contains("@"))) {
                // 已经配置
                return url;
            }
            username = URLUtil.encodeAll(username);
            password = URLUtil.encodeAll(password);
            URL url1 = getUrl(username, password, url);
            return url1.toString();
        }
        // ssh 原样返回
        return url;
    }

    private static URL getUrl(String username, String password, String url) throws MalformedURLException {
        String userInfo = username + ":" + password;
        return new URL(null, url, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL u) throws IOException {
                return null;
            }

            @Override
            protected void setURL(URL u, String protocol, String host, int port, String authority, String userInfo2, String path, String query, String ref) {
                super.setURL(u, protocol, host, port, String.format("%s@%s", userInfo, authority), userInfo, path, query, ref);
            }
        });
    }

    private String warpSsh(String command) {
        int protocol = (int) parameter.getOrDefault("protocol", 0);
        if (protocol == 0) {
            return command;
        } else if (protocol == 1) {
            // TODO 需要实现本地 git ssh 指定证书拉取
            File rsaFile = (File) parameter.get("rsaFile");
            if (FileUtil.isFile(rsaFile)) {
                throw new IllegalStateException("暂时不支持本地 git 指定证书拉取代码");
            }
            // 默认的方式去执行
            return command;
        } else {
            throw new IllegalArgumentException("不支持的 protocol" + protocol);
        }
    }

    @Override
    public Tuple branchAndTagList() throws Exception {
        String command = String.format("git ls-remote %s", this.getCovertUrl());
        command = this.warpSsh(command);
        String result = CommandUtil.execSystemCommand(command);
        List<String> branchRemote = new ArrayList<>();
        List<String> tagRemote = new ArrayList<>();
        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(result, "\n");
        for (String branch : list) {
            List<String> list1 = io.voyager1.util.ConvertUtil.splitTrim(branch, "\t");
            String last = (list1 == null || list1.isEmpty() ? null : list1.get(list1.size() - 1));
            if ((last != null && last.startsWith(Constants.R_HEADS))) {
                branchRemote.add((last != null && last.startsWith(Constants.R_HEADS) ? last.substring(Constants.R_HEADS.length()) : last));
            } else if ((last != null && last.startsWith(Constants.R_TAGS))) {
                tagRemote.add((last != null && last.startsWith(Constants.R_TAGS) ? last.substring(Constants.R_TAGS.length()) : last));
            }
        }
        branchRemote.sort((o1, o2) -> VersionComparator.INSTANCE.compare(o2, o1));
        tagRemote.sort((o1, o2) -> VersionComparator.INSTANCE.compare(o2, o1));
        return new Tuple(branchRemote, tagRemote);
    }

    @Override
    public String[] pull() throws Exception {
        String branchName = (String) parameter.get("branchName");
        Assert.hasText(branchName, "没有 branch name");
        return pull(branchName);
    }

    @Override
    public String[] pullByTag() throws Exception {
        String tagName = (String) parameter.get("tagName");
        Assert.hasText(tagName, "没有 tag name");
        return pull(tagName);
    }

    private String[] pull(String branchOrTag) throws IOException {
        PrintWriter printWriter = (PrintWriter) parameter.get("logWriter");
        boolean needClone = this.needClone();
        if (needClone) {
            // clone
            this.reClone(printWriter, branchOrTag);
        }
        File saveFile = getSaveFile();

        {
            Boolean strictlyEnforce = (Boolean) parameter.get("strictlyEnforce");
            strictlyEnforce = strictlyEnforce != null && strictlyEnforce;
            // 更新
            /*CommandUtil.exec(saveFile, null, line -> {
                printWriter.println(line);
                printWriter.flush();
            }, "git", "pull");*/
            int code = CommandUtil.exec(saveFile, null, line -> {
                printWriter.println(line);
                printWriter.flush();
            }, "git", "fetch", "--all");
            if (code != 0 && strictlyEnforce) {
                return new String[]{null, null, "git fetch失败状态码:" + code};
            }
            code = CommandUtil.exec(saveFile, null, line -> {
                printWriter.println(line);
                printWriter.flush();
            }, "git", "reset", "--hard", "origin/" + branchOrTag);
            if (code != 0 && strictlyEnforce) {
                return new String[]{null, null, "git reset --hard失败状态码:" + code};
            }
            code = CommandUtil.exec(saveFile, null, line -> {
                printWriter.println(line);
                printWriter.flush();
            }, "git", "submodule", "update", "--init", "--remote", "-f", "--recursive");
            if (code != 0 && strictlyEnforce) {
                return new String[]{null, null, "git submodule update 失败状态码:" + code};
            }
        }
        // 获取提交日志
        String[] command = {"git", "log", "-1", branchOrTag};
        String[] commitId = new String[1];
        StringBuilder commitInfo = new StringBuilder();
        AtomicBoolean nextMsg = new AtomicBoolean(false);

        CommandUtil.exec(saveFile, null, line -> {
            printWriter.println(line);
            printWriter.flush();
            if ((commitId[0] == null || commitId[0].isEmpty()) && (line != null && line.toLowerCase().startsWith("commit".toLowerCase()))) {
                List<String> list = io.voyager1.util.ConvertUtil.splitTrim(line, " ");
                commitId[0] = (1 < list.size() ? list.get(1) : null);
            }
            if ((line != null && line.toLowerCase().startsWith("Date:".toLowerCase()))) {
                nextMsg.set(true);
            } else if (nextMsg.get()) {
                commitInfo.append(line).append("\n");
            }
        }, command);
        return new String[]{commitId[0], commitInfo.toString()};
    }

    private void reClone(PrintWriter printWriter, String branchOrTag) throws IOException {
        String string = "自动重新克隆存储库";
        printWriter.println("SystemGit: " + string);
        // 先删除本地目录
        File savePath = getSaveFile();
        if (!FileUtil.clean(savePath)) {
            FileUtil.del(savePath.toPath());
        }
        String depthStr = Optional.ofNullable((Integer) parameter.get("depth"))
            .map(integer -> {
                if (integer > 0) {
                    return integer;
                }
                return null;
            })
            .map(integer -> "--depth=" + integer)
            .orElse("");
        Map<String, String> env = new HashMap<>(4);
        Optional.ofNullable((Integer) parameter.get("timeout"))
            .map(integer -> {
                if (integer > 0) {
                    return integer;
                }
                return null;
            }).ifPresent(integer -> env.put("GIT_HTTP_TIMEOUT", String.valueOf(integer)));
        //
        String[] command = new String[]{"git", "clone", "--recursive", depthStr, "-b", branchOrTag, this.getCovertUrl(), savePath.getAbsolutePath()};
        FileUtil.mkdir(savePath);
        CommandUtil.exec(savePath, env, line -> {
            printWriter.println(line);
            printWriter.flush();
        }, command);
    }

    /**
     * 是否存在GIT仓库
     */
    private boolean needClone() throws MalformedURLException {
        File savePath = getSaveFile();
        File file = FileUtil.file(savePath, Constants.DOT_GIT);
        if (!FileUtil.exist(file)) {
            return true;
        }
        // 判断远程
        String url = (String) parameter.get("url");
        String checkRemote = CommandUtil.execSystemCommand("git remote -v", savePath);
        if (!StrUtil.containsAny(checkRemote, url, this.getCovertUrl())) {
            return true;
        }
        String branchName = getBranchName();
        if ((branchName != null && !branchName.isEmpty())) {
            String checkBranch = CommandUtil.execSystemCommand("git rev-parse --abbrev-ref HEAD", savePath);
            checkBranch = (checkBranch == null ? null : checkBranch.trim());
            return !java.util.Objects.equals(checkBranch, branchName);
        }
        // tag 模式
        return true;
    }
}
