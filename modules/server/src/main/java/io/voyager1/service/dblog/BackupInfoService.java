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
import org.springframework.data.domain.Sort;
import io.voyager1.util.LocalDateTimeUtil;
import io.voyager1.util.DateUtil;

import io.voyager1.util.CollUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.FileUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.event.ISystemTask;
import io.voyager1.plugin.IPlugin;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Voyager1Manifest;
import io.voyager1.common.ServerConst;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.common.i18n.I18nThreadUtil;
import io.voyager1.core.entity.BackupInfoEntity;
import io.voyager1.core.jpa.JpaBaseService;
import io.voyager1.core.repository.BackupInfoRepository;
import io.voyager1.db.DbExtConfig;
import io.voyager1.system.ExtConfigBean;
import io.voyager1.model.data.BackupInfoModel;
import io.voyager1.model.enums.BackupStatusEnum;
import io.voyager1.model.enums.BackupTypeEnum;
import io.voyager1.model.user.UserModel;
import io.voyager1.plugin.PluginFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 备份数据库 service
 *
 * @since 2021-11-18
 **/
@Service
@Slf4j
public class BackupInfoService extends JpaBaseService<BackupInfoModel, BackupInfoEntity> implements ISystemTask {

    private final DbExtConfig dbExtConfig;
    private final BackupInfoRepository backupInfoRepository;
    private final javax.sql.DataSource dataSource;

    public BackupInfoService(DbExtConfig dbExtConfig, BackupInfoRepository backupInfoRepository, javax.sql.DataSource dataSource) {
        this.dbExtConfig = dbExtConfig;
        this.backupInfoRepository = backupInfoRepository;
        this.dataSource = dataSource;
    }

    @Override
    protected JpaRepository<BackupInfoEntity, String> repository() {
        return backupInfoRepository;
    }

    @Override
    protected JpaSpecificationExecutor<BackupInfoEntity> specExecutor() {
        return backupInfoRepository;
    }

    @Override
    protected Class<BackupInfoEntity> entityClass() {
        return BackupInfoEntity.class;
    }

    @Override
    protected Class<BackupInfoModel> modelClass() {
        return BackupInfoModel.class;
    }

    /**
     * 检查数据库备份
     */
    @Override
    public void executeTask() {
        if (dbExtConfig.getMode() != DbExtConfig.Mode.H2) {
            return;
        }
        try {
            BaseServerController.resetInfo(UserModel.EMPTY);
            // 创建备份
            this.createAutoBackup();
            // 删除历史备份
            this.deleteAutoBackup();
        } finally {
            BaseServerController.removeEmpty();
        }
    }

    /**
     * 删除历史 自动备份信息
     */
    private void deleteAutoBackup() {
        Integer autoBackupReserveDay = dbExtConfig.getAutoBackupReserveDay();
        if (autoBackupReserveDay != null && autoBackupReserveDay > 0) {
            //
            Entity entity = Entity.create();
            entity.set("backupType", BackupTypeEnum.AUTO.getCode());
            entity.set("createTimeMillis", " < " + (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(autoBackupReserveDay)));
            List<BackupInfoModel> entities = this.listByEntity(entity);
            if (entities != null) {
                for (BackupInfoModel model : entities) {
                    this.delByKey(model.getId());
                }
            }
        }
    }

    /**
     * 创建自动备份数据
     */
    private void createAutoBackup() {
        // 自动备份
        Integer autoBackupIntervalDay = dbExtConfig.getAutoBackupIntervalDay();
        if (autoBackupIntervalDay != null && autoBackupIntervalDay > 0) {
            BackupInfoModel backupInfoModel = new BackupInfoModel();
            backupInfoModel.setBackupType(BackupTypeEnum.AUTO.getCode());
            List<BackupInfoModel> infoModels = this.queryList(backupInfoModel, 1, Sort.by(Sort.Order.desc("createTimeMillis")));
            BackupInfoModel first = (infoModels == null || infoModels.isEmpty() ? null : infoModels.get(0));
            if (first != null) {
                Long createTimeMillis = first.getCreateTimeMillis();
                long interval = System.currentTimeMillis() - createTimeMillis;
                if (interval < TimeUnit.DAYS.toMillis(autoBackupIntervalDay)) {
                    return;
                }
            }
            this.autoBackup();
        }
    }

    /**
     * 自动备份
     */
    public Future<BackupInfoModel> autoBackup() {
        if (dbExtConfig.getMode() != DbExtConfig.Mode.H2) {
            return null;
        }
        // 执行数据库备份
        return this.backupToSql(null, BackupTypeEnum.AUTO);
    }


    /**
     * 触发器备份
     */
    public Future<BackupInfoModel> triggerBackup() {
        if (dbExtConfig.getMode() != DbExtConfig.Mode.H2) {
            return null;
        }
        try {
            BaseServerController.resetInfo(UserModel.EMPTY);
            // 执行数据库备份
            return this.backupToSql(null, BackupTypeEnum.TRIGGER);
        } finally {
            BaseServerController.removeEmpty();
        }
    }

