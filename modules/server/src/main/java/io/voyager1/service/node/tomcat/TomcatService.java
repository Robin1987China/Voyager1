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

//package io.voyager1.service.node.tomcat;
//
//import com.alibaba.fastjson2.JSONArray;
//import com.alibaba.fastjson2.JSONObject;
//import io.voyager1.common.forward.NodeForward;
//import io.voyager1.common.forward.NodeUrl;
//import io.voyager1.model.data.NodeModel;
//import io.voyager1.service.node.NodeService;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartHttpServletRequest;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//
///**
// * tomcat
// *
// * 
// */
//@Service
//public class TomcatService {
//
//
//	private final NodeService nodeService;
//
//	public TomcatService(NodeService nodeService) {
//		this.nodeService = nodeService;
//	}
//
//	/**
//	 * 查询tomcat列表
//	 *
//	 * @param nodeModel 节点信息
//	 * @return tomcat的信息
//	 */
//	public JSONArray getTomcatList(NodeModel nodeModel) {
//		if (!nodeModel.isOpenStatus()) {
//			return null;
//		}
//		return NodeForward.requestData(nodeModel, NodeUrl.Tomcat_List, JSONArray.class, null, null);
//	}
//
//	/**
//	 * 查询tomcat信息
//	 *
//	 * @param nodeModel 节点信息
//	 * @param id tomcat的id
//	 * @return tomcat的信息
//	 */
//	public JSONObject getTomcatInfo(NodeModel nodeModel, String id) {
//		return NodeForward.requestData(nodeModel, NodeUrl.Tomcat_GetItem, JSONObject.class, "id", id);
//	}
//
//
//	public JSONArray getTomcatProjectList(NodeModel nodeModel, String id) {
//		return NodeForward.requestData(nodeModel, NodeUrl.Tomcat_GetTomcatProjectList, JSONArray.class, "id", id);
//	}
//
//	/**
//	 * tomcat项目管理
//	 *
//	 * @param nodeModel 节点信息
//	 * @param request 请求信息
//	 * @return 操作结果
//	 */
//	public String tomcatProjectManage(NodeModel nodeModel, HttpServletRequest request) {
//		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_TomcatProjectManage).toString();
//	}
//
//	/**
//	 * 新增Tomcat
//	 *
//	 * @param nodeModel 节点信息
//	 * @param request 请求信息
//	 * @return 操作结果
//	 */
//	public String addTomcat(NodeModel nodeModel, HttpServletRequest request) {
//		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Add).toString();
//	}
//
//	/**
//	 * 更新Tomcat信息
//	 *
//	 * @param nodeModel 节点信息
//	 * @param request 请求信息
//	 * @return 操作结果
//	 */
//	public String updateTomcat(NodeModel nodeModel, HttpServletRequest request) {
//		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Update).toString();
//	}
//
//	/**
//	 * 查询tomcat运行状态
//	 *
//	 * @param nodeModel 节点信息
//	 * @param request 请求信息
//	 * @return 操作结果
//	 */
//	public String getTomcatStatus(NodeModel nodeModel, HttpServletRequest request) {
//		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_GetTomcatStatus).toString();
//	}
//
//	/**
//	 * 启动tomcat
//	 *
//	 * @param nodeModel 节点信息
//	 * @param request 请求信息
//	 * @return 操作结果
//	 */
//	public String start(NodeModel nodeModel, HttpServletRequest request) {
//		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Start).toString();
//	}
//
//	/**
//	 * 停止tomcat
//	 *
//	 * @param nodeModel 节点信息
//	 * @param request 请求信息
//	 * @return 操作结果
//	 */
//	public String stop(NodeModel nodeModel, HttpServletRequest request) {
//		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Stop).toString();
//	}
//
//	/**
//	 * 重启tomcat
//	 *
//	 * @param nodeModel 节点信息
//	 * @param request 请求信息
//	 * @return 操作结果
//	 */
//	public String restart(NodeModel nodeModel, HttpServletRequest request) {
//		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Restart).toString();
//	}
//
//	/**
//	 * 删除tomcat
//	 *
//	 * @param nodeModel 节点信息
//	 * @param request 请求信息
//	 * @return 操作结果
//	 */
//	public String delete(NodeModel nodeModel, HttpServletRequest request) {
//		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_Delete).toString();
//	}
//
//	/**
//	 * 获取文件列表
//	 *
//	 * @param nodeModel 节点信息
//	 * @param request 请求信息
//	 * @return 操作结果
//	 */
//	public String getFileList(NodeModel nodeModel, HttpServletRequest request) {
//		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_File_GetFileList).toString();
//	}
//
//	/**
//	 * 上传文件
//	 *
//	 * @param nodeModel 节点信息
//	 * @param request 请求信息
//	 * @return 操作结果
//	 */
//	public String upload(NodeModel nodeModel, HttpServletRequest request) {
//		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_File_Upload).toString();
//	}
//
//	/**
//	 * 下载文件
//	 *
//	 * @param nodeModel 节点信息
//	 * @param request 请求信息
//	 * @param response 响应信息
//	 */
//	public void download(NodeModel nodeModel, HttpServletRequest request, HttpServletResponse response) {
//		NodeForward.requestDownload(nodeModel, request, response, NodeUrl.Tomcat_File_Download);
//	}
//
//	/**
//	 * 删除文件
//	 *
//	 * @param nodeModel 节点信息
//	 * @param request 请求信息
//	 * @return 操作结果
//	 */
//	public String deleteFile(NodeModel nodeModel, HttpServletRequest request) {
//		return NodeForward.request(nodeModel, request, NodeUrl.Tomcat_File_DeleteFile).toString();
//	}
//
//	/**
//	 * 上传War包
//	 *
//	 * @param node 节点信息
//	 * @param multiRequest 请求信息
//	 * @return 操作结果
//	 */
//	public String uploadWar(NodeModel node, MultipartHttpServletRequest multiRequest) {
//		return NodeForward.requestMultipart(node, multiRequest, NodeUrl.Tomcat_File_UploadWar).toString();
//	}
//}
