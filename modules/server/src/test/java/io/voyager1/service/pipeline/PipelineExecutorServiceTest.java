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

package io.voyager1.service.pipeline;

import io.voyager1.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.ApplicationStartTest;
import io.voyager1.model.data.PipelineConfigModel;
import io.voyager1.model.data.PipelineExecuteRecordModel;
import io.voyager1.model.enums.PipelineExecuteStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Pipeline 引擎集成测试（exec/approval/审批恢复）
 *
 * @since 2026/8/7
 */
public class PipelineExecutorServiceTest extends ApplicationStartTest {

    @Autowired
    private PipelineConfigService pipelineConfigService;
    @Autowired
    private PipelineExecutorService pipelineExecutorService;
    @Autowired
    private PipelineExecuteRecordService executeRecordService;

    private String createPipelineConfig(String stagesJson) {
        return pipelineConfigService.saveConfig(null, "test-pipeline", "test-build", "[]", stagesJson, true, "测试");
    }

    private String execStageJson() {
        JSONArray array = new JSONArray();
        JSONObject stage = new JSONObject();
        stage.put("id", "exec-1");
        stage.put("type", "exec");
        JSONObject params = new JSONObject();
        params.put("command", "echo pipeline-exec-test");
        stage.put("params", params);
        array.add(stage);
        return array.toJSONString();
    }

    private String execAndApprovalStagesJson() {
        JSONArray array = new JSONArray();
        JSONObject exec = new JSONObject();
        exec.put("id", "exec-1");
        exec.put("type", "exec");
        JSONObject execParams = new JSONObject();
        execParams.put("command", "echo pipeline-exec-test");
        exec.put("params", execParams);
        array.add(exec);
        JSONObject approval = new JSONObject();
        approval.put("id", "approve-1");
        approval.put("type", "approval");
        array.add(approval);
        return array.toJSONString();
    }

    @BeforeEach
    public void clean() {
        io.voyager1.common.BaseServerController.resetInfo(io.voyager1.model.user.UserModel.EMPTY);
        // 清理历史执行记录
        for (PipelineExecuteRecordModel record : executeRecordService.listByPipelineId("test-pipeline")) {
            executeRecordService.delByKey(record.getId());
        }
    }

    private void waitStatus(String executeId, int targetCode, long timeoutMs) throws InterruptedException {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            PipelineExecuteRecordModel record = executeRecordService.getByKey(executeId);
            if (record != null && record.getStatus() == targetCode) {
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
        PipelineExecuteRecordModel record = executeRecordService.getByKey(executeId);
        Assertions.fail("等待状态超时, 当前状态: " + (record == null ? "无记录" : record.getStatus()));
    }

    @Test
    public void testExecStageSuccess() throws InterruptedException {
        String pipelineId = createPipelineConfig(execStageJson());
        pipelineExecutorService.trigger(pipelineId, "manual", "tester");
        // 等待执行完成（exec 应快速成功）
        java.util.List<PipelineExecuteRecordModel> records = executeRecordService.listByPipelineId(pipelineId);
        Assertions.assertEquals(1, records.size());
        waitStatus(records.get(0).getId(), PipelineExecuteStatus.Success.getCode(), 20000);
        PipelineExecuteRecordModel record = executeRecordService.getByKey(records.get(0).getId());
        Assertions.assertEquals(PipelineExecuteStatus.Success.getCode(), record.getStatus());
        Assertions.assertNotNull(record.getStages());
        Assertions.assertTrue(record.getStages().contains("exec-1"));
    }

    @Test
    public void testApprovalWaitAndResume() throws InterruptedException {
        String pipelineId = createPipelineConfig(execAndApprovalStagesJson());
        pipelineExecutorService.trigger(pipelineId, "manual", "tester");
        java.util.List<PipelineExecuteRecordModel> records = executeRecordService.listByPipelineId(pipelineId);
        Assertions.assertEquals(1, records.size());
        String executeId = records.get(0).getId();
        // 等待挂起（WaitApproval）
        waitStatus(executeId, PipelineExecuteStatus.WaitApproval.getCode(), 20000);
        // 审批通过 → 继续执行并完成
        pipelineExecutorService.approval(executeId, true, "approver");
        waitStatus(executeId, PipelineExecuteStatus.Success.getCode(), 20000);
        PipelineExecuteRecordModel record = executeRecordService.getByKey(executeId);
        Assertions.assertEquals(PipelineExecuteStatus.Success.getCode(), record.getStatus());
    }

    @Test
    public void testApprovalDeny() throws InterruptedException {
        String pipelineId = createPipelineConfig(execAndApprovalStagesJson());
        pipelineExecutorService.trigger(pipelineId, "manual", "tester");
        java.util.List<PipelineExecuteRecordModel> records = executeRecordService.listByPipelineId(pipelineId);
        String executeId = records.get(0).getId();
        waitStatus(executeId, PipelineExecuteStatus.WaitApproval.getCode(), 20000);
        // 审批拒绝 → 取消
        pipelineExecutorService.approval(executeId, false, "approver");
        waitStatus(executeId, PipelineExecuteStatus.Cancel.getCode(), 20000);
    }

    @Test
    public void testParseStages() {
        String pipelineId = createPipelineConfig(execAndApprovalStagesJson());
        PipelineConfigModel model = pipelineConfigService.getByKey(pipelineId);
        java.util.List<PipelineConfigService.PipelineStage> stages = pipelineConfigService.parseStages(model);
        Assertions.assertEquals(2, stages.size());
        Assertions.assertEquals("exec-1", stages.get(0).getId());
        Assertions.assertEquals("approval", stages.get(1).getType());
    }
}
