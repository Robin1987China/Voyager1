<template>
  <div>
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="5"
      :active-page="activePage"
      table-name="dispatch"
      :columns="columns"
      size="medium"
      :data="list"
      bordered
      row-key="id"
      :pagination="pagination"
      :scroll="{
        x: 'max-content'
      }"
      @refresh="loadData"
      @change="
        (pagination, filters, sorter) => {
          listQuery = CHANGE_PAGE(listQuery, { pagination, sorter })
          loadData()
        }
      "
    >
      <template #title>
        <n-space wrap class="search-box">
          <n-input
            v-model:value="listQuery['%id%']"
            class="search-input-item"
            :placeholder="$t('i18n_83aa7f3123')"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['%name%']"
            class="search-input-item"
            :placeholder="$t('i18n_d7ec2d3fea')"
            @press-enter="loadData"
          />
          <n-select
            v-model:value="listQuery.group"
            clearable
            :placeholder="$t('i18n_ec22193ed1')"
            class="search-input-item"
            :options="groupList"
            @update:value="loadData"
          />
          <n-select
            v-model:value="listQuery.outGivingProject"
            clearable
            :placeholder="$t('i18n_9e2e02ef08')"
            class="search-input-item"
            :options="[
              { label: $t('i18n_0c1de8295a'), value: 1 },
              { label: $t('i18n_1c3cf7f5f0'), value: 0 }
            ]"
          />
          <n-select
            v-model:value="listQuery.status"
            clearable
            :placeholder="$t('i18n_e1c965efff')"
            class="search-input-item"
            :options="Object.entries(statusMap).map(([key, name]) => ({ label: name, value: key }))"
          />
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
              </span>
            </template>
            $t('i18n_4838a3bd20')
          </n-tooltip>
          <n-button type="primary" @click="handleLink">{{ $t('i18n_30d9d4f5c9') }}</n-button>
          <n-button type="primary" @click="handleAdd">{{ $t('i18n_47e4123886') }}</n-button>

          <!-- <n-statistic format=" s 秒" title="刷新倒计时" :value="countdownTime" @finish="silenceLoadData" /> -->
        </n-space>
      </template>
      <template #tableHelp>
        <n-tooltip>
          <template #trigger>
            <QuestionCircleOutlined />
          </template>

          <div>{{ $t('i18n_3aa69a563b') }}</div>

          <div>
            <ul>
              <li>{{ $t('i18n_5f5cd1bb1e') }}</li>
              <li>{{ $t('i18n_9bf4e3c9de') }}</li>
            </ul>
          </div>
        </n-tooltip>
      </template>
      <template #tableBodyCell="{ column, text, record }">
        <template v-if="column.tooltip">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{ text || '' }}</span>
                </span>
              </span>
            </template>
            text
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'id'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <n-button
                  v-if="record.outGivingProject"
                  size="small"
                  style="padding: 0"
                  text
                  @click="handleEditDispatchProject(record)"
                  >{{ text }}</n-button
                >
                <n-button v-else size="small" style="padding: 0" text @click="handleEditDispatch(record)">{{
                  text
                }}</n-button>
              </span>
            </template>
            text
          </n-tooltip>
        </template>
        <template v-else-if="column.dataIndex === 'name'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <n-button size="small" style="padding: 0" text @click="handleViewStatus(record)"
                  ><FullscreenOutlined />{{ text }}</n-button
                >
              </span>
            </template>
            text
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'status'">
          <n-tooltip placement="topLeft">
            <template #trigger>
              <span class="tw">
                <n-tag v-if="text === 2" color="green">{{ statusMap[text] || $t('i18n_1622dc9b6b') }}</n-tag>
                <n-tag v-else-if="text === 1" color="orange">{{ statusMap[text] || $t('i18n_1622dc9b6b') }}</n-tag>
                <n-tag v-else-if="text === 3 || text === 4" color="red">{{
                  statusMap[text] || $t('i18n_1622dc9b6b')
                }}</n-tag>
                <n-tag v-else>{{ statusMap[text] || $t('i18n_1622dc9b6b') }}</n-tag>
              </span>
            </template>
            `${record.statusMsg || ''}`
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'clearOld'">
          <n-tooltip>
            <template #trigger>
              <n-switch
                size="small"
                :checked-label="$t('i18n_0a60ac8f02')"
                :unchecked-label="$t('i18n_c9744f45e7')"
                disabled
                :checked="text"
              />
            </template>
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'afterOpt'">
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span>{{
                    afterOptList.filter((item) => {
                      return item.value === text
                    }).length &&
                    afterOptList.filter((item) => {
                      return item.value === text
                    })[0].title
                  }}</span>
                </span>
              </span>
            </template>
            {{
              afterOptList.filter((item) => {
                return item.value === text
              }).length &&
              afterOptList.filter((item) => {
                return item.value === text
              })[0].title
            }}
          </n-tooltip>
        </template>

        <template v-else-if="column.dataIndex === 'outGivingProject'">
          <span v-if="text">{{ $t('i18n_0c1de8295a') }}</span>
          <span v-else>{{ $t('i18n_1c3cf7f5f0') }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'operation'">
          <n-space>
            <n-button size="small" type="primary" @click="handleDispatch(record)">
              {{ $t('i18n_59c316e560') }}
            </n-button>

            <n-button
              v-if="record.outGivingProject"
              size="small"
              type="primary"
              @click="handleEditDispatchProject(record)"
              >{{ $t('i18n_95b351c862') }}</n-button
            >
            <n-button v-else size="small" type="primary" @click="handleEditDispatch(record)">
              {{ $t('i18n_95b351c862') }}
            </n-button>

            <n-dropdown
              :options="[
                {
                  label: $t('i18n_30e855a053'),
                  key: '0',
                  disabled: record.status !== 1,
                  props: { onClick: () => handleCancel(record) }
                },
                {
                  label: record.outGivingProject ? $t('i18n_2f4aaddde3') : $t('i18n_cbdcabad50'),
                  key: '1',
                  props: { onClick: () => handleDelete(record, '') }
                },
                { label: $t('i18n_7327966572'), key: '2', props: { onClick: () => handleDelete(record, 'thorough') } },
                { label: $t('i18n_663393986e'), key: '3', props: { onClick: () => handleUnbind(record) } }
              ]"
            >
              <a @click="(e) => e.preventDefault()">
                {{ $t('i18n_0ec9eaf9c3') }}
                <DownOutlined />
              </a>
            </n-dropdown>
          </n-space>
        </template>
      </template>
    </CustomTable>
    <!-- 新增/编辑关联项目 -->
    <CustomModal
      v-if="linkDispatchVisible"
      v-model:open="linkDispatchVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="900px"
      :title="temp.type === 'edit' ? $t('i18n_5afe5e7ed4') : $t('i18n_c889b9f67d')"
      :mask-closable="false"
      @ok="handleLinkDispatchOk"
      @cancel="clearDispatchList"
    >
      <n-form ref="linkDispatchForm" :rules="rules" :model="temp">
        <n-form-item path="id">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_f6dee0f3ff') }}

                  <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                </span>
              </template>
              {{ $t('i18n_b28836fe97') }}
            </n-tooltip>
          </template>
          <n-input
            v-if="temp.type === 'edit'"
            v-model:value="temp.id"
            :max-length="50"
            :disabled="temp.type === 'edit'"
            :placeholder="$t('i18n_7ce511154f')"
          />
          <template v-else>
            <n-input
              v-model:value="temp.id"
              :max-length="50"
              :placeholder="$t('i18n_7ce511154f')"
              @keyup.enter="
                () => {
                  temp = { ...temp, id: randomStr(6) }
                }
              "
            >
              <template #enterButton>
                <n-button type="primary">
                  {{ $t('i18n_6709f4548f') }}
                </n-button>
              </template>
            </n-input>
          </template>
          <!-- <n-input v-model="temp.id" :maxLength="50" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改" /> -->
        </n-form-item>

        <n-form-item :label="$t('i18n_9d89cbf245')" path="name">
          <n-grid>
            <n-grid-item :span="10">
              <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_9d89cbf245')" />
            </n-grid-item>
            <n-grid-item :span="4" style="text-align: right">{{ $t('i18n_1b973fc4d1') }}</n-grid-item>
            <n-grid-item :span="10">
              <n-form-item>
                <custom-select
                  v-model:value="temp.group"
                  :max-length="50"
                  :data="groupList"
                  :input-placeholder="$t('i18n_bd0362bed3')"
                  :select-placeholder="$t('i18n_3e8c9c54ee')"
                >
                  <template #suffix>
                    <n-tooltip>
                      <template #trigger>
                        <QuestionCircleOutlined />
                      </template>

                      <div>
                        {{ $t('i18n_bd7c7abc8c') }}
                      </div>
                    </n-tooltip>
                  </template>
                </custom-select>
              </n-form-item>
            </n-grid-item>
          </n-grid>
        </n-form-item>

        <n-form-item :label="$t('i18n_6a6c857285')" required>
          <n-list
            item-layout="horizontal"
            :data="dispatchList"
            :locale="{
              emptyText: $t('i18n_cfd482e5ef')
            }"
            :row-key="
              (item) => {
                return item.nodeId + item.projectId + item.index
              }
            "
          >
            <template #renderItem="{ item, index }">
              <n-list-item>
                <n-space>
                  <div>{{ $t('i18n_9b280a6d2d') }}</div>
                  <n-select
                    :placeholder="$t('i18n_f8a613d247')"
                    :not-found-content="$t('i18n_8f8f88654f')"
                    style="width: 140px"
                    :value="item.nodeId ? item.nodeId : undefined"
                    :disabled="
                      item.nodeId || (nodeIdMap[item.nodeId] && nodeIdMap[item.nodeId].openStatus !== 1) ? true : false
                    "
                    :options="
                      nodeProjectsList.map((nodeItemList) => ({
                        label: nodeNameMap[nodeItemList.id],
                        value: nodeItemList.id
                      }))
                    "
                    @update:value="(nodeId) => handleNodeListChange(nodeId, index)"
                  />
                  <span>{{ $t('i18n_8198e4461a') }} </span>
                  <n-select
                    style="width: 300px"
                    :placeholder="item.placeholder"
                    :value="item.projectId ? item.projectId : undefined"
                    :disabled="dispatchList[index].disabled"
                    :options="
                      (
                        (nodeProjectsList.filter((nitem) => nitem.id == item.nodeId)[0] &&
                          nodeProjectsList.filter((nitem) => nitem.id == item.nodeId)[0].projects) ||
                        []
                      ).map((project) => ({
                        label: `${project.outGivingProject ? $t('i18n_8e2ed8ae0d') : ''} ${project.name}`,
                        value: project.projectId,
                        disabled:
                          project.outGivingProject ||
                          dispatchList.filter(
                            (d, nowIndex) =>
                              d.nodeId === project.nodeId && d.projectId === project.projectId && nowIndex !== index
                          ).length > 0
                      }))
                    "
                    @update:value="(projectId) => handleProjectChange(projectId, index)"
                  />
                  <n-button type="primary" danger size="small" @click="delDispachList(index)"
                    ><DeleteOutlined />
                  </n-button>
                </n-space>
              </n-list-item>
            </template>
          </n-list>
          <n-button type="primary" size="small" @click="addDispachList">{{ $t('i18n_66ab5e9f24') }}</n-button>
        </n-form-item>
        <n-form-item :label="$t('i18n_dfcc9e3c45')" path="afterOpt">
          <n-select
            v-model:value="temp.afterOpt"
            :placeholder="$t('i18n_3322338140')"
            :options="afterOptList.map((item) => ({ label: item.title, value: item.value }))"
          />
        </n-form-item>
        <n-form-item v-if="temp.afterOpt === 2 || temp.afterOpt === 3" path="intervalTime">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_a5d550f258') }}

                  <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                </span>
              </template>

              {{ $t('i18n_55b2d0904f') }}, {{ $t('i18n_e9ec2b0bee') }},{{ $t('i18n_c8c45e8467') }}
              <li>{{ $t('i18n_a0e31d89ff') }}</li>
            </n-tooltip>
          </template>
          <n-input-number
            v-model:value="temp.intervalTime"
            :min="0"
            :placeholder="$t('i18n_d7ac764d3a')"
            style="width: 100%"
          />
        </n-form-item>
        <n-form-item path="secondaryDirectory" :label="$t('i18n_871cc8602a')">
          <n-input v-model:value="temp.secondaryDirectory" :placeholder="$t('i18n_9c99e8bec9')" />
        </n-form-item>
        <n-form-item path="clearOld">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  {{ $t('i18n_2223ff647d') }}

                  <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                </span>
              </template>

              {{ $t('i18n_b343663a14') }}
            </n-tooltip>
          </template>
          <n-grid>
            <n-grid-item :span="4">
              <n-switch
                v-model:value="temp.clearOld"
                :checked-label="$t('i18n_0a60ac8f02')"
                :unchecked-label="$t('i18n_c9744f45e7')"
              />
            </n-grid-item>

            <n-grid-item :span="4" style="text-align: right">
              <n-tooltip v-if="temp.type !== 'edit'">
                <template #trigger>
                  <QuestionCircleOutlined />
                </template>
                {{ $t('i18n_300fbf3891') }}
              </n-tooltip>
              {{ $t('i18n_7b2cbfada9') }}
            </n-grid-item>
            <n-grid-item :span="4">
              <n-form-item>
                <n-switch
                  v-model:value="temp.uploadCloseFirst"
                  :checked-label="$t('i18n_0a60ac8f02')"
                  :unchecked-label="$t('i18n_c9744f45e7')"
              /></n-form-item>
            </n-grid-item>
          </n-grid>
        </n-form-item>
        <n-form-item path="webhook">
          <template #label>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  WebHooks
                  <QuestionCircleOutlined v-show="!temp.id" />
                </span>
              </template>

              <ul>
                <li>{{ $t('i18n_b186c667dc') }}</li>
                <li>{{ $t('i18n_0bc45241af') }}</li>
                <li>status {{ $t('i18n_5d5fd4170f') }}:{{ $t('i18n_be3a4d368e') }}</li>
                <li>{{ $t('i18n_c96f47ec1b') }}</li>
              </ul>
            </n-tooltip>
          </template>
          <n-input v-model:value="temp.webhook" :placeholder="$t('i18n_89a40a1a8b')" />
        </n-form-item>
      </n-form>
    </CustomModal>
    <!-- 创建/编辑分发项目 -->
    <CustomModal
      v-if="editDispatchVisible"
      v-model:open="editDispatchVisible"
      destroy-on-close
      :confirm-loading="confirmLoading"
      width="60vw"
      :title="temp.type === 'edit' ? $t('i18n_6c72e9d9de') : $t('i18n_934156d92c')"
      :mask-closable="false"
      @ok="handleEditDispatchOk"
    >
      <n-spin :tip="$t('i18n_4360e5056b')" :spinning="editDispatchLoading">
        <n-form ref="editDispatchForm" :rules="rules" :model="temp">
          <n-alert
            v-if="!nodeList || !nodeList.length"
            :title="$t('i18n_4b027f3979')"
            type="warning"
            show-icon
            style="margin-bottom: 10px"
          >
            <template #description>{{ $t('i18n_c3aeddb10d') }}</template>
          </n-alert>
          <n-form-item path="id">
            <template #label>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    {{ $t('i18n_f6dee0f3ff') }}

                    <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                  </span>
                </template>
                {{ $t('i18n_b28836fe97') }}
              </n-tooltip>
            </template>
            <n-input
              v-if="temp.type === 'edit'"
              v-model:value="temp.id"
              :max-length="50"
              :disabled="temp.type === 'edit'"
              :placeholder="$t('i18n_ef651d15b0')"
            />
            <template v-else>
              <n-input
                v-model:value="temp.id"
                :max-length="50"
                :placeholder="$t('i18n_ef651d15b0')"
                @keyup.enter="
                  () => {
                    temp = { ...temp, id: randomStr(6) }
                  }
                "
              >
                <template #enterButton>
                  <n-button type="primary">
                    {{ $t('i18n_6709f4548f') }}
                  </n-button>
                </template>
              </n-input>
            </template>
            <!-- <n-input v-model="temp.id" :maxLength="50" :disabled="temp.type === 'edit'" placeholder="创建之后不能修改,分发 ID 等同于项目 ID" /> -->
          </n-form-item>
          <n-form-item :label="$t('i18n_9d89cbf245')" path="name">
            <n-grid>
              <n-grid-item :span="10">
                <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_1ed46c4a59')" />
              </n-grid-item>
              <n-grid-item :span="4" style="text-align: right">{{ $t('i18n_1b973fc4d1') }}</n-grid-item>
              <n-grid-item :span="10">
                <n-form-item>
                  <custom-select
                    v-model:value="temp.group"
                    :max-length="50"
                    :data="groupList"
                    :input-placeholder="$t('i18n_bd0362bed3')"
                    :select-placeholder="$t('i18n_3e8c9c54ee')"
                  >
                    <template #suffix>
                      <n-tooltip>
                        <template #trigger>
                          <QuestionCircleOutlined />
                        </template>

                        <div>
                          {{ $t('i18n_bd7c7abc8c') }}
                        </div>
                      </n-tooltip>
                    </template>
                  </custom-select>
                </n-form-item>
              </n-grid-item>
            </n-grid>
          </n-form-item>

          <n-form-item path="runMode">
            <template #label>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    {{ $t('i18n_17d444b642') }}

                    <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                  </span>
                </template>

                <ul>
                  <li><b>Dsl</b> {{ $t('i18n_2356fe4af2') }}</li>
                  <li>
                    <b>ClassPath</b> java -classpath xxx
                    {{ $t('i18n_fa4aa1b93b') }}
                  </li>
                  <li><b>Jar</b> java -jar xxx {{ $t('i18n_fa4aa1b93b') }}</li>
                  <li>
                    <b>JarWar</b> java -jar Springboot war
                    {{ $t('i18n_fa4aa1b93b') }}
                  </li>
                  <li>
                    <b>JavaExtDirsCp</b> java -Djava.ext.dirs=lib -cp conf:run.jar $MAIN_CLASS
                    {{ $t('i18n_fa4aa1b93b') }}
                  </li>
                  <li><b>File</b> {{ $t('i18n_5d6f47d670') }},{{ $t('i18n_61955b0e4b') }}</li>
                </ul>
              </n-tooltip>
            </template>

            <n-select
              v-model:value="temp.runMode"
              :placeholder="$t('i18n_26a3378645')"
              :options="
                runModeArray
                  .filter((item) => item.onlyNode !== true)
                  .map((item) => ({ label: `[${item.name}] ${item.desc}`, value: item.name }))
              "
            />
          </n-form-item>

          <n-form-item path="whitelistDirectory" class="voyager1-project-whitelist">
            <template #label>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    {{ $t('i18n_aabdc3b7c0') }}

                    <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                  </span>
                </template>

                <ul>
                  <li>{{ $t('i18n_f89cc4807e') }}</li>
                  <li>{{ $t('i18n_353707f491') }}</li>
                  <li>{{ $t('i18n_fe828cefd9') }}</li>
                  <li>
                    {{ $t('i18n_556499017a') }} <br />&nbsp;&nbsp;<b>{{ $t('i18n_67141abed6') }}</b>
                  </li>
                </ul>
              </n-tooltip>
            </template>
            <n-input-group compact>
              <n-select
                v-model:value="temp.whitelistDirectory"
                style="width: 50%"
                :placeholder="$t('i18n_1d38b2b2bc')"
                :options="accessList"
              />
              <n-form-item>
                <n-input v-model:value="temp.lib" style="width: 50%" :placeholder="$t('i18n_d6937acda5')"
              /></n-form-item>
            </n-input-group>
            <template #help>
              <div>
                {{ $t('i18n_8e9bd127fb') }}

                <n-button
                  size="small"
                  text
                  @click="
                    () => {
                      configDir = true
                    }
                  "
                >
                  <InfoCircleOutlined /> {{ $t('i18n_1e5533c401') }}
                </n-button>
              </div>
            </template>
          </n-form-item>

          <n-form-item v-show="filePath !== ''" :label="$t('i18n_8283f063d7')">
            <n-alert :title="filePath" type="success" />
          </n-form-item>
          <n-form-item v-show="temp.runMode === 'Dsl'" path="dslContent">
            <template #label>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    DSL {{ $t('i18n_2d711b09bd') }}

                    <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                  </span>
                </template>

                <p>{{ $t('i18n_8d5956ca2a') }}</p>
                <p>{{ $t('i18n_3517aa30c2') }}</p>
                <p>
                  <b>status</b>
                  {{ $t('i18n_ca69dad8fc') }}:$pid <b>$pid {{ $t('i18n_07a8af8c03') }}</b
                  >{{ $t('i18n_d2f484ff7e') }}
                </p>
                <p>{{ $t('i18n_9f52492fbc') }}</p>
              </n-tooltip>
            </template>
            <template #help>
              <div>
                scriptId{{ $t('i18n_3241c7c05f') }}
                <n-button
                  text
                  size="small"
                  @click="
                    () => {
                      viewScriptVisible = true
                    }
                  "
                >
                  {{ $t('i18n_6de1ecc549') }}
                </n-button>
              </div>
            </template>
            <n-form-item>
              <!-- <n-tabs>
                <n-tab-pane name="1" tab="DSL 配置">
                  <code-editor
                    height="40vh"
                    v-model:content="temp.dslContent"
                    :options="{ mode: 'yaml', tabSize: 2 }"
                  ></code-editor>
                </n-tab-pane>
                <n-tab-pane name="2" tab="配置示例">
                  <code-editor
                    height="40vh"
                    v-model:content="PROJECT_DSL_DEFATUL"
                    :options="{
                      mode: 'yaml',
                      tabSize: 2,
                      readOnly: true
                    }"
                  ></code-editor>
                </n-tab-pane>
              </n-tabs> -->
              <code-editor
                v-show="dslEditTabKey === 'content'"
                v-model:content="temp.dslContent"
                height="40vh"
                :show-tool="true"
                :options="{ mode: 'yaml', tabSize: 2 }"
                :placeholder="$t('i18n_1c8190b0eb')"
              >
                <template #tool_before>
                  <n-radio-group v-model:value="dslEditTabKey" size="small">
                    <n-radio-button value="content">DSL {{ $t('i18n_224e2ccda8') }}</n-radio-button>
                    <n-radio-button value="demo">{{ $t('i18n_da79c2ec32') }}</n-radio-button>
                  </n-radio-group>
                </template>
              </code-editor>
              <code-editor
                v-show="dslEditTabKey === 'demo'"
                v-model:content="PROJECT_DSL_DEFATUL"
                height="40vh"
                :show-tool="true"
                :options="{ mode: 'yaml', tabSize: 2, readOnly: true }"
              >
                <template #tool_before>
                  <n-radio-group v-model:value="dslEditTabKey" size="small">
                    <n-radio-button value="content">DSL {{ $t('i18n_224e2ccda8') }}</n-radio-button>
                    <n-radio-button value="demo">{{ $t('i18n_da79c2ec32') }}</n-radio-button>
                  </n-radio-group>
                </template>
              </code-editor>
            </n-form-item>
          </n-form-item>
          <n-form-item v-show="noFileModes.includes(temp.runMode)">
            <template #label>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    {{ $t('i18n_2ce44aba57') }}

                    <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                  </span>
                </template>

                <ul>
                  <li>{{ $t('i18n_12934d1828') }}</li>
                  <li>{{ $t('i18n_138776a1dc') }}</li>
                  <li>{{ $t('i18n_95c5c939e4') }}</li>
                </ul>
              </n-tooltip>
            </template>
            <n-select v-model:value="temp.logPath" :placeholder="$t('i18n_99f0996c0a')" :options="accessList" />
          </n-form-item>
          <n-form-item v-show="noFileModes.includes(temp.runMode)">
            <template #label>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    {{ $t('i18n_340eb70415') }}
                    <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                  </span>
                </template>

                <ul>
                  <li>{{ $t('i18n_401c396b51') }}</li>
                  <li>
                    {{ $t('i18n_aef1a0752a') }}
                  </li>
                </ul>
              </n-tooltip>
            </template>
            <n-input v-model:value="temp.logCharset" :placeholder="$t('i18n_c6c2497dbe')" />
          </n-form-item>
          <n-form-item
            v-show="javaModes.includes(temp.runMode) && temp.runMode !== 'Jar'"
            label="Main Class"
            path="mainClass"
          >
            <n-input v-model:value="temp.mainClass" :placeholder="$t('i18n_ef800ed466')" />
          </n-form-item>
          <n-form-item
            v-show="javaModes.includes(temp.runMode) && temp.runMode === 'JavaExtDirsCp'"
            label="JavaExtDirsCp"
            path="javaExtDirsCp"
          >
            <n-input
              v-model:value="temp.javaExtDirsCp"
              :placeholder="`-Dext.dirs=xxx: -cp xx  ${$t('i18n_c53021f06d')}:xx】`"
            />
          </n-form-item>
          <n-form-item :label="$t('i18n_dfcc9e3c45')" path="afterOpt">
            <n-select
              v-model:value="temp.afterOpt"
              :placeholder="$t('i18n_3322338140')"
              :options="afterOptList.map((item) => ({ label: item.title, value: item.value }))"
            />
          </n-form-item>
          <n-form-item v-if="temp.afterOpt === 2 || temp.afterOpt === 3" path="intervalTime">
            <template #label>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    {{ $t('i18n_a5d550f258') }}

                    <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                  </span>
                </template>

                {{ $t('i18n_55b2d0904f') }},{{ $t('i18n_e9ec2b0bee') }},{{ $t('i18n_c8c45e8467') }}
                <li>{{ $t('i18n_a0e31d89ff') }}</li>
              </n-tooltip>
            </template>
            <n-input-number
              v-model:value="temp.intervalTime"
              :min="0"
              :placeholder="$t('i18n_d7ac764d3a')"
              style="width: 100%"
            />
          </n-form-item>
          <n-form-item path="secondaryDirectory" :label="$t('i18n_871cc8602a')">
            <n-input v-model:value="temp.secondaryDirectory" :placeholder="$t('i18n_9c99e8bec9')" />
          </n-form-item>
          <n-form-item path="clearOld">
            <template #label>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    {{ $t('i18n_2223ff647d') }}

                    <QuestionCircleOutlined v-if="temp.type !== 'edit'" />
                  </span>
                </template>

                {{ $t('i18n_b343663a14') }}
              </n-tooltip>
            </template>
            <n-grid>
              <n-grid-item :span="4">
                <n-switch
                  v-model:value="temp.clearOld"
                  :checked-label="$t('i18n_0a60ac8f02')"
                  :unchecked-label="$t('i18n_c9744f45e7')"
                />
              </n-grid-item>
              <n-grid-item :span="4" style="text-align: right">
                <n-tooltip v-if="temp.type !== 'edit'">
                  <template #trigger>
                    <QuestionCircleOutlined />
                  </template>
                  {{ $t('i18n_300fbf3891') }}
                </n-tooltip>
                {{ $t('i18n_7b2cbfada9') }}
              </n-grid-item>
              <n-grid-item :span="4">
                <n-form-item>
                  <n-switch
                    v-model:value="temp.uploadCloseFirst"
                    :checked-label="$t('i18n_0a60ac8f02')"
                    :unchecked-label="$t('i18n_c9744f45e7')"
                  />
                </n-form-item>
              </n-grid-item>
            </n-grid>
          </n-form-item>
          <n-form-item path="webhook">
            <template #label>
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    WebHooks

                    <QuestionCircleOutlined v-show="!temp.id" />
                  </span>
                </template>

                <ul>
                  <li>{{ $t('i18n_b186c667dc') }}</li>
                  <li>{{ $t('i18n_0bc45241af') }}</li>
                  <li>status {{ $t('i18n_5d5fd4170f') }}:{{ $t('i18n_be3a4d368e') }}</li>
                  <li>{{ $t('i18n_c96f47ec1b') }}</li>
                </ul>
              </n-tooltip>
            </template>
            <n-input v-model:value="temp.webhook" :placeholder="$t('i18n_89a40a1a8b')" />
          </n-form-item>
          <!-- 节点 -->
          <n-form-item :label="$t('i18n_6a6c857285')" path="nodeId">
            <n-select
              v-model:value="temp.nodeIdList"
              filterable
              multiple
              :placeholder="$t('i18n_b07a33c3a8')"
              :options="nodeList.map((node) => ({ label: `${node.name}`, value: node.id }))"
            />
          </n-form-item>
          <n-collapse>
            <n-collapse-item v-for="nodeId in temp.nodeIdList" :key="nodeId" :header="nodeNameMap[nodeId] || nodeId">
              <n-form-item v-show="javaModes.includes(temp.runMode)" :label="$t('i18n_497bc3532b')" path="jvm">
                <n-input
                  v-model:value="temp[`${nodeId}_jvm`]"
                  type="textarea"
                  :auto-size="{ minRows: 3, maxRows: 3 }"
                  :placeholder="$t('i18n_eef3653e9a', { slot1: $t('i18n_3d0a2df9ec'), slot2: $t('i18n_eb5bab1c31') })"
                />
              </n-form-item>
              <n-form-item v-show="javaModes.includes(temp.runMode)" :label="$t('i18n_e5098786d3')" path="args">
                <n-input
                  v-model:value="temp[`${nodeId}_args`]"
                  type="textarea"
                  :auto-size="{ minRows: 3, maxRows: 3 }"
                  :placeholder="`Main ${$t('i18n_6a9231c3ba')}. ${$t('i18n_848e4e21da')}.port=8080`"
                />
              </n-form-item>
              <n-form-item v-show="noFileModes.includes(temp.runMode)" path="autoStart">
                <template #label>
                  <n-tooltip>
                    <template #trigger>
                      <span class="tw">
                        {{ $t('i18n_8388c637f6') }}

                        <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                      </span>
                    </template>
                    {{ $t('i18n_d4e03f60a9') }}
                  </n-tooltip>
                </template>
                <template #help>
                  <div>
                    {{ $t('i18n_71584de972') }}<b>{{ $t('i18n_1e4a59829d') }}</b
                    >{{ $t('i18n_0360fffb40') }}
                  </div>
                </template>
                <n-switch
                  v-model:value="temp[`${nodeId}_autoStart`]"
                  :checked-label="$t('i18n_8493205602')"
                  :unchecked-label="$t('i18n_d58a55bcee')"
                />
                {{ $t('i18n_1022c545d1') }}
              </n-form-item>
              <n-form-item path="disableScanDir">
                <template #label>
                  <n-tooltip>
                    <template #trigger> {{ $t('i18n_df59a2804d') }} </template>
                  </n-tooltip>
                </template>
                <template #help>
                  <div>{{ $t('i18n_b7c139ed75') }}</div>
                </template>
                <div>
                  <n-switch
                    v-model:value="temp[`${nodeId}_disableScanDir`]"
                    :checked-label="$t('i18n_ced3d28cd1')"
                    :unchecked-label="$t('i18n_56525d62ac')"
                  />
                </div>
              </n-form-item>
              <n-form-item v-if="temp.runMode === 'Dsl'" path="dslEnv" :label="$t('i18n_fba5f4f19a')">
                <!-- <n-input
                  v-model:value="temp[`${nodeId}_dslEnv`]"
                  placeholder="DSL{{$t('i18n_3867e350eb')}},{{$t('i18n_9324290bfe')}}=values1&keyvalue2"
                /> -->
                <parameter-widget v-model:value="temp[`${nodeId}_dslEnv`]"></parameter-widget>
              </n-form-item>
              <n-form-item v-show="noFileModes.includes(temp.runMode)" path="token">
                <template #label>
                  <n-tooltip>
                    <template #trigger>
                      <span class="tw">
                        WebHooks
                        <QuestionCircleOutlined v-show="temp.type !== 'edit'" />
                      </span>
                    </template>

                    <ul>
                      <li>{{ $t('i18n_f7b9764f0f') }}</li>
                      <li>{{ $t('i18n_b91961bf0b') }}</li>
                      <li>type {{ $t('i18n_b10b082c25') }}</li>
                    </ul>
                  </n-tooltip>
                </template>
                <n-input v-model:value="temp[`${nodeId}_token`]" :placeholder="$t('i18n_6c776e9d91')" />
              </n-form-item>
            </n-collapse-item>
          </n-collapse>
        </n-form>
      </n-spin>
    </CustomModal>
    <!-- 分发项目 -->
    <start-dispatch
      v-if="dispatchVisible"
      :data="temp"
      @cancel="
        () => {
          dispatchVisible = false
          loadData()
        }
      "
    />

    <!-- 分发状态 -->
    <Status
      v-if="drawerStatusVisible"
      :id="temp.id"
      :name="temp.name"
      @close="
        () => {
          drawerStatusVisible = false
        }
      "
    />
    <!-- 配置工作空间授权目录 -->
    <CustomModal
      v-if="configDir"
      v-model:open="configDir"
      destroy-on-close
      :title="`${$t('i18n_eee6510292')}`"
      :footer="null"
      width="50vw"
      :mask-closable="false"
      @cancel="
        () => {
          configDir = false
        }
      "
    >
      <whiteList
        v-if="configDir"
        @cancel="
          () => {
            configDir = false
            loadAccesList()
          }
        "
      ></whiteList>
    </CustomModal>
    <!-- 查看服务端脚本 -->
    <CustomDrawer
      v-if="viewScriptVisible"
      destroy-on-close
      :title="`${$t('i18n_dd1d14efd6')}`"
      placement="right"
      :open="viewScriptVisible"
      width="70vw"
      @close="
        () => {
          viewScriptVisible = false
        }
      "
    >
      <scriptPage
        v-if="viewScriptVisible"
        choose="checkbox"
        @cancel="
          () => {
            viewScriptVisible = false
          }
        "
      ></scriptPage>
    </CustomDrawer>
  </div>
