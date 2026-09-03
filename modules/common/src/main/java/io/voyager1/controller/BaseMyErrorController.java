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

import io.voyager1.core.api.ApiResult;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.servlet.error.AbstractErrorController;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @see org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController
 */
@Slf4j
public abstract class BaseMyErrorController extends AbstractErrorController {

    public static final Supplier<String> FILE_MAX_SIZE_MSG = () -> "上传文件太大了,请重新选择一个较小的文件上传吧";

    public BaseMyErrorController(ErrorAttributes errorAttributes) {
        super(errorAttributes);
    }

    @RequestMapping
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        HttpStatus status = getStatus(request);
        if (status == HttpStatus.NO_CONTENT) {
            return new ResponseEntity<>(status);
        }
        Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        // 判断异常信息
        Object attribute = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Map<String, Object> body = new HashMap<>(5);
        body.put(ApiResult.CODE, HttpStatus.INTERNAL_SERVER_ERROR.value());
        String msg = "啊哦，好像哪里出错了，请稍候再试试吧~";
        if (attribute instanceof MaxUploadSizeExceededException) {
            // 上传文件大小异常
            msg = FILE_MAX_SIZE_MSG.get();
            log.error("发生文件上传异常：{}  {}", statusCode, requestUri);
        } else if (status == HttpStatus.NOT_FOUND) {
            msg = "没有找到对应的资源";
            body.put(ApiResult.DATA, requestUri);
        } else {
            log.error("发生异常：{}  {}", statusCode, requestUri);
        }
        body.put(ApiResult.MSG, msg);

        return new ResponseEntity<>(body, HttpStatus.OK);
    }
}
