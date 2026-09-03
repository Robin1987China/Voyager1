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

package io.voyager1.service.dblog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import io.voyager1.util.CollUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.DateUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.core.api.ApiResult;
import io.voyager1.core.entity.BuildHistoryLogEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.BuildHistoryLogRepository;
import io.voyager1.event.ISystemTask;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.build.BuildExtraModule;
import io.voyager1.build.BuildUtil;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.BuildExtConfig;
import io.voyager1.model.BaseDbModel;
import io.voyager1.model.EnvironmentMapBuilder;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.data.BuildInfoModel;
import io.voyager1.model.enums.BuildStatus;
import io.voyager1.model.log.BuildHistoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 构建历史db
 *
 * @since 2019/7/20
 */
@Service
@Slf4j
public class DbBuildHistoryLogService extends JpaWorkspaceService<BuildHistoryLog, BuildHistoryLogEntity> implements ISystemTask {

    private final BuildInfoService buildService;
    private final BuildExtConfig buildExtConfig;
    private final BuildHistoryLogRepository buildHistoryLogRepository;

    public DbBuildHistoryLogService(BuildInfoService buildService,
                                    BuildExtConfig buildExtConfig,
                                    BuildHistoryLogRepository buildHistoryLogRepository) {
        this.buildService = buildService;
        this.buildExtConfig = buildExtConfig;
        this.buildHistoryLogRepository = buildHistoryLogRepository;
    }

    @Override
    protected JpaRepository<BuildHistoryLogEntity, String> repository() {
        return buildHistoryLogRepository;
    }

    @Override
    protected JpaSpecificationExecutor<BuildHistoryLogEntity> specExecutor() {
        return buildHistoryLogRepository;
    }

    @Override
    protected Class<BuildHistoryLogEntity> entityClass() {
        return BuildHistoryLogEntity.class;
    }

    @Override
    protected Class<BuildHistoryLog> modelClass() {
        return BuildHistoryLog.class;
    }

    /**
     * 更新状态
     *
     * @param logId         记录id
     * @param resultDirFile 构建产物目录
     */
    public void updateResultDirFile(String logId, String resultDirFile) {
        if (logId == null || (resultDirFile == null || resultDirFile.isEmpty())) {
            return;
        }

        BuildHistoryLog buildHistoryLog = new BuildHistoryLog();
        buildHistoryLog.setId(logId);
        buildHistoryLog.setResultDirFile(resultDirFile);
        this.updateById(buildHistoryLog);
    }


    /**
     * 清理文件并删除记录
     *
     * @param buildHistoryLog 构建记录
     * @return json
     */
    public ApiResult<String> deleteLogAndFile(BuildHistoryLog buildHistoryLog) {
        if (buildHistoryLog == null) {
            return ApiResult.success("没有对应构建记录,忽略删除");
        }
        BuildInfoModel item = buildService.getByKey(buildHistoryLog.getBuildDataId());
        if (item != null) {
            File logFile = BuildUtil.getLogFile(item.getId(), buildHistoryLog.getBuildNumberId());
            if (logFile != null) {
                File dataFile = logFile.getParentFile();
                if (dataFile.exists()) {
                    boolean s = FileUtil.del(dataFile);
                    if (!s) {
                        return new ApiResult<>(500, "清理文件失败");
                    }
                }
            }
        }
        int count = this.delByKey(buildHistoryLog.getId());
        return new ApiResult<>(200, "删除成功", String.valueOf(count));
    }

    @Override
    protected void fillSelectResult(BuildHistoryLog data) {
        super.fillSelectResult(data);
        Optional.ofNullable(data).ifPresent(buildHistoryLog -> {
            // 不能返回环境变量的信息（存在隐私字段）将隐私变量隐藏后返回
            EnvironmentMapBuilder builder = buildHistoryLog.toEnvironmentMapBuilder();
            buildHistoryLog.setBuildEnvCache(builder.toPrivacyDataJsonStr());
        });

    }