</template>
<script>
import {
  DeleteOutlined,
  DownOutlined,
  FullscreenOutlined,
  InfoCircleOutlined,
  QuestionCircleOutlined
} from '@ant-design/icons-vue'

import { h } from 'vue'
import { NIcon } from 'naive-ui'

import Status from './status'
import codeEditor from '@/components/codeEditor'
import {
  afterOptList,
  delDisPatchProject,
  editDispatch,
  editDispatchProject,
  getDishPatchList,
  getDispatchWhiteList,
  releaseDelDisPatch,
  statusMap,
  unbindOutgiving,
  dispatchStatusMap,
  cancelOutgiving
} from '@/api/dispatch'
import { getNodeListAll, getProjectListAll } from '@/api/node'
import { getProjectData, javaModes, noFileModes, runModeArray, getProjectGroupAll } from '@/api/node-project'
import {
  CHANGE_PAGE,
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  randomStr,
  itemGroupBy,
  parseTime
} from '@/utils/const'
import { PROJECT_DSL_DEFATUL } from '@/utils/const-i18n'
import scriptPage from '@/pages/script/script-list'
import CustomSelect from '@/components/customSelect'
import whiteList from '@/pages/dispatch/white-list'
import StartDispatch from './start'
export default {
  components: {
    codeEditor,
    CustomSelect,
    whiteList,
    Status,
    StartDispatch,
    scriptPage
  },
  data() {
    return {
      loading: false,
      confirmLoading: false,
      editDispatchLoading: false,
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      statusMap,
      javaModes,
      noFileModes,
      runModeArray,
      dispatchStatusMap,
      PROJECT_DSL_DEFATUL,
      list: [],
      accessList: [],
      nodeList: [],
      projectList: [],
      groupList: [],
      afterOptList,
      targetKeys: [],
      // reqId: "",
      temp: {},
      configDir: false,
      viewScriptVisible: false,
      linkDispatchVisible: false,
      editDispatchVisible: false,
      dispatchVisible: false,

      nodeProjectsList: [],
      nodeNameMap: {},
      nodeIdMap: {},
      dispatchList: [],
      totalProjectNum: 0,
      columns: [
        {
          title: this.$t('i18n_f6dee0f3ff'),
          key: 'id',
          ellipsis: true,
          width: 110
        },
        {
          title: this.$t('i18n_9d89cbf245'),
          key: 'name',
          ellipsis: true,
          width: 200
        },
        {
          title: this.$t('i18n_d438e83c16'),
          key: 'group',
          sorter: true,
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_9e2e02ef08'),
          key: 'outGivingProject',
          width: '90px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_c71a67ab03'),
          key: 'afterOpt',
          ellipsis: true,
          width: '150px'
        },
        {
          title: this.$t('i18n_2223ff647d'),
          key: 'clearOld',
          align: 'center',
          ellipsis: true,
          width: '100px'
        },
        {
          title: this.$t('i18n_a5d550f258'),
          key: 'intervalTime',
          width: 90,
          ellipsis: true
        },

        {
          title: this.$t('i18n_3fea7ca76c'),
          key: 'status',
          ellipsis: true,
          width: 110
        },
        {
          title: this.$t('i18n_871cc8602a'),
          key: 'secondaryDirectory',
          ellipsis: true,
          width: 110
        },
        {
          title: this.$t('i18n_eca37cb072'),
          key: 'createTimeMillis',
          ellipsis: true,
          sorter: true,
          render: (row) => parseTime(row['createTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_1303e638b5'),
          key: 'modifyTimeMillis',
          ellipsis: true,
          sorter: true,
          render: (row) => parseTime(row['modifyTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_9baca0054e'),
          key: 'modifyUser',
          width: '130px',
          ellipsis: true,
          sorter: true
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',

          fixed: 'right',
          width: '210px',
          align: 'center'
        }
      ],

      rules: {
        id: [{ required: true, message: this.$t('i18n_646a518953'), trigger: 'blur' }],
        name: [{ required: true, message: this.$t('i18n_4371e2b426'), trigger: 'blur' }],
        projectId: [{ required: true, message: this.$t('i18n_9fc2e26bfa'), trigger: 'blur' }],
        runMode: [{ required: true, message: this.$t('i18n_4310e9ed7d'), trigger: 'blur' }],
        whitelistDirectory: [{ required: true, message: this.$t('i18n_1d38b2b2bc'), trigger: 'blur' }],
        lib: [{ required: true, message: this.$t('i18n_d9657e2b5f'), trigger: 'blur' }],
        afterOpt: [{ required: true, message: this.$t('i18n_3322338140'), trigger: 'blur' }]
      },
      // countdownTime: Date.now(),
      // refreshInterval: 5,

      viewDispatchManager: false,
      dslEditTabKey: 'content',
      drawerStatusVisible: false
    }
  },
  computed: {
    filePath() {
      return (this.temp.whitelistDirectory || '') + (this.temp.lib || '')
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    },
    // 分页
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    }
  },
  watch: {},
  created() {
    this.loadData()
    this.loadNodeList()
    this.loadGroupList()
  },
  methods: {
    randomStr,
    CHANGE_PAGE,

    // 静默
    // silenceLoadData() {
    //   if (this.$attrs.routerUrl !== this.$route.path) {
    //     //   // 重新计算倒计时
    //     //   // this.countdownTime = Date.now() + this.refreshInterval * 1000
    //     return
    //   }
    //   getDishPatchList(this.listQuery, false).then((res) => {
    //     if (res.code === 200) {
    //       this.list = res.data.result
    //       this.listQuery.total = res.data.total
    //       //

    //       // 重新计算倒计时
    //       // this.countdownTime = Date.now() + this.refreshInterval * 1000
    //     }
    //   })
    // },
    // 加载数据
    loadData(pointerEvent) {
      return new Promise((resolve) => {
        this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
        this.loading = true

        // = false;
        getDishPatchList(this.listQuery).then((res) => {
          if (res.code === 200) {
            this.list = res.data.result
            this.listQuery.total = res.data.total
            // 重新计算倒计时
            this.countdownTime = Date.now() + this.refreshInterval * 1000
          }
          this.loading = false
          resolve()
        })
      })
    },
    // 加载项目授权列表
    loadAccesList() {
      getDispatchWhiteList().then((res) => {
        if (res.code === 200) {
          this.accessList = res.data.outGivingArray || []
        }
      })
    },
    loadGroupList() {
      getProjectGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },

    // 关联分发
    handleLink() {
      this.$refs['linkDispatchForm'] && this.$refs['linkDispatchForm'].resetFields()
      this.temp = {
        type: 'add',
        id: '',
        name: '',
        projectId: ''
      }
      this.loadNodeList(() => {
        this.loadProjectListAll()
      })
      this.linkDispatchVisible = true
    },
    // 编辑分发
    handleEditDispatch(record) {
      this.$nextTick(() => {
        this.$refs['linkDispatchForm'] && this.$refs['linkDispatchForm'].resetFields()
      })
      this.loadNodeList(() => {
        this.loadProjectListAll(() => {
          //分发节点重新渲染
          this.temp = {}
          this.dispatchList = []
          JSON.parse(record.outGivingNodeProjectList).forEach((ele) => {
            this.dispatchList.push({
              nodeId: ele.nodeId,
              projectId: ele.projectId,
              index: this.dispatchList.length
              // project: this.nodeProjectsList.filter((item) => item.id === ele.nodeId)[0].projects,
            })
            // console.log(this.dispatchList);
          })
          this.temp = { ...this.temp }

          this.temp = {
            ...this.temp,
            type: 'edit',
            projectId: record.projectId,
            name: record.name,
            afterOpt: record.afterOpt,
            id: record.id,
            intervalTime: record.intervalTime,
            clearOld: record.clearOld,
            secondaryDirectory: record.secondaryDirectory || '',
            uploadCloseFirst: record.uploadCloseFirst,
            group: record.group,
            webhook: record.webhook
          }
          // console.log(this.temp);
          this.linkDispatchVisible = true
        })
      })
    },
    // 选择项目
    selectProject(value) {
      this.targetKeys = []
      this.nodeList.forEach((node) => {
        node.disabled = true
      })
      this.nodeProjectMap[value].forEach((nodeId) => {
        this.nodeList.forEach((node) => {
          if (node.key === nodeId) {
            node.disabled = false
          }
        })
      })
    },
    // 穿梭框筛选
    filterOption(inputValue, option) {
      return option.title.indexOf(inputValue) > -1
    },
    // 穿梭框 change
    handleChange(targetKeys) {
      this.targetKeys = targetKeys
    },
    // 提交关联项目
    handleLinkDispatchOk() {
      // 检验表单
      this.$refs['linkDispatchForm'].validate().then(() => {
        // 校验分发节点数据
        if (this.dispatchList.length < 1) {
          $notification.error({ message: this.$t('i18n_637c9a8819') })
          return false
        }
        this.dispatchList.forEach((item, index) => {
          this.temp['node_' + item.nodeId + '_' + index] = item.projectId
        })
        this.confirmLoading = true
        // 提交
        editDispatch(this.temp)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.targetKeys = []
              this.$refs['linkDispatchForm'].resetFields()
              this.linkDispatchVisible = false
              this.clearDispatchList()
              this.loadData()
            } else {
              this.targetKeys = []
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 新增分发项目
    handleAdd() {
      this.loadNodeList(() => {
        this.temp = {
          type: 'add',
          id: '',
          name: '',
          afterOpt: undefined,
          runMode: undefined,
          mainClass: '',
          javaExtDirsCp: '',
          whitelistDirectory: undefined,
          lib: '',
          nodeIdList: [],
          intervalTime: undefined,
          clearOld: false
        }

        this.loadAccesList()
        this.loadGroupList()

        this.editDispatchVisible = true

        this.$refs['editDispatchForm']?.resetFields()
      })
    },
    // 编辑分发项目
    handleEditDispatchProject(record) {
      this.$nextTick(() => {
        this.$refs['editDispatchForm'] && this.$refs['editDispatchForm'].resetFields()
      })
      this.editDispatchLoading = true
      this.editDispatchVisible = true
      this.loadNodeList(() => {
        //
        this.temp = {}
        JSON.parse(record.outGivingNodeProjectList).forEach(async (ele) => {
          const params = {
            id: ele.projectId,
            nodeId: ele.nodeId
          }
          const res = await getProjectData(params)
          if (res.code === 200) {
            // 如果 temp.id 不存在
            if (!this.temp.id) {
              this.temp = {
                id: res.data.id,
                name: res.data.name,
                type: 'edit',
                afterOpt: record.afterOpt,
                runMode: res.data.runMode,
                mainClass: res.data.mainClass,
                javaExtDirsCp: res.data.javaExtDirsCp,
                whitelistDirectory: res.data.whitelistDirectory,
                lib: res.data.lib,
                logPath: res.data.logPath,
                logCharset: res.data.logCharset,
                dslContent: res.data.dslContent,
                nodeIdList: [],
                intervalTime: record.intervalTime,
                clearOld: record.clearOld,
                secondaryDirectory: record.secondaryDirectory,
                uploadCloseFirst: record.uploadCloseFirst,
                group: record.group,
                webhook: record.webhook
              }
            }
            // 新增 nodeIdList
            this.temp.nodeIdList.push(ele.nodeId)
            // 新增 jvm token args
            this.temp[`${ele.nodeId}_jvm`] = res.data.jvm || ''
            this.temp[`${ele.nodeId}_token`] = res.data.token || ''
            this.temp[`${ele.nodeId}_args`] = res.data.args || ''
            this.temp[`${ele.nodeId}_autoStart`] = res.data.autoStart
            this.temp[`${ele.nodeId}_disableScanDir`] = res.data.disableScanDir
            this.temp[`${ele.nodeId}_dslEnv`] = res.data.dslEnv || ''

            this.temp = { ...this.temp }
          }
        })

        // 加载其他数据
        this.loadAccesList()
        this.loadGroupList()

        this.editDispatchLoading = false
      })
    },

    // 提交创建分发项目
    handleEditDispatchOk() {
      // 检验表单
      this.$refs['editDispatchForm'].validate().then(() => {
        const tempData = Object.assign({}, this.temp)
        // 检查
        if (tempData.nodeIdList.length < 1) {
          $notification.warn({
            message: this.$t('i18n_b8545de30e')
          })
          return false
        }
        // 设置 reqId
        // this.temp.reqId = this.reqId;

        tempData.nodeIds = tempData.nodeIdList.join(',')
        delete tempData.nodeIdList
        this.confirmLoading = true
        // 提交
        editDispatchProject(tempData)
          .then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.$refs['editDispatchForm'].resetFields()
              this.editDispatchVisible = false
              this.loadData()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },

    // 处理分发
    handleDispatch(record) {
      this.temp = Object.assign({ type: 'upload' }, record)
      this.dispatchVisible = true
    },

    // 删除
    handleDelete(record, thorough) {
      if (record.outGivingProject) {
        $confirm({
          title: this.$t('i18n_c4535759ee'),
          zIndex: 1009,
          content: thorough ? this.$t('i18n_78ba02f56b') : this.$t('i18n_1125c4a50b'),
          okText: this.$t('i18n_e83a256e4f'),
          cancelText: this.$t('i18n_625fb26b4b'),
          onOk: () => {
            return delDisPatchProject({ id: record.id, thorough: thorough }).then((res) => {
              if (res.code === 200) {
                $notification.success({
                  message: res.msg
                })
                this.loadData()
              }
            })
          }
        })
        return
      }
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_2f8dc4fb66'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return releaseDelDisPatch(record.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
            }
          })
        }
      })
    },
    // 解绑
    handleUnbind(record) {
      const html = `
      <b style='font-size: 20px;'>
        ${this.$t('i18n_efe71e9bec')}
      </b>
      <ul style='font-size: 20px;color:red;font-weight: bold;'>
          <li>${this.$t('i18n_46aca09f01')}</b></li>
          <li>${this.$t('i18n_5c93055d9c')}</li>
          <li>${this.$t('i18n_27d0c8772c')}</li>
      </ul>`

      $confirm({
        title: this.$t('i18n_9362e6ddf8'),
        zIndex: 1009,
        content: h('div', null, [h('p', { innerHTML: html }, null)]),
        okButtonProps: { type: 'primary', size: 'small', danger: true },
        cancelButtonProps: { type: 'primary' },
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return unbindOutgiving(record.id).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })

              this.loadData()
            }
          })
        }
      })
    },

    loadProjectListAll(fn) {
      getProjectListAll().then((res) => {
        if (res.code === 200) {
          this.totalProjectNum = res.data ? res.data.length : 0
          this.nodeProjectsList = itemGroupBy(res.data, 'nodeId', 'id', 'projects')
          // console.log(this.nodeProjectsList);
          fn && fn()
        }
      })
    },
    // 加载节点以及项目
    loadNodeList(fn) {
      this.nodeList = []
      getNodeListAll().then((res) => {
        if (res.code === 200) {
          this.nodeList = res.data
          this.nodeList.map((item) => {
            this.nodeNameMap[item.id] = item.name
            this.nodeIdMap[item.id] = item
          })
          fn && fn()
        }
      })
    },
    // 选择节点
    handleNodeListChange(nodeId, index) {
      const nodeData = this.nodeProjectsList.filter((item) => item.id === nodeId)[0]
      if (nodeData.projects.length === 0) {
        this.dispatchList[index].placeholder = this.$t('i18n_1d843d7b45')
        this.dispatchList[index].disabled = true
      } else {
        this.dispatchList[index].placeholder = this.$t('i18n_9fc2e26bfa')
        this.dispatchList[index].disabled = false
      }
      this.dispatchList[index].nodeId = nodeId
    },
    // 选择项目
    handleProjectChange(value, index) {
      this.dispatchList[index].projectId = value
    },
    // 新增分发
    addDispachList() {
      if (this.dispatchList.length >= this.totalProjectNum) {
        $notification.error({
          message: this.$t('i18n_d35a9990f4')
        })
        return false
      }
      this.dispatchList.push({
        index: this.dispatchList.length,
        placeholder: this.$t('i18n_3ee7756087'),
        disabled: true
      })
    },
    // 删除分发
    delDispachList(value) {
      this.dispatchList.splice(value, 1)
    },
    // 清理缓存
    clearDispatchList() {
      this.dispatchList = []
    },

    // 取消
    handleCancel(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_b1192f8f8e'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return cancelOutgiving({ id: record.id }).then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.loadData()
              // this.silenceLoadData()
            }
          })
        }
      })
    },
    // 查看项目状态
    handleViewStatus(item) {
      this.drawerStatusVisible = true
      this.temp = { ...item }
    }
  }
}
</script>
<style scoped>
:deep(.n-statistic .n-statistic-value__content),
:deep(.n-statistic .n-statistic-value__prefix),
:deep(.n-statistic .n-statistic-value__suffix) {
  font-size: 16px;
}
</style>
