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

package io.voyager1.git;

import io.voyager1.plugin.IPlugin;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.ApplicationStartTest;
import io.voyager1.plugin.PluginFactory;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * @since 2023/4/10
 */
@Slf4j
public class TestGit extends ApplicationStartTest {


    @Test
    public void test1() {
        System.out.println(1);
    }

    @Test
    public void test() throws Exception {
        IPlugin plugin = PluginFactory.getPlugin("git-clone");
        Map<String, Object> map = new HashMap<>();
//        map.put("gitProcessType", "JGit");
        map.put("gitProcessType", "SystemGit");
        map.put("url", "https://github.com/octocat/Hello-World.git");
//        map.put("url", "https://github.com/octocat/Hello-World");
//        map.put("rsaFile", FileUtil.file(FileUtil.getUserHomePath(), ".ssh", "id_rsa"));
        map.put("reduceProgressRatio", 1);
        map.put("timeout", 60);
        map.put("protocol", 1);
        map.put("username", "");
        map.put("password", "");
        Object obj = plugin.execute("branchAndTagList", map);
        System.err.println(obj);
    }
}
