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

package io.voyager1.service;

import io.voyager1.util.BeanUtil;
import io.voyager1.util.CopyOptions;
import io.voyager1.util.CollUtil;
import io.voyager1.util.FileUtil;
import io.voyager1.util.MapUtil;
import io.voyager1.util.LockUtil;
import io.voyager1.util.ClassUtil;
import io.voyager1.util.StrUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import io.voyager1.Voyager1Application;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.model.BaseModel;
import io.voyager1.system.Voyager1RuntimeException;
import io.voyager1.util.JsonFileUtil;
import org.springframework.util.Assert;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

/**
 * 标准操作Service
 *
 * @since 2019/3/14
 */
public abstract class BaseOperService<T extends BaseModel> {

    private final String fileName;
    private final Class<T> typeArgument;
    private final Lock lock = LockUtil.createStampLock().asWriteLock();

    public BaseOperService(String fileName) {
        this.fileName = fileName;
        this.typeArgument = (Class<T>) ClassUtil.getTypeArgument(this.getClass());
    }

    /**
     * 获取所有数据
     *
     * @return list
     */
    public List<T> list() {
        return list(typeArgument);
    }

    public int size() {
        List<T> list = this.list();
        return (list == null ? 0 : list.size());
    }

    public <E> List<E> list(Class<E> cls) {
        JSONObject jsonObject = getJSONObject();
        if (jsonObject == null) {
            return new ArrayList<>();
        }
        JSONArray jsonArray = JsonFileUtil.formatToArray(jsonObject);
        return jsonArray.toJavaList(cls);
    }

    public JSONObject getJSONObject() {
        Objects.requireNonNull(fileName, "没有配置fileName");
        return getJSONObject(fileName);
    }

    /**
     * 工具id 获取 实体
     *
     * @param id 数据id
     * @return T
     */
    public T getItem(String id) {
        Objects.requireNonNull(fileName, "没有配置fileName");
        return getJsonObjectById(fileName, id, typeArgument);
    }


    /**
     * 添加实体
     *
     * @param t 实体
     */
    public void addItem(T t) {
        Objects.requireNonNull(fileName, "没有配置fileName");
        try {
            lock.lock();
            saveJson(fileName, t);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 删除实体
     *
     * @param id 数据id
     */
    public void deleteItem(String id) {
        Objects.requireNonNull(fileName, "没有配置fileName");
        try {
            lock.lock();
            deleteJson(fileName, id);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 修改实体
     *
     * @param t 实体
     */
    public void updateItem(T t) {
        Objects.requireNonNull(fileName, "没有配置fileName");
        try {
            lock.lock();
            updateJson(fileName, t);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 根据数据Id 修改
     *
     * @param updateData 实体
     * @param id         数据Id
     */
    public void updateById(T updateData, String id) {
        Objects.requireNonNull(fileName, "没有配置fileName");
        try {
            lock.lock();
            T item = getItem(id);
            Assert.notNull(item, "数据不存在");
            BeanUtil.copyProperties(updateData, item, CopyOptions.create().ignoreNullValue());
            updateJson(fileName, item);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取数据文件的路径，如果文件不存在，则创建一个
     *
     * @param filename 文件名
     * @return path
     */
    protected String getDataFilePath(String filename) {
        return FileUtil.normalize(Voyager1Application.getInstance().getDataPath() + "/" + filename);
    }

    /**
     * 保存json对象
     *
     * @param filename 文件名
     * @param json     json数据
     */
    protected void saveJson(String filename, BaseModel json) {
        String key = json.getId();
        // 读取文件，如果存在记录，则抛出异常
        JSONObject allData = getJSONObject(filename);
        if (allData != null) {
            // 判断是否存在数据
            if (allData.containsKey(key)) {
                throw new Voyager1RuntimeException(String.format("数据Id已经存在啦：%s : %s", filename, key));
            }
        } else {
            allData = new JSONObject();
        }
        allData.put(key, json.toJson());
        JsonFileUtil.saveJson(getDataFilePath(filename), allData);
    }

    /**
     * 修改json对象
     *
     * @param filename 文件名
     * @param json     json数据
     */
    protected void updateJson(String filename, BaseModel json) {
        String key = json.getId();
        // 读取文件，如果不存在记录，则抛出异常
        JSONObject allData = getJSONObject(filename);
        JSONObject data = allData.getJSONObject(key);

        // 判断是否存在数据
        if (MapUtil.isEmpty(data)) {
            throw new Voyager1RuntimeException("数据不存在:" + key);
        } else {
            allData.put(key, json.toJson());
            JsonFileUtil.saveJson(getDataFilePath(filename), allData);
        }
    }

    /**
     * 删除json对象
     *
     * @param filename 文件
     * @param key      key
     */
    protected void deleteJson(String filename, String key) {
        // 读取文件，如果存在记录，则抛出异常
        JSONObject allData = getJSONObject(filename);
        if (allData == null) {
            return;
        }
        //Assert.notNull(allData, "没有任何数据");
        //JSONObject data = allData.getJSONObject(key);
        allData.remove(key);
        JsonFileUtil.saveJson(getDataFilePath(filename), allData);

    }

    /**
     * 读取整个json文件
     *
     * @param filename 文件名
     * @return json
     */
    protected JSONObject getJSONObject(String filename) {
        try {
            return (JSONObject) JsonFileUtil.readJson(getDataFilePath(filename));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    protected T getJsonObjectById(String file, String id, Class<T> cls) {
        if ((id == null || id.isEmpty())) {
            return null;
        }
        JSONObject jsonObject = getJSONObject(file);
        if (jsonObject == null) {
            return null;
        }
        jsonObject = jsonObject.getJSONObject(id);
        if (jsonObject == null) {
            return null;
        }
        return jsonObject.toJavaObject(cls);
    }
}
