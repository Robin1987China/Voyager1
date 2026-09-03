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

package io.voyager1.exception;

import io.voyager1.controller.BaseMyErrorController;
import io.voyager1.core.api.ApiResult;
import io.voyager1.system.Voyager1RuntimeException;
import io.voyager1.util.ExceptionUtil;
import io.voyager1.util.MoveVisitor;
import io.voyager1.util.SystemUtil;
import io.voyager1.util.ValidateException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.nio.file.AccessDeniedException;


@Slf4j
public abstract class BaseExceptionHandler {

    /**
     * 声明要捕获的异常
     *
     * @param request 请求
     * @param e       异常
     */
    @ExceptionHandler({Voyager1RuntimeException.class, RuntimeException.class, Exception.class})
    @ResponseBody
    public ApiResult<String> defExceptionHandler(HttpServletRequest request, Exception e) {
        if (e instanceof Voyager1RuntimeException) {
            log.error("global handle exception: {} {}", request.getRequestURI(), e.getMessage(), e.getCause());
            return new ApiResult<>(500, e.getMessage());
        } else {
            log.error("global handle exception: {}", request.getRequestURI(), e);
            boolean causedBy = ExceptionUtil.isCausedBy(e, AccessDeniedException.class);
            if (causedBy) {
                return new ApiResult<>(500, "操作文件权限异常,请手动处理：" + e.getMessage());
            }
            return new ApiResult<>(500, "服务异常：" + e.getMessage());
        }
    }

    @ExceptionHandler({NullPointerException.class})
    @ResponseBody
    public ApiResult<String> defNullPointerExceptionHandler(HttpServletRequest request, Exception e) {
        log.error("global NullPointerException: {}", request.getRequestURI(), e);
        String voyager1Type = (System.getenv("VOYAGER1_TYPE") != null ? System.getenv("VOYAGER1_TYPE") : (System.getProperty("VOYAGER1_TYPE") != null ? System.getProperty("VOYAGER1_TYPE") : ""));
        return new ApiResult<>(500, voyager1Type + "程序错误,空指针");
    }

    /**
     * 声明要捕获的异常 (参数或者状态异常)
     *
     * @param request 请求
     * @param e       异常
     * @see MoveVisitor
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class, ValidateException.class})
    @ResponseBody
    public ApiResult<String> paramExceptionHandler(HttpServletRequest request, Exception e) {
        String message = e.getMessage();
        if (log.isDebugEnabled()) {
            log.debug("controller  {}", request.getRequestURI(), e);
        } else {
            log.warn("controller {} {}", request.getRequestURI(), message);
        }
        //
        if (SystemUtil.getOsInfo().isWindows()) {
            // Target must be a directory
            if ((message != null && message.toLowerCase().contains("Target must be a directory".toLowerCase()))) {
                String s = "当前系统环境为 windows 请检查对应操作文件是否被其他进程占用或者停止运行对应项目后再试";
                return new ApiResult<>(405, message + " " + s);
            }
        }
        return new ApiResult<>(405, message);
    }


    @ExceptionHandler({HttpMessageNotReadableException.class, HttpMessageConversionException.class})
    @ResponseBody
    public ApiResult<String> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("参数解析异常:{}", e.getMessage());
        return new ApiResult<>(HttpStatus.EXPECTATION_FAILED.value(), "传入的参数格式不正确");
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeNotSupportedException.class})
    @ResponseBody
    public ApiResult<String> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        return new ApiResult<>(HttpStatus.METHOD_NOT_ALLOWED.value(), "不被支持的请求方式", e.getMessage());
    }

    @ExceptionHandler({NoHandlerFoundException.class})
    @ResponseBody
    public ApiResult<String> handleNoHandlerFoundException(NoHandlerFoundException e) {
        return new ApiResult<>(HttpStatus.NOT_FOUND.value(), "没有找到对应的资源", e.getMessage());
    }

    /**
     * 上传文件大小超出限制
     *
     * @param e 异常
     */
    @ExceptionHandler({MaxUploadSizeExceededException.class})
    @ResponseBody
    public ApiResult<String> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.error("上传文件大小超出限制", e);
        return new ApiResult<>(HttpStatus.NOT_ACCEPTABLE.value(), BaseMyErrorController.FILE_MAX_SIZE_MSG.get(), e.getMessage());
    }

    @ExceptionHandler({ConstructorException.class})
    @ResponseBody
    public ApiResult<String> handleConstructorException(ConstructorException e) {
        log.warn("yml 配置内容错误", e);
        return new ApiResult<>(HttpStatus.EXPECTATION_FAILED.value(), "yml 配置内容格式有误请检查后重新操作（请检查是否有非法字段）：" + e.getMessage());
    }

    @ExceptionHandler({ScannerException.class})
    @ResponseBody
    public ApiResult<String> handleScannerException(ScannerException e) {
        log.warn("ScannerException", e);
        return new ApiResult<>(HttpStatus.EXPECTATION_FAILED.value(), "yml 配置内容格式有误请检查后重新操作（不要使用 \t(TAB) 缩进）：" + e.getMessage());
    }

}
