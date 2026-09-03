<template>
  <div>
    <n-tabs default-active-key="1">
      <n-tab-pane name="1" :tab="$t('i18n_08b55fea3c')">
        <!-- 数据表格 -->
        <CustomTable
          is-show-tools
          default-auto-refresh
          :auto-refresh-time="5"
          table-name="assets-ssh-list"
          :empty-description="$t('i18n_13d10a9b78')"
          :active-page="activePage"
          :data="list"
          :columns="columns"
          size="medium"
          :pagination="pagination"
          bordered
          row-key="id"
          :row-selection="rowSelection"
          :scroll="{
            x: 'max-content'
          }"
          @change="changePage"
          @refresh="loadData"
        >
          <template #title>
            <n-space wrap class="search-box">
              <n-input
                v-model:value="listQuery['%name%']"
                class="search-input-item"
                :placeholder="$t('i18n_46ad87708f')"
                @press-enter="loadData"
              />
              <n-input
                v-model:value="listQuery['%host%']"
                class="search-input-item"
                placeholder="host"
                @press-enter="loadData"
              />
              <n-select
                v-model:value="listQuery.groupName"
                filterable
                clearable
                :placeholder="$t('i18n_829abe5a8d')"
                class="search-input-item"
                :options="groupList"
              />

              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    <n-button type="primary" :loading="loading" @click="loadData"
                      >{{ $t('i18n_e5f71fc31e') }}
                    </n-button>
                  </span>
                </template>
                $t('i18n_4838a3bd20')
              </n-tooltip>

              <n-button type="primary" @click="handleAdd">{{ $t('i18n_66ab5e9f24') }}</n-button>
              <n-button :disabled="!tableSelections.length" type="primary" @click="syncToWorkspaceShow()">
                {{ $t('i18n_82d2c66f47') }}</n-button
              >
              <n-button type="primary" @click="handlerExportData()"
                ><DownloadOutlined />{{ $t('i18n_55405ea6ff') }}</n-button
              >
              <n-dropdown
                :options="[
                  { label: $t('i18n_2e505d23f7'), key: '0', props: { onClick: () => handlerImportTemplate() } }
                ]"
              >
                <n-upload
                  name="file"
                  accept=".csv"
                  :show-file-list="false"
                  :multiple="false"
                  :custom-request="beforeUpload"
                >
                  <n-button type="primary"><UploadOutlined /> {{ $t('i18n_8d9a071ee2') }}<DownOutlined /> </n-button>
                </n-upload>
              </n-dropdown>
            </n-space>
          </template>
          <template #tableHelp>
            <n-tooltip>
              <template #trigger>
                <QuestionCircleOutlined />
              </template>

              <div>
                <ul>
                  <li>{{ $t('i18n_cc3a8457ea') }}</li>
                  <li>{{ $t('i18n_c4b5d36ff0') }}</li>
                  <li>{{ $t('i18n_1278df0cfc') }}</li>
                </ul>
              </div>
            </n-tooltip>
          </template>
          <template #tableBodyCell="{ column, text, record }">
            <template v-if="column.dataIndex === 'name'">
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    <n-button style="padding: 0" text size="small" @click="handleEdit(record)"> {{ text }}</n-button>
                  </span>
                </template>
                text
              </n-tooltip>
            </template>
            <template v-else-if="column.tooltip">
              <n-tooltip>
                <template #trigger>
                  {{ text }}
                </template>
                text
              </n-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'host'">
              <n-tooltip>
                <template #trigger> {{ text }}:{{ record.port }} </template>
                `${text}:${record.port}`
              </n-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'osName'">
              <n-popover>
                <template #trigger>
                  {{ text || $t('i18n_1622dc9b6b') }}
                </template>
                <template #header>{{ $t('i18n_b7ea5e506c') }}</template>

                <p>{{ $t('i18n_c17aefeebf') }}{{ record.osName }}</p>
                <p>{{ $t('i18n_f425f59044') }}{{ record.osVersion }}</p>
                <p>CPU{{ $t('i18n_045cd62da3') }}{{ record.osCpuIdentifierName }}</p>
                <p>{{ $t('i18n_07a0e44145') }}{{ record.hostName }}</p>
                <p>{{ $t('i18n_8a745296f4') }}{{ formatDuration(record.osSystemUptime) }}</p>
              </n-popover>
            </template>
            <template v-else-if="column.dataIndex === 'nodeId'">
              <template v-if="record.status !== 2">
                <!-- 禁用监控不显示 -->
                <div v-if="record.javaVersion">
                  <n-popover v-if="record.voyager1AgentPid > 0">
                    <template #trigger>
                      <span class="tw">
                        <n-tag color="green"> {{ record.voyager1AgentPid }}</n-tag>
                      </span>
                    </template>
                    <template #header>{{ $t('i18n_caf335a345') }}</template>

                    <p>{{ $t('i18n_b17299f3fb') }}{{ record.voyager1AgentPid }}</p>
                    <p>java{{ $t('i18n_2684c4634d') }}{{ record.javaVersion }}</p>
                  </n-popover>
                  <n-button v-else size="small" type="primary" @click="install(record)">{{
                    $t('i18n_334a1b5206')
                  }}</n-button>
                </div>

                <n-tag v-else color="orange">no java</n-tag>
              </template>
              <template v-else>-</template>
            </template>
            <template v-else-if="column.dataIndex === 'dockerInfo'">
              <template v-if="record.status !== 2">
                <!-- 禁用监控不显示 -->
                <n-popover v-if="record.dockerInfo">
                  <template #trigger>
                    <span class="tw">
                      <n-tag color="green">{{ $t('i18n_df9497ea98') }}</n-tag>
                    </span>
                  </template>
                  <template #header>{{ $t('i18n_5a7ea53d18') }}</template>

                  <p>{{ $t('i18n_461ec75a5a') }}{{ JSON.parse(record.dockerInfo).path }}</p>
                  <p>{{ $t('i18n_2684c4634d') }}{{ JSON.parse(record.dockerInfo).version }}</p>
                </n-popover>

                <n-tag v-else>{{ $t('i18n_d7d11654a7') }}</n-tag>
              </template>
              <template v-else>-</template>
            </template>
            <template v-else-if="column.dataIndex === 'status'">
              <n-tooltip>
                <template #trigger>
                  <span class="tw">
                    <n-tag :color="statusMap[record.status] && statusMap[record.status].color">{{
                      (statusMap[record.status] && statusMap[record.status].desc) || $t('i18n_1622dc9b6b')
                    }}</n-tag>
                  </span>
                </template>
                `${record.statusMsg || $t('i18n_77e100e462')}`
              </n-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'renderSize'">
              <n-tooltip placement="topLeft">
                <template #trigger>
                  <span class="tw">
                    <span class="tw">
                      <span>{{ renderSize(text) }}</span>
                    </span>
                  </span>
                </template>
                renderSize(text)
              </n-tooltip>
            </template>
            <template v-else-if="column.dataIndex === 'osOccupyMemory'">
              <n-tooltip placement="topLeft">
                <template #trigger>
                  <span class="tw">
                    <span class="tw">
                      <span>{{ formatPercent(record.osOccupyMemory) }}/{{ renderSize(record.osMoneyTotal) }}</span>
                    </span>
                  </span>
                </template>
                `${$t('i18n_ca32cdfd59')}${formatPercent(record.osOccupyMemory)},${$t( 'i18n_a0a3d583b9'
                )}${renderSize(record.osMoneyTotal)}`
              </n-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'osMaxOccupyDisk'">
              <n-popover>
                <template #trigger>
                  <span class="tw">
                    <span class="tw">
                      <span
                        >{{ formatPercent(record.osMaxOccupyDisk) }} / {{ renderSize(record.osFileStoreTotal) }}</span
                      >
                    </span>
                  </span>
                </template>
                <template #header>{{ $t('i18n_a74b62f4bb') }}</template>

                <p>{{ $t('i18n_7e359f4b71') }}{{ renderSize(record.osFileStoreTotal) }}</p>
                <p>{{ $t('i18n_de17fc0b78') }}{{ formatPercent(record.osMaxOccupyDisk) }}</p>
                <p>{{ $t('i18n_ba452d57f2') }}{{ record.osMaxOccupyDiskName }}</p>
              </n-popover>
            </template>

            <template v-else-if="column.dataIndex === 'osOccupyCpu'">
              <n-tooltip placement="topLeft">
                <template #trigger>
                  <span class="tw">
                    <span class="tw">
                      <span
                        >{{ (formatPercent2Number(record.osOccupyCpu) || '-') + '%' }} / {{ record.osCpuCores }}</span
                      >
                    </span>
                  </span>
                </template>
                `CPU${$t('i18n_afb9fe400b')}${formatPercent2Number(record.osOccupyCpu)}%,CPU${$t( 'i18n_40349f5514'
                )}${record.osCpuCores}`
              </n-tooltip>
            </template>

            <template v-else-if="column.dataIndex === 'operation'">
              <n-space>
                <n-dropdown
                  :options="[
                    {
                      label: $t('i18n_a3296ef4f6'),
                      key: '0',
                      icon: () => h(NIcon, null, { default: () => h(FullscreenOutlined) }),
                      props: { onClick: () => handleTerminal(record, true) }
                    }
                  ]"
                >
                  <n-button size="small" type="primary" @click="handleTerminal(record, false)"
                    >{{ $t('i18n_4722bc0c56') }}<DownOutlined
                  /></n-button>
                </n-dropdown>
                <n-button size="small" type="primary" @click="syncToWorkspaceShow(record)">{{
                  $t('i18n_e39de3376e')
                }}</n-button>
                <n-button size="small" type="primary" @click="handleFile(record)">{{ $t('i18n_2a0c4740f1') }}</n-button>
                <n-button size="small" type="primary" @click="handleViewWorkspaceSsh(record)">{{
                  $t('i18n_1c3cf7f5f0')
                }}</n-button>

                <n-dropdown
                  :options="[
                    { label: $t('i18n_95b351c862'), key: '0', props: { onClick: () => handleEdit(record) } },
                    { label: $t('i18n_2f4aaddde3'), key: '1', props: { onClick: () => handleDelete(record) } },
                    { label: $t('i18n_3ed3733078'), key: '2', props: { onClick: () => handleViewLog(record) } }
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
        <!-- 编辑区 -->
        <CustomModal
          v-if="editSshVisible"
          v-model:open="editSshVisible"
          destroy-on-close
          :confirm-loading="confirmLoading"
          width="600px"
          :title="$t('i18n_7a30792e2a')"
          :mask-closable="false"
          @ok="handleEditSshOk"
        >
          <n-form ref="editSshForm" :rules="rules" :model="temp">
            <n-form-item :label="$t('i18n_10f6fc171a')" path="name">
              <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_10f6fc171a')" />
            </n-form-item>
            <n-form-item :label="$t('i18n_1014b33d22')" path="group">
              <custom-select
                v-model:value="temp.groupName"
                :data="groupList"
                :input-placeholder="$t('i18n_bd0362bed3')"
                :select-placeholder="$t('i18n_9cac799f2f')"
              >
                <template #suffix>
                  <n-tooltip>
                    <template #trigger>
                      <QuestionCircleOutlined />
                    </template>

                    <div>
                      {{ $t('i18n_bd7c7abc8c') }}
                    </div>
                    <div>
                      {{ $t('i18n_f92d505ff5') }}
                    </div>
                  </n-tooltip>
                </template>
              </custom-select>
            </n-form-item>
            <n-form-item label="Host" path="host">
              <n-input-group compact name="host">
                <n-input v-model:value="temp.host" style="width: 70%" :placeholder="$t('i18n_3d83a07747')" />
                <n-form-item>
                  <n-input-number
                    v-model:value="temp.port"
                    style="width: 30%"
                    :min="1"
                    :placeholder="$t('i18n_39c7644388')"
                  />
                </n-form-item>
              </n-input-group>
            </n-form-item>
            <n-form-item :label="$t('i18n_b33c7279b3')" path="connectType">
              <n-radio-group v-model:value="temp.connectType" :options="options" />
            </n-form-item>
            <n-form-item path="user">
              <template #label>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      {{ $t('i18n_819767ada1') }}

                      <QuestionCircleOutlined v-if="!temp.id" />
                    </span>
                  </template>

                  {{ $t('i18n_f0a1428f65') }}<b>$ref.wEnv.xxxx</b> xxxx {{ $t('i18n_c1b72e7ded') }}
                </n-tooltip>
              </template>
              <n-input v-model:value="temp.user" :placeholder="$t('i18n_1fd02a90c3')">
                <template #suffix>
                  <n-tooltip v-if="temp.id">
                    <template #trigger>
                      <span class="tw">
                        <n-button size="small" type="primary" danger @click="handerRestHideField(temp)">{{
                          $t('i18n_4403fca0c0')
                        }}</n-button>
                      </span>
                    </template>
                    $t('i18n_b408105d69')
                  </n-tooltip>
                </template>
              </n-input>
            </n-form-item>
            <!-- 新增时需要填写 -->
            <!--				<n-form-item v-if="temp.type === 'add'" label="Password" path="password">-->
            <!--					<n-input type="password" v-model="temp.password" placeholder="密码"/>-->
            <!--				</n-form-item>-->
            <!-- 修改时可以不填写 -->
            <n-form-item
              :name="`${temp.type === 'add' && temp.connectType === 'PASS' ? 'password' : 'password-update'}`"
            >
              <template #label>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      {{ $t('i18n_a810520460') }}

                      <QuestionCircleOutlined v-if="!temp.id" />
                    </span>
                  </template>

                  {{ $t('i18n_63dd96a28a') }}<b>$ref.wEnv.xxxx</b> xxxx {{ $t('i18n_c1b72e7ded') }}
                </n-tooltip>
              </template>
              <!-- <n-input type="password" v-model="temp.password" :placeholder="`${temp.type === 'add' ? '密码' : '密码若没修改可以不用填写'}`" /> -->
              <custom-input
                :input="temp.password"
                :env-list="envVarList"
                :placeholder="`${temp.type === 'add' ? $t('i18n_a810520460') : $t('i18n_6c08692a3a')}`"
                @change="
                  (v) => {
                    temp = { ...temp, password: v }
                  }
                "
              >
              </custom-input>
            </n-form-item>
            <n-form-item v-if="temp.connectType === 'PUBKEY'" path="privateKey">
              <template #label>
                <n-tooltip placement="top-start">
                  <template #trigger>
                    <span class="tw">
                      {{ $t('i18n_fcef976c7a') }}

                      <QuestionCircleOutlined v-if="temp.type !== 'edit'" />
                    </span>
                  </template>
                  {{ $t('i18n_a2a0f52afe') }}
                </n-tooltip>
              </template>

              <n-input
                v-model:value="temp.privateKey"
                type="textarea"
                :auto-size="{ minRows: 3, maxRows: 5 }"
                :placeholder="$t('i18n_22482533ff')"
              />
            </n-form-item>
            <n-form-item :label="$t('i18n_6143a714d0')" path="charset">
              <n-input v-model:value="temp.charset" :placeholder="$t('i18n_6143a714d0')" />
            </n-form-item>
            <n-form-item :label="$t('i18n_67425c29a5')" path="timeout">
              <n-input-number
                v-model:value="temp.timeout"
                :min="1"
                :placeholder="$t('i18n_cb156269db')"
                style="width: 100%"
              />
            </n-form-item>
            <n-form-item :label="$t('i18n_649231bdee')" path="suffix">
              <template #help>
                {{ $t('i18n_4f095befc0') }}<span style="color: red">{{ $t('i18n_6adcbc6663') }}</span>
              </template>
              <n-input
                v-model:value="temp.allowEditSuffix"
                type="textarea"
                :rows="5"
                style="resize: none"
                :placeholder="$t('i18n_01081f7817')"
              />
            </n-form-item>
          </n-form>
        </CustomModal>
        <!-- 安装节点 -->
        <CustomModal
          v-if="nodeVisible"
          v-model:open="nodeVisible"
          destroy-on-close
          width="80%"
          :title="$t('i18n_f1b2828c75')"
          :footer="null"
          :mask-closable="false"
          @cancel="
            () => {
              nodeVisible = false
              loadData()
            }
          "
        >
          <fastInstall v-if="nodeVisible"></fastInstall>
        </CustomModal>
        <!-- 文件管理 -->
        <CustomDrawer
          v-if="drawerVisible"
          destroy-on-close
          :title="`${temp.name} ${$t('i18n_8780e6b3d1')}`"
          placement="right"
          width="90vw"
          :open="drawerVisible"
          @close="
            () => {
              drawerVisible = false
            }
          "
        >
          <ssh-file v-if="drawerVisible" :machine-ssh-id="temp.id" />
        </CustomDrawer>
        <!-- Terminal -->
        <CustomModal
          v-if="terminalVisible"
          v-model:open="terminalVisible"
          destroy-on-close
          :style="{
            maxWidth: '100vw',
            top: terminalFullscreen ? 0 : false,
            paddingBottom: 0
          }"
          :width="terminalFullscreen ? '100vw' : '80vw'"
          :body-style="{
            padding: '0 10px',
            paddingTop: '10px',
            marginRight: '10px',
            height: `${terminalFullscreen ? 'calc(100vh - 80px)' : '70vh'}`,
            display: 'flex',
            flexDirection: 'column'
          }"
          :title="temp.name"
          :footer="null"
          :mask-closable="false"
        >
          <terminal2 v-if="terminalVisible" :machine-ssh-id="temp.id" />
        </CustomModal>
        <!-- 操作日志 -->
        <CustomModal
          v-if="viewOperationLog"
          v-model:open="viewOperationLog"
          destroy-on-close
          :title="$t('i18n_cda84be2f6')"
          width="80vw"
          :footer="null"
          :mask-closable="false"
        >
          <OperationLog v-if="viewOperationLog" :machine-ssh-id="temp.id"></OperationLog>
        </CustomModal>
        <!-- 查看 ssh 关联工作空间的信息 -->
        <CustomModal
          v-if="viewWorkspaceSsh"
          v-model:open="viewWorkspaceSsh"
          destroy-on-close
          width="50%"
          :title="$t('i18n_0e5f01b9be')"
          :footer="null"
          :mask-closable="false"
        >
          <n-space direction="vertical" style="width: 100%">
            <n-alert
              v-if="workspaceSshList && workspaceSshList.length"
              :title="$t('i18n_1d0269cb77')"
              type="info"
              show-icon
            />
            <n-list bordered :data="workspaceSshList">
              <template #renderItem="{ item }">
                <n-list-item style="display: block">
                  <n-grid>
                    <n-grid-item :span="10">SSH{{ $t('i18n_5b47861521') }}{{ item.name }}</n-grid-item>
                    <n-grid-item :span="10"
                      >{{ $t('i18n_2358e1ef49') }}{{ item.workspace && item.workspace.name }}</n-grid-item
                    >
                    <n-grid-item :span="4">
                      <n-button v-if="item.workspace" size="small" type="primary" @click="configWorkspaceSsh(item)"
                        >{{ $t('i18n_224e2ccda8') }}
                      </n-button>
                      <n-button v-else size="small" type="primary" danger @click="handleDeleteWorkspaceItem(item)"
                        >{{ $t('i18n_2f4aaddde3') }}
                      </n-button>
                    </n-grid-item>
                  </n-grid>
                </n-list-item>
              </template>
            </n-list>
          </n-space>
        </CustomModal>
        <CustomModal
          v-if="configWorkspaceSshVisible"
          v-model:open="configWorkspaceSshVisible"
          destroy-on-close
          :confirm-loading="confirmLoading"
          width="50%"
          :title="$t('i18n_13627c5c46')"
          :mask-closable="false"
          @ok="handleConfigWorkspaceSshOk"
        >
          <n-form ref="editConfigWorkspaceSshForm" :rules="rules" :model="temp">
            <n-form-item label="">
              <n-alert :title="$t('i18n_ce7e6e0ea9')" banner />
            </n-form-item>
            <n-form-item :label="$t('i18n_10f6fc171a')">
              <n-input
                v-model:value="temp.name"
                :disabled="true"
                :max-length="50"
                :placeholder="$t('i18n_10f6fc171a')"
              />
            </n-form-item>
            <n-form-item :label="$t('i18n_6a588459d0')">
              <n-input
                v-model:value="temp.workspaceName"
                :disabled="true"
                :max-length="50"
                :placeholder="$t('i18n_6a588459d0')"
              />
            </n-form-item>

            <n-form-item path="fileDirs">
              <template #label>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      {{ $t('i18n_7a3c815b1e') }}

                      <QuestionCircleOutlined />
                    </span>
                  </template>
                  {{ $t('i18n_d0874922f0') }}
                </n-tooltip>
              </template>
              <n-input
                v-model:value="temp.fileDirs"
                type="textarea"
                :auto-size="{ minRows: 3, maxRows: 5 }"
                :placeholder="$t('i18n_baefd3db91')"
              />
            </n-form-item>

            <n-form-item :label="$t('i18n_649231bdee')" path="suffix">
              <n-input
                v-model:value="temp.allowEditSuffix"
                type="textarea"
                :rows="5"
                style="resize: none"
                :placeholder="$t('i18n_01081f7817')"
              />
            </n-form-item>
            <n-form-item path="notAllowedCommand">
              <template #label>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      {{ $t('i18n_a39340ec59') }}

                      <QuestionCircleOutlined />
                    </span>
                  </template>

                  {{ $t('i18n_6bb5ba7438') }}
                  <ul>
                    <li>{{ $t('i18n_7114d41b1d') }}</li>
                    <li>{{ $t('i18n_d8bf90b42b') }}</li>
                  </ul>
                </n-tooltip>
              </template>
              <n-input
                v-model:value="temp.notAllowedCommand"
                type="textarea"
                :auto-size="{ minRows: 3, maxRows: 5 }"
                :placeholder="$t('i18n_b6afcf9851')"
              />
            </n-form-item>
          </n-form>
        </CustomModal>
        <!-- 分配到其他工作空间 -->
        <CustomModal
          v-if="syncToWorkspaceVisible"
          v-model:open="syncToWorkspaceVisible"
          destroy-on-close
          :confirm-loading="confirmLoading"
          :title="$t('i18n_ef8525efce')"
          :mask-closable="false"
          @ok="handleSyncToWorkspace"
        >
          <n-space direction="vertical" style="width: 100%">
            <n-alert :title="$t('i18n_138a676635')" type="warning" show-icon>
              <template #description>{{ $t('i18n_a63fe7b615') }}</template>
            </n-alert>
            <n-form :model="temp">
              <n-form-item :label="$t('i18n_b4a8c78284')" path="workspaceId">
                <n-select
                  v-model:value="temp.workspaceId"
                  filterable
                  :placeholder="$t('i18n_b3bda9bf9e')"
                  :options="workspaceList.map((item) => ({ label: item.name, value: item.id }))"
                />
              </n-form-item>
            </n-form>
          </n-space>
        </CustomModal>
      </n-tab-pane>
      <n-tab-pane name="2" :tab="$t('i18n_d7c077c6f6')"> <OperationLog type="machinessh"></OperationLog></n-tab-pane>
    </n-tabs>
  </div>
