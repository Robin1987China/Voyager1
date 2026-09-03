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

package io.voyager1.controller;

import io.voyager1.util.FileUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.util.ResourceUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * robots 接口
 *
 * @since 2022/3/5
 */
@RestController
public class RobotsController {

    @GetMapping(value = "robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public void robots(HttpServletResponse response) {
        URL resource = ResourceUtil.getResource("robots.txt");
        String readString = FileUtil.readString(resource, StandardCharsets.UTF_8);
        JakartaServletUtil.write(response, readString, MediaType.TEXT_PLAIN_VALUE);
    }
}
