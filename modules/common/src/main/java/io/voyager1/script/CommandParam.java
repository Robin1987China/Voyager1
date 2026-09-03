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

package io.voyager1.script;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import io.voyager1.model.BaseJsonModel;
import io.voyager1.util.Opt;
import io.voyager1.util.StringUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 脚本参数
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CommandParam extends BaseJsonModel {
    /**
     * 参数值
     */
    private String value;
    /**
     * 描述
     */
    private String desc;

    public static String convertToParam(String defArgs) {
        JSONValidator.Type type = StringUtil.validatorJson(defArgs);
        if (type == null || type == JSONValidator.Type.Value) {
            // 旧版本的数据
            List<CommandParam> commandParams = CommandParam.convertLineStr(defArgs);
            return commandParams == null ? null : JSONObject.toJSONString(commandParams);
        } else if (type == JSONValidator.Type.Object) {
            return defArgs;
        } else {
            return defArgs;
        }
    }

    public static String toCommandLine(String params) {
        JSONValidator.Type type = StringUtil.validatorJson(params);
        if (type == null || type == JSONValidator.Type.Value) {
            // 兼容旧数据
            return params;
        }
        List<CommandParam> paramList = params(params);
        return Optional.ofNullable(paramList)
            .map(commandParams -> commandParams.stream()
                .map(CommandParam::getValue)
                .collect(Collectors.joining(" ")))
            .orElse("");
    }

    public static List<String> toCommandList(String params) {
        JSONValidator.Type type = StringUtil.validatorJson(params);
        if (type == null || type == JSONValidator.Type.Value) {
            // 兼容旧数据
            return io.voyager1.util.ConvertUtil.splitTrim(params, " ");
        }
        List<CommandParam> paramList = params(params);
        return Optional.ofNullable(paramList)
            .map(commandParams -> commandParams.stream()
                .map(CommandParam::getValue)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()))
            .orElse(Collections.emptyList());
    }

    public static List<CommandParam> params(String defParams) {
        return StringUtil.jsonConvertArray(defParams, CommandParam.class);
    }

    public static String checkStr(String str) {
        return Opt.ofBlankAble(str)
            .map(s -> {
                List<CommandParam> params = params(s);
                return JSONObject.toJSONString(params);
            }).orElse("");
    }

    public static List<CommandParam> convertLineStr(String defArgs) {
        List<String> list = io.voyager1.util.ConvertUtil.splitTrim(defArgs, " ");
        return Optional.ofNullable(list)
            .map(strings -> {
                List<CommandParam> commandParams1 = new ArrayList<>(strings.size());
                for (int i = 0; i < strings.size(); i++) {
                    CommandParam commandParam = new CommandParam();
                    commandParam.setValue(strings.get(i));
                    commandParam.setDesc("参数" + (i + 1));
                    commandParams1.add(commandParam);
                }
                return commandParams1;
            })
            .orElse(null);
    }

}
