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

package io.voyager1;

/**
 * apiDoc 通用文档块
 *
 * @since 2022/2/28
 */
public interface ApiDoc {

    /**
     * 登录用户返回消息体
     *
     *
     * @apiDefine loginUser
     * @apiUse defResultJson
     * @apiHeader {String} Authorization 用户token
     * @apiPermission login-user
     * @apiSuccess (800) {none} data 需要登录
     * @apiSuccess (801) {none} data 登录信息过期,但是可以续期
     * @apiSuccess (302) {none} data 当前用户没有操作权限
     * @apiSuccess (999) {none} data 当前 IP 不能访问
     */
    void loginUser();

    /**
     * 默认的通用返回消息体
     *
     *
     * @apiDefine defResultJson
     * @apiSuccessExample {json} Success-Response:
     * HTTP/1.1 200 OK
     * {
     *   "code": "200",
     *   "msg": "成功",
     *   "data": {},
     * }
     */
    void defResultJson();
}
