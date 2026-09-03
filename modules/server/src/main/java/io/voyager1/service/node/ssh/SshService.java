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

package io.voyager1.service.node.ssh;

import io.voyager1.common.ServerConst;
import io.voyager1.configuration.BuildExtConfig;
import io.voyager1.core.entity.SshEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.SshRepository;
import io.voyager1.func.assets.model.MachineSshModel;
import io.voyager1.func.assets.server.MachineSshServer;
import io.voyager1.model.data.SshModel;
import io.voyager1.plugins.JschLogger;
import io.voyager1.util.ChannelType;
import io.voyager1.util.FileUtil;
import io.voyager1.util.IoUtil;
import io.voyager1.util.JschUtil;
import io.voyager1.util.LogRecorder;
import io.voyager1.util.MySftp;
import io.voyager1.util.NumberUtil;
import io.voyager1.util.Sftp;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSH 信息服务。
 */
@Service
@Slf4j
public class SshService extends JpaWorkspaceService<SshModel, SshEntity> {

    @Resource
    @Lazy
    private MachineSshServer machineSshServer;
    private final BuildExtConfig buildExtConfig;
    private final SshRepository repository;

    public SshService(SshRepository repository, BuildExtConfig buildExtConfig) {
        this.repository = repository;
        this.buildExtConfig = buildExtConfig;
        JSch.setLogger(JschLogger.LOGGER);
    }

    @Override
    protected JpaRepository<SshEntity, String> repository() { return repository; }

    @Override
    protected JpaSpecificationExecutor<SshEntity> specExecutor() { return repository; }

    @Override
    protected Class<SshEntity> entityClass() { return SshEntity.class; }

    @Override
    protected Class<SshModel> modelClass() { return SshModel.class; }

    @Override
    protected void fillSelectResult(SshModel data) {
        if (data == null) return;
        if (!(data.getPassword() != null && data.getPassword().toLowerCase().startsWith(ServerConst.REF_WORKSPACE_ENV.toLowerCase()))) {
            data.setPassword(null);
        }
        data.setPrivateKey(null);
    }

    @Override
    protected void fillInsert(SshModel sshModel) {
        sshModel.setHost("");
        sshModel.setUser("");
        sshModel.setPort(0);
    }

    public Session getSessionByModel(SshModel sshModel) {
        return machineSshServer.getSessionByModelNoFill(this.getMachineSshModel(sshModel));
    }

    public Session getSessionByModel(MachineSshModel sshModel) {
        return machineSshServer.getSessionByModelNoFill(sshModel);
    }

    public MachineSshModel getMachineSshModel(SshModel sshModel) {
        MachineSshModel m = machineSshServer.getByKey(sshModel.getMachineSshId(), false);
        Assert.notNull(m, "不存在对应的资产SSH");
        return m;
    }

    public void uploadDir(MachineSshModel machineSshModel, String remotePath, File desc) {
        Session session = null;
        ChannelSftp channel = null;
        try {
            session = this.getSessionByModel(machineSshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            try (Sftp sftp = new Sftp(channel, machineSshModel.charset(), machineSshModel.timeout())) {
                sftp.syncUpload(desc, remotePath);
            }
        } finally {
            JschUtil.close(channel);
            JschUtil.close(session);
        }
    }

    public void download(SshModel sshModel, String remoteFile, File save) throws IOException, SftpException {
        Session session = null;
        ChannelSftp channel = null;
        OutputStream output = null;
        try {
            session = this.getSessionByModel(sshModel);
            channel = (ChannelSftp) JschUtil.openChannel(session, ChannelType.SFTP);
            output = Files.newOutputStream(save.toPath());
            channel.get(remoteFile, output);
        } finally {
            IoUtil.close(output);
            JschUtil.close(channel);
            JschUtil.close(session);
        }
    }

    public void syncToWorkspace(String ids, String nowWorkspaceId, String workspaceId) {
        io.voyager1.util.ConvertUtil.splitTrim(ids, ",").forEach(id -> {
            SshModel data = this.getByKey(id, nowWorkspaceId);
            Assert.notNull(data, "没有对应的ssh信息");
            SshModel where = new SshModel();
            where.setWorkspaceId(workspaceId);
            where.setMachineSsh(data.getMachineSsh());
            Assert.isNull(this.queryByBean(where), "对应的工作空间已经存在当前 SSH 啦");
            data.setId(null);
            data.setWorkspaceId(workspaceId);
            data.setCreateTimeMillis(null);
            data.setModifyTimeMillis(null);
            data.setModifyUser(null);
            data.setHost(null);
            data.setUser(null);
            data.setPassword(null);
            data.setPort(null);
            data.setConnectType(null);
            data.setCharset(null);
            data.setPrivateKey(null);
            data.setTimeout(null);
            this.insert(data);
        });
    }

    public long countByMachine(String machineSshId) {
        SshModel nodeModel = new SshModel();
        nodeModel.setMachineSshId(machineSshId);
        return this.count(nodeModel);
    }

    public void existsSsh(String workspaceId, String machineSshId) {
        SshModel where = new SshModel();
        where.setWorkspaceId(workspaceId);
        where.setMachineSshId(machineSshId);
        Assert.isNull(this.queryByBean(where), () -> "对应工作空间已经存在该 ssh 啦");
    }

    public boolean existsSsh2(String workspaceId, String machineSshId) {
        SshModel where = new SshModel();
        where.setWorkspaceId(workspaceId);
        where.setMachineSshId(machineSshId);
        return this.exists(where);
    }

    public void insert(MachineSshModel machineSshModel, String workspaceId) {
        SshModel data = new SshModel();
        data.setWorkspaceId(workspaceId);
        data.setName(machineSshModel.getName());
        data.setGroup(machineSshModel.getGroupName());
        data.setMachineSshId(machineSshModel.getId());
        this.insert(data);
    }

    public MySftp.ProgressMonitor createProgressMonitor(LogRecorder logRecorder) {
        Set<Integer> progressRangeList = ConcurrentHashMap.newKeySet((int) Math.floor((float) 100 / buildExtConfig.getLogReduceProgressRatio()));
        return new MySftp.ProgressMonitor() {
            @Override public void rest() { progressRangeList.clear(); }
            @Override public void progress(String desc, long max, long now) {
                double progressPercentage = Math.floor(((float) now / max) * 100);
                int progressRange = (int) Math.floor(progressPercentage / buildExtConfig.getLogReduceProgressRatio());
                if (progressRangeList.add(progressRange)) {
                    logRecorder.system("上传文件进度:{} {}/{} {} ", desc, FileUtil.readableFileSize(now), FileUtil.readableFileSize(max), NumberUtil.formatPercent(((float) now / max), 0));
                }
            }
        };
    }

    @org.springframework.transaction.annotation.Transactional
    public int updateMachineSshId(String machineSshId, String host, Integer port, String user, String connectType) {
        java.util.List<SshEntity> list = repository.findByHostAndPortAndUserAndConnectType(host, port, user, connectType);
        for (SshEntity e : list) {
            e.setMachineSshId(machineSshId);
            repository.save(e);
        }
        return list.size();
    }

    public SshModel getByMachineSshId(String id) {
        SshModel model = new SshModel();
        model.setMachineSshId(id);
        return queryByBean(model);
    }
}
