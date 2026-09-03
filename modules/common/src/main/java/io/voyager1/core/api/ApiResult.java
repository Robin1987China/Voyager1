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

package io.voyager1.core.api;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriterImplToString;
import com.alibaba.fastjson2.writer.ObjectWriterProvider;

import java.io.Serializable;
import java.math.BigInteger;

/**
 * 统一 API 响应体（原创实现）。
 * <p>
 * 对外契约：{@code {"code":200,"msg":"...","data":...}}；成功码 {@code 200}，失败码 {@code 405}；
 * {@code Long} 序列化为字符串、枚举按名称序列化（与旧实现行为一致，保证前后端契约不变）。
 */
public final class ApiResult<T> implements Serializable {

    public static final String CODE = "code";
    public static final String MSG = "msg";
    public static final String DATA = "data";

    public static final int DEFAULT_SUCCESS_CODE = 200;
    public static final int DEFAULT_FAIL_CODE = 405;

    static {
        ObjectWriterProvider writerProvider = JSONFactory.getDefaultObjectWriterProvider();
        // long 类型自动转字符串，避免前端精度丢失
        writerProvider.register(Long.class, ObjectWriterImplToString.INSTANCE);
        writerProvider.register(long.class, ObjectWriterImplToString.INSTANCE);
        writerProvider.register(BigInteger.class, ObjectWriterImplToString.INSTANCE);
        writerProvider.register(Long.TYPE, ObjectWriterImplToString.INSTANCE);
        JSONFactory.setUseJacksonAnnotation(false);
        JSON.config(JSONWriter.Feature.WriteEnumsUsingName);
    }

    private int code;
    private String msg;
    private T data;

    public ApiResult() {
    }

    public ApiResult(int code, String msg) {
        this(code, msg, null);
    }

    public ApiResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    /**
     * 是否成功（code == 200）。
     */
    public boolean success() {
        return this.code == DEFAULT_SUCCESS_CODE;
    }

    /**
     * 是否失败（code != 200）。
     */
    public boolean fail() {
        return this.code != DEFAULT_SUCCESS_CODE;
    }

    /**
     * 将 data 转换为指定类型。
     */
    public <E> E getData(Class<E> type) {
        return JSON.to(type, this.data);
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }

    public JSONObject toJson() {
        return (JSONObject) JSON.toJSON(this);
    }

    // ---------- 静态工厂 ----------

    public static <T> ApiResult<T> success(String msg) {
        return success(msg, (T) null);
    }

    public static <T> ApiResult<T> success(String msg, T data) {
        return new ApiResult<>(DEFAULT_SUCCESS_CODE, msg, data);
    }

    public static <T> ApiResult<T> fail(String msg) {
        return fail(msg, (T) null);
    }

    public static <T> ApiResult<T> fail(String msg, T data) {
        return new ApiResult<>(DEFAULT_FAIL_CODE, msg, data);
    }

    public static <T> ApiResult<T> success(String template, Object... args) {
        return success(String.format(template, args), (T) null);
    }

    public static <T> ApiResult<T> fail(String template, Object... args) {
        return fail(String.format(template, args), (T) null);
    }

    // ---------- JSON 辅助 ----------

    public static JSONObject toJson(int code, String msg) {
        return toJson(code, msg, null);
    }

    public static <T> JSONObject toJson(int code, String msg, T data) {
        return new ApiResult<>(code, msg, data).toJson();
    }

    public static String getString(int code, String msg) {
        return getString(code, msg, null);
    }

    public static String getString(int code, String msg, Object data) {
        return toJson(code, msg, data).toString();
    }
}
