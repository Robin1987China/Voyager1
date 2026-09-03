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

package io.voyager1.service.user;

import io.voyager1.core.entity.UserBindWorkspaceEntity;
import io.voyager1.core.jpa.DataService;
import io.voyager1.core.repository.UserBindWorkspaceRepository;
import io.voyager1.model.data.WorkspaceModel;
import io.voyager1.model.user.UserBindWorkspaceModel;
import io.voyager1.model.user.UserModel;
import io.voyager1.model.user.UserPermissionGroupBean;
import io.voyager1.permission.MethodFeature;
import io.voyager1.service.system.WorkspaceService;
import io.voyager1.util.CollUtil;
import io.voyager1.util.ConvertUtil;
import io.voyager1.util.DateTime;
import io.voyager1.util.DateUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.util.Week;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户工作空间绑定服务。
 * <p>
 * 已从承继存储框架（BaseDbService）搬家到 JPA 仓库（UserBindWorkspaceRepository），对外契约不变。
 *
 * @since 2021/12/4
 */
@Service
public class UserBindWorkspaceService implements DataService<UserBindWorkspaceModel> {

    private final UserBindWorkspaceRepository repository;
    private final WorkspaceService workspaceService;
    private final UserPermissionGroupServer userPermissionGroupServer;

    public static final String SSH_COMMAND_NOT_LIMITED = "-sshCommandNotLimited";

    public UserBindWorkspaceService(UserBindWorkspaceRepository repository,
                                    WorkspaceService workspaceService,
                                    UserPermissionGroupServer userPermissionGroupServer) {
        this.repository = repository;
        this.workspaceService = workspaceService;
        this.userPermissionGroupServer = userPermissionGroupServer;
    }

    @Override
    public UserBindWorkspaceModel getByKey(String id) {
        UserBindWorkspaceEntity entity = repository.findById(id).orElse(null);
        return entity == null ? null : toModel(entity);
    }

    public List<UserBindWorkspaceModel> listById(Collection<String> ids) {
        return repository.findAllById(ids).stream().map(this::toModel).collect(Collectors.toList());
    }

    @Transactional
    public void updateUserWorkspace(String userId, List<String> workspace) {
        Assert.notEmpty(workspace, "没有任何工作空间信息");
        List<UserBindWorkspaceEntity> entities = new HashSet<>(workspace).stream()
            .filter(s -> {
                s = (s != null && s.endsWith(SSH_COMMAND_NOT_LIMITED) ? s.substring(0, s.length() - SSH_COMMAND_NOT_LIMITED.length()) : s);
                MethodFeature[] values = MethodFeature.values();
                for (MethodFeature value : values) {
                    s = StrUtil.removeSuffix(s, "-" + value.name());
                }
                return workspaceService.exists(new WorkspaceModel(s));
            })
            .map(s -> {
                long now = System.currentTimeMillis();
                UserBindWorkspaceEntity entity = new UserBindWorkspaceEntity();
                entity.setWorkspaceId(s);
                entity.setUserId(userId);
                entity.setId(UserBindWorkspaceModel.getId(userId, s));
                entity.setCreateTimeMillis(now);
                entity.setModifyTimeMillis(now);
                return entity;
            })
            .collect(Collectors.toList());
        repository.deleteByUserId(userId);
        repository.saveAll(entities);
    }

    public List<UserBindWorkspaceModel> listUserWorkspace(String userId) {
        return repository.findByUserId(userId).stream().map(this::toModel).collect(Collectors.toList());
    }

    public boolean existsWorkspace(String workspaceId) {
        return repository.existsByWorkspaceId(workspaceId);
    }

    public List<WorkspaceModel> listUserWorkspaceInfo(UserModel userModel) {
        if (userModel.isSuperSystemUser()) {
            return workspaceService.list();
        }
        String permissionGroup = userModel.getPermissionGroup();
        List<String> list = ConvertUtil.splitTrim(permissionGroup, "@");
        list = (list != null ? list : new ArrayList<>());
        list.add(userModel.getId());
        List<UserBindWorkspaceModel> userBindWorkspaceModels = repository.findByUserIdIn(list)
            .stream().map(this::toModel).collect(Collectors.toList());
        Assert.notEmpty(userBindWorkspaceModels, "没有任何工作空间信息,请联系管理授权");
        List<String> collect = userBindWorkspaceModels.stream().map(UserBindWorkspaceModel::getWorkspaceId).collect(Collectors.toList());
        return workspaceService.listById(collect);
    }

    @Transactional
    public void deleteByUserId(String userId) {
        repository.deleteByUserId(userId);
    }

    private List<UserBindWorkspaceModel> existsList(UserModel userModel, String workspaceId) {
        String permissionGroup = userModel.getPermissionGroup();
        List<String> list = ConvertUtil.splitTrim(permissionGroup, "@");
        list = list.stream()
            .map(s -> UserBindWorkspaceModel.getId(s, workspaceId))
            .collect(Collectors.toList());
        list.add(UserBindWorkspaceModel.getId(userModel.getId(), workspaceId));
        return this.listById(list);
    }

    public boolean exists(UserModel userModel, String workspaceId) {
        List<UserBindWorkspaceModel> workspaceModels = this.existsList(userModel, workspaceId);
        return (workspaceModels != null && !workspaceModels.isEmpty());
    }

