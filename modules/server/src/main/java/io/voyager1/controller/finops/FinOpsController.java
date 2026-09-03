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

package io.voyager1.controller.finops;

import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.model.data.CostBudgetModel;
import io.voyager1.model.data.CostTagRuleModel;
import io.voyager1.permission.ClassFeature;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.finops.CostBillService;
import io.voyager1.service.finops.CostBudgetService;
import io.voyager1.service.finops.CostTagRuleService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * FinOps 成本管理 API
 *
 * @since 2026/8/31
 */
@RestController
@RequestMapping(value = "/finops")
@Feature(cls = ClassFeature.SYSTEM_ASSETS_MACHINE)
public class FinOpsController extends BaseServerController {

    private final CostBillService costBillService;
    private final CostTagRuleService costTagRuleService;
    private final CostBudgetService costBudgetService;

    public FinOpsController(CostBillService costBillService,
                            CostTagRuleService costTagRuleService,
                            CostBudgetService costBudgetService) {
        this.costBillService = costBillService;
        this.costTagRuleService = costTagRuleService;
        this.costBudgetService = costBudgetService;
    }

    /**
     * CSV 导入成本明细
     */
    @PostMapping(value = "bill/import", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Integer> importBill(String accountId, String csvContent) {
        return ApiResult.success("导入成功", costBillService.importCsv(accountId, csvContent));
    }

    /**
     * 从云厂商账单 API 采集账单明细
     */
    @PostMapping(value = "bill/sync", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<Integer> syncBill(String accountId, String billingCycle) {
        return ApiResult.success("采集完成", costBillService.syncBills(accountId, billingCycle));
    }

    /**
     * 成本多维分析
     */
    @PostMapping(value = "bill/analyze", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<Map<String, Object>>> analyze(String groupBy, String startDate, String endDate) {
        return ApiResult.success("", costBillService.analyze(groupBy, startDate, endDate));
    }

    /**
     * 成本汇总
     */
    @PostMapping(value = "bill/total", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<Double> total(String startDate, String endDate) {
        return ApiResult.success("", costBillService.totalAmount(startDate, endDate));
    }

    /**
     * 保存标签分摊规则
     */
    @PostMapping(value = "tag-rule/save", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> saveTagRule(String id, String vendor, String tagKey, String tagValue, String projectId, String projectName) {
        CostTagRuleModel rule = CostTagRuleModel.builder()
            .vendor(vendor)
            .tagKey(tagKey)
            .tagValue(tagValue)
            .projectId(projectId)
            .projectName(projectName)
            .build();
        rule.setId(id);
        return ApiResult.success("保存成功", costTagRuleService.save(rule));
    }

    /**
     * 标签分摊规则列表
     */
    @PostMapping(value = "tag-rule/list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<CostTagRuleModel>> listTagRules() {
        return ApiResult.success("", costTagRuleService.list());
    }

    /**
     * 删除标签分摊规则
     */
    @PostMapping(value = "tag-rule/delete", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> deleteTagRule(String id) {
        costTagRuleService.delete(id);
        return ApiResult.success("删除成功", id);
    }

    /**
     * 保存预算
     */
    @PostMapping(value = "budget/save", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> saveBudget(String id, String name, String scopeType, String scopeValue, Double monthlyLimit) {
        CostBudgetModel budget = CostBudgetModel.builder()
            .name(name)
            .scopeType(scopeType)
            .scopeValue(scopeValue)
            .monthlyLimit(monthlyLimit)
            .build();
        budget.setId(id);
        return ApiResult.success("保存成功", costBudgetService.save(budget));
    }

    /**
     * 预算列表
     */
    @PostMapping(value = "budget/list", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<CostBudgetModel>> listBudgets() {
        return ApiResult.success("", costBudgetService.list());
    }

    /**
     * 删除预算
     */
    @PostMapping(value = "budget/delete", produces = "application/json")
    @Feature(method = MethodFeature.EDIT)
    public ApiResult<String> deleteBudget(String id) {
        costBudgetService.delete(id);
        return ApiResult.success("删除成功", id);
    }

    /**
     * 预算超支检查
     */
    @PostMapping(value = "budget/check", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<Map<String, Object>>> checkBudget(String month) {
        return ApiResult.success("", costBillService.checkBudget(month));
    }

    /**
     * 成本优化建议（闲置资源识别）
     */
    @PostMapping(value = "optimize/idle", produces = "application/json")
    @Feature(method = MethodFeature.LIST)
    public ApiResult<List<Map<String, Object>>> idleResources() {
        return ApiResult.success("", costBillService.listIdleResources());
    }
}
