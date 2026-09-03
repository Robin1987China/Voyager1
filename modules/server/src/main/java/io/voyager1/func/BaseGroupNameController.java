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

package io.voyager1.func;

import io.voyager1.core.api.ApiResult;
import io.voyager1.common.BaseServerController;
import io.voyager1.core.jpa.JpaBaseService;
import io.voyager1.model.BaseGroupNameModel;
import io.voyager1.permission.Feature;
import io.voyager1.permission.MethodFeature;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collection;

/**
 * @since 2023/2/25
 */
public abstract class BaseGroupNameController extends BaseServerController {

    protected final JpaBaseService<? extends BaseGroupNameModel, ?> dbService;

    protected BaseGroupNameController(JpaBaseService<? extends BaseGroupNameModel, ?> dbService) {
        this.dbService = dbService;
    }


    @GetMapping(value = "list-group", produces = MediaType.APPLICATION_JSON_VALUE)
    @Feature(method = MethodFeature.LIST)
    public ApiResult<Collection<String>> listGroup() {
        Collection<String> list = dbService.listGroupName();
        return ApiResult.success("", list);
    }
}
