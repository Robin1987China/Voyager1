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

package io.voyager1.common.forward;

import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.BytesResource;
import org.springframework.util.unit.DataSize;
import io.voyager1.util.Opt;
import io.voyager1.util.MapUtil;
import io.voyager1.util.EnumUtil;
import io.voyager1.util.IdUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.DigestUtil;
import io.voyager1.util.JakartaServletUtil;
import io.voyager1.common.SpringContextHolder;
import io.voyager1.model.BaseIdModel;
import io.voyager1.core.api.ApiResult;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import lombok.Lombok;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.BaseServerController;
import io.voyager1.common.Const;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.configuration.NodeConfig;
import io.voyager1.exception.AgentAuthorizeException;
import io.voyager1.exception.AgentException;
import io.voyager1.func.assets.model.MachineNodeModel;
import io.voyager1.func.assets.server.MachineNodeServer;
import io.voyager1.model.data.NodeModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.system.ServerConfig;
import io.voyager1.transport.*;
import io.voyager1.util.StrictSyncFinisher;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 节点请求转发
 *
 * @since 2019/4/16
 */
@Slf4j
public class NodeForward {

    /**
     * 创建代理
     *
     * @param type      代理类型
     * @param httpProxy 代理地址
     * @return proxy
     */
    public static Proxy crateProxy(String type, String httpProxy) {
        if ((httpProxy != null && !httpProxy.isEmpty())) {
            List<String> split = io.voyager1.util.ConvertUtil.splitTrim(httpProxy, ":");
            String host = (split == null || split.isEmpty() ? null : split.get(0));
            int port = ConvertUtil.toInt((split == null || split.isEmpty() ? null : split.get(split.size() - 1)), 0);
            Proxy.Type type1 = EnumUtil.fromString(Proxy.Type.class, type, Proxy.Type.HTTP);
            return new Proxy(type1, new InetSocketAddress(host, port));
        }
        return null;
    }

    public static INodeInfo parseNodeInfo(NodeModel nodeModel) {
        Assert.hasText(nodeModel.getMachineId(), "节点信息不完整,缺少机器id");
        MachineNodeServer machineNodeServer = SpringContextHolder.getBean(MachineNodeServer.class);
        MachineNodeModel model = machineNodeServer.getByKey(nodeModel.getMachineId(), false);
        Assert.notNull(model, "对应的机器信息不存在");
        return model;
    }

    public static INodeInfo coverNodeInfo(MachineNodeModel machineNodeModel) {
        if ((machineNodeModel.getId() == null || machineNodeModel.getId().isEmpty())) {
            // 新增的情况
            return machineNodeModel;
        }
        MachineNodeServer machineNodeServer = SpringContextHolder.getBean(MachineNodeServer.class);
        MachineNodeModel model = machineNodeServer.getByKey(machineNodeModel.getId(), false);
        Optional.ofNullable(model)
            .ifPresent(exits -> {
                String password = Opt.ofBlankAble(machineNodeModel.getVoyager1Password()).orElse(exits.getVoyager1Password());
                machineNodeModel.setVoyager1Password(password);
            });
        return machineNodeModel;
    }

    /**
     * 创建节点 url
     *
     * @param iNodeInfo       节点信息
     * @param nodeUrl         节点功能 url
     * @param dataContentType 传输的数据类型
     */
    public static IUrlItem parseUrlItem(INodeInfo iNodeInfo, String workspaceId, NodeUrl nodeUrl, DataContentType dataContentType) {
        //
        Map<String, String> header = NodeForward.createHeader();
        return new DefaultUrlItem(nodeUrl, iNodeInfo.timeout(), workspaceId, dataContentType, header);
    }

    /**
     * 创建节点 url
     *
     * @param iNodeInfo 节点信息
     * @param nodeUrl   节点功能 url
     */
    public static IUrlItem parseUrlItem(INodeInfo iNodeInfo, String workspaceId, NodeUrl nodeUrl) {
        //
        Map<String, String> header = NodeForward.createHeader();
        return new DefaultUrlItem(nodeUrl, iNodeInfo.timeout(), workspaceId, DataContentType.FORM_URLENCODED, header);
    }

