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

package io.voyager1.func.files.service;

import com.alibaba.fastjson2.JSONObject;
import io.voyager1.core.entity.FileReleaseTaskTemplateEntity;
import io.voyager1.core.jpa.JpaWorkspaceService;
import io.voyager1.core.repository.FileReleaseTaskTemplateRepository;
import io.voyager1.func.files.model.FileReleaseTaskTemplate;
import io.voyager1.func.files.model.IFileStorage;
import io.voyager1.core.jpa.JpaQuerySupport;
import io.voyager1.util.DigestUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;

/**
 * 文件发布模板服务。
 * <p>
 * 已从承继存储框架（BaseWorkspaceService）搬家到 JPA（JpaWorkspaceService + FileReleaseTaskTemplateRepository），对外契约不变。
 *
 * @since 2025/1/9
 */
@Service
public class FileReleaseTaskTemplateService extends JpaWorkspaceService<FileReleaseTaskTemplate, FileReleaseTaskTemplateEntity> {

    private final FileReleaseTaskTemplateRepository repository;

    public FileReleaseTaskTemplateService(FileReleaseTaskTemplateRepository repository) {
        this.repository = repository;
    }

    @Override
    protected JpaRepository<FileReleaseTaskTemplateEntity, String> repository() {
        return repository;
    }

    @Override
    protected JpaSpecificationExecutor<FileReleaseTaskTemplateEntity> specExecutor() {
        return repository;
    }

    @Override
    protected Class<FileReleaseTaskTemplateEntity> entityClass() {
        return FileReleaseTaskTemplateEntity.class;
    }

    @Override
    protected Class<FileReleaseTaskTemplate> modelClass() {
        return FileReleaseTaskTemplate.class;
    }

    public FileReleaseTaskTemplate getTemplate(String workspaceId, Integer fileType, String templateTag) {
        Map<String, String> pm = new HashMap<>();
        pm.put("workspaceId", workspaceId);
        if (fileType != null && (fileType == 1 || fileType == 2)) {
            pm.put("fileType", String.valueOf(fileType));
        }
        pm.put("templateTag", templateTag);
        org.springframework.data.domain.Page<FileReleaseTaskTemplateEntity> page = specExecutor().findAll(
            JpaQuerySupport.specification(pm),
            PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "modifyTimeMillis")
                .and(Sort.by(Sort.Direction.DESC, "createTimeMillis"))));
        return page.getContent().isEmpty() ? null : toModel(page.getContent().get(0));
    }

    public void add(String type, String workspaceId, IFileStorage storage, Integer fileType, JSONObject data) {
        String templateTag;
        String nameTag;
        switch (type) {
            case "id":
                templateTag = "id:" + storage.getId();
                nameTag = "ID";
                break;
            case "alias":
                String aliasCode = storage.getAliasCode();
                if ((aliasCode == null || aliasCode.isEmpty())) {
                    templateTag = "id:" + storage.getId();
                    nameTag = "ID";
                } else {
                    templateTag = "alias:" + aliasCode;
                    nameTag = "别名";
                }
                break;
            default:
                return;
        }
        String dataId = DigestUtil.sha1(fileType + templateTag + workspaceId);
        FileReleaseTaskTemplate fileReleaseTaskTemplate = new FileReleaseTaskTemplate();
        fileReleaseTaskTemplate.setId(dataId);
        fileReleaseTaskTemplate.setWorkspaceId(workspaceId);
        fileReleaseTaskTemplate.setFileType(fileType);
        fileReleaseTaskTemplate.setName(storage.getName() + "-" + nameTag + "发布模板");
        fileReleaseTaskTemplate.setData(data.toJSONString());
        fileReleaseTaskTemplate.setTemplateTag(templateTag);
        this.upsert(fileReleaseTaskTemplate);
    }
}
