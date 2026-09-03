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

import io.voyager1.cloud.CloudBill;
import io.voyager1.cloud.CloudCredential;
import io.voyager1.core.entity.CostBillEntity;
import io.voyager1.core.repository.CostBillRepository;
import io.voyager1.model.data.CloudAccountModel;
import io.voyager1.model.data.CostBillModel;
import io.voyager1.model.data.CostBudgetModel;
import io.voyager1.service.cloud.CloudService;
import io.voyager1.service.cloud.provider.CloudProviderRegistry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FinOps 成本服务（明细导入 + 分析 + 预算检查）。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA：CRUD 走仓库，聚合分析走 EntityManager 原生查询（列名白名单校验），对外契约不变。
 *
 * @since 2026/8/31
 */
@Service
@Slf4j
public class CostBillService {

    private final CostBillRepository repository;
    private final CloudService cloudService;
    private final CostBudgetService costBudgetService;
    private final CloudProviderRegistry providerRegistry;
    private final EntityManager entityManager;

    public CostBillService(CostBillRepository repository, CloudService cloudService, CostBudgetService costBudgetService,
                           CloudProviderRegistry providerRegistry, EntityManager entityManager) {
        this.repository = repository;
        this.cloudService = cloudService;
        this.costBudgetService = costBudgetService;
        this.providerRegistry = providerRegistry;
        this.entityManager = entityManager;
    }