    @Override
    public void insert(BuildHistoryLog buildHistoryLog) {
        super.insert(buildHistoryLog);
        // 清理单个
        BuildExtraModule build = BuildExtraModule.build(buildHistoryLog);
        int resultKeepCount = (build.getResultKeepCount() != null ? build.getResultKeepCount() : 0);
        int buildItemMaxHistoryCount = buildExtConfig.getItemMaxHistoryCount();
        if (resultKeepCount > 0 || buildItemMaxHistoryCount > 0) {
            // 至少有一个配置
            int useCount;
            if (resultKeepCount > 0 && buildItemMaxHistoryCount > 0) {
                // 都配置过，使用最小值
                useCount = Math.min(resultKeepCount, buildItemMaxHistoryCount);
            } else {
                // 只配置了一处，使用最大值
                useCount = Math.max(resultKeepCount, buildItemMaxHistoryCount);
            }
            super.autoLoopClear("startTime", useCount, entity -> this.fillClearWhere(entity, buildHistoryLog.getBuildDataId()), this.predicate());
        }
    }

    private Predicate<BuildHistoryLog> predicate() {
        return buildHistoryLog1 -> {
            ApiResult<String> jsonMessage = this.deleteLogAndFile(buildHistoryLog1);
            if (!jsonMessage.success()) {
                log.warn("{} {} {}", buildHistoryLog1.getBuildName(), buildHistoryLog1.getBuildNumberId(), jsonMessage);
                return false;
            }
            return true;
        };
    }

    private void fillClearWhere(Entity entity, String buildDataId) {
        entity.set("buildDataId", buildDataId);
        // 清理单项构建历史保留个数只判断（构建结束、发布中、发布失败、发布失败）有效构建状态，避免无法保留有效构建历史
        entity.set("status", new java.util.ArrayList<>(java.util.Arrays.asList(BuildStatus.Success.getCode())));
    }

    @Override
    protected void executeClearImpl(int count) {
        // 清理总数据
        int buildMaxHistoryCount = buildExtConfig.getMaxHistoryCount();
        int saveCount = Math.min(count, buildMaxHistoryCount);
        if (saveCount <= 0) {
            // 不清除
            return;
        }
        super.autoLoopClear("startTime", saveCount,
            null,
            buildHistoryLog1 -> {
                ApiResult<String> jsonMessage = this.deleteLogAndFile(buildHistoryLog1);
                if (!jsonMessage.success()) {
                    log.warn("{} {} {}", buildHistoryLog1.getBuildName(), buildHistoryLog1.getBuildNumberId(), jsonMessage);
                    return false;
                }
                return true;
            });
    }

    @Override
    protected String[] clearTimeColumns() {
        return super.clearTimeColumns();
    }

    @Override
    public void executeTask() {
        List<BuildInfoModel> buildInfoModels = buildService.hasResultKeep();
        if ((buildInfoModels == null || buildInfoModels.isEmpty())) {
            return;
        }
        for (BuildInfoModel buildInfoModel : buildInfoModels) {
            Integer resultKeepDay = buildInfoModel.getResultKeepDay();
            if (resultKeepDay == null || resultKeepDay <= 0) {
                continue;
            }
            log.debug("自动删除过期的构建历史相关文件：{} {}", buildInfoModel.getName(), resultKeepDay);
            Entity entity = Entity.create();
            this.fillClearWhere(entity, buildInfoModel.getId());
            DateTime date = DateTime.now();
            date = DateUtil.offsetDay(date, -resultKeepDay);
            date = DateUtil.beginOfDay(date);
            entity.set("startTime", "< " + date.getTime());
            while (true) {
                Pageable page = PageRequest.of(0, 50, Sort.by(Sort.Order.desc("startTime")));
                PageResultDto<BuildHistoryLog> pageResult = this.listPage(entity, page);
                if (pageResult.isEmpty()) {
                    break;
                }
                List<String> ids = pageResult.getResult()
                    .stream()
                    .filter(this.predicate())
                    .map(BaseDbModel::getId)
                    .collect(Collectors.toList());
                //
                this.delByKey(ids, null);
            }
        }
    }
}
