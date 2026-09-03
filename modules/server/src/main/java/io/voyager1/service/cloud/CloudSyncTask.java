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

package io.voyager1.service.cloud;

import io.voyager1.common.ILoadEvent;
import io.voyager1.cron.CronUtils;
import io.voyager1.model.data.CloudAccountModel;
import io.voyager1.service.finops.CostBillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 云资源定时同步任务（实例同步 + 账单采集）
 *
 * @since 2026/8/12
 */
@Configuration
@Slf4j
public class CloudSyncTask implements ILoadEvent {

    private final CloudService cloudService;
    private final CostBillService costBillService;

    public CloudSyncTask(CloudService cloudService, CostBillService costBillService) {
        this.cloudService = cloudService;
        this.costBillService = costBillService;
    }

    @Override
    public void afterPropertiesSet(ApplicationContext applicationContext) throws Exception {
        // 每 30 分钟同步一次云实例
        CronUtils.upsert("cloud_instance_sync", "0 0/30 * * * ?", () -> {
            try {
                int count = cloudService.syncAllAccounts();
                if (count > 0) {
                    log.info("云实例定时同步完成：{} 个实例", count);
                }
            } catch (Exception e) {
                log.error("云实例定时同步失败", e);
            }
        });
        // 每月 1 号采集上月账单
        CronUtils.upsert("cloud_bill_sync", "0 0 1 * * ?", () -> {
            try {
                String lastMonth = java.time.YearMonth.now().minusMonths(1).toString();
                List<CloudAccountModel> accounts = cloudService.listAccounts();
                int total = 0;
                for (CloudAccountModel account : accounts) {
                    try {
                        total += costBillService.syncBills(account.getId(), lastMonth);
                    } catch (Exception e) {
                        log.warn("账单采集失败 {}: {}", account.getId(), e.getMessage());
                    }
                }
                if (total > 0) {
                    log.info("账单采集完成：{} 条", total);
                }
            } catch (Exception e) {
                log.error("账单定时采集失败", e);
            }
        });
    }
}
