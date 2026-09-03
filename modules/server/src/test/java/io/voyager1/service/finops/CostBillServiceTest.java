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

package io.voyager1.service.finops;

import io.voyager1.ApplicationStartTest;
import io.voyager1.model.data.CloudInstanceModel;
import io.voyager1.model.data.CostBudgetModel;
import io.voyager1.service.cloud.CloudInstanceService;
import io.voyager1.service.cloud.CloudService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * FinOps 成本服务测试（H2 内存库 + 假 CSV，无云依赖）
 *
 * @since 2026/8/31
 */
public class CostBillServiceTest extends ApplicationStartTest {

    @Autowired
    private CostBillService costBillService;

    @Autowired
    private CostBudgetService costBudgetService;

    @Autowired
    private CloudService cloudService;

    @Autowired
    private CloudInstanceService cloudInstanceService;

    @BeforeEach
    public void cleanTables() {
        costBillService.execute("delete from COST_BILL");
        costBillService.execute("delete from COST_BUDGET");
        costBillService.execute("delete from COST_TAG_RULE");
        costBillService.execute("delete from CLOUD_INSTANCE");
        costBillService.execute("delete from CLOUD_ACCOUNT");
    }

    private String newAccount() {
        return cloudService.saveAccount(null, "测试账号-" + java.util.UUID.randomUUID(), "aliyun", "test-ak", "test-sk", null, "cn-hangzhou", null);
    }

    private static final String CSV = "billDate,serviceName,resourceId,region,tagKey,tagValue,amount,currency\n"
        + "2026-08-01,ECS,i-001,cn-hangzhou,env,prod,100.5,CNY\n"
        + "2026-08-02,RDS,rds-001,cn-hangzhou,env,prod,50.0,CNY\n"
        + "2026-08-03,ECS,i-002,cn-beijing,env,dev,30.0,CNY\n";

    @Test
    public void testImportCsvAndTotalAmount() {
        String accountId = newAccount();
        int count = costBillService.importCsv(accountId, CSV);
        Assertions.assertEquals(3, count, "应导入 3 条明细");
        Double total = costBillService.totalAmount("2026-08-01", "2026-08-31");
        Assertions.assertEquals(180.5, total, 0.001, "总金额应为 100.5+50+30");
    }

    @Test
    public void testImportCsvSkipsHeader() {
        String accountId = newAccount();
        int count = costBillService.importCsv(accountId, CSV);
        Assertions.assertEquals(3, count);
        // 表头行不应被当作明细导入
        List<Map<String, Object>> rows = costBillService.analyze("serviceName", null, null);
        Assertions.assertFalse(rows.stream().anyMatch(r -> "billDate".equals(r.get("groupKey"))));
    }

    @Test
    public void testAnalyzeByService() {
        String accountId = newAccount();
        costBillService.importCsv(accountId, CSV);
        List<Map<String, Object>> rows = costBillService.analyze("serviceName", null, null);
        // ECS = 130.5, RDS = 50.0，按金额降序
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals("ECS", rows.get(0).get("groupKey"));
        Assertions.assertEquals(130.5, ((Number) rows.get(0).get("totalAmount")).doubleValue(), 0.001);
        Assertions.assertEquals("RDS", rows.get(1).get("groupKey"));
        Assertions.assertEquals(50.0, ((Number) rows.get(1).get("totalAmount")).doubleValue(), 0.001);
    }

    @Test
    public void testAnalyzeIllegalGroupByFallsBackToService() {
        String accountId = newAccount();
        costBillService.importCsv(accountId, CSV);
        // 非法维度应回退到 serviceName，而不是抛 SQL 注入异常
        List<Map<String, Object>> rows = costBillService.analyze("1; drop table COST_BILL", null, null);
        Assertions.assertEquals(2, rows.size());
    }

    @Test
    public void testImportCsvFieldMissingThrows() {
        String accountId = newAccount();
        String bad = "billDate,serviceName,resourceId,region,tagKey,tagValue,amount,currency\n2026-08-01,ECS,i-001\n";
        Assertions.assertThrows(IllegalArgumentException.class, () -> costBillService.importCsv(accountId, bad));
    }

    @Test
    public void testImportCsvInvalidAmountThrows() {
        String accountId = newAccount();
        String bad = "billDate,serviceName,resourceId,region,tagKey,tagValue,amount,currency\n2026-08-01,ECS,i-001,cn-hangzhou,env,prod,abc,CNY\n";
        Assertions.assertThrows(IllegalArgumentException.class, () -> costBillService.importCsv(accountId, bad));
    }

    @Test
    public void testBudgetOverflow() {
        String accountId = newAccount();
        costBillService.importCsv(accountId, CSV);
        CostBudgetModel budget = CostBudgetModel.builder()
            .name("测试预算")
            .scopeType("account")
            .scopeValue(accountId)
            .monthlyLimit(100.0)
            .build();
        costBudgetService.save(budget);
        List<Map<String, Object>> over = costBillService.checkBudget("2026-08");
        Assertions.assertEquals(1, over.size(), "成本 180.5 应超过预算 100");
        Assertions.assertEquals("测试预算", over.get(0).get("name"));
        Assertions.assertEquals(80.5, ((Number) over.get(0).get("overAmount")).doubleValue(), 0.001);
    }

    @Test
    public void testBudgetNotOverflow() {
        String accountId = newAccount();
        costBillService.importCsv(accountId, CSV);
        CostBudgetModel budget = CostBudgetModel.builder()
            .name("充足预算")
            .scopeType("account")
            .scopeValue(accountId)
            .monthlyLimit(1000.0)
            .build();
        costBudgetService.save(budget);
        List<Map<String, Object>> over = costBillService.checkBudget("2026-08");
        Assertions.assertTrue(over.isEmpty(), "成本 180.5 不应超过预算 1000");
    }

    @Test
    public void testIdleResources() {
        String accountId = newAccount();
        String instanceId = "i-idle-" + java.util.UUID.randomUUID();
        CloudInstanceModel instance = CloudInstanceModel.builder()
            .accountId(accountId)
            .instanceId(instanceId)
            .name("闲置实例")
            .status("Stopped")
            .regionId("cn-hangzhou")
            .build();
        cloudInstanceService.saveInstance(instance);
        List<Map<String, Object>> idle = costBillService.listIdleResources();
        Assertions.assertTrue(idle.stream().anyMatch(r -> instanceId.equals(r.get("instanceId"))), "应识别 Stopped 实例为闲置");
    }
}
