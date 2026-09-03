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

package io.voyager1.common.commander;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.CollUtil;
import io.voyager1.util.ArrayUtil;
import io.voyager1.util.ObjectUtil;
import io.voyager1.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 命令操作执行结果
 *
 * @since 2022/11/30
 */
@Data
public class CommandOpResult {

    /**
     * 是否成功
     */
    private Boolean success;
    /**
     * 进程id
     */
    private Integer pid;
    /**
     * 多个进程 id
     */
    private Integer[] pids;
    /**
     * 端口
     */
    private String ports;
    /**
     * 状态消息
     */
    private String statusMsg;
    /**
     * 执行结果
     */
    private final List<String> msgs = new ArrayList<>();

    /**
     * 执行是否成功
     *
     * @return true 成功
     */
    public boolean isSuccess() {
        return success != null && success;
    }

    /**
     * 构建结构对象
     *
     * @param msg 结果消息
     * @return result
     */
    public static CommandOpResult of(String msg) {
        int[] pidsArray = null;
        String ports = null;
        if ((msg != null && msg.startsWith(AbstractProjectCommander.RUNNING_TAG))) {
            List<String> list = io.voyager1.util.ConvertUtil.splitTrim(msg, ":");
            String pids = (1 < list.size() ? list.get(1) : null);
            pidsArray = StrUtil.splitToInt(pids, ",");
            //
            ports = (2 < list.size() ? list.get(2) : null);
        }
        int mainPid = (ArrayUtil.get(pidsArray, 0) != null ? ArrayUtil.get(pidsArray, 0) : 0);
        CommandOpResult result = of(mainPid > 0, msg);
        if (ArrayUtil.length(pidsArray) > 1) {
            // 仅有多个进程号，才返回 pids
            result.pids = ArrayUtil.wrap(pidsArray);
        }
        result.pid = mainPid;
        result.ports = ports;
        result.statusMsg = msg;
        return result;
    }

    public static CommandOpResult of(boolean success) {
        return of(success, (List<String>) null);
    }

    public static CommandOpResult of(boolean success, String msg) {
        CommandOpResult commandOpResult = new CommandOpResult();
        commandOpResult.success = success;
        commandOpResult.appendMsg(msg);
        return commandOpResult;
    }

    public static CommandOpResult of(boolean success, List<String> msg) {
        CommandOpResult commandOpResult = new CommandOpResult();
        commandOpResult.success = success;
        Optional.ofNullable(msg).ifPresent(commandOpResult.msgs::addAll);
        return commandOpResult;
    }

    public CommandOpResult appendMsg(String msg) {
        if ((msg == null || msg.isEmpty())) {
            return this;
        }
        msgs.add(msg);
        return this;
    }

    public CommandOpResult appendMsg(List<String> msgs) {
        for (String msg : msgs) {
            this.appendMsg(msg);
        }
        return this;
    }

    public String msgStr() {
        return String.join(",", msgs);
    }

    public String toString() {
        return JSONObject.toJSONString(this);
    }
}
