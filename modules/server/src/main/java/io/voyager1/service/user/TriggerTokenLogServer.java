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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

import io.voyager1.util.CollStreamUtil;
import io.voyager1.util.BetweenFormatter;
import io.voyager1.util.MapUtil;
import io.voyager1.util.IdUtil;
import io.voyager1.util.StrUtil;
import io.voyager1.core.db.Entity;
import io.voyager1.event.ISystemTask;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import io.voyager1.common.i18n.I18nMessageUtil;
import io.voyager1.core.entity.TriggerTokenLogEntity;
import io.voyager1.core.jpa.JpaBaseService;
import io.voyager1.core.repository.TriggerTokenLogRepository;
import io.voyager1.model.PageResultDto;
import io.voyager1.model.user.TriggerTokenLogBean;
import io.voyager1.model.user.UserModel;
import io.voyager1.service.ITriggerToken;
import io.voyager1.util.StringUtil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 2022/7/22
 */
@Service
@Slf4j
public class TriggerTokenLogServer extends JpaBaseService<TriggerTokenLogBean, TriggerTokenLogEntity> implements ISystemTask {

    private final UserService userService;
    private final List<ITriggerToken> triggerTokens;
    private final Map<String, ITriggerToken> triggerTokenMap;
    private final TriggerTokenLogRepository triggerTokenLogRepository;

    public TriggerTokenLogServer(UserService userService,
                                 List<ITriggerToken> triggerTokens,
                                 TriggerTokenLogRepository triggerTokenLogRepository) {
        this.userService = userService;
        this.triggerTokens = triggerTokens;
        this.triggerTokenLogRepository = triggerTokenLogRepository;
        triggerTokenMap = CollStreamUtil.toMap(triggerTokens, ITriggerToken::typeName, iTriggerToken -> iTriggerToken);
    }

    @Override
    protected JpaRepository<TriggerTokenLogEntity, String> repository() {
        return triggerTokenLogRepository;
    }

    @Override
    protected JpaSpecificationExecutor<TriggerTokenLogEntity> specExecutor() {
        return triggerTokenLogRepository;
    }

    @Override
    protected Class<TriggerTokenLogEntity> entityClass() {
        return TriggerTokenLogEntity.class;
    }

    @Override
    protected Class<TriggerTokenLogBean> modelClass() {
        return TriggerTokenLogBean.class;
    }

    /**
     * 获取类型
     *
     * @param type 类型名称
     * @return 接口
     */
    public ITriggerToken getByType(String type) {
        return MapUtil.get(triggerTokenMap, type, ITriggerToken.class);
    }

    /**
     * 删除触发器
     *
     * @param id Id
     */
    public void delete(String id) {
        TriggerTokenLogBean tokenLogBean = this.getByKey(id);
        if (tokenLogBean == null) {
            return;
        }
        ITriggerToken token = triggerTokens.stream()
            .filter(iTriggerToken -> java.util.Objects.equals(iTriggerToken.typeName(), tokenLogBean.getType()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("没有对应的触发器类型：" + tokenLogBean.getType()));
        String sql = "update " + tokenLogBean.getType() + " set triggerToken='' where id=?";
        this.execute(sql, tokenLogBean.getDataId());
        this.delByKey(id);
    }

    /**
     * 获取所有类型
     *
     * @return list
     */
    public List<JSONObject> allType() {
        return triggerTokens.stream()
            .map(iTriggerToken -> {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", iTriggerToken.typeName());
                jsonObject.put("desc", iTriggerToken.getDataDesc());
                return jsonObject;
            })
            .collect(Collectors.toList());
    }

    /**
     * 通过用户ID 删除数据
     *
     * @param userId 用户d
     */
    public void delByUserId(String userId) {
        TriggerTokenLogBean tokenLogBean = new TriggerTokenLogBean();
        tokenLogBean.setUserId(userId);
        this.delByBean(tokenLogBean);
    }

    /**
     * 通过 token 获取用户ID
     *
     * @param token token
     * @param type  数据类型
     * @return user
     */
    public UserModel getUserByToken(String token, String type) {
        TriggerTokenLogBean tokenLogBean = this.getByKey(token);
        if (tokenLogBean != null) {
            UserModel userModel = userService.getByKey(tokenLogBean.getUserId());
            if (userModel != null && java.util.Objects.equals(type, tokenLogBean.getType())) {
                boolean demoUser = userModel.isDemoUser();
                Assert.state(!demoUser, "当前用户触发器不可用");
                // 修改触发次数
                String sql = "update " + this.getTableName() + " set triggerCount=ifnull(triggerCount,0)+1 where id=?";
                int execute = this.execute(sql, tokenLogBean.getId());
                return userModel;
            }
        }
        //
        return null;
    }

    /**
     * 重启生成 token
     *
     * @param oldToken 之前版本 token
     * @param type     类型
     * @param dataId   数据ID
     * @param userId   用户ID
     * @return 新 token
     */
    public String restToken(String oldToken, String type, String dataId, String userId) {
        if ((oldToken != null && !oldToken.isEmpty())) {
            this.delByKey(oldToken);
        }
        // 创建 token
        return this.createToken(type, dataId, userId);
    }

    /**
     * 创建新 token
     *
     * @param type   类型
     * @param dataId 数据ID
     * @param userId 用户ID
     * @return token
     */
    private String createToken(String type, String dataId, String userId) {
        TriggerTokenLogBean trigger = new TriggerTokenLogBean();
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "");
        trigger.setId(uuid);
        trigger.setTriggerToken(uuid);
        trigger.setType(type);
        trigger.setDataId(dataId);
        trigger.setUserId(userId);
        this.insert(trigger);
        return uuid;
    }

    @Override
    public void executeTask() {
        if (triggerTokens == null) {
            return;
        }
        log.debug("clean trigger token start...");
        long start = System.currentTimeMillis();
        // 调用方法处理逻辑
        cleanTriggerToken();
        log.debug("clean trigger token end... cost time: {}", StringUtil.formatBetween(System.currentTimeMillis() - start, BetweenFormatter.Level.MILLISECOND));
    }

    /**
     * @since 2023-04-13
     */
    private void cleanTriggerToken() {
        // 统计删除条数
        int delCount = 0;
        for (ITriggerToken triggerToken : triggerTokens) {
            TriggerTokenLogBean tokenLogBean = new TriggerTokenLogBean();
            tokenLogBean.setType(triggerToken.typeName());
            try {
                int pageNumber = 1;
                while (true) {
                    Pageable page = PageRequest.of(pageNumber - 1, 50);
                    Entity entity = new Entity();
                    entity.set("type", triggerToken.typeName());
                    entity.setFieldNames("id", "dataId");
                    PageResultDto<TriggerTokenLogBean> pageResult = this.listPage(entity, page);
                    if (pageResult.isEmpty()) {
                        break;
                    }
                    List<String> ids = new ArrayList<>();
                    List<TriggerTokenLogBean> result = pageResult.getResult();
                    for (TriggerTokenLogBean bean : result) {
                        //
                        String dataId = bean.getDataId();
                        if (triggerToken.exists(dataId)) {
                            continue;
                        }
                        String id = bean.getId();
                        ids.add(id);
                    }
                    // 删除 token
                    this.delByKey(ids);
                    if (pageResult.getTotalPage() <= pageNumber) {
                        break;
                    }
                    pageNumber++;
                    delCount += ids.size();
                }
            } catch (Exception e) {
                log.error("执行清理 token[{}] 异常", triggerToken.typeName(), e);
            }
        }
        if (delCount > 0) {
            log.info("clean trigger token count: {}", delCount);
        }
    }
}
