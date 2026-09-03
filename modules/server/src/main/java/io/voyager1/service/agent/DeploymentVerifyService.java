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

package io.voyager1.service.agent;

import com.alibaba.fastjson2.JSONObject;
import io.voyager1.model.data.VersionModel;
import io.voyager1.model.enums.VersionStatus;
import io.voyager1.service.version.VersionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 部署验证与回滚（Phase2 自主验证/回滚）
 *
 * @since 2026/8/25
 */
@Service
@Slf4j
public class DeploymentVerifyService {

    private final VersionService versionService;

    public DeploymentVerifyService(VersionService versionService) {
        this.versionService = versionService;
    }

    /**
     * 部署失败自动回滚（打回已提测版本，解锁 CI）
     *
     * @param versionId 版本 id
     * @param reason    失败原因
     * @return 是否已回滚
     */
    public boolean rollbackOnFailure(String versionId, String reason) {
        VersionModel version = versionService.getByKey(versionId);
        if (version == null) {
            log.warn("回滚失败：版本不存在 {}", versionId);
            return false;
        }
        if (version.getStatus() == VersionStatus.Submitted.getCode()) {
            versionService.returnVersion(versionId, "部署失败自动回滚: " + reason);
            log.info("部署失败自动回滚版本: {} {}", versionId, version.getVersion());
            return true;
        }
        log.info("版本 {} 当前状态 {} 无需回滚", versionId, version.getStatus());
        return false;
    }

    /**
     * 部署结果校验（简化：状态为 0 视为成功）
     *
     * @param deployStatus 部署记录状态（0 成功 / 1 失败）
     * @return 校验结果
     */
    public JSONObject verify(int deployStatus) {
        JSONObject result = new JSONObject();
        result.put("success", deployStatus == 0);
        result.put("message", deployStatus == 0 ? "部署成功" : "部署失败");
        return result;
    }
}
