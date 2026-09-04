<template>
  <div>
    <n-spin :tip="$t('i18n_bebcd7388f')" :spinning="loading">
      <n-card>
        <template #title>
          <n-steps v-model:current="stepsCurrent" size="small" :items="stepsItems" @change="stepsChange"></n-steps>
        </template>
        <n-form ref="editBuildForm" :rules="rules" :model="temp">
          <div v-show="stepsCurrent === 0">
            <n-alert :title="$t('i18n_514b320d25')" type="info" show-icon>
              <template #description>
                <ul>
                  <li>
                    {{ $t('i18n_85cfcdd88b') }}
                    <ul>
                      <li>{{ $t('i18n_7a5dd04619') }}</li>
                      <li>{{ $t('i18n_a08cbeb238') }}</li>
                      <li>{{ $t('i18n_8813ff5cf8') }}</li>
                    </ul>
                  </li>
                  <li>
                    {{ $t('i18n_92f0744426') }}
                    <ul>
                      <li>{{ $t('i18n_1de9b781bd') }}</li>
                    </ul>
                  </li>

                  <li>{{ $t('i18n_200707a186') }}</li>
                  <li v-if="getExtendPlugins.indexOf('inDocker') > -1" style="color: red">
                    {{ $t('i18n_f5f65044ea') }}
                  </li>
                </ul>
              </template>
            </n-alert>
            <n-form-item path="buildMode">
              <template #label> {{ $t('i18n_17a74824de') }} </template>
              <n-space>
                <n-radio-group
                  v-model:value="temp.buildMode"
                  :disabled="temp.id ? true : false"
                  name="buildMode"
                  @change="changeBuildMode"
                >
                  <n-radio
                    v-for="item in buildModeArray"
                    :key="item.value"
                    :disabled="item.disabled"
                    :value="item.value"
                    >{{ item.name }}</n-radio
                  >
                </n-radio-group>
              </n-space>
            </n-form-item>
            <template v-if="temp.buildMode === 1">
              <n-form-item>
                <template #help>
                  <n-button
                    text
                    @click="
                      () => {
                        dockerListVisible = 1
                      }
                    "
                  >
                    {{ $t('i18n_e06497b0fb') }}
                  </n-button>
                </template>
                <template #label> {{ $t('i18n_b44479d4b8') }} </template>
                <n-spin :tip="$t('i18n_73c980987a')" :spinning="dockerAllTagLoading">
                  <n-space v-if="dockerAllTagList && dockerAllTagList.length">
                    <n-tag v-for="(item, index) in dockerAllTagList" :key="index">{{ item }}</n-tag>
                  </n-space>
                  <span v-else style="color: red; font-weight: bold">{{ $t('i18n_28e0fcdf93') }}</span>
                </n-spin>
              </n-form-item>
              <n-alert :title="$t('i18n_3174d1022d')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('i18n_148484b985') }}</li>
                    <li>{{ $t('i18n_21d81c6726') }}</li>
                    <li>
                      {{ $t('i18n_caa9b5cd94') }} <b style="color: red">fromTag</b>
                      {{ $t('i18n_9caecd931b') }}
                    </li>
                  </ul>
                </template>
              </n-alert>
            </template>
          </div>
          <div v-show="stepsCurrent === 1">
            <n-form-item :label="$t('i18n_d7ec2d3fea')" path="name">
              <n-grid>
                <n-grid-item :span="10">
                  <n-input v-model:value="temp.name" :max-length="50" :placeholder="$t('i18n_d7ec2d3fea')" />
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
            <n-form-item :label="$t('i18n_b3ef35a359')" path="repositoryId">
              <n-input
                :value="`${
                  tempRepository ? tempRepository.name + '[' + tempRepository.gitUrl + ']' : $t('i18n_ad311f3211')
                }`"
                read-only
                :placeholder="$t('i18n_ad311f3211')"
                :enter-button="$t('i18n_f89fa9b6c6')"
                @keyup.enter="
                  () => {
                    repositoryisible = true
                  }
                "
              />
            </n-form-item>
            <template v-if="tempRepository && tempRepository.repoType === 0">
              <n-form-item :label="$t('i18n_bfc04cfda7')" path="branchName">
                <n-grid>
                  <n-grid-item :span="10">
                    <custom-select
                      v-model:value="temp.branchName"
                      :disabled="temp.branchTagName ? true : false"
                      :data="branchList"
                      :can-reload="true"
                      :input-placeholder="$t('i18n_c618659cea')"
                      :select-placeholder="$t('i18n_c0a9e33e29')"
                      @on-refresh-select="loadBranchList"
                      @change="
                        () => {
                          $refs['editBuildForm'] && $refs['editBuildForm'].clearValidate()
                        }
                      "
                    >
                      <template #inputTips>
                        <div>
                          {{ $t('i18n_89f5ca6928') }}(AntPathMatcher)
                          <ul>
                            <li>? {{ $t('i18n_9973159a4d') }}</li>
                            <li>* {{ $t('i18n_32f882ae24') }}</li>
                            <li>** {{ $t('i18n_45b88fc569') }}</li>
                          </ul>
                        </div>
                      </template>
                    </custom-select>
                  </n-grid-item>
                  <n-grid-item :span="4" style="text-align: right"> {{ $t('i18n_14d342362f') }}(TAG)：</n-grid-item>
                  <n-grid-item :span="10">
                    <n-form-item>
                      <custom-select
                        v-model:value="temp.branchTagName"
                        :data="branchTagList"
                        :can-reload="true"
                        :input-placeholder="$t('i18n_30e6f71a18')"
                        :select-placeholder="$t('i18n_2d58b0e650')"
                        @on-refresh-select="loadBranchList"
                        @change="
                          () => {
                            $refs['editBuildForm'] && $refs['editBuildForm'].clearValidate()
                          }
                        "
                      >
                        <template #inputTips>
                          <div>
                            {{ $t('i18n_89f5ca6928') }}(AntPathMatcher)
                            <ul>
                              <li>? {{ $t('i18n_9973159a4d') }}</li>
                              <li>* {{ $t('i18n_32f882ae24') }}</li>
                              <li>** {{ $t('i18n_45b88fc569') }}</li>
                            </ul>
                          </div>
                        </template>
                      </custom-select></n-form-item
                    >
                  </n-grid-item>
                </n-grid>
              </n-form-item>
              <n-form-item
                v-if="getExtendPlugins.indexOf('system-git') > -1"
                :label="$t('i18n_0253279fb8')"
                name="cloneDepth"
              >
                <n-input-number
                  v-model:value="tempExtraData.cloneDepth"
                  style="width: 100%"
                  :placeholder="$t('i18n_a59d075d85')"
                />
              </n-form-item>
            </template>
          </div>

          <!-- 构建流程 -->
          <div v-show="stepsCurrent === 2">
            <n-form-item v-if="temp.buildMode === 0" path="script">
              <template #label>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      {{ $t('i18n_c2ee58c247') }}

                      <QuestionCircleOutlined v-if="!temp.id" />
                    </span>
                  </template>

                  {{ $t('i18n_c7689f4c9a') }}<b>{{ $t('i18n_2b607a562a') }} </b>{{ $t('i18n_7e33f94952') }}
                  <b>cd xxx && mvn clean package</b>
                </n-tooltip>
              </template>
              <template #help>
                <div>{{ $t('i18n_1ad696efdc') }}</div>
                <div>{{ $t('i18n_872ad6c96e') }}</div>
              </template>

              <!-- <n-input type="textarea"
                  v-model:value="temp.script"
                  :auto-size="{ minRows: 2, maxRows: 6 }"
                  clearable
                  placeholder="构建执行的命令(非阻塞命令)，如：mvn clean package、npm run build。支持变量：${BUILD_ID}、${BUILD_NAME}、${BUILD_SOURCE_FILE}、${BUILD_NUMBER_ID}、仓库目录下 .env、工作空间变量"
                /> -->
              <n-form-item>
                <code-editor v-model:content="temp.script" :show-tool="true" :options="{ mode: 'shell' }" height="40vh">
                  <template #tool_before>
                    <n-space>
                      <n-button
                        text
                        @click="
                          () => {
                            viewScriptTemplVisible = true
                          }
                        "
                      >
                        {{ $t('i18n_e3ead2bd0d') }}
                      </n-button>
                      <n-button
                        text
                        @click="
                          () => {
                            chooseScriptVisible = 2
                          }
                        "
                      >
                        {{ $t('i18n_36d4046bd6') }}
                      </n-button>
                    </n-space>
                  </template>
                </code-editor>
              </n-form-item>
            </n-form-item>
            <n-form-item v-if="temp.buildMode === 1" path="script">
              <template #label>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      DSL {{ $t('i18n_2d711b09bd') }}

                      <QuestionCircleOutlined v-if="!temp.id" />
                    </span>
                  </template>

                  <p>{{ $t('i18n_ba3a679655') }}</p>
                  <ul>
                    <li>{{ $t('i18n_55d4a79358') }}</li>
                    <li>{{ $t('i18n_713c986135') }}</li>
                    <li>{{ $t('i18n_db81a464ba') }}</li>
                  </ul>
                  <div>
                    {{ $t('i18n_590b9ce766') }}
                    <ol>
                      <li>
                        {{ $t('i18n_fdfd501269') }}
                      </li>
                      <li>{{ $t('i18n_0793aa7ba3') }}</li>
                      <li>{{ $t('i18n_0bfcab4978') }}</li>
                      <li>
                        {{ $t('i18n_e5915f5dbb') }}
                      </li>
                      <li>
                        {{ $t('i18n_48fe457960') }}
                      </li>
                    </ol>
                  </div>
                </n-tooltip>
              </template>
              <!-- <n-tabs>
                <n-tab-pane name="1" tab="DSL 配置">
                  <n-form-item>
                    <code-editor
                      height="40vh"
                      v-model:content="temp.script"
                      :options="{ mode: 'yaml', tabSize: 2 }"
                    ></code-editor>
                  </n-form-item>
                </n-tab-pane>
                <n-tab-pane name="2" tab="配置示例">
                  <n-form-item>
                    <code-editor
                      height="40vh"
                      v-model:content="dslDefault"
                      :options="{
                        mode: 'yaml',
                        tabSize: 2,

                        readOnly: true
                      }"
                    ></code-editor>
                  </n-form-item>
                </n-tab-pane>
              </n-tabs> -->
              <n-form-item>
                <code-editor
                  v-show="dslEditTabKey === 'content'"
                  v-model:content="temp.script"
                  height="40vh"
                  :show-tool="true"
                  :options="{ mode: 'yaml', tabSize: 2 }"
                  :placeholder="$t('i18n_af14cd6893')"
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
                  v-model:content="dslDefault"
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
                </code-editor></n-form-item
              >
            </n-form-item>
            <n-form-item v-if="temp.buildMode !== undefined" path="resultDirFile" class="voyager1-target-dir">
              <template #label>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      {{ $t('i18n_c972010694') }}

                      <QuestionCircleOutlined v-if="!temp.id" />
                    </span>
                  </template>

                  <div>
                    {{ $t('i18n_84592cd99c') }}
                    <b>mvn clean package</b> {{ $t('i18n_1a55f76ace') }}
                    <b> modules/server/target/server-2.4.2-release</b>
                  </div>
                  <div><br /></div>
                  <!-- 只有本地构建支持 模糊匹配 -->
                  <div v-if="temp.buildMode === 0">
                    {{ $t('i18n_89f5ca6928') }}(AntPathMatcher){{ $t('i18n_35cb4b85a9') }}
                    <ul>
                      <li>? {{ $t('i18n_9973159a4d') }}</li>
                      <li>* {{ $t('i18n_32f882ae24') }}</li>
                      <li>** {{ $t('i18n_45b88fc569') }}</li>
                    </ul>
                  </div>
                </n-tooltip>
              </template>
              <n-input v-model:value="temp.resultDirFile" :max-length="200" :placeholder="$t('i18n_2cdbbdabf1')" />
            </n-form-item>

            <n-alert v-if="temp.buildMode === undefined" :title="$t('i18n_46032a715e')" banner />
            <template v-else>
              <n-form-item :label="$t('i18n_3867e350eb')" path="buildEnvParameter">
                <n-input
                  v-model:value="temp.buildEnvParameter"
                  type="textarea"
                  :placeholder="$t('i18n_b3913b9bb7')"
                  :auto-size="{ minRows: 3, maxRows: 5 }"
                />
              </n-form-item>
              <n-form-item :label="$t('i18n_0227161b3e')" path="commandExecMode">
                <n-radio-group v-model:value="tempExtraData.commandExecMode" button-style="solid">
                  <n-radio-button value="default">{{ $t('i18n_18c63459a2') }}</n-radio-button>
                  <n-radio-button value="apache_exec">{{ $t('i18n_c9daf4ad6b') }}</n-radio-button>
                </n-radio-group>
                <template #help>{{ $t('i18n_75c63f427a') }}</template>
              </n-form-item>
            </template>
          </div>

          <div v-show="stepsCurrent === 3">
            <n-form-item path="releaseMethod">
              <template #label>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      {{ $t('i18n_3c91490844') }}

                      <QuestionCircleOutlined v-if="!temp.id" />
                    </span>
                  </template>

                  <ul>
                    <li>{{ $t('i18n_2c635c80ec') }}</li>
                    <li>{{ $t('i18n_df9d1fedc5') }}</li>
                    <li>{{ $t('i18n_7e9f0d2606') }}</li>
                    <li>
                      {{ $t('i18n_58cbd04f02') }}
                    </li>
                    <li>{{ $t('i18n_cbee7333e4') }}</li>
                    <li>
                      {{ $t('i18n_6f5b238dd2') }}
                    </li>
                    <li>{{ $t('i18n_5fbde027e3') }}</li>
                  </ul>
                </n-tooltip>
              </template>
              <n-space>
                <n-radio-group v-model:value="temp.releaseMethod" name="releaseMethod">
                  <n-radio v-for="(val, key) in releaseMethodMap" :key="key" :value="parseInt(key)">{{ val }}</n-radio>
                </n-radio-group>
              </n-space>
            </n-form-item>
            <div v-if="!temp.releaseMethod" style="text-align: center">
              {{ $t('i18n_29b48a76be') }}
            </div>
            <template v-else>
              <template v-if="temp.releaseMethod === 0">
                {{ $t('i18n_52ef46c618') }},{{ $t('i18n_657f3883e3') }}
              </template>
              <!-- 节点分发 -->
              <template v-if="temp.releaseMethod === 1">
                <n-form-item :label="$t('i18n_bc8752e529')" path="releaseMethodDataId">
                  <n-select
                    v-model:value="tempExtraData.releaseMethodDataId_1"
                    filterable
                    clearable
                    :placeholder="$t('i18n_2560e962cf')"
                    :options="dispatchList.map((dispatch) => ({ label: dispatch.name, value: dispatch.id }))"
                  />
                </n-form-item>
                <n-form-item path="projectSecondaryDirectory" :label="$t('i18n_871cc8602a')">
                  <n-input
                    v-model:value="tempExtraData.projectSecondaryDirectory"
                    :placeholder="$t('i18n_f9f061773e')"
                  />
                </n-form-item>
              </template>

              <!-- 项目 -->
              <template v-if="temp.releaseMethod === 2">
                <n-form-item :label="$t('i18n_dbba7e107a')" path="releaseMethodDataIdList">
                  <n-cascader
                    v-model:value="temp.releaseMethodDataIdList"
                    :options="cascaderList"
                    :placeholder="$t('i18n_35488f5ba8')"
                  >
                    <template #action>
                      <ReloadOutlined @click="loadNodeProjectList" />
                    </template>
                  </n-cascader>
                </n-form-item>
                <n-form-item :label="$t('i18n_89050136f8')" path="afterOpt">
                  <n-select
                    v-model:value="tempExtraData.afterOpt"
                    filterable
                    clearable
                    :placeholder="$t('i18n_3322338140')"
                    :options="afterOptListSimple.map((opt) => ({ label: opt.title, value: opt.value }))"
                  />
                </n-form-item>
                <n-form-item path="projectSecondaryDirectory" :label="$t('i18n_871cc8602a')">
                  <n-input
                    v-model:value="tempExtraData.projectSecondaryDirectory"
                    :placeholder="$t('i18n_9c99e8bec9')"
                  />
                </n-form-item>
              </template>
              <!-- SSH -->
              <template v-if="temp.releaseMethod === 3">
                <n-form-item path="releaseMethodDataId" :help="$t('i18n_7de5541032')">
                  <template #label>
                    <n-tooltip>
                      <template #trigger>
                        <span class="tw">
                          {{ $t('i18n_b188393ea7') }}

                          <QuestionCircleOutlined v-if="!temp.id" />
                        </span>
                      </template>

                      {{ $t('i18n_7de5541032') }}
                    </n-tooltip>
                  </template>
                  <n-grid>
                    <n-grid-item :span="22">
                      <n-select
                        v-model:value="tempExtraData.releaseMethodDataId_3"
                        filterable
                        multiple
                        :placeholder="$t('i18n_260a3234f2')"
                        :options="sshList.map((ssh) => ({ label: ssh.name, value: ssh.id, disabled: !ssh.fileDirs }))"
                      />
                    </n-grid-item>
                    <n-grid-item :span="1" style="margin-left: 10px">
                      <ReloadOutlined @click="loadSshList" />
                    </n-grid-item>
                  </n-grid>
                </n-form-item>
                <n-form-item path="releaseMethodDataId" :help="$t('i18n_abb6b7260b')">
                  <template #label>
                    <n-tooltip>
                      <template #trigger>
                        <span class="tw">
                          {{ $t('i18n_dbb2df00cf') }}
                          <QuestionCircleOutlined v-if="!temp.id" />
                        </span>
                      </template>
                      $t('i18n_abb6b7260b')
                    </n-tooltip>
                  </template>
                  <n-input-group compact>
                    <n-select
                      v-model:value="tempExtraData.releaseSshDir"
                      filterable
                      clearable
                      style="width: 30%"
                      :placeholder="$t('i18n_260a3234f2')"
                      :options="selectSshDirs"
                    />
                    <n-form-item>
                      <n-input
                        v-model:value="tempExtraData.releasePath2"
                        style="width: 70%"
                        :placeholder="$t('i18n_a75a5a9525')"
                      />
                    </n-form-item>
                  </n-input-group>
                </n-form-item>
              </template>

              <n-form-item v-if="temp.releaseMethod === 3" path="releaseBeforeCommand">
                <!-- sshCommand -->
                <template #label>
                  <n-tooltip>
                    <template #trigger>
                      <span class="tw">
                        {{ $t('i18n_e44f59f2d9') }}

                        <QuestionCircleOutlined v-if="!temp.id" />
                      </span>
                    </template>

                    {{ $t('i18n_b53dedd3e0') }}
                    <ul>
                      <li>{{ $t('i18n_9be8ff8367') }}</li>
                      <li>{{ $t('i18n_5fbde027e3') }}</li>
                    </ul>
                  </n-tooltip>
                </template>
                <template #help>
                  {{ $t('i18n_234e967afe') }}
                </template>
                <!-- <n-input type="textarea"
                  v-model:value="tempExtraData.releaseBeforeCommand"
                  clearable
                  :auto-size="{ minRows: 2, maxRows: 10 }"
                  :rows="3"
                  placeholder=""
                /> -->
                <n-form-item>
                  <code-editor
                    v-model:content="tempExtraData.releaseBeforeCommand"
                    height="40vh"
                    :show-tool="true"
                    :options="{ mode: 'shell' }"
                  >
                    <template #tool_before>
                      <n-tag>{{ $t('i18n_eb5bab1c31') }}</n-tag></template
                    >
                  </code-editor>
                </n-form-item>
              </n-form-item>
              <n-form-item v-if="temp.releaseMethod === 3 || temp.releaseMethod === 4" path="releaseCommand">
                <!-- sshCommand LocalCommand -->
                <template #label>
                  <n-tooltip>
                    <template #trigger>
                      <span class="tw">
                        {{ $t('i18n_923f8d2688') }}

                        <QuestionCircleOutlined v-if="!temp.id" />
                      </span>
                    </template>

                    {{ $t('i18n_fb9d826b2f') }}
                    <ul>
                      <li>{{ $t('i18n_9be8ff8367') }}</li>
                      <li>{{ $t('i18n_5fbde027e3') }}</li>
                    </ul>
                  </n-tooltip>
                </template>
                <template #help>
                  <div>{{ $t('i18n_9fb12a2d14') }}</div>
                  <div>{{ $t('i18n_872ad6c96e') }}</div>
                </template>
                <n-form-item>
                  <code-editor
                    v-model:content="tempExtraData.releaseCommand"
                    height="40vh"
                    :show-tool="true"
                    :options="{ mode: 'shell' }"
                  >
                    <template #tool_before>
                      <n-tag>{{ $t('i18n_537b39a8b5') }}</n-tag></template
                    >
                  </code-editor>
                </n-form-item>
              </n-form-item>

              <n-form-item v-if="temp.releaseMethod === 2 || temp.releaseMethod === 3" path="clearOld">
                <template #label>
                  <n-tooltip>
                    <template #trigger>
                      <span class="tw">
                        {{ $t('i18n_2223ff647d') }}

                        <QuestionCircleOutlined v-if="!temp.id" />
                      </span>
                    </template>

                    {{ $t('i18n_b343663a14') }}
                  </n-tooltip>
                </template>
                <n-form-item>
                  <n-grid>
                    <n-grid-item :span="4">
                      <n-switch
                        v-model:value="tempExtraData.clearOld"
                        :checked-label="$t('i18n_0a60ac8f02')"
                        :unchecked-label="$t('i18n_c9744f45e7')"
                      />
                    </n-grid-item>
                    <template v-if="temp.releaseMethod === 2">
                      <n-grid-item :span="4" style="text-align: right">
                        <n-tooltip>
                          <template #trigger>
                            <span class="tw">
                              {{ $t('i18n_702afc34a0') }}

                              <QuestionCircleOutlined v-if="!temp.id" />
                            </span>
                          </template>

                          {{ $t('i18n_762e05a901') }}
                          <ul>
                            <li>
                              {{ $t('i18n_19f974ef6a') }}
                            </li>
                            <li>{{ $t('i18n_7764df7ccc') }}</li>
                          </ul>
                        </n-tooltip>
                      </n-grid-item>
                      <n-grid-item :span="4">
                        <n-switch
                          v-model:value="tempExtraData.diffSync"
                          :checked-label="$t('i18n_0a60ac8f02')"
                          :unchecked-label="$t('i18n_c9744f45e7')"
                        />
                      </n-grid-item>
                      <n-grid-item :span="4" style="text-align: right">
                        <n-tooltip>
                          <template #trigger>
                            <span class="tw">
                              {{ $t('i18n_7b2cbfada9') }}

                              <QuestionCircleOutlined v-if="!temp.id" />
                            </span>
                          </template>

                          {{ $t('i18n_300fbf3891') }}
                        </n-tooltip>
                      </n-grid-item>
                      <n-grid-item :span="4">
                        <n-switch
                          v-model:value="tempExtraData.projectUploadCloseFirst"
                          :checked-label="$t('i18n_0a60ac8f02')"
                          :unchecked-label="$t('i18n_c9744f45e7')"
                        />
                      </n-grid-item>
                    </template>
                  </n-grid>
                </n-form-item>
              </n-form-item>
              <!-- docker -->
              <template v-if="temp.releaseMethod === 5">
                <n-form-item path="fromTag">
                  <template #label>
                    <n-tooltip>
                      <template #trigger>
                        <span class="tw">
                          {{ $t('i18n_690a3d1a69') }}

                          <QuestionCircleOutlined v-if="!temp.id" />
                        </span>
                      </template>

                      {{ $t('i18n_bd5d9b3e93') }}
                    </n-tooltip>
                  </template>
                  <n-input v-model:value="tempExtraData.fromTag" :placeholder="$t('i18n_2be2175cd7')" />
                </n-form-item>

                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      <n-form-item path="dockerfile" label="Dockerfile">
                        <n-input v-model:value="tempExtraData.dockerfile" :placeholder="$t('i18n_ab9a0ee5bd')" />
                      </n-form-item>
                    </span>
                  </template>
                  $t('i18n_61bfa4e925')
                </n-tooltip>
                <n-form-item path="dockerTag" :label="$t('i18n_9a77f3523e')">
                  <n-tooltip>
                    <template #trigger>
                      <n-input v-model:value="tempExtraData.dockerTag" :placeholder="$t('i18n_250a999bb2')" />
                    </template>
                    $t('i18n_fa57a7afad')
                  </n-tooltip>
                </n-form-item>
                <n-form-item path="dockerBuildArgs" :label="$t('i18n_244d5a0ed8')">
                  <n-grid>
                    <n-grid-item :span="10">
                      <n-tooltip>
                        <template #trigger>
                          <n-input v-model:value="tempExtraData.dockerBuildArgs" :placeholder="$t('i18n_6e70d2fb91')" />
                        </template>
                        $t('i18n_a34545bd16')
                      </n-tooltip>
                    </n-grid-item>
                    <n-grid-item :span="4" style="text-align: right">{{ $t('i18n_3f016aa454') }}</n-grid-item>
                    <n-grid-item :span="10">
                      <n-form-item>
                        <n-tooltip>
                          <template #trigger>
                            <n-input
                              v-model:value="tempExtraData.dockerImagesLabels"
                              :placeholder="$t('i18n_b922323119')"
                            />
                          </template>
                          {{ $t('i18n_7a4ecc606c') }}
                        </n-tooltip></n-form-item
                      >
                    </n-grid-item>
                  </n-grid>
                </n-form-item>
                <n-form-item path="swarmId">
                  <template #label>
                    <n-tooltip>
                      <template #trigger>
                        <span class="tw">
                          {{ $t('i18n_5011e53403') }}

                          <QuestionCircleOutlined v-if="!temp.id" />
                        </span>
                      </template>

                      {{ $t('i18n_639fd37242') }}
                    </n-tooltip>
                  </template>
                  <n-select
                    v-model:value="tempExtraData.dockerSwarmId"
                    filterable
                    clearable
                    :placeholder="$t('i18n_a5e9874a96')"
                    :options="[
                      { label: $t('i18n_1e88a0cfaf'), value: '' },
                      ...dockerSwarmList.map((item1) => ({ label: item1.name, value: item1.id }))
                    ]"
                    @update:value="selectSwarm()"
                  >
                    <template #action>
                      <ReloadOutlined @click="loadDockerSwarmListAll" />
                    </template>
                  </n-select>
                </n-form-item>
                <n-form-item path="pushToRepository" :label="` `" :colon="false">
                  <n-form-item>
                    <n-grid>
                      <n-grid-item :span="6" style="text-align: right">
                        <n-space>
                          <n-tooltip>
                            <template #trigger>
                              <span class="tw">
                                {{ $t('i18n_601426f8f2') }}

                                <QuestionCircleOutlined v-if="!temp.id" />
                              </span>
                            </template>

                            {{ $t('i18n_6d5f0fb74b') }}
                          </n-tooltip>

                          <n-switch
                            v-model:value="tempExtraData.pushToRepository"
                            :checked-label="$t('i18n_0a60ac8f02')"
                            :unchecked-label="$t('i18n_c9744f45e7')"
                          />
                        </n-space>
                      </n-grid-item>
                      <n-grid-item :span="6" style="text-align: right">
                        <n-space>
                          <n-tooltip>
                            <template #trigger>
                              <span class="tw">
                                {{ $t('i18n_c43743d213') }}
                                <QuestionCircleOutlined v-if="!temp.id" />
                              </span>
                            </template>
                            {{ $t('i18n_f1e3ef0def') }}
                          </n-tooltip>

                          <n-switch
                            v-model:value="tempExtraData.pushToRepositoryAfterDelete"
                            :disabled="!tempExtraData.pushToRepository"
                            :checked-label="$t('i18n_0a60ac8f02')"
                            :unchecked-label="$t('i18n_c9744f45e7')"
                          />
                        </n-space>
                      </n-grid-item>
                      <n-grid-item :span="6" style="text-align: right">
                        <n-space>
                          <n-tooltip>
                            <template #trigger>
                              <span class="tw">
                                {{ $t('i18n_159a3a8037') }}

                                <QuestionCircleOutlined v-if="!temp.id" />
                              </span>
                            </template>
                            {{ $t('i18n_ab968d842f') }}
                          </n-tooltip>

                          <n-switch
                            v-model:value="tempExtraData.dockerBuildPull"
                            :checked-label="$t('i18n_0a60ac8f02')"
                            :unchecked-label="$t('i18n_c9744f45e7')"
                          />
                        </n-space>
                      </n-grid-item>
                    </n-grid>
                  </n-form-item>
                </n-form-item>
                <n-form-item path="pushToRepository2" :label="` `" :colon="false">
                  <n-form-item>
                    <n-grid>
                      <n-grid-item :span="6" style="text-align: right">
                        <n-space>
                          <n-tooltip>
                            <template #trigger>
                              <span class="tw">
                                {{ $t('i18n_9fb61a9936') }}

                                <QuestionCircleOutlined v-if="!temp.id" />
                              </span>
                            </template>

                            {{ $t('i18n_8c7ce1da57') }}
                          </n-tooltip>

                          <n-switch
                            v-model:value="tempExtraData.dockerTagIncrement"
                            :checked-label="$t('i18n_0a60ac8f02')"
                            :unchecked-label="$t('i18n_c9744f45e7')"
                          />
                        </n-space>
                      </n-grid-item>
                      <n-grid-item :span="6" style="text-align: right">
                        <n-space>
                          <n-tooltip>
                            <template #trigger>
                              <span class="tw">
                                no-cache

                                <QuestionCircleOutlined v-if="!temp.id" />
                              </span>
                            </template>
                            {{ $t('i18n_28b69f9233') }}
                          </n-tooltip>

                          <n-switch
                            v-model:value="tempExtraData.dockerNoCache"
                            :checked-label="$t('i18n_0a60ac8f02')"
                            :unchecked-label="$t('i18n_c9744f45e7')"
                          />
                        </n-space>
                      </n-grid-item>
                    </n-grid>
                  </n-form-item>
                </n-form-item>
                <n-form-item v-if="tempExtraData.dockerSwarmId" path="dockerSwarmServiceName">
                  <template #label>
                    <n-tooltip>
                      <template #trigger>
                        <span class="tw">
                          {{ $t('i18n_b5ce5efa6e') }}

                          <QuestionCircleOutlined v-if="!temp.id" />
                        </span>
                      </template>

                      {{ $t('i18n_2f67a19f9d') }}
                    </n-tooltip>
                  </template>
                  <n-form-item>
                    <n-select
                      v-model:value="tempExtraData.dockerSwarmServiceName"
                      clearable
                      :placeholder="$t('i18n_2ad3428664')"
                      :options="
                        swarmServiceListOptions.map((item2) => ({ label: item2.spec.name, value: item2.spec.name }))
                      "
                    />
                  </n-form-item>
                </n-form-item>
              </template>
            </template>
          </div>

          <div v-show="stepsCurrent === 4">
            <n-form-item path="cacheBuild">
              <template #label>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      {{ $t('i18n_b6a828205d') }}

                      <QuestionCircleOutlined v-if="!temp.id" />
                    </span>
                  </template>

                  {{ $t('i18n_44473c1406') }}
                </n-tooltip>
              </template>
              <n-form-item>
                <n-grid>
                  <n-grid-item :span="2">
                    <n-tooltip>
                      <template #trigger>
                        <n-switch
                          v-model:value="tempExtraData.cacheBuild"
                          :checked-label="$t('i18n_0a60ac8f02')"
                          :unchecked-label="$t('i18n_c9744f45e7')"
                        />
                      </template>
                      $t('i18n_12afa77947')
                    </n-tooltip>
                  </n-grid-item>
                  <n-grid-item :span="6" style="text-align: right">
                    <n-space>
                      <n-tooltip>
                        <template #trigger>
                          <span class="tw">
                            {{ $t('i18n_2499b03cc5') }}

                            <QuestionCircleOutlined v-if="!temp.id" />
                          </span>
                        </template>
                        {{ $t('i18n_e0ae638e73') }}
                      </n-tooltip>
                      <n-switch
                        v-model:value="tempExtraData.saveBuildFile"
                        :checked-label="$t('i18n_0a60ac8f02')"
                        :unchecked-label="$t('i18n_c9744f45e7')"
                      />
                    </n-space>
                  </n-grid-item>

                  <n-grid-item :span="6" style="text-align: right">
                    <n-space>
                      <n-tooltip>
                        <template #trigger>
                          <span class="tw">
                            {{ $t('i18n_36d00eaa3f') }}

                            <QuestionCircleOutlined v-if="!temp.id" />
                          </span>
                        </template>

                        {{ $t('i18n_4cbc5505c7') }}
                      </n-tooltip>
                      <n-switch
                        v-model:value="tempExtraData.checkRepositoryDiff"
                        :checked-label="$t('i18n_0a60ac8f02')"
                        :unchecked-label="$t('i18n_c9744f45e7')"
                      />
                    </n-space>
                  </n-grid-item>
                  <n-grid-item :span="6" style="text-align: right">
                    <n-space>
                      <n-tooltip>
                        <template #trigger>
                          <span class="tw">
                            {{ $t('i18n_a9c52ffd40') }}

                            <QuestionCircleOutlined v-if="!temp.id" />
                          </span>
                        </template>

                        {{ $t('i18n_07b6bb5e40') }}
                      </n-tooltip>
                      <n-switch
                        v-model:value="tempExtraData.strictlyEnforce"
                        :checked-label="$t('i18n_0a60ac8f02')"
                        :unchecked-label="$t('i18n_c9744f45e7')"
                      />
                    </n-space>
                  </n-grid-item>
                </n-grid>
              </n-form-item>
            </n-form-item>
            <n-form-item path="webhook">
              <template #label>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      WebHooks

                      <QuestionCircleOutlined v-if="!temp.id" />
                    </span>
                  </template>

                  <ul>
                    <li>{{ $t('i18n_78b2da536d') }}</li>
                    <li>{{ $t('i18n_9f6090c819') }}</li>
                    <li>{{ $t('i18n_a805615d15') }}</li>
                    <li>{{ $t('i18n_c96f47ec1b') }}</li>
                  </ul>
                </n-tooltip>
              </template>
              <n-input v-model:value="temp.webhook" :placeholder="$t('i18n_7561bc005e')" />
            </n-form-item>
            <n-form-item :label="$t('i18n_db9296212a')" path="autoBuildCron">
              <n-auto-complete
                v-model:value="temp.autoBuildCron"
                :placeholder="$t('i18n_8ffded102f')"
                :options="CRON_DATA_SOURCE"
              >
                <template #option="item"> {{ item.title }} {{ item.value }} </template>
              </n-auto-complete>
            </n-form-item>
            <n-form-item path="noticeScriptId">
              <template #label>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      {{ $t('i18n_192496786d') }}

                      <QuestionCircleOutlined v-if="!temp.id" />
                    </span>
                  </template>

                  <ul>
                    <li>{{ $t('i18n_6a49f994b1') }}</li>
                    <li>
                      {{ $t('i18n_9ff5504901') }}
                    </li>
                    <li>{{ $t('i18n_27054fefec') }}</li>
                    <li>
                      <b>{{ $t('i18n_edb881412a') }}</b>
                    </li>
                  </ul>
                </n-tooltip>
              </template>
              <template #help>{{ $t('i18n_b095ceda99') }}</template>
              <n-input
                :value="`${
                  tempExtraData ? tempExtraData.noticeScriptId || $t('i18n_b9b176e37a') : $t('i18n_b9b176e37a')
                }`"
                read-only
                :placeholder="$t('i18n_b9b176e37a')"
                :enter-button="$t('i18n_a056d9c4b3')"
                @keyup.enter="
                  () => {
                    chooseScriptVisible = 1
                  }
                "
              >
                <template v-if="tempExtraData && tempExtraData.noticeScriptId" #prefix>
                  <span
                    @click="
                      () => {
                        tempExtraData = {
                          ...tempExtraData,
                          noticeScriptId: ''
                        }
                      }
                    "
                  >
                    {{ $t('i18n_54506fe138') }}
                  </span>
                </template>
              </n-input>
            </n-form-item>
            <n-form-item path="attachEnv">
              <template #label>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      {{ $t('i18n_2351006eae') }}

                      <QuestionCircleOutlined v-if="!temp.id" />
                    </span>
                  </template>

                  <ul>
                    <li>{{ $t('i18n_7ef30cfd31') }}</li>
                    <li>{{ $t('i18n_2d7020be7d') }}</li>
                    <li>{{ $t('i18n_4f4c28a1fb') }}</li>
                    <li>{{ $t('i18n_b437a4d41d') }}</li>
                    <li>
                      {{ $t('i18n_6928f50eb3') }}<b>USE_TAR_GZ=1</b>
                      {{ $t('i18n_d5269713c7') }}
                      <b>tar.gz</b> {{ $t('i18n_045f89697e') }}
                    </li>
                  </ul>
                </n-tooltip>
              </template>
              <n-input v-model:value="tempExtraData.attachEnv" :placeholder="$t('i18n_14dd5937e4')" />
            </n-form-item>
            <n-form-item path="cacheBuild">
              <template #label>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      {{ $t('i18n_0f5fc9f300') }}

                      <QuestionCircleOutlined v-if="!temp.id" />
                    </span>
                  </template>

                  {{ $t('i18n_1819d0cdda') }}
                </n-tooltip>
              </template>
              <n-grid>
                <n-grid-item :span="4">
                  <n-switch
                    v-model:value="tempExtraData.syncFileStorage"
                    :checked-label="$t('i18n_6a620e3c07')"
                    :unchecked-label="$t('i18n_db709d591b')"
                  />
                </n-grid-item>
                <n-grid-item :span="6" style="text-align: right">
                  <n-form-item>
                    <n-space>
                      <n-tooltip>
                        <template #trigger>
                          <span class="tw">
                            {{ $t('i18n_7f7ee903da') }}

                            <QuestionCircleOutlined v-if="!temp.id" />
                          </span>
                        </template>
                        {{ $t('i18n_ba619a0942') }},{{ $t('i18n_8a1767a0d2') }}
                      </n-tooltip>
                      <n-switch
                        v-model:value="tempExtraData.releaseHideFile"
                        :checked-label="$t('i18n_0a60ac8f02')"
                        :unchecked-label="$t('i18n_c9744f45e7')"
                      />
                    </n-space>
                  </n-form-item>
                </n-grid-item>

                <n-grid-item :span="7" style="text-align: right">
                  <n-form-item>
                    <n-space>
                      <n-tooltip>
                        <template #trigger>
                          <span class="tw">
                            {{ $t('i18n_824607be6b') }}

                            <QuestionCircleOutlined v-if="!temp.id" />
                          </span>
                        </template>

                        {{ $t('i18n_50f975c08e') }}
                      </n-tooltip>
                      <n-input-number v-model:value="temp.resultKeepDay" :min="0" />
                    </n-space>
                  </n-form-item>
                </n-grid-item>

                <n-grid-item :span="7" style="text-align: right">
                  <n-form-item>
                    <n-space>
                      <n-tooltip>
                        <template #trigger>
                          <span class="tw">
                            {{ $t('i18n_50411665d7') }}

                            <QuestionCircleOutlined v-if="!temp.id" />
                          </span>
                        </template>

                        {{ $t('i18n_5dc78cb700') }}
                      </n-tooltip>
                      <n-input-number v-model:value="tempExtraData.resultKeepCount" :min="0" />
                    </n-space>
                  </n-form-item>
                </n-grid-item>
              </n-grid>
            </n-form-item>
            <n-form-item :label="$t('i18n_2f5e828ecd')" path="aliasCode" :help="$t('i18n_8c67370ee5')">
              <n-grid>
                <n-grid-item :span="10">
                  <n-input
                    v-model:value="temp.aliasCode"
                    :max-length="50"
                    :placeholder="$t('i18n_8fbcdbc785')"
                    @keyup.enter="
                      () => {
                        temp = { ...temp, aliasCode: randomStr(6) }
                      }
                    "
                  >
                    <template #enterButton>
                      <n-button type="primary">
                        {{ $t('i18n_6709f4548f') }}
                      </n-button>
                    </template>
                  </n-input>
                </n-grid-item>
                <n-grid-item :span="1" style="text-align: right"></n-grid-item>
                <n-grid-item :span="10">
                  <n-form-item>
                    <n-tooltip>
                      <template #trigger>
                        <span class="tw">
                          {{ $t('i18n_6f73c7cf47') }}

                          <QuestionCircleOutlined v-if="!temp.id" />
                        </span>
                      </template>

                      {{ $t('i18n_bb5aac6004') }}
                    </n-tooltip>
                    <n-input-number v-model:value="tempExtraData.fileStorageKeepDay" :min="0" />
                  </n-form-item>
                </n-grid-item>
              </n-grid>
            </n-form-item>
            <n-form-item path="excludeReleaseAnt">
              <template #label>
                <n-tooltip>
                  <template #trigger>
                    <span class="tw">
                      {{ $t('i18n_eb164b696d') }}

                      <QuestionCircleOutlined v-if="!temp.id" />
                    </span>
                  </template>

                  <ul>
                    <li>{{ $t('i18n_f86324a429') }}</li>
                  </ul>
                </n-tooltip>
              </template>
              <n-input v-model:value="tempExtraData.excludeReleaseAnt" :placeholder="$t('i18n_1e5ca46c26')" />
            </n-form-item>
          </div>
        </n-form>
      </n-card>
    </n-spin>
    <!-- 选择仓库 -->
    <CustomDrawer
      v-if="repositoryisible"
      destroy-on-close
      :title="`${$t('i18n_f89fa9b6c6')}`"
      placement="right"
      :open="repositoryisible"
      width="85vw"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          repositoryisible = false
        }
      "
    >
      <repository
        v-if="repositoryisible"
        ref="repository"
        :choose="true"
        :choose-val="tempRepository && tempRepository.id"
        @confirm="
          (repositoryId) => {
            temp = {
              ...temp,
              repositoryId: repositoryId,
              branchName: '',
              branchTagName: ''
            }
            repositoryisible = false
            changeRepositpry()
          }
        "
        @cancel="
          () => {
            repositoryisible = false
          }
        "
      >
      </repository>
      <template #footer>
        <n-space>
          <n-button
            @click="
              () => {
                repositoryisible = false
              }
            "
          >
            {{ $t('i18n_625fb26b4b') }}
          </n-button>
          <n-button
            type="primary"
            @click="
              () => {
                $refs['repository'].handerConfirm()
              }
            "
          >
            {{ $t('i18n_e83a256e4f') }}
          </n-button>
        </n-space>
      </template>
    </CustomDrawer>
    <!-- 选择脚本 -->
    <CustomDrawer
      v-if="chooseScriptVisible != 0"
      destroy-on-close
      :title="`${$t('i18n_a056d9c4b3')}`"
      placement="right"
      :open="chooseScriptVisible != 0"
      width="70vw"
      :footer-style="{ textAlign: 'right' }"
      @close="
        () => {
          chooseScriptVisible = 0
        }
      "
    >
      <scriptPage
        v-if="chooseScriptVisible"
        ref="scriptPage"
        :choose="chooseScriptVisible === 1 ? 'checkbox' : 'radio'"
        :choose-val="
          chooseScriptVisible === 1
            ? tempExtraData.noticeScriptId
            : temp.script?.indexOf('$ref.script.') != -1
              ? temp.script.replace('$ref.script.', '')
              : ''
        "
        mode="choose"
        @confirm="
          (id) => {
            if (chooseScriptVisible === 1) {
              tempExtraData = { ...tempExtraData, noticeScriptId: id }
            } else if (chooseScriptVisible === 2) {
              temp = { ...temp, script: '$ref.script.' + id }
            }
            chooseScriptVisible = 0
          }
        "
        @cancel="
          () => {
            chooseScriptVisible = 0
          }
        "
      ></scriptPage>
      <template #footer>
        <n-space>
          <n-button
            @click="
              () => {
                chooseScriptVisible = false
              }
            "
          >
            {{ $t('i18n_625fb26b4b') }}
          </n-button>
          <n-button
            type="primary"
            @click="
              () => {
                $refs['scriptPage'].handerConfirm()
              }
            "
          >
            {{ $t('i18n_e83a256e4f') }}
          </n-button>
        </n-space>
      </template>
    </CustomDrawer>
    <!-- 查看容器 -->
    <CustomDrawer
      v-if="dockerListVisible != 0"
      destroy-on-close
      :title="`${$t('i18n_998b7c48a8')}`"
      placement="right"
      :open="dockerListVisible != 0"
      width="70vw"
      @close="
        () => {
          dockerListVisible = 0
        }
      "
    >
      <docker-list v-if="dockerListVisible" ref="dockerlist"></docker-list>
    </CustomDrawer>

    <!-- 查看命令示例 -->
    <CustomModal
      v-if="viewScriptTemplVisible"
      v-model:open="viewScriptTemplVisible"
      destroy-on-close
      width="50vw"
      :title="$t('i18n_f087eb347c')"
      :footer="null"
      :mask-closable="false"
    >
      <n-collapse
        :active-key="
          buildScipts.map((item, index) => {
            return index + ''
          })
        "
      >
        <n-collapse-item v-for="(group, index) in buildScipts" :key="`${index}`" :header="group.title">
          <n-list size="small" bordered>
<n-list-item v-for="(item, index) in group.children" :key="index">
                <n-space>
                  {{ item.title }}

                  <SwapOutlined
                    @click="
                      () => {
                        temp = { ...temp, script: item.value }
                        viewScriptTemplVisible = false
                      }
                    "
                  />
                </n-space>
              </n-list-item>
</n-list>
        </n-collapse-item>
      </n-collapse>
    </CustomModal>
  </div>
</template>
<script>
import { QuestionCircleOutlined, ReloadOutlined, SwapOutlined } from '@ant-design/icons-vue'

import codeEditor from '@/components/codeEditor'
import repository from '@/pages/repository/list.vue'
import scriptPage from '@/pages/script/script-list.vue'
import DockerList from '@/pages/docker/list'
import CustomSelect from '@/components/customSelect'
import { dockerSwarmListAll, dockerSwarmServicesList } from '@/api/docker-swarm'
import {
  getBuildGroupAll,
  editBuild,
  getBranchList,
  buildModeMap,
  releaseMethodMap,
  getBuildGet
} from '@/api/build-info'
import { getSshListAll } from '@/api/ssh'
import { getRepositoryInfo } from '@/api/repository'
import { getNodeListAll, getProjectListAll } from '@/api/node'
// import { getScriptListAll } from "@/api/server-script";
import { getDishPatchListAll } from '@/api/dispatch'
import { itemGroupBy, randomStr } from '@/utils/const'
import { CRON_DATA_SOURCE } from '@/utils/const-i18n'

import { useGuideStore } from '@/stores/guide'
import { afterOptListSimple } from '@/api/dispatch'
import { dockerAllTag } from '@/api/docker-api'
export default {
  components: {
    CustomSelect,
    codeEditor,
    repository,
    scriptPage,
    DockerList
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    data: {
      type: Object,
      default: null
    },
    editSteps: {
      type: Number,
      default: 0
    }
  },
  emits: ['confirm', 'update:editSteps', 'changeBuildMode', 'saveChange'],
  data() {
    return {
      //   afterOptList,
      afterOptListSimple,
      releaseMethodMap,
      CRON_DATA_SOURCE,
      buildModeMap,
      // 当前仓库信息
      tempRepository: null,
      repositoryisible: false,
      chooseScriptVisible: 0,
      // 当前构建信息的 extraData 属性
      tempExtraData: {},
      viewScriptTemplVisible: false,
      buildScipts: [
        {
          title: this.$t('i18n_044b38221e'),
          children: [
            {
              title: this.$t('i18n_2b0f199da0'),
              value: 'mvn clean package -Dmaven.test.skip=true'
            },
            {
              title: this.$t('i18n_461fdd1576'),
              value: 'mvn clean package -Dmaven.test.skip=true -Pprod'
            },
            {
              title: this.$t('i18n_2a1d1da97a'),
              value: 'mvn clean package -Dmaven.test.skip=true -Ptest'
            },
            {
              title: this.$t('i18n_b36e87fe5b'),
              value: 'mvn clean package -DskipTests'
            },
            {
              title: 'mvn clean package',
              value: 'mvn clean package'
            },
            {
              title: this.$t('i18n_1ae2955867'),
              value: 'mvn -f xxx/pom.xml clean package'
            },
            {
              title: this.$t('i18n_ad9a677940'),
              value: 'mvn -s xxx/settings.xml clean package'
            }
          ]
        },
        {
          title: this.$t('i18n_2c921271d5'),
          children: [
            {
              title: this.$t('i18n_b7f770d80b'),
              value: 'npm i && npm run build'
            },
            {
              title: this.$t('i18n_e0ea800e34'),
              value: 'npm i && npm run build:prod'
            },
            {
              title: this.$t('i18n_88b4b85562'),
              value: 'npm i && npm run build:stage'
            },
            {
              title: this.$t('i18n_fcbf0d0a55'),
              value: 'yarn && yarn run build'
            },
            {
              title: this.$t('i18n_329e2e0b2e'),
              value: 'yarn && yarn --cwd xxx build'
            }
          ]
        }
      ],

      branchList: [],
      branchTagList: [],
      dispatchList: [],
      cascaderList: [],
      sshList: [],
      dockerSwarmList: [],
      //集群下 服务下拉数据
      swarmServiceListOptions: [],
      // scriptList: [],
      groupList: [],
      temp: {},
      rules: {
        name: [{ required: true, message: this.$t('i18n_fea996d31e'), trigger: 'blur' }],
        buildMode: [{ required: true, message: this.$t('i18n_e3e85de50c'), trigger: 'blur' }],
        releaseMethod: [{ required: true, message: this.$t('i18n_6d7f0f06be'), trigger: 'blur' }],
        branchName: [{ required: true, message: this.$t('i18n_50951f5e74'), trigger: 'blur' }],
        script: [{ required: true, message: this.$t('i18n_67aa1c0169'), trigger: 'blur' }],
        resultDirFile: [{ required: true, message: this.$t('i18n_cc92cf1e25'), trigger: 'blur' }],
        // releasePath: [{ required: true, message: '请填写发布目录', trigger: 'blur' }],
        repositoryId: [
          {
            required: true,
            message: this.$t('i18n_03c1f7c142'),
            trigger: 'blur'
          }
        ]
      },
      rulesSteps: [
        ['buildMode'],
        ['name', 'branchName', 'repositoryId'],
        ['script', 'resultDirFile'],
        ['releaseMethod']
        // name: [{ required: true, message: '请填写构建名称', trigger: 'blur', : 1 }],
        // buildMode: [{ required: true, message: '请选择构建方式', trigger: 'blur', : 0 }],
        // releaseMethod: [{ required: true, message: '请选择发布操作', trigger: 'blur', : 3 }],
        // branchName: [{ required: true, message: '请选择分支', trigger: 'blur', : 1 }],
        // script: [{ required: true, message: '请填写构建命令', trigger: 'blur', : 2 }],
        // resultDirFile: [{ required: true, message: '请填写产物目录', trigger: 'blur', : 2 }],
        // // releasePath: [{ required: true, message: '请填写发布目录', trigger: 'blur' }],
        // repositoryId: [{ required: true, message: '请填选择构建的仓库', trigger: 'blur', : 1 }]
      ],
      stepsCurrent: 0,
      stepsItems: [
        {
          title: this.$t('i18n_17a74824de')
        },
        {
          title: this.$t('i18n_6ea1fe6baa')

          // status: 'process'
        },
        {
          title: this.$t('i18n_a2ae15f8a7')

          // status: 'wait'
        },
        {
          title: this.$t('i18n_3c91490844')

          // status: 'wait'
        },
        {
          title: this.$t('i18n_9ab433e930')

          // status: 'wait'
        }
      ],

      dslDefault:
        this.$t('i18n_ee19907fad') +
        '\n' +
        this.$t('i18n_d242bc3990') +
        '\n' +
        this.$t('i18n_429d4dbc55') +
        '\n' +
        'runsOn: ubuntu-1ms-latest\n' +
        this.$t('i18n_f1a2a46f52') +
        '\n' +
        'fromTag: xxx\n' +
        this.$t('i18n_36df970248') +
        '\n' +
        this.$t('i18n_30ff009ab3') +
        '\n' +
        this.$t('i18n_ed40308fe9') +
        '\n' +
        this.$t('i18n_df5f80946d') +
        '\n' +
        this.$t('i18n_2296651945') +
        '\n' +
        'steps:\n' +
        '  - uses: java\n' +
        '    version: 8\n' +
        '  - uses: maven\n' +
        '    version: 3.8.7\n' +
        '  - uses: node\n' +
        '    version: 16.3.0\n' +
        '#  - uses: go\n' +
        '#    version: 1.17.6\n' +
        '#  - uses: python3\n' +
        '#    version: 3.6.6\n' +
        this.$t('i18n_23559b6453') +
        '\n' +
        '  - uses: cache\n' +
        '    path: /root/.m2\n' +
        this.$t('i18n_1abf39bdb6') +
        '\n' +
        '    type: global\n' +
        this.$t('i18n_f63345630c') +
        '\n' +
        '  - uses: cache\n' +
        '    path: ${VOYAGER1_WORKING_DIR}/web-vue/node_modules\n' +
        this.$t('i18n_5457c2e99f') +
        '\n' +
        '    mode: copy\n' +
        '  - run: npm config set registry https://registry.npmmirror.com\n' +
        this.$t('i18n_7bcc3f169c') +
        '\n' +
        '  - run: cd  ${VOYAGER1_WORKING_DIR}/web-vue && npm i && npm run build\n' +
        '  - run: cd ${VOYAGER1_WORKING_DIR} && mvn package -s script/settings.xml\n' +
        this.$t('i18n_ea89a319ec') +
        '\n' +
        '# binds:\n' +
        '#  - /Users/user/.m2/settings.xml:/root/.m2/\n' +
        this.$t('i18n_8d90b15eaf') +
        '\n' +
        '# dirChildrenOnly = true will create /var/data/titi and /var/data/tata dirChildrenOnly = false will create /var/data/root/titi and /var/data/root/tata\n' +
        '# copy:\n' +
        '#  - /Users/user/.m2/settings.xml:/root/.m2/:false\n' +
        this.$t('i18n_2b94686a65') +
        '\n' +
        'env:\n' +
        '  NODE_OPTIONS: --max-old-space-size=900\n' +
        this.$t('i18n_993a5c7eee') +
        '\n' +
        '#hostConfig:\n' +
        '#  CpuShares: 1',
      loading: false,
      dockerListVisible: 0,
      dockerAllTagList: [],
      dockerAllTagLoading: true,
      dslEditTabKey: 'content'
    }
  },
  computed: {
    ...mapGetters(useGuideStore, ['getExtendPlugins']),
    selectSshDirs() {
      if (!this.sshList || this.sshList.length <= 0) {
        return []
      }
      const findArray = this.sshList.filter((item) => {
        if (Array.isArray(this.tempExtraData.releaseMethodDataId_3)) {
          return item.id === this.tempExtraData.releaseMethodDataId_3[0]
        }
        return item.id === this.tempExtraData.releaseMethodDataId_3
      })
      if (findArray.length) {
        const fileDirs = findArray[0].fileDirs
        if (!fileDirs) {
          return []
        }
        return JSON.parse(fileDirs).map((item) => {
          return (item + '/').replace(new RegExp('//', 'gm'), '/')
        })
      }
      return []
    },
    buildModeArray() {
      return Object.keys(this.buildModeMap).map((item) => {
        return {
          value: parseInt(item),
          disabled: parseInt(item) === 0 && this.getExtendPlugins.indexOf('inDocker') > -1,
          name: this.buildModeMap[item]
        }
      })
    }
  },
  watch: {
    editSteps: {
      handler(v) {
        this.stepsCurrent = v
      },
      immediate: true
    }
  },
  created() {
    if (this.id) {
      this.refresh()
    } else {
      if (Object.keys(this.data).length) {
        // 复制
        this.handleEdit(this.data)
      } else {
        this.handleAdd()
      }
    }
    this.loadGroupList()
  },
  methods: {
    randomStr,
    refresh() {
      this.loading = true
      getBuildGet({
        id: this.id
      })
        .then((res) => {
          if (res.data) {
            this.handleEdit(res.data)
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    // 新增
    handleAdd() {
      this.temp = { resultKeepDay: 0 }
      this.branchList = []
      // this.tempRepository = {};
      // this.loadRepositoryList();
      this.loadDispatchList()
      this.loadNodeProjectList()
      this.loadSshList()
      this.loadDockerSwarmListAll()
      // this.loadScriptListList();

      this.tempExtraData = {
        cacheBuild: true,
        saveBuildFile: true,
        resultKeepCount: 0,
        fileStorageKeepDay: 0
      }
      this.$refs['editBuildForm']?.restoreValidation()
    },
    // 修改
    handleEdit(record) {
      this.$refs['editBuildForm']?.restoreValidation()
      this.temp = Object.assign({}, record)
      this.temp.buildMode = this.temp.buildMode || 0
      if (this.temp.buildMode === 1) {
        this.loadDockerAllTag()
      }
      // 设置当前临时的 额外构建信息
      this.tempExtraData = JSON.parse(record.extraData || '{}') || {}
      if (typeof this.tempExtraData === 'string') {
        this.tempExtraData = JSON.parse(this.tempExtraData)
      }
      if (this.tempExtraData.cacheBuild === undefined) {
        this.tempExtraData.cacheBuild = true
      }
      if (this.tempExtraData.saveBuildFile === undefined) {
        this.tempExtraData.saveBuildFile = true
      }
      if (this.tempExtraData.resultKeepCount === undefined) {
        this.tempExtraData.resultKeepCount = 0
      }
      if (this.tempExtraData.fileStorageKeepDay === undefined) {
        this.tempExtraData.fileStorageKeepDay = 0
      }

      // 设置发布方式的数据
      if (this.tempExtraData.releaseMethodDataId) {
        if (record.releaseMethod === 1) {
          this.tempExtraData.releaseMethodDataId_1 = this.tempExtraData.releaseMethodDataId
        }
        if (record.releaseMethod === 2) {
          // 数据迁移后修改原始字段
          this.temp = {
            ...this.temp,
            releaseMethodDataIdList: (record.releaseMethodDataId || this.tempExtraData.releaseMethodDataId).split(':')
          }
        }
        if (record.releaseMethod === 3) {
          this.tempExtraData.releaseMethodDataId_3 = this.tempExtraData.releaseMethodDataId.split(',')
        }
      }
      this.tempExtraData = { ...this.tempExtraData }
      this.changeRepositpry(true)

      this.loadDispatchList()
      this.loadDockerSwarmListAll()
      this.loadNodeProjectList()
      // this.loadScriptListList();
      this.loadSshList().then(() => {
        if (this.tempExtraData.releaseMethodDataId_3) {
          //
          const findDirs = this.selectSshDirs
            .filter((item) => {
              return this.tempExtraData.releasePath && this.tempExtraData.releasePath.indexOf(item) > -1
            })
            .sort((item1, item2) => {
              return item2.length - item1.length
            })
          const releaseSshDir = findDirs[0] || ''
          this.tempExtraData = {
            ...this.tempExtraData,
            releaseSshDir: releaseSshDir,
            releasePath2: (this.tempExtraData.releasePath || '').slice(releaseSshDir.length)
          }
        }
      })
      // 默认打开构建流程
      // this.stepsCurrent = this.editSteps
    },
    // // 加载脚本列表
    // loadScriptListList() {
    //   getScriptListAll().then((res) => {
    //     if (res.code === 200) {
    //       this.scriptList = res.data;
    //     }
    //   });
    // },
    // 加载节点分发列表
    loadDispatchList() {
      this.dispatchList = []
      getDishPatchListAll().then((res) => {
        if (res.code === 200) {
          this.dispatchList = res.data
        }
      })
    },
    // 加载节点项目列表
    loadNodeProjectList() {
      this.cascaderList = []
      getNodeListAll().then((res0) => {
        if (res0.code !== 200) {
          return
        }
        getProjectListAll().then((res) => {
          if (res.code === 200) {
            let temp = itemGroupBy(res.data, 'nodeId', 'value', 'children')

            this.cascaderList = temp.map((item) => {
              let findArra = res0.data.filter((res0Item) => {
                return res0Item.id === item.value
              })
              item.label = findArra.length ? findArra[0].name : this.$t('i18n_1622dc9b6b')
              item.children = item.children.map((item2) => {
                return {
                  label: item2.name,
                  value: item2.projectId
                }
              })
              return item
            })
          }
        })
      })
    },
    // 获取仓库分支
    loadBranchList() {
      if (this.tempRepository?.repoType !== 0) {
        return
      }
      this.loadBranchListById(this.tempRepository?.id)
    },
    loadBranchListById(id) {
      this.branchList = []
      this.branchTagList = []
      const params = {
        repositoryId: id
      }
      this.loading = true
      getBranchList(params)
        .then((res) => {
          if (res.code === 200) {
            this.branchList = res.data?.branch || []
            this.branchTagList = res.data?.tags || []
          }
        })
        .finally(() => {
          this.loading = false
        })
    },
    // 提交节点数据
    handleEditBuildOk(build) {
      this.$emit('saveChange', true)
      // 检验表单
      this.$refs['editBuildForm']
        .validate()
        .then(() => {
          const tempExtraData = Object.assign({}, this.tempExtraData)
          // 设置参数
          if (this.temp.releaseMethod === 2) {
            if (this.temp.releaseMethodDataIdList.length < 2) {
              $notification.warn({
                message: this.$t('i18n_8309cec640')
              })
              return false
            }
            tempExtraData.releaseMethodDataId_2_node = this.temp.releaseMethodDataIdList[0]
            tempExtraData.releaseMethodDataId_2_project = this.temp.releaseMethodDataIdList[1]
          } else if (this.temp.releaseMethod === 3) {
            //  (this. tempExtraData.releasePath || '').slice(releaseSshDir.length);
            tempExtraData.releasePath = (
              (tempExtraData.releaseSshDir || '') +
              '/' +
              (tempExtraData.releasePath2 || '')
            ).replace(new RegExp('//', 'gm'), '/')
            tempExtraData.releaseMethodDataId_3 = (tempExtraData.releaseMethodDataId_3 || []).join(',')
          }

          this.temp = {
            ...this.temp,
            extraData: JSON.stringify(tempExtraData),
            resultKeepDay: this.temp.resultKeepDay || 0
          }
          this.$emit('saveChange', true)
          // 提交数据
          editBuild(this.temp)
            .then((res) => {
              if (res.code === 200) {
                // 成功
                $notification.success({
                  message: res.msg
                })
                //
                this.$emit('confirm', build, res.data, this.temp.buildEnvParameter)
              }
            })
            .finally(() => {
              this.$emit('saveChange', false)
            })
        })
        .catch(({ errorFields }) => {
          this.$emit('saveChange', false)
          if (errorFields && errorFields[0]) {
            // console.log(errorFields[0])
            const msg = errorFields[0].errors && errorFields[0].errors[0]
            if (msg) {
              $notification.warn({
                message: msg
              })
              // console.log(error)
            }
            // 切换到对应的流程
            const filedName = errorFields[0].name && errorFields[0].name[0]
            if (filedName) {
              for (let itemIndex in this.rulesSteps) {
                const item = this.rulesSteps[itemIndex]
                // console.log(itemIndex, filedName)
                if (item.includes(filedName)) {
                  this.stepsChange(Number(itemIndex))
                  break
                }

                // this.rulesSteps.forEach((item, index) => {
                //   if (item.includes(filedName)) {
                //     this.stepsChange(index)
                //   }
                // })
              }
            }
          }
        })
    },
    // 选择仓库
    changeRepositpry(noPullBranch) {
      getRepositoryInfo({
        id: this.temp.repositoryId
      }).then((res) => {
        if (res.code === 200) {
          this.tempRepository = res.data
          if (noPullBranch === true) {
            //
          } else {
            // 刷新分支
            this.loadBranchList()
          }
        }
      })
    },
    // 加载 SSH 列表
    loadSshList() {
      return new Promise((resolve) => {
        this.sshList = []
        getSshListAll().then((res) => {
          if (res.code === 200) {
            this.sshList = res.data
            resolve()
          }
        })
      })
    },
    //
    loadDockerSwarmListAll() {
      dockerSwarmListAll().then((res) => {
        this.dockerSwarmList = res.data
      })
    },
    // 选择发布集群时 渲染服务名称 数据
    selectSwarm() {
      this.swarmServiceListOptions = []
      this.tempExtraData = {
        ...this.tempExtraData,
        dockerSwarmServiceName: undefined
      }
      if (this.tempExtraData.dockerSwarmId) {
        // 选中时才处理
        dockerSwarmServicesList('', {
          id: this.tempExtraData.dockerSwarmId
        }).then((res) => {
          if (res.code === 200) {
            this.swarmServiceListOptions = res.data
          }
        })
      } else {
        this.swarmServiceListOptions = []
      }
    },
    // 分组数据
    loadGroupList() {
      getBuildGroupAll().then((res) => {
        if (res.data) {
          this.groupList = res.data
        }
      })
    },
    changeBuildMode(e) {
      if (e.target.value === 1) {
        this.loadDockerAllTag()
      }
      this.temp = { ...this.temp, script: '' }
      this.$emit('changeBuildMode', e.target.value)
    },
    // 查询 docker tag
    loadDockerAllTag() {
      this.dockerAllTagLoading = true
      dockerAllTag()
        .then((res) => {
          if (res.code === 200) {
            this.dockerAllTagList = res.data || []
          }
        })
        .finally(() => {
          this.dockerAllTagLoading = false
        })
    },
    stepsChange(current) {
      this.$emit('update:editSteps', current)
    }
  }
}
</script>