    /**
     * 创建节点 url
     *
     * @param nodeModel       节点信息
     * @param nodeUrl         节点功能 url
     * @param dataContentType 传输的数据类型
     */
    public static <T> T createUrlItem(NodeModel nodeModel, NodeUrl nodeUrl, DataContentType dataContentType, BiFunction<INodeInfo, IUrlItem, T> consumer) {
        INodeInfo parseNodeInfo = parseNodeInfo(nodeModel);
        Map<String, String> header = NodeForward.createHeader();
        //
        IUrlItem iUrlItem = new DefaultUrlItem(nodeUrl, parseNodeInfo.timeout(), nodeModel.getWorkspaceId(), dataContentType, header);
        return consumer.apply(parseNodeInfo, iUrlItem);
    }

    private static <T> T createUrlItem(NodeModel nodeModel, NodeUrl nodeUrl, BiFunction<INodeInfo, IUrlItem, T> consumer) {
        return createUrlItem(nodeModel, nodeUrl, DataContentType.FORM_URLENCODED, consumer);
    }

    private static <T> T createUrlItem(INodeInfo nodeInfo, String workspaceId, NodeUrl nodeUrl, BiFunction<INodeInfo, IUrlItem, T> consumer) {
        return createUrlItem(nodeInfo, workspaceId, nodeUrl, DataContentType.FORM_URLENCODED, consumer);
    }

    private static <T> T createUrlItem(INodeInfo nodeInfo, String workspaceId, NodeUrl nodeUrl, DataContentType dataContentType, BiFunction<INodeInfo, IUrlItem, T> consumer) {
        //
        Map<String, String> header = NodeForward.createHeader();
        IUrlItem iUrlItem = new DefaultUrlItem(nodeUrl, nodeInfo.timeout(), workspaceId, dataContentType, header);
        return consumer.apply(nodeInfo, iUrlItem);
    }