    /**
     * CSV 导入成本明细。
     */
    @Transactional
    public int importCsv(String accountId, String csvContent) {
        Assert.hasText(accountId, "云账号不能为空");
        Assert.hasText(csvContent, "CSV 内容不能为空");
        CloudAccountModel account = cloudService.getByKey(accountId);
        Assert.notNull(account, "云账号不存在: " + accountId);
        String vendor = account.getVendor();
        String[] lines = csvContent.split("\\r?\\n");
        List<CostBillEntity> entities = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (i == 0 && line.contains("billDate")) {
                continue;
            }
            String[] cols = line.split(",");
            if (cols.length < 7) {
                throw new IllegalArgumentException("第 " + (i + 1) + " 行字段不足（需 billDate,serviceName,resourceId,region,tagKey,tagValue,amount[,currency]）");
            }
            CostBillModel bill = CostBillModel.builder()
                .accountId(accountId)
                .vendor(vendor)
                .billDate(cols[0].trim())
                .serviceName(cols[1].trim())
                .resourceId(cols[2].trim())
                .region(cols[3].trim())
                .tagKey(cols[4].trim())
                .tagValue(cols[5].trim())
                .amount(this.parseAmount(cols[6]))
                .currency(cols.length >= 8 ? cols[7].trim() : "CNY")
                .build();
            entities.add(this.toEntity(bill));
        }
        repository.saveAll(entities);
        return entities.size();
    }

    private Double parseAmount(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("金额格式不正确: " + value);
        }
    }

    /**
     * 从云厂商账单 API 采集账单明细并落库。
     */
    @Transactional
    public int syncBills(String accountId, String billingCycle) {
        CloudAccountModel account = cloudService.getByKey(accountId);
        Assert.notNull(account, "云账号不存在: " + accountId);
        CloudCredential credential = cloudService.decryptCredential(accountId);
        List<CloudBill> bills;
        try {
            bills = providerRegistry.get(credential.getVendor()).listBills(credential, credential.getRegion(), billingCycle);
        } catch (UnsupportedOperationException e) {
            throw new IllegalStateException("该厂商不支持账单采集: " + credential.getVendor());
        } catch (Exception e) {
            throw new IllegalStateException("账单采集失败: " + e.getMessage(), e);
        }
        List<CostBillEntity> entities = new ArrayList<>();
        for (CloudBill bill : bills) {
            CostBillModel model = CostBillModel.builder()
                .accountId(accountId)
                .vendor(account.getVendor())
                .billDate(bill.getBillDate())
                .serviceName(bill.getServiceName())
                .resourceId(bill.getResourceId())
                .region(bill.getRegion())
                .amount(bill.getAmount())
                .currency(bill.getCurrency())
                .build();
            entities.add(this.toEntity(model));
        }
        repository.saveAll(entities);
        return entities.size();
    }

    /**
     * 成本多维分析（按维度聚合）。
     */
    public List<Map<String, Object>> analyze(String groupBy, String startDate, String endDate) {
        String column = this.safeGroupColumn(groupBy);
        StringBuilder sql = new StringBuilder("select ").append(column)
            .append(" as groupKey, sum(amount) as totalAmount from COST_BILL where 1=1");
        List<Object> params = new ArrayList<>();
        if (startDate != null && !startDate.isEmpty()) {
            sql.append(" and billDate >= ?");
            params.add(startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            sql.append(" and billDate <= ?");
            params.add(endDate);
        }
        sql.append(" group by ").append(column).append(" order by totalAmount desc");
        List<Object[]> rows = this.nativeRows(sql.toString(), params.toArray());
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("groupKey", row[0]);
            item.put("totalAmount", row[1]);
            result.add(item);
        }
        return result;
    }

    /**
     * 成本汇总（总金额）。
     */
    public Double totalAmount(String startDate, String endDate) {
        StringBuilder sql = new StringBuilder("select sum(amount) as total from COST_BILL where 1=1");
        List<Object> params = new ArrayList<>();
        if (startDate != null && !startDate.isEmpty()) {
            sql.append(" and billDate >= ?");
            params.add(startDate);
        }
        if (endDate != null && !endDate.isEmpty()) {
            sql.append(" and billDate <= ?");
            params.add(endDate);
        }
        List<Object[]> rows = this.nativeRows(sql.toString(), params.toArray());
        if (rows.isEmpty() || rows.get(0)[0] == null) {
            return 0D;
        }
        return toDouble(rows.get(0)[0]);
    }

    private String safeGroupColumn(String groupBy) {
        if (groupBy == null || groupBy.isEmpty()) {
            return "serviceName";
        }
        switch (groupBy) {
            case "serviceName":
            case "region":
            case "vendor":
            case "accountId":
            case "tagKey":
            case "projectId":
                return groupBy;
            default:
                return "serviceName";
        }
    }

    /**
     * 检查预算超支。
     */
    public List<Map<String, Object>> checkBudget(String month) {
        List<CostBudgetModel> budgets = costBudgetService.list();
        List<Map<String, Object>> result = new ArrayList<>();
        for (CostBudgetModel budget : budgets) {
            Double current = this.currentAmountByScope(budget.getScopeType(), budget.getScopeValue(), month);
            if (current != null && budget.getMonthlyLimit() != null && current > budget.getMonthlyLimit()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("budgetId", budget.getId());
                item.put("name", budget.getName());
                item.put("monthlyLimit", budget.getMonthlyLimit());
                item.put("currentAmount", current);
                item.put("overAmount", current - budget.getMonthlyLimit());
                result.add(item);
            }
        }
        return result;
    }

    private Double currentAmountByScope(String scopeType, String scopeValue, String month) {
        String startDate = month + "-01";
        String endDate = month + "-31";
        StringBuilder sql = new StringBuilder("select sum(amount) as total from COST_BILL where billDate >= ? and billDate <= ?");
        List<Object> params = new ArrayList<>();
        params.add(startDate);
        params.add(endDate);
        if ("account".equals(scopeType)) {
            sql.append(" and accountId = ?");
            params.add(scopeValue);
        } else if ("project".equals(scopeType)) {
            sql.append(" and projectId = ?");
            params.add(scopeValue);
        } else if ("tag".equals(scopeType)) {
            sql.append(" and tagKey = ?");
            params.add(scopeValue);
        }
        List<Object[]> rows = this.nativeRows(sql.toString(), params.toArray());
        if (rows.isEmpty() || rows.get(0)[0] == null) {
            return 0D;
        }
        return toDouble(rows.get(0)[0]);
    }

    /**
     * 成本优化建议：识别长时间停止的闲置云实例。
     */
    public List<Map<String, Object>> listIdleResources() {
        List<Object[]> rows = this.nativeRows("select instanceId, name, regionId from CLOUD_INSTANCE where status = 'Stopped'");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("instanceId", row[0]);
            item.put("name", row[1]);
            item.put("regionId", row[2]);
            item.put("suggestion", "实例长期停止，建议评估是否释放以降低成本");
            result.add(item);
        }
        return result;
    }

    /**
     * 执行原生 SQL（测试清理/运维用），返回影响行数。
     */
    @Transactional
    public int execute(String sql) {
        return entityManager.createNativeQuery(sql).executeUpdate();
    }

    private CostBillEntity toEntity(CostBillModel model) {
        long now = System.currentTimeMillis();
        CostBillEntity entity = new CostBillEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCreateTimeMillis(now);
        entity.setModifyTimeMillis(now);
        entity.setAccountId(model.getAccountId());
        entity.setVendor(model.getVendor());
        entity.setBillDate(model.getBillDate());
        entity.setServiceName(model.getServiceName());
        entity.setResourceId(model.getResourceId());
        entity.setRegion(model.getRegion());
        entity.setTagKey(model.getTagKey());
        entity.setTagValue(model.getTagValue());
        entity.setProjectId(model.getProjectId());
        entity.setAmount(model.getAmount());
        entity.setCurrency(model.getCurrency());
        return entity;
    }

    private List<Object[]> nativeRows(String sql, Object... params) {
        Query query = entityManager.createNativeQuery(sql);
        for (int i = 0; i < params.length; i++) {
            query.setParameter(i + 1, params[i]);
        }
        List<?> raw = query.getResultList();
        List<Object[]> rows = new ArrayList<>();
        for (Object o : raw) {
            if (o instanceof Object[]) {
                rows.add((Object[]) o);
            } else {
                rows.add(new Object[]{o});
            }
        }
        return rows;
    }

    private static Double toDouble(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : Double.parseDouble(value.toString());
    }
}
