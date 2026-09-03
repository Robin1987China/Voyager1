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

package io.voyager1.plugin;

import io.voyager1.util.ConvertUtil;
import io.voyager1.util.ClassUtil;
import com.alibaba.fastjson2.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 插件模块接口
 *
 * @since 2021/12/22
 */
public interface IPlugin extends AutoCloseable {

    /**
     * 数据目录 key
     */
    String DATE_PATH_KEY = "VOYAGER1_DATE_PATH";
    /**
     * Voyager1 版本
     */
    String VOYAGER1_VERSION_KEY = "VOYAGER1_VERSION";


    /**
     * 执行插件方法
     *
     * @param main      拦截到到对象
     * @param parameter 执行方法传人的参数
     * @return 返回值
     * @throws Exception 异常
     */
    Object execute(Object main, Map<String, Object> parameter) throws Exception;

    /**
     * 执行插件方法
     *
     * @param main       主参数
     * @param parameters 其他参数
     * @return 结果
     * @throws Exception 异常
     */
    default Object execute(Object main, Object... parameters) throws Exception {
        // 处理参数
        int length = parameters.length;
        Map<String, Object> map = new HashMap<>(length / 2);
        for (int i = 0; i < length; i += 2) {
            map.put(parameters[i].toString(), parameters[i + 1]);
        }
        return this.execute(main, map);
    }

    /**
     * 执行插件方法
     *
     * @param main       拦截到到对象
     * @param parameters 其他参数
     * @param <T>        泛型
     * @param cls        返回值类型
     * @return 返回值
     * @throws Exception 异常
     */
    default <T> T execute(Object main, Class<T> cls, Object... parameters) throws Exception {
        Object execute = this.execute(main, parameters);
        return this.convertResult(execute, cls);
    }

    /**
     * 执行插件方法
     *
     * @param main      拦截到到对象
     * @param parameter 执行方法传人的参数
     * @param <T>       泛型
     * @param cls       返回值类型
     * @return 返回值
     * @throws Exception 异常
     */
    default <T> T execute(Object main, Map<String, Object> parameter, Class<T> cls) throws Exception {
        Object execute = this.execute(main, parameter);
        return this.convertResult(execute, cls);
    }

    /**
     * 转换结果
     *
     * @param execute 结果
     * @param cls     返回值类型
     * @param <T>     泛型
     * @return 返回值类型
     * @throws Exception 异常
     */
    @SuppressWarnings("unchecked")
    default <T> T convertResult(Object execute, Class<T> cls) {
        if (execute == null) {
            return null;
        }
        Class<?> aClass = execute.getClass();
        if (ClassUtil.isSimpleValueType(aClass)) {
            return (T) ConvertUtil.convert(aClass, execute);
        }
        // json 数据
        if (execute instanceof JSONObject) {
            JSONObject jsonObject = (JSONObject) execute;
            return jsonObject.to(cls);
        }
        return (T) execute;
    }

    /**
     * 系统关闭，插件资源释放
     *
     * @throws Exception 异常
     */
    @Override
    default void close() throws Exception {
    }
}