</template>
<script>
import { DownOutlined, DownloadOutlined, QuestionCircleOutlined, UploadOutlined } from '@ant-design/icons-vue'

import { h } from 'vue'
import { NIcon } from 'naive-ui'
import { FullscreenOutlined } from '@ant-design/icons-vue'

import {
  machineSshListData,
  machineSshListGroup,
  machineSshEdit,
  machineSshDelete,
  machineListGroupWorkspaceSsh,
  machineSshSaveWorkspaceConfig,
  machineSshDistribute,
  restHideField,
  importTemplate,
  exportData,
  importData,
  statusMap
} from '@/api/system/assets-ssh'
import {
  COMPUTED_PAGINATION,
  PAGE_DEFAULT_LIST_QUERY,
  parseTime,
  CHANGE_PAGE,
  renderSize,
  formatPercent,
  formatDuration,
  formatPercent2Number
} from '@/utils/const'
import fastInstall from '@/pages/node/fast-install.vue'
import CustomSelect from '@/components/customSelect'
import CustomInput from '@/components/customInput'
import SshFile from '@/pages/ssh/ssh-file'
import Terminal2 from '@/pages/ssh/terminal.vue'
import OperationLog from '@/pages/system/assets/ssh/operation-log'
import { deleteForeSsh } from '@/api/ssh'
import { getWorkspaceEnvAll, getWorkSpaceListAll } from '@/api/workspace'