    /**
     * 备份数据库 SQL 文件
     *
     * @param tableNameList 需要备份的表名称列表，如果是全库备份，则不需要
     */
    public Future<BackupInfoModel> backupToSql(final List<String> tableNameList) {
        // 判断备份类型
        BackupTypeEnum backupType = BackupTypeEnum.ALL;
        if (!CollectionUtils.isEmpty(tableNameList)) {
            backupType = BackupTypeEnum.PART;
        }
        return this.backupToSql(tableNameList, backupType);
    }

    /**
     * 备份数据库 SQL 文件
     *
     * @param tableNameList 需要备份的表名称列表，如果是全库备份，则不需要
     */
    private Future<BackupInfoModel> backupToSql(final List<String> tableNameList, BackupTypeEnum backupType) {
        final String fileName = LocalDateTimeUtil.format(LocalDateTimeUtil.now(), "yyyyMMddHHmmss");

        // 设置默认备份 SQL 的文件地址
        File file = FileUtil.file(new File(ExtConfigBean.getPath(), "db"), DbExtConfig.BACKUP_DIRECTORY_NAME, fileName + DbExtConfig.SQL_FILE_SUFFIX);
        final String backupSqlPath = FileUtil.getAbsolutePath(file);

        Voyager1Manifest instance = Voyager1Manifest.getInstance();
        // 先构造备份信息插入数据库
        BackupInfoModel backupInfoModel = new BackupInfoModel();
        String timeStamp = instance.getTimeStamp();
        try {
            DateTime parse = DateUtil.parse(timeStamp);
            backupInfoModel.setBaleTimeStamp(parse.getTime());
        } catch (Exception ignored) {
        }
        backupInfoModel.setName(fileName);
        backupInfoModel.setVersion(instance.getVersion());
        backupInfoModel.setBackupType(backupType.getCode());
        backupInfoModel.setFilePath(backupSqlPath);
        this.insert(backupInfoModel);
        // 开启一个子线程去执行任务，任务完成之后修改对应的数据库备份信息
        return I18nThreadUtil.execAsync(() -> {
            // 修改用的实体类
            BackupInfoModel backupInfo = new BackupInfoModel();
            backupInfo.setId(backupInfoModel.getId());
            try {
                log.debug("启动一个新线程来执行 H2 数据库备份...启动");
                try (java.sql.Connection conn = dataSource.getConnection(); java.sql.Statement st = conn.createStatement()) {
                    String scriptSql = "SCRIPT DROP TO '" + backupSqlPath + "'";
                    if (!CollectionUtils.isEmpty(tableNameList)) {
                        scriptSql += " TABLE " + String.join(",", tableNameList);
                    }
                    st.execute(scriptSql);
                }
                // 修改备份任务执行完成
                backupInfo.setFileSize(FileUtil.size(file));
                backupInfo.setSha1Sum(DigestUtil.sha1(file));
                backupInfo.setStatus(BackupStatusEnum.SUCCESS.getCode());
                this.updateById(backupInfo);
                log.debug("启动一个新线程来执行 H2 数据库备份...成功");
            } catch (Exception e) {
                // 记录错误日志信息，修改备份任务执行失败
                log.error("备份 h2 数据库异常", e);
                backupInfo.setStatus(BackupStatusEnum.FAILED.getCode());
                this.updateById(backupInfo);
            }
            return backupInfo;
        });
    }

    /**
     * 根据 SQL 文件还原数据库
     * 还原数据库时只能同步，防止该过程中修改数据造成数据不一致
     *
     * @param backupSqlPath 备份 sql 文件地址
     */
    public boolean restoreWithSql(String backupSqlPath) {
        try {
            long startTs = System.currentTimeMillis();
            IPlugin plugin = PluginFactory.getPlugin("db-h2");
            Map<String, Object> map = new HashMap<>(10);
            map.put("backupSqlPath", backupSqlPath);
            plugin.execute("restoreBackupSql", map);
            // h2BackupService.restoreBackupSql(backupSqlPath);
            long endTs = System.currentTimeMillis();
            log.debug("restore H2 Database backup...success...cast {} ms", endTs - startTs);
            return true;
        } catch (Exception e) {
            // 记录错误日志信息，返回数据库备份还原执行失败
            log.error("restore H2 Database backup...catch exception...message: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * load table name list from h2 database
     *
     * @return list
     */
    public List<String> h2TableNameList() {
        String sql = "show tables;";
        List<io.voyager1.core.db.Entity> list = this.query(sql);
        // 筛选字段
        return list.stream()
            .filter(entity -> StringUtils.hasLength(String.valueOf(entity.get(ServerConst.TABLE_NAME))))
            .flatMap(entity -> Stream.of(String.valueOf(entity.get(ServerConst.TABLE_NAME))))
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public int delByKey(String keyValue) {
        // 根据 id 查询备份信息
        BackupInfoModel backupInfoModel = this.getByKey(keyValue);
        Objects.requireNonNull(backupInfoModel, "备份数据不存在");

        // 删除对应的文件
        boolean del = FileUtil.del(backupInfoModel.getFilePath());
        Assert.state(del, "删除备份数据文件失败");

        // 删除备份信息
        return this.delByKey(keyValue);
    }
}