    public UserBindWorkspaceModel.PermissionResult checkPermission(UserModel userModel, String workspaceId) {
        List<UserBindWorkspaceModel> workspaceModels = this.existsList(userModel, workspaceId);
        if ((workspaceModels == null || workspaceModels.isEmpty())) {
            return UserBindWorkspaceModel.PermissionResult.builder()
                .state(UserBindWorkspaceModel.PermissionResultEnum.FAIL)
                .msg("您没有对应管理权限:-3")
                .build();
        }
        List<String> permissionGroupIds = workspaceModels.stream()
            .map(UserBindWorkspaceModel::getUserId)
            .collect(Collectors.toList());
        List<UserPermissionGroupBean> permissionGroups = userPermissionGroupServer.listById(permissionGroupIds);
        if ((permissionGroups == null || permissionGroups.isEmpty())) {
            return UserBindWorkspaceModel.PermissionResult.builder()
                .state(UserBindWorkspaceModel.PermissionResultEnum.FAIL)
                .msg("您没有对应管理权限:-2")
                .build();
        }
        Optional<JSONObject> prohibitExecuteRule = this.findProhibitExecuteRule(permissionGroups);
        if (prohibitExecuteRule.isPresent()) {
            String msg = prohibitExecuteRule.map(jsonObject -> {
                String reason = jsonObject.getString("reason");
                String startTime = jsonObject.getString("startTime");
                String endTime = jsonObject.getString("endTime");
                if ((reason == null || reason.isEmpty())) {
                    return String.format("【禁止操作】当前时段禁止执行 %s 至 %s", startTime, endTime);
                }
                return String.format("【禁止操作】%s %s 至 %s", reason, startTime, endTime);
            }).orElse("【禁止操作】当前时段禁止执行");
            return UserBindWorkspaceModel.PermissionResult.builder()
                .state(UserBindWorkspaceModel.PermissionResultEnum.MISS_PROHIBIT)
                .msg(msg)
                .build();
        }
        return this.checkAllowExecute(permissionGroups);
    }

    private UserBindWorkspaceModel.PermissionResult checkAllowExecute(List<UserPermissionGroupBean> permissionGroups) {
        List<JSONObject> allowExecuteListRule = permissionGroups.stream()
            .map(UserPermissionGroupBean::getAllowExecute)
            .filter(Objects::nonNull)
            .map(JSONArray::parseArray)
            .filter(CollUtil::isNotEmpty)
            .flatMap(jsonArray -> jsonArray.stream().map(o -> (JSONObject) o))
            .collect(Collectors.toList());
        if ((allowExecuteListRule == null || allowExecuteListRule.isEmpty())) {
            return UserBindWorkspaceModel.PermissionResult.builder().state(UserBindWorkspaceModel.PermissionResultEnum.SUCCESS).build();
        }
        Optional<JSONObject> allowExecuteRule = allowExecuteListRule.stream()
            .filter(jsonObject -> {
                DateTime now = DateTime.now();
                Week nowWeek = now.dayOfWeekEnum();
                int nowWeekInt = nowWeek.getIso8601Value();
                JSONArray week = jsonObject.getJSONArray("week");
                if ((week == null || week.isEmpty())) {
                    return false;
                }
                if (!(week != null && week.contains(nowWeekInt))) {
                    return false;
                }
                String startTime = jsonObject.getString("startTime");
                String endTime = jsonObject.getString("endTime");
                DateTime startDate = DateUtil.parseTimeToday(startTime);
                DateTime endDate = DateUtil.parseTimeToday(endTime);
                return DateUtil.isIn(DateTime.now(), startDate, endDate);
            })
            .findAny();
        if (allowExecuteRule.isPresent()) {
            return UserBindWorkspaceModel.PermissionResult.builder().state(UserBindWorkspaceModel.PermissionResultEnum.SUCCESS).build();
        }
        String ruleStr = allowExecuteListRule.stream()
            .map(jsonObject -> {
                JSONArray week = jsonObject.getJSONArray("week");
                String weekStr = week.stream()
                    .map(o -> ConvertUtil.toInt(o, 0))
                    .map(weekInt -> DayOfWeek.of(weekInt))
                    .map(Week::of)
                    .map(week1 -> week1.toChinese(""))
                    .collect(Collectors.joining(","));
                String startTime = jsonObject.getString("startTime");
                String endTime = jsonObject.getString("endTime");
                return String.format("周%s 的 %s 至 %s", weekStr, startTime, endTime);
            })
            .collect(Collectors.joining(" "));
        return UserBindWorkspaceModel.PermissionResult.builder()
            .state(UserBindWorkspaceModel.PermissionResultEnum.MISS_PERIOD)
            .msg("【禁止操作】当前时间不在可执行的时间段内,限制时间段:" + ruleStr)
            .build();
    }

    private Optional<JSONObject> findProhibitExecuteRule(List<UserPermissionGroupBean> permissionGroups) {
        return permissionGroups.stream()
            .map(UserPermissionGroupBean::getProhibitExecute)
            .filter(Objects::nonNull)
            .map(JSONArray::parseArray)
            .filter(CollUtil::isNotEmpty)
            .flatMap(jsonArray -> jsonArray.stream().map(o -> (JSONObject) o))
            .filter(jsonObject -> {
                String startTime = jsonObject.getString("startTime");
                String endTime = jsonObject.getString("endTime");
                if (((startTime == null || startTime.isEmpty()) || (endTime == null || endTime.isEmpty()))) {
                    return false;
                }
                DateTime startDate = DateUtil.parse(startTime);
                DateTime endDate = DateUtil.parse(endTime);
                return DateUtil.isIn(DateTime.now(), startDate, endDate);
            })
            .findFirst();
    }

    private UserBindWorkspaceModel toModel(UserBindWorkspaceEntity entity) {
        UserBindWorkspaceModel model = new UserBindWorkspaceModel();
        model.setId(entity.getId());
        model.setCreateTimeMillis(entity.getCreateTimeMillis());
        model.setModifyTimeMillis(entity.getModifyTimeMillis());
        model.setUserId(entity.getUserId());
        model.setWorkspaceId(entity.getWorkspaceId());
        return model;
    }
}