export default {
  components: {
    fastInstall,
    CustomSelect,
    Terminal2,
    SshFile,
    OperationLog,
    CustomInput
  },
  setup() {
    // 模板内联 dropdown options 的 icon 函数需访问模块作用域的 h/NIcon/图标组件
    return { h, NIcon, FullscreenOutlined }
  },
  data() {
    return {
      loading: true,
      groupList: [],
      list: [],
      listQuery: Object.assign({}, PAGE_DEFAULT_LIST_QUERY),
      editSshVisible: false,
      temp: {},
      statusMap,
      // tempPwd: '',
      options: [
        { label: this.$t('i18n_a810520460'), value: 'PASS' },
        { label: this.$t('i18n_d40b511510'), value: 'PUBKEY' }
      ],

      columns: [
        {
          title: this.$t('i18n_d7ec2d3fea'),
          key: 'name',
          width: 120,
          sorter: true,
          ellipsis: true
        },

        {
          title: 'Host',
          key: 'host',
          width: 120,
          sorter: true,
          ellipsis: true
        },
        // { title: "Port", key: "port", sorter: true, width: 80, ellipsis: true,},
        {
          title: this.$t('i18n_819767ada1'),
          key: 'user',
          sorter: true,
          width: '80px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_cdc478d90c'),
          key: 'osName',
          width: 120,
          sorter: true,
          ellipsis: true
        },
        {
          title: 'CPU',
          key: 'osOccupyCpu',
          sorter: true,
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_9932551cd5'),
          key: 'osOccupyMemory',
          sorter: true,
          width: '100px',
          ellipsis: true
        },
        {
          title: this.$t('i18n_1d650a60a5'),
          key: 'osMaxOccupyDisk',
          sorter: true,
          width: '100px',
          ellipsis: true
        },
        // { title: "编码格式", key: "charset", sorter: true, width: 120, ellipsis: true,},
        {
          title: this.$t('i18n_7912615699'),
          key: 'status',
          ellipsis: true,
          align: 'center',
          width: '100px'
        },
        {
          title: this.$t('i18n_b86224e030'),
          key: 'nodeId',

          width: '80px',
          ellipsis: true
        },
        {
          title: 'docker',
          key: 'dockerInfo',

          width: '80px',
          ellipsis: true
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
          sorter: true,
          ellipsis: true,
          render: (row) => parseTime(row['modifyTimeMillis']),
          width: '170px'
        },
        {
          title: this.$t('i18n_2b6bc0f293'),
          key: 'operation',

          width: '310px',
          align: 'center',
          // ellipsis: true,
          fixed: 'right'
        }
      ],

      // 表单校验规则
      rules: {
        name: [{ required: true, message: this.$t('i18n_06e2f88f42'), trigger: 'blur' }],
        host: [{ required: true, message: this.$t('i18n_81485b76d8'), trigger: 'blur' }],
        port: [{ required: true, message: this.$t('i18n_8d0fa2ee2d'), trigger: 'blur' }],
        connectType: [
          {
            required: true,
            message: this.$t('i18n_4ed1662cae'),
            trigger: 'blur'
          }
        ],

        user: [{ required: true, message: this.$t('i18n_3103effdfd'), trigger: 'blur' }],
        password: [{ required: true, message: this.$t('i18n_209f2b8e91'), trigger: 'blur' }]
      },
      nodeVisible: false,

      terminalVisible: false,
      terminalFullscreen: false,
      viewOperationLog: false,
      drawerVisible: false,
      workspaceSshList: [],
      viewWorkspaceSsh: false,
      configWorkspaceSshVisible: false,
      syncToWorkspaceVisible: false,
      workspaceList: [],
      tableSelections: [],
      envVarList: [],
      confirmLoading: false
    }
  },
  computed: {
    pagination() {
      return COMPUTED_PAGINATION(this.listQuery)
    },
    rowSelection() {
      return {
        onChange: (selectedRowKeys) => {
          this.tableSelections = selectedRowKeys
        },
        selectedRowKeys: this.tableSelections
      }
    },
    activePage() {
      return this.$attrs.routerUrl === this.$route.path
    }
  },
  created() {
    this.loadData()
    this.loadGroupList()
    this.getWorkEnvList()
  },
  methods: {
    formatDuration,
    renderSize,
    formatPercent,
    formatPercent2Number,
    getWorkEnvList() {
      getWorkspaceEnvAll({
        workspaceId: 'GLOBAL'
      }).then((res) => {
        if (res.code === 200) {
          this.envVarList = res.data
        }
      })
    },
    // 加载数据
    loadData(pointerEvent) {
      this.loading = true
      this.listQuery.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : this.listQuery.page
      machineSshListData(this.listQuery).then((res) => {
        if (res.code === 200) {
          this.list = res.data.result
          this.listQuery.total = res.data.total
          //
        }
        this.loading = false
      })
    },
    // 获取所有的分组
    loadGroupList() {
      machineSshListGroup().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    // 新增 SSH
    handleAdd() {
      this.temp = {
        charset: 'UTF-8',
        port: 22,
        timeout: 5,
        connectType: 'PASS'
      }
      this.editSshVisible = true
      // @author jzy 08-04
      this.$refs['editSshForm'] && this.$refs['editSshForm'].resetFields()
    },
    // 修改
    handleEdit(record) {
      this.temp = Object.assign({}, record, {
        allowEditSuffix: record.allowEditSuffix ? JSON.parse(record.allowEditSuffix).join('\r\n') : ''
      })

      this.temp = {
        ...this.temp,

        timeout: record.timeout || 5
      }
      this.editSshVisible = true
      // @author jzy 08-04
      this.$refs['editSshForm'] && this.$refs['editSshForm'].resetFields()
      this.loadGroupList()
    },
    // 提交 SSH 数据
    handleEditSshOk() {
      // 检验表单
      this.$refs['editSshForm'].validate().then(() => {
        // 提交数据
        this.confirmLoading = true
        machineSshEdit(this.temp)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.editSshVisible = false
              this.loadData()
              this.loadGroupList()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 分页、排序、筛选变化时触发
    changePage(pagination, filters, sorter) {
      this.listQuery = CHANGE_PAGE(this.listQuery, { pagination, sorter })
      this.loadData()
    },
    // 安装节点
    install() {
      this.nodeVisible = true
    },
    // 进入终端
    handleTerminal(record, terminalFullscreen) {
      this.temp = Object.assign({}, record)
      this.terminalVisible = true
      this.terminalFullscreen = terminalFullscreen
    },
    // 删除
    handleDelete(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        content: this.$t('i18n_0aa639865c'),
        zIndex: 1009,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return machineSshDelete({
            id: record.id
          }).then((res) => {
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

    // 操作日志
    handleViewLog(record) {
      this.temp = Object.assign({}, record)
      this.viewOperationLog = true
    },
    // 查看工作空间的 ssh
    handleViewWorkspaceSsh(item) {
      machineListGroupWorkspaceSsh({
        id: item.id
      }).then((res) => {
        if (res.code === 200) {
          this.temp = {
            machineSshId: item.id
          }
          this.viewWorkspaceSsh = true
          this.workspaceSshList = res.data
        }
      })
    },
    // 配置 ssh
    configWorkspaceSsh(item) {
      this.temp = {
        ...this.temp,
        id: item.id,
        name: item.name,
        fileDirs: item.fileDirs ? JSON.parse(item.fileDirs).join('\r\n') : '',
        allowEditSuffix: item.allowEditSuffix ? JSON.parse(item.allowEditSuffix).join('\r\n') : '',
        workspaceName: item.workspace?.name,
        notAllowedCommand: item.notAllowedCommand
      }
      this.configWorkspaceSshVisible = true
    },
    // 提交 SSH 配置 数据
    handleConfigWorkspaceSshOk() {
      // 检验表单
      this.$refs['editConfigWorkspaceSshForm'].validate().then(() => {
        this.confirmLoading = true
        // 提交数据
        machineSshSaveWorkspaceConfig(this.temp)
          .then((res) => {
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              this.configWorkspaceSshVisible = false
              machineListGroupWorkspaceSsh({
                id: this.temp.machineSshId
              }).then((res) => {
                if (res.code === 200) {
                  this.workspaceSshList = res.data
                }
              })
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      })
    },
    // 删除工作空间的数据
    handleDeleteWorkspaceItem(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_2ff65378a4'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: async () => {
          const { code, msg } = await deleteForeSsh(record.id)
          if (code === 200) {
            $notification.success({
              message: msg
            })
            const res = await machineListGroupWorkspaceSsh({
              id: this.temp.machineSshId
            })
            if (res.code === 200) {
              this.workspaceSshList = res.data
            }
          }
        }
      })
    },
    // 文件管理
    handleFile(record) {
      this.temp = Object.assign({}, record)

      this.drawerVisible = true
    },
    // 加载工作空间数据
    loadWorkSpaceListAll() {
      getWorkSpaceListAll().then((res) => {
        if (res.code === 200) {
          this.workspaceList = res.data
        }
      })
    },
    // 同步到其他工作情况
    syncToWorkspaceShow(item) {
      this.syncToWorkspaceVisible = true
      this.loadWorkSpaceListAll()
      if (item) {
        this.temp = {
          ids: item.id
        }
      }
    },
    handleSyncToWorkspace() {
      if (!this.temp.workspaceId) {
        $notification.warn({
          message: this.$t('i18n_b3bda9bf9e')
        })
        return false
      }
      if (!this.temp.ids) {
        this.temp = { ...this.temp, ids: this.tableSelections.join(',') }
        this.tableSelections = []
      }
      this.confirmLoading = true
      // 同步
      machineSshDistribute(this.temp)
        .then((res) => {
          if (res.code == 200) {
            $notification.success({
              message: res.msg
            })

            this.syncToWorkspaceVisible = false
            return false
          }
        })
        .finally(() => {
          this.confirmLoading = false
        })
    },
    // 清除隐藏字段
    handerRestHideField(record) {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_f7f340d946'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return restHideField(record.id).then((res) => {
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
    // 下载导入模板
    handlerImportTemplate() {
      window.open(importTemplate(), '_blank')
    },
    handlerExportData() {
      window.open(exportData({ ...this.listQuery }), '_blank')
    },
    beforeUpload({ file, onFinish, onError }) {
      const formData = new FormData()
      formData.append('file', file.file)
      importData(formData)
        .then((res) => {
          if (res.code === 200) {
            $notification.success({
              message: res.msg
            })
            this.loadData()
          }
          onFinish()
        })
        .catch(onError)
    }
  }
}
</script>