    private static Map<String, String> createHeader() {
        Map<String, String> header = new HashMap<>();
        UserModel userByThreadLocal = BaseServerController.getUserByThreadLocal();
        header.put(Const.VOYAGER1_SERVER_USER_NAME, Optional.ofNullable(userByThreadLocal).map(BaseIdModel::getId).orElse(""));
        // 语言（无请求上下文或客户端未带 Accept-Language 时为 null，HttpClient 不允许 null 头）
        String language = I18nMessageUtil.getLanguageByRequest();
        if (language != null && !language.isEmpty()) {
            header.put(HttpHeaders.ACCEPT_LANGUAGE, language);
        }
        return header;
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @param <T>       泛型
     * @return JSON
     */
    public static <T> ApiResult<T> request(NodeModel nodeModel, HttpServletRequest request, NodeUrl nodeUrl, String... removeKeys) {
        return request(nodeModel, request, nodeUrl, removeKeys, new String[]{});
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @param <T>       泛型
     * @return JSON
     */
    public static <T> ApiResult<T> request(NodeModel nodeModel, HttpServletRequest request, NodeUrl nodeUrl, String[] removeKeys, String... appendData) {
        Map<String, String> map = Optional.ofNullable(request)
            .map(JakartaServletUtil::getParamMap)
            .map(map1 -> MapUtil.removeAny(map1, removeKeys))
            .map(map2 -> {
                for (int i = 0; i < appendData.length; i += 2) {
                    map2.put(appendData[i], appendData[i + 1]);
                }
                return map2;
            })
            .orElse(null);

        TypeReference<ApiResult<T>> tTypeReference = new TypeReference<ApiResult<T>>() {
        };
        return createUrlItem(nodeModel, nodeUrl,
            (nodeInfo, urlItem) ->
                TransportServerFactory.get().executeToType(nodeInfo, urlItem, map, tTypeReference)
        );
    }

    /**
     * 普通消息转发
     *
     * @param machineNodeModel 机器
     * @param request          请求
     * @param nodeUrl          节点的url
     * @param <T>              泛型
     * @return JSON
     */
    public static <T> ApiResult<T> request(MachineNodeModel machineNodeModel, HttpServletRequest request, NodeUrl nodeUrl, String... removeKeys) {
        return request(machineNodeModel, request, nodeUrl, removeKeys, new String[]{});
    }

    /**
     * 普通消息转发
     *
     * @param machineNodeModel 机器
     * @param request          请求
     * @param nodeUrl          节点的url
     * @param <T>              泛型
     * @return JSON
     */
    public static <T> ApiResult<T> request(MachineNodeModel machineNodeModel, HttpServletRequest request, NodeUrl nodeUrl, String[] removeKeys, String... appendData) {
        Map<String, String> map = Optional.ofNullable(request)
            .map(JakartaServletUtil::getParamMap)
            .map(map1 -> MapUtil.removeAny(map1, removeKeys))
            .map(map2 -> {
                for (int i = 0; i < appendData.length; i += 2) {
                    map2.put(appendData[i], appendData[i + 1]);
                }
                return map2;
            })
            .orElse(null);
        TypeReference<ApiResult<T>> tTypeReference = new TypeReference<ApiResult<T>>() {
        };
        INodeInfo nodeInfo1 = coverNodeInfo(machineNodeModel);
        return createUrlItem(nodeInfo1, "", nodeUrl,
            (nodeInfo, urlItem) ->
                TransportServerFactory.get().executeToType(nodeInfo, urlItem, map, tTypeReference)
        );
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel  节点
     * @param nodeUrl    节点的url
     * @param jsonObject 数据
     * @return JSON
     */
    public static <T> ApiResult<T> request(NodeModel nodeModel, NodeUrl nodeUrl, JSONObject jsonObject) {
        TypeReference<ApiResult<T>> tTypeReference = new TypeReference<ApiResult<T>>() {
        };
        return createUrlItem(nodeModel, nodeUrl, (nodeInfo, urlItem) -> TransportServerFactory.get().executeToType(nodeInfo, urlItem, jsonObject, tTypeReference));
    }

    /**
     * 普通消息转发
     *
     * @param machineNodeModel 节点
     * @param nodeUrl          节点的url
     * @param jsonObject       数据
     * @return JSON
     */
    public static <T> ApiResult<T> request(MachineNodeModel machineNodeModel, NodeUrl nodeUrl, JSONObject jsonObject) {
        TypeReference<ApiResult<T>> typeReference = new TypeReference<ApiResult<T>>() {
        };
        INodeInfo nodeInfo = coverNodeInfo(machineNodeModel);
        return createUrlItem(nodeInfo, "", nodeUrl, (nodeInfo1, urlItem) -> TransportServerFactory.get().executeToType(nodeInfo1, urlItem, jsonObject, typeReference));
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel  节点
     * @param nodeUrl    节点的url
     * @param jsonObject 数据
     * @return JSON
     */
    public static <T> ApiResult<T> requestSharding(NodeModel nodeModel, NodeUrl nodeUrl, JSONObject jsonObject, File file, Function<JSONObject, ApiResult<T>> doneCallback, BiConsumer<Long, Long> streamProgress) throws IOException {
        INodeInfo nodeInfo = parseNodeInfo(nodeModel);
        return requestSharding(nodeInfo, nodeModel.getWorkspaceId(), nodeUrl, jsonObject, file, File::getName, doneCallback, streamProgress);
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel  节点
     * @param nodeUrl    节点的url
     * @param jsonObject 数据
     * @return JSON
     */
    public static <T> ApiResult<T> requestSharding(NodeModel nodeModel, NodeUrl nodeUrl, JSONObject jsonObject, File file, String fileName, Function<JSONObject, ApiResult<T>> doneCallback, BiConsumer<Long, Long> streamProgress) throws IOException {
        INodeInfo nodeInfo = parseNodeInfo(nodeModel);
        return requestSharding(nodeInfo, nodeModel.getWorkspaceId(), nodeUrl, jsonObject, file, file1 -> fileName, doneCallback, streamProgress);
    }

    /**
     * 普通消息转发
     *
     * @param machineNodeModel 节点
     * @param nodeUrl          节点的url
     * @param jsonObject       数据
     * @return JSON
     */
    public static <T> ApiResult<T> requestSharding(MachineNodeModel machineNodeModel, NodeUrl nodeUrl, JSONObject jsonObject, File file, Function<JSONObject, ApiResult<T>> doneCallback, BiConsumer<Long, Long> streamProgress) throws IOException {
        INodeInfo nodeInfo = coverNodeInfo(machineNodeModel);
        return requestSharding(nodeInfo, "", nodeUrl, jsonObject, file, File::getName, doneCallback, streamProgress);
    }

    /**
     * 普通消息转发
     *
     * @param nodeInfo       节点
     * @param workspaceId    工作空间id
     * @param streamProgress 进度回调
     * @param nodeUrl        节点的url
     * @param jsonObject     数据
     * @return JSON
     */
    private static <T> ApiResult<T> requestSharding(INodeInfo nodeInfo, String workspaceId, NodeUrl nodeUrl, JSONObject jsonObject, File file, Function<File, String> fileNameFn, Function<JSONObject, ApiResult<T>> doneCallback, BiConsumer<Long, Long> streamProgress) throws IOException {
        IUrlItem urlItem = parseUrlItem(nodeInfo, workspaceId, nodeUrl, DataContentType.FORM_URLENCODED);
        ServerConfig serverConfig = SpringContextHolder.getBean(ServerConfig.class);
        NodeConfig nodeConfig = serverConfig.getNode();
        long length = file.length();
        String fileName = fileNameFn.apply(file);
        Assert.state(length > 0, "空文件不能上传" + file.getAbsolutePath());
        String md5 = DigestUtil.md5(file);
        int fileSliceSize = nodeConfig.getUploadFileSliceSize();
        //如果小数点大于1，整数加一 例如4.1 =》5
        long chunkSize = DataSize.ofMegabytes(fileSliceSize).toBytes();
        int total = (int) Math.ceil((double) length / chunkSize);
        Queue<Integer> queueList = new ConcurrentLinkedDeque<>();
        for (int i = 0; i < total; i++) {
            queueList.offer(i);
        }
        List<Integer> success = Collections.synchronizedList(new ArrayList<>(total));
        // 并发数
        int concurrent = nodeConfig.getUploadFileConcurrent();
        AtomicReference<ApiResult<T>> failureMessage = new AtomicReference<>();
        AtomicReference<ApiResult<T>> succeedMessage = new AtomicReference<>();
        AtomicLong atomicProgressSize = new AtomicLong(0);
        JSONObject sliceData = new JSONObject();
        sliceData.put("sliceId", java.util.UUID.randomUUID().toString().replace("-", ""));
        sliceData.put("totalSlice", total);
        sliceData.put("fileSumMd5", md5);
        TransportServer transportServer = TransportServerFactory.get();
        TypeReference<ApiResult<T>> typeReference = new TypeReference<ApiResult<T>>() {
        };
        // 需要计算 并发数和最大任务数，如果任务数小于并发数则使用任务数
        try (StrictSyncFinisher syncFinisher = new StrictSyncFinisher(Math.min(concurrent, total), total)) {
            Runnable runnable = () -> {
                // 取出任务
                Integer currentChunk = queueList.poll();
                if (currentChunk == null) {
                    return;
                }
                JSONObject uploadData = jsonObject.clone();
                try {
                    try (FileInputStream inputStream = new FileInputStream(file)) {
                        try (FileChannel inputChannel = inputStream.getChannel()) {
                            //分配缓冲区，设定每次读的字节数
                            ByteBuffer byteBuffer = ByteBuffer.allocate((int) chunkSize);
                            // 移动到指定位置开始读取
                            inputChannel.position(currentChunk * chunkSize);
                            inputChannel.read(byteBuffer);
                            //上面把数据写入到了buffer，所以可知上面的buffer是写模式，调用flip把buffer切换到读模式，读取数据
                            byteBuffer.flip();
                            byte[] array = new byte[byteBuffer.remaining()];
                            byteBuffer.get(array, 0, array.length);
                            uploadData.put("file", new BytesResource(array, fileName + "." + currentChunk));
                            uploadData.put("nowSlice", currentChunk);
                            uploadData.putAll(sliceData);
                        }
                    }
                    // 上传
                    ApiResult<T> message = transportServer.executeToType(nodeInfo, urlItem, uploadData, typeReference);
                    if (message.success()) {
                        // 使用成功的个数计算
                        success.add(currentChunk);
                        long end = Math.min(length, ((success.size() - 1) * chunkSize) + chunkSize);
                        // 保存线程安全顺序回调进度信息
                        atomicProgressSize.set(Math.max(end, atomicProgressSize.get()));
                        streamProgress.accept(length, atomicProgressSize.get());
                        succeedMessage.set(message);
                    } else {
                        log.warn("分片上传异常：{} {}", nodeUrl, message);
                        // 终止上传
                        queueList.clear();
                        failureMessage.set(message);
                    }
                } catch (Exception e) {
                    log.error("分片上传文件异常", e);
                    // 终止上传
                    queueList.clear();
                    failureMessage.set(new ApiResult<>(500, "上传异常：" + e.getMessage()));
                }
            };
            for (int i = 0; i < total; i++) {
                syncFinisher.addWorker(runnable);
            }
            syncFinisher.start();
        }
        ApiResult<T> message = failureMessage.get();
        if (message != null) {
            return message;
        }
        // 判断是否都成功
        Assert.state(success.size() == total, String.format("上传异常, 完成数量不匹配 {}/{}", success.size(), total));
        //
        return Optional.ofNullable(doneCallback)
            .map(function -> function.apply(sliceData))
            .orElseGet(succeedMessage::get);
    }

    /**
     * 普通消息转发
     *
     * @param nodeModel  节点
     * @param nodeUrl    节点的url
     * @param pName      主参数名
     * @param pVal       主参数值
     * @param parameters 其他参数
     * @return JSON
     */
    public static <T> ApiResult<T> request(NodeModel nodeModel, NodeUrl nodeUrl, String pName, Object pVal, Object... parameters) {

        INodeInfo parseNodeInfo = parseNodeInfo(nodeModel);
        return request(parseNodeInfo, nodeModel.getWorkspaceId(), nodeUrl, pName, pVal, parameters);
    }

    /**
     * 普通消息转发
     *
     * @param machineNodeModel 节点
     * @param workspaceId      工作空间id
     * @param nodeUrl          节点的url
     * @param pName            主参数名
     * @param pVal             主参数值
     * @param parameters       其他参数
     * @return JSON
     */
    public static <T> ApiResult<T> request(MachineNodeModel machineNodeModel, String workspaceId, NodeUrl nodeUrl, String pName, Object pVal, Object... parameters) {
        // Map.of 不允许 null 值（新增机器时 id 为 null 会抛 NPE），改用 HashMap
        Map<String, Object> parametersMap = new java.util.HashMap<>();
        parametersMap.put(pName, pVal);
        for (int i = 0; i < parameters.length; i += 2) {
            parametersMap.put(parameters[i].toString(), parameters[i + 1]);
        }
        INodeInfo nodeInfo = coverNodeInfo(machineNodeModel);
        return request(nodeInfo, workspaceId, nodeUrl, pName, pVal, parameters);
    }

    /**
     * 普通消息转发
     *
     * @param nodeInfo    节点
     * @param workspaceId 工作空间id
     * @param nodeUrl     节点的url
     * @param pName       主参数名
     * @param pVal        主参数值
     * @param parameters  其他参数
     * @return JSON
     */
    private static <T> ApiResult<T> request(INodeInfo nodeInfo, String workspaceId, NodeUrl nodeUrl, String pName, Object pVal, Object... parameters) {
        // Map.of 不允许 null 值（新增机器时 id 为 null 会抛 NPE），改用 HashMap
        Map<String, Object> parametersMap = new java.util.HashMap<>();
        parametersMap.put(pName, pVal);
        for (int i = 0; i < parameters.length; i += 2) {
            parametersMap.put(parameters[i].toString(), parameters[i + 1]);
        }
        IUrlItem iUrlItem = parseUrlItem(nodeInfo, workspaceId, nodeUrl);
        return TransportServerFactory.get().executeToType(nodeInfo, iUrlItem, parametersMap, new TypeReference<ApiResult<T>>() {
        });
    }

    /**
     * post body 消息转发
     *
     * @param nodeModel 节点
     * @param nodeUrl   节点的url
     * @param jsonData  数据
     * @param <T>       泛型
     * @return JSON
     */
    public static <T> ApiResult<T> requestBody(NodeModel nodeModel, NodeUrl nodeUrl, JSONObject jsonData) {
        TypeReference<ApiResult<T>> tTypeReference = new TypeReference<ApiResult<T>>() {
        };
        return createUrlItem(nodeModel, nodeUrl, DataContentType.JSON,
            (nodeInfo, urlItem) -> TransportServerFactory.get().executeToType(nodeInfo, urlItem, jsonData, tTypeReference));

    }

    /**
     * 普通消息转发,并解析数据
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @param tClass    要解析的类
     * @param <T>       泛型
     * @return T
     */
    public static <T> T requestData(NodeModel nodeModel, NodeUrl nodeUrl, HttpServletRequest request, Class<T> tClass) {
        INodeInfo parseNodeInfo = parseNodeInfo(nodeModel);
        return requestData(parseNodeInfo, nodeModel.getWorkspaceId(), nodeUrl, request, tClass);
    }

    /**
     * 普通消息转发,并解析数据
     *
     * @param machineNodeModel 节点
     * @param request          请求
     * @param nodeUrl          节点的url
     * @param tClass           要解析的类
     * @param <T>              泛型
     * @return T
     */
    public static <T> T requestData(MachineNodeModel machineNodeModel, NodeUrl nodeUrl, HttpServletRequest request, Class<T> tClass) {
        INodeInfo nodeInfo = coverNodeInfo(machineNodeModel);
        return requestData(nodeInfo, "", nodeUrl, request, tClass);
    }

    /**
     * 普通消息转发,并解析数据
     *
     * @param nodeInfo1 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @param tClass    要解析的类
     * @param <T>       泛型
     * @return T
     */
    private static <T> T requestData(INodeInfo nodeInfo1, String workspaceId, NodeUrl nodeUrl, HttpServletRequest request, Class<T> tClass) {
        Map<String, String> map = Optional.ofNullable(request).map(JakartaServletUtil::getParamMap).orElse(null);
        IUrlItem iUrlItem = parseUrlItem(nodeInfo1, workspaceId, nodeUrl);
        return TransportServerFactory.get().executeToTypeOnlyData(nodeInfo1, iUrlItem, map, tClass);
    }


    /**
     * 上传文件消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param nodeUrl   节点的url
     * @return json
     */
    public static ApiResult<String> requestMultipart(NodeModel nodeModel, MultipartHttpServletRequest request, NodeUrl nodeUrl) {
        INodeInfo parseNodeInfo = parseNodeInfo(nodeModel);
        return requestMultipart(parseNodeInfo, nodeModel.getWorkspaceId(), request, nodeUrl);
    }

    /**
     * 上传文件消息转发
     *
     * @param machineNodeModel 节点
     * @param request          请求
     * @param nodeUrl          节点的url
     * @return json
     */
    public static ApiResult<String> requestMultipart(MachineNodeModel machineNodeModel, MultipartHttpServletRequest request, NodeUrl nodeUrl) {
        INodeInfo nodeInfo = coverNodeInfo(machineNodeModel);
        return requestMultipart(nodeInfo, "", request, nodeUrl);
    }

    /**
     * 上传文件消息转发
     *
     * @param nodeInfo 节点
     * @param request  请求
     * @param nodeUrl  节点的url
     * @return json
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ApiResult<String> requestMultipart(INodeInfo nodeInfo, String workspaceId, MultipartHttpServletRequest request, NodeUrl nodeUrl) {
        IUrlItem iUrlItem = parseUrlItem(nodeInfo, workspaceId, nodeUrl);
        //
        Map params = JakartaServletUtil.getParamMap(request);
        //
        Map<String, MultipartFile> fileMap = request.getFileMap();
        fileMap.forEach((s, multipartFile) -> {
            try {
                params.put(s, new BytesResource(multipartFile.getBytes(), multipartFile.getOriginalFilename()));
            } catch (IOException e) {
                log.error("转发文件异常", e);
                throw Lombok.sneakyThrow(e);
            }
        });
        TypeReference<ApiResult<String>> tTypeReference = new TypeReference<ApiResult<String>>() {
        };
        return TransportServerFactory.get().executeToType(nodeInfo, iUrlItem, params, tTypeReference);

    }

    /**
     * 下载文件消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param response  响应
     * @param nodeUrl   节点的url
     */
    public static void requestDownload(NodeModel nodeModel, HttpServletRequest request, HttpServletResponse response, NodeUrl nodeUrl) {
        //
        Map<String, String> params = JakartaServletUtil.getParamMap(request);
        createUrlItem(nodeModel, nodeUrl, (nodeInfo, urlItem) -> {
            TransportServerFactory.get().download(nodeInfo, urlItem, params, downloadCallback -> {
                Opt.ofBlankAble(downloadCallback.getContentDisposition())
                    .ifPresent(s -> response.setHeader(HttpHeaders.CONTENT_DISPOSITION, s));
                response.setContentType(downloadCallback.getContentType());
                JakartaServletUtil.write(response, downloadCallback.getInputStream());
            });
            return null;
        });
    }

    /**
     * 下载文件消息转发
     *
     * @param nodeModel 节点
     * @param request   请求
     * @param response  响应
     * @param nodeUrl   节点的url
     */
    public static void requestDownload(MachineNodeModel nodeModel, HttpServletRequest request, HttpServletResponse response, NodeUrl nodeUrl) {
        //
        Map<String, String> params = JakartaServletUtil.getParamMap(request);
        INodeInfo nodeInfo = coverNodeInfo(nodeModel);
        IUrlItem iUrlItem = parseUrlItem(nodeInfo, "", nodeUrl);
        TransportServerFactory.get().download(nodeInfo, iUrlItem, params, downloadCallback -> {
            Opt.ofBlankAble(downloadCallback.getContentDisposition())
                .ifPresent(s -> response.setHeader(HttpHeaders.CONTENT_DISPOSITION, s));
            response.setContentType(downloadCallback.getContentType());
            JakartaServletUtil.write(response, downloadCallback.getInputStream());
        });
    }

    public static <T> T toJsonMessage(String body, TypeReference<T> tTypeReference) {
        if ((body == null || body.isEmpty())) {
            throw new AgentException("agent 端响应内容为空");
        }
        T data = JSON.parseObject(body, tTypeReference);
        if (data instanceof ApiResult) {
            ApiResult<?> jsonMessage = (ApiResult<?>) data;
            if (jsonMessage.getCode() == Const.AUTHORIZE_ERROR) {
                throw new AgentAuthorizeException(new ApiResult<>(jsonMessage.getCode(), jsonMessage.getMsg()));
            }
        } else {
            throw new IllegalStateException("消息转换异常");
        }
        return data;
    }
}
