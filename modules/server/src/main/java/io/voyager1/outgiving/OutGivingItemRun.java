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

package io.voyager1.outgiving;

import io.voyager1.util.BetweenFormatter;
import io.voyager1.util.FileUtil;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSONObject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.AfterOpt;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.log.OutGivingLog;
import io.voyager1.model.outgiving.OutGivingModel;
import io.voyager1.model.outgiving.OutGivingNodeProject;
import io.voyager1.service.node.NodeService;
import io.voyager1.service.outgiving.DbOutGivingLogService;
import io.voyager1.util.StringUtil;

import java.io.File;
import java.util.concurrent.Callable;

/**
 * @since 2021/12/10
 */
@Slf4j
@Setter
public class OutGivingItemRun implements Callable<OutGivingNodeProject.Status> {

    private final String outGivingId;
    private final OutGivingNodeProject outGivingNodeProject;
    private final NodeModel nodeModel;
    private final File file;
    private final AfterOpt afterOpt;
    private final boolean unzip;
    private final boolean clearOld;
    private final Integer sleepTime;
    private final String secondaryDirectory;
    private final Boolean closeFirst;
    private int stripComponents;
    private String fileName;

    public OutGivingItemRun(OutGivingModel item,
                            OutGivingNodeProject outGivingNodeProject,
                            File file,
                            boolean unzip,
                            Integer sleepTime) {
        this.outGivingId = item.getId();
        this.secondaryDirectory = item.getSecondaryDirectory();
        this.clearOld = item.clearOld();
        this.closeFirst = item.getUploadCloseFirst();
        this.unzip = unzip;
        this.outGivingNodeProject = outGivingNodeProject;
        this.file = file;
        this.afterOpt = (EnumUtil.likeValueOf(AfterOpt.class, item.getAfterOpt()) != null ? EnumUtil.likeValueOf(AfterOpt.class, item.getAfterOpt()) : AfterOpt.No);
        //
        NodeService nodeService = SpringContextHolder.getBean(NodeService.class);
        this.nodeModel = nodeService.getByKey(outGivingNodeProject.getNodeId());
        //
        this.sleepTime = sleepTime;
    }

    @Override
    public OutGivingNodeProject.Status call() {
        OutGivingNodeProject.Status result;
        long time = System.currentTimeMillis();
        String fileSize = FileUtil.readableFileSize(file);
        this.fileName = (this.fileName == null || this.fileName.isEmpty() ? this.file.getName() : this.fileName);
        try {
            if (this.outGivingNodeProject.getDisabled() != null && this.outGivingNodeProject.getDisabled()) {
                // 禁用
                this.updateStatus(this.outGivingId, OutGivingNodeProject.Status.Cancel, "当前项目被禁用");
                return OutGivingNodeProject.Status.Cancel;
            }
            this.updateStatus(this.outGivingId, OutGivingNodeProject.Status.Ing, "开始分发");
            //
            ApiResult<String> jsonMessage = OutGivingRun.fileUpload(file, this.fileName, this.secondaryDirectory,
                this.outGivingNodeProject.getProjectId(),
                unzip,
                afterOpt,
                this.nodeModel, this.clearOld,
                this.sleepTime, this.closeFirst, this.stripComponents, (total, progressSize) -> {

                    String logId = OutGivingRun.getLogId(outGivingId, outGivingNodeProject);
                    //
                    OutGivingLog outGivingLog = new OutGivingLog();
                    outGivingLog.setId(logId);
                    outGivingLog.setFileSize(total);
                    outGivingLog.setProgressSize(progressSize);
                    //
                    DbOutGivingLogService dbOutGivingLogService = SpringContextHolder.getBean(DbOutGivingLogService.class);
                    dbOutGivingLogService.updateById(outGivingLog);
                });
            result = jsonMessage.success() ? OutGivingNodeProject.Status.Ok : OutGivingNodeProject.Status.Fail;

            JSONObject jsonObject = jsonMessage.toJson();
            jsonObject.put("upload_duration", StringUtil.formatBetween(System.currentTimeMillis() - time, BetweenFormatter.Level.MILLISECOND, 2));
            jsonObject.put("upload_file_size", fileSize);
            this.updateStatus(this.outGivingId, result, jsonObject.toString());
        } catch (Exception e) {
            log.error("{} {} 分发异常保存", this.outGivingNodeProject.getNodeId(), this.outGivingNodeProject.getProjectId(), e);
            result = OutGivingNodeProject.Status.Fail;
            JSONObject jsonObject = ApiResult.toJson(500, e.getMessage());
            jsonObject.put("upload_duration", StringUtil.formatBetween(System.currentTimeMillis() - time, BetweenFormatter.Level.MILLISECOND, 2));
            jsonObject.put("upload_file_size", fileSize);
            this.updateStatus(this.outGivingId, result, jsonObject.toString());
        }
        return result;
    }

    /**
     * 更新状态
     *
     * @param outGivingId 分发id
     * @param status      状态
     * @param msg         消息描述
     */
    private void updateStatus(String outGivingId, OutGivingNodeProject.Status status, String msg) {
        String logId = OutGivingRun.getLogId(outGivingId, outGivingNodeProject);
        OutGivingLog outGivingLog = new OutGivingLog();
        outGivingLog.setId(logId);
        outGivingLog.setStatus(status.getCode());
        outGivingLog.setResult(msg);
        if (status == OutGivingNodeProject.Status.Ok || status == OutGivingNodeProject.Status.Fail) {
            outGivingLog.setEndTime(System.currentTimeMillis());
        }
        DbOutGivingLogService dbOutGivingLogService = SpringContextHolder.getBean(DbOutGivingLogService.class);
        dbOutGivingLogService.updateById(outGivingLog);
    }
}
