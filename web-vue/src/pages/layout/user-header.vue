<template>
  <div class="user-header">
    <n-button-group>
      <!-- 工作空间信息 -->
      <n-button v-if="mode === 'normal'" dashed class="workspace voyager1-workspace btn-group-item">
        <div class="workspace-name">
          <n-tooltip placement="bottom">
            <template #trigger>
              <span class="workspace-trigger">
                <SwitcherOutlined />
                {{ selectWorkspace.name }}
                <template v-if="myClusterList.length > 1 && selectWorkspace.clusterInfoId">
                  /
                  {{
                    myClusterList.find((item) => {
                      return item.id === selectWorkspace.clusterInfoId
                    }) &&
                    myClusterList.find((item) => {
                      return item.id === selectWorkspace.clusterInfoId
                    }).name
                  }}
                </template>
                <template v-if="!inClusterUrl">
                  <SwapOutlined @click="handleClusterChange(selectCluster)" />
                </template>
              </span>
            </template>

            <!-- 【】\u3011 -->
            {{ $t('i18n_8f36f2ede7') }}{{ selectWorkspace.name }} {{ `\u3010` }}{{ $t('i18n_3bf9c5b8af')
            }}{{ selectWorkspace.group || $t('i18n_71dc8feb59') }}
            {{ `\u3011` }}
          </n-tooltip>
        </div>
      </n-button>
      <n-button v-if="mode === 'management'" dashed>
        <div class="workspace-name">
          <n-tooltip placement="bottom">
            <template #trigger>
              <span class="tw">
                <span class="tw">
                  <span class="workspace-trigger">
                    <ClusterOutlined />
                    {{ selectCluster && selectCluster.name }}
                  </span>
                </span>
              </span>
            </template>
            `${$t('i18n_f668c8c881')}${selectCluster && selectCluster.name}`
          </n-tooltip>
        </div>
      </n-button>
      <n-dropdown :options="userMenuOptions" trigger="click" @select="handleDropSelect">
        <n-button type="primary" class="btn-group-item">
          <span class="user-name-switch">{{ getUserInfo.name }}</span>
          <DownOutlined />
        </n-button>
      </n-dropdown>
    </n-button-group>

    <!-- 修改密码区 -->
    <CustomModal
      v-if="updateNameVisible"
      v-model:open="updateNameVisible"
      :show-footer="false"
      :width="'60vw'"
      :title="$t('i18n_629a6ad325')"
      :footer="null"
      :mask-closable="false"
    >
      <n-tabs v-model:value="temp.tabActiveKey" @update:value="tabChange">
        <n-tab-pane :name="1" :tab="$t('i18n_7fc88aeeda')">
          <n-spin tip="Loading..." :spinning="confirmLoading">
            <n-form ref="pwdForm" :rules="rules" :model="temp" @submit.prevent="handleUpdatePwdOk">
              <n-form-item :label="$t('i18n_01e94436d1')" path="oldPwd">
                <n-input v-model:value="temp.oldPwd" type="password" :placeholder="$t('i18n_9c19a424dc')" />
              </n-form-item>
              <n-form-item :label="$t('i18n_bf7da0bf02')" path="newPwd">
                <n-input v-model:value="temp.newPwd" type="password" :placeholder="$t('i18n_abdd7ea830')" />
              </n-form-item>
              <n-form-item :label="$t('i18n_3fbdde139c')" path="confirmPwd">
                <n-input v-model:value="temp.confirmPwd" type="password" :placeholder="$t('i18n_a7a9a2156a')" />
              </n-form-item>
              <n-form-item>
                <n-grid type="flex" justify="center">
                  <n-grid-item :span="2">
                    <n-button type="primary" attr-type="submit" :loading="confirmLoading">{{
                      $t('i18n_80cfc33cbe')
                    }}</n-button>
                  </n-grid-item>
                </n-grid>
              </n-form-item>
            </n-form>
          </n-spin>
        </n-tab-pane>
      </n-tabs>
    </CustomModal>
    <!-- 修改用户资料区 -->
    <CustomModal
      v-if="updateUserVisible"
      v-model:open="updateUserVisible"
      :confirm-loading="confirmLoading"
      :title="$t('i18n_ed367abd1a')"
      :mask-closable="false"
      @ok="handleUpdateUserOk"
    >
      <n-form ref="userForm" :rules="rules" :model="temp">
        <n-form-item :label="$t('i18n_e0f937d57f')" path="token">
          <n-input v-model:value="temp.token" disabled placeholder="Token">
            <template #suffix>
              <copy-text :text="temp.token" />
            </template>
          </n-input>
        </n-form-item>
        <n-form-item :label="$t('i18n_e6bf31e8e6')" path="md5Token">
          <n-input v-model:value="temp.md5Token" disabled placeholder="Token">
            <template #suffix>
              <copy-text :text="temp.md5Token" />
            </template>
          </n-input>
        </n-form-item>
        <n-form-item :label="$t('i18n_23eb0e6024')" path="name">
          <n-input v-model:value="temp.name" :placeholder="$t('i18n_23eb0e6024')" />
        </n-form-item>
        <n-form-item :label="$t('i18n_6ab78fa2c4')" path="email">
          <n-input v-model:value="temp.email" :placeholder="$t('i18n_6ab78fa2c4')" />
        </n-form-item>
        <n-form-item v-show="showCode" :label="$t('i18n_e3cf0abd35')" path="code">
          <n-grid :x-gap="8">
            <n-grid-item :span="15">
              <n-input v-model:value="temp.code" :placeholder="$t('i18n_e3cf0abd35')" />
            </n-grid-item>
            <n-grid-item :span="4">
              <n-button type="primary" :disabled="!temp.email" @click="sendEmailCode">{{
                $t('i18n_c5c3583bfc')
              }}</n-button>
            </n-grid-item>
          </n-grid>
        </n-form-item>
        <n-form-item :label="$t('i18n_55e99f5106')" path="dingDing">
          <n-input v-model:value="temp.dingDing" :placeholder="$t('i18n_55e99f5106')" />
        </n-form-item>
        <n-form-item :label="$t('i18n_2246d128cb')" path="workWx">
          <n-input v-model:value="temp.workWx" :placeholder="$t('i18n_2246d128cb')" />
        </n-form-item>
      </n-form>
    </CustomModal>
    <!-- 个性配置区 -->
    <CustomModal
      v-if="customizeVisible"
      v-model:open="customizeVisible"
      :title="$t('i18n_cb09b98416')"
      :footer="null"
      :mask-closable="false"
      width="50%"
      @ok="customizeVisible = false"
    >
      <n-form :model="temp">
        <n-alert banner>
          <template #message> {{ $t('i18n_bf93517805') }},{{ $t('i18n_52b526ab9e') }} </template>
        </n-alert>
        <!-- <n-form-item label="页面导航">
          <n-space>
            <n-switch
              :checked-label="开"
              @click="toggleGuide"
              :value="!this.guideStatus"
              :disabled="this.getDisabledGuide"
              :unchecked-label="关"
            />

            <div v-if="!this.guideStatus">
              重置导航
              <RestOutlined @click="restGuide" />
            </div>
          </n-space>
        </n-form-item> -->
        <n-form-item :label="$t('i18n_156af3b3d1')">
          <template #help>{{ $t('i18n_ecdf9093d0') }}</template>

          <n-switch
            :checked-label="$t('i18n_0a60ac8f02')"
            :value="menuMultipleFlag"
            :unchecked-label="$t('i18n_c9744f45e7')"
            @click="toggleMenuMultiple"
          />
        </n-form-item>
        <!-- <n-form-item label="页面配置">
          <n-space>
            自动撑开：
            <n-switch
              :checked-label="是"
              @click="toggleFullScreenFlag"
              :value="this.fullScreenFlag"
              :unchecked-label="否"
            />
          </n-space>
        </n-form-item>
        <n-form-item label="滚动条显示">
          <n-space>
            全局配置：
            <n-switch
              :checked-label="显示"
              @click="toggleScrollbarFlag"
              :value="this.scrollbarFlag"
              :unchecked-label="不显示"
            />
          </n-space>
        </n-form-item> -->
        <n-form-item :label="$t('i18n_0113fc41fc')">
          <template #help>{{ $t('i18n_b5fdd886b6') }}</template>

          <n-switch
            :checked-label="$t('i18n_185926bf98')"
            :value="fullscreenViewLog"
            :unchecked-label="$t('i18n_c5a2c23d89')"
            @click="toggleFullscreenViewLog"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_5d9c139f38')">
          <n-radio-group v-model:value="themeView" button-style="solid">
            <n-radio-button v-for="item in getSupportThemes" :key="item.value" :value="item.value">
              {{ item.label }}
            </n-radio-button>
          </n-radio-group>

          <template #help>{{ $t('i18n_2b4bb321d7') }}</template>
        </n-form-item>
        <n-form-item :label="$t('i18n_593e04dfad')">
          <n-radio-group v-model:value="menuThemeView" button-style="solid">
            <n-radio-button value="light">{{ $t('i18n_48d0a09bdd') }}</n-radio-button>
            <n-radio-button value="dark">{{ $t('i18n_41e8e8b993') }}</n-radio-button>
          </n-radio-group>

          <template #help>{{ $t('i18n_fbfeb76b33') }}</template>
        </n-form-item>

        <n-form-item :label="$t('i18n_4f50cd2a5e')">
          <n-switch
            :checked-label="$t('i18n_03e59bb33c')"
            :value="compactView"
            :unchecked-label="$t('i18n_43e534acf9')"
            @click="toggleCompactView"
          />
        </n-form-item>
        <n-form-item :label="$t('i18n_295bb704f5')">
          <template #help>{{ $t('i18n_92f9a3c474') }}</template>

          <n-select
            v-model:value="locale"
            style="width: 220px"
            :options="supportLang.map((item) => ({ label: item.label, value: item.value }))"
          />
        </n-form-item>
      </n-form>
    </CustomModal>
    <!-- 查看操作日志 -->
    <CustomModal
      v-if="viewLogVisible"
      v-model:open="viewLogVisible"
      :width="'90vw'"
      :title="$t('i18n_cda84be2f6')"
      :footer="null"
      :mask-closable="false"
      @ok="viewLogVisible = false"
    >
      <user-log v-if="viewLogVisible"></user-log>
    </CustomModal>
  </div>
</template>
<script>
import { ClusterOutlined, DownOutlined, SwitcherOutlined } from '@ant-design/icons-vue'

import { h } from 'vue'
import { NIcon } from 'naive-ui'
import {
  BarsOutlined,
  LockOutlined,
  LogoutOutlined,
  ProfileOutlined,
  RestOutlined,
  RetweetOutlined,
  SkinOutlined,
  SwapOutlined
} from '@ant-design/icons-vue'
import { editUserInfo, getUserInfo, myWorkspace, sendEmailCode, updatePwd, clusterList } from '@/api/user/user'

import sha1 from 'js-sha1'
// import Vue from 'vue'
import { itemGroupBy } from '@/utils/const'
import UserLog from './user-log.vue'
import { mapState } from 'pinia'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import { useGuideStore } from '@/stores/guide'
import { supportLang } from '@/i18n'
import { useAllMenuStore } from '@/stores/menu2'
export default {
  components: {
    UserLog
  },
  inject: ['reload'],
  props: {
    mode: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      supportLang,
      collapsed: false,
      // 修改密码框
      updateNameVisible: false,
      updateUserVisible: false,
      temp: {},

      myWorkspaceList: [],
      myClusterList: [],
      currentClusterId: '',
      selectWorkspace: {},
      customizeVisible: false,
      // 表单校验规则
      rules: {
        name: [
          { required: true, message: this.$t('i18n_916ff9eddd'), trigger: 'blur' },
          { max: 10, message: this.$t('i18n_6446b6c707'), trigger: 'blur' },
          { min: 2, message: this.$t('i18n_6446b6c707'), trigger: 'blur' }
        ],

        oldPwd: [
          { required: true, message: this.$t('i18n_9c19a424dc'), trigger: 'blur' },
          { max: 20, message: this.$t('i18n_f4b7c18635'), trigger: 'blur' },
          { min: 6, message: this.$t('i18n_f4b7c18635'), trigger: 'blur' }
        ],

        newPwd: [
          { required: true, message: this.$t('i18n_abdd7ea830'), trigger: 'blur' },
          { max: 20, message: this.$t('i18n_f4b7c18635'), trigger: 'blur' },
          { min: 6, message: this.$t('i18n_f4b7c18635'), trigger: 'blur' }
        ],

        confirmPwd: [
          { required: true, message: this.$t('i18n_a7a9a2156a'), trigger: 'blur' },
          { max: 20, message: this.$t('i18n_f4b7c18635'), trigger: 'blur' },
          { min: 6, message: this.$t('i18n_f4b7c18635'), trigger: 'blur' }
        ],

        email: [
          // { required: true, message: "请输入邮箱", trigger: "blur" }
        ]
      },
      viewLogVisible: false,
      confirmLoading: false
    }
  },
  computed: {
    userMenuOptions() {
      const options = []
      const divider = (key) => ({ type: 'divider', key })
      const icon = (component) => () => h(NIcon, null, { default: () => h(component) })
      const clusterName = (item) => {
        if (!(this.myClusterList.length > 1 && item.clusterInfoId)) {
          return ''
        }
        const cluster = this.myClusterList.find((item2) => item2.id === item.clusterInfoId)
        return cluster ? `【${cluster.name}】` : ''
      }
      const workspaceItem = (item) => ({
        label: `${item.name || this.$t('i18n_71dc8feb59')}${clusterName(item)}`,
        key: `ws-${item.id}`,
        disabled: item.id === this.selectWorkspace.id
      })

      if (this.mode === 'normal') {
        const wsChildren = []
        if (this.myWorkspaceList.length === 1) {
          const children = (this.myWorkspaceList[0] && this.myWorkspaceList[0].children) || []
          children.forEach((item, index) => {
            wsChildren.push(workspaceItem(item))
            if (index < children.length - 1) {
              wsChildren.push(divider(`ws-d-${item.id}`))
            }
          })
        } else if (this.myWorkspaceList.length > 1) {
          this.myWorkspaceList.forEach((group, index1) => {
            wsChildren.push({
              label: group.value || this.$t('i18n_71dc8feb59'),
              key: `ws-group-${index1}`,
              children: ((children) => {
                const arr = []
                children.forEach((item, index) => {
                  arr.push(workspaceItem(item))
                  if (index < children.length - 1) {
                    arr.push(divider(`ws-g${index1}-d-${item.id}`))
                  }
                })
                return arr
              })(group.children || [])
            })
            if (index1 < this.myWorkspaceList.length - 1) {
              wsChildren.push(divider(`ws-gd-${index1}`))
            }
          })
        }
        options.push({
          label: this.$t('i18n_ccb2fdd838'),
          key: 'ws-sub',
          icon: icon(RetweetOutlined),
          children: wsChildren
        })
        options.push(divider('d-ws'))
      }

      if (this.mode === 'management') {
        const clusterChildren = []
        this.myClusterList.forEach((item, index) => {
          clusterChildren.push({
            label: item.name,
            key: `cluster-${item.id}`,
            disabled: item.id === this.selectCluster?.id || !item.url
          })
          if (index < this.myClusterList.length - 1) {
            clusterChildren.push(divider(`cluster-d-${item.id}`))
          }
        })
        options.push({
          label: this.$t('i18n_d61b8fde35'),
          key: 'cluster-sub',
          icon: icon(RetweetOutlined),
          children: clusterChildren
        })
      }

      options.push(
        { label: this.$t('i18n_629a6ad325'), key: 'pwd', icon: icon(LockOutlined) },
        divider('d-pwd'),
        { label: this.$t('i18n_d7cc44bc02'), key: 'user', icon: icon(ProfileOutlined) },
        divider('d-user'),
        { label: this.$t('i18n_cda84be2f6'), key: 'userlog', icon: icon(BarsOutlined) },
        divider('d-userlog'),
        { label: this.$t('i18n_b4fd7afd31'), key: 'customize', icon: icon(SkinOutlined) },
        divider('d-customize'),
        { label: this.$t('i18n_44efd179aa'), key: 'logout', icon: icon(LogoutOutlined) },
        divider('d-logout'),
        { label: this.$t('i18n_86c1eb397d'), key: 'logout-swap', icon: icon(SwapOutlined) },
        divider('d-logout-swap'),
        { label: this.$t('i18n_a795fa52cd'), key: 'logout-all', icon: icon(RestOutlined) }
      )

      return options
    },
    ...mapState(useUserStore, ['getToken', 'getUserInfo']),
    ...mapState(useAppStore, ['getWorkspaceId']),
    ...mapState(useGuideStore, [
      'getGuideCache',
      'getDisabledGuide',
      'getThemeView',
      'getMenuThemeView',
      'getLocale',
      'getSupportThemes'
    ]),
    showCode() {
      return this.getUserInfo.email !== this.temp.email
    },
    guideStatus() {
      return this.getGuideCache.close
    },
    menuMultipleFlag() {
      return this.getGuideCache.menuMultipleFlag === undefined ? true : this.getGuideCache.menuMultipleFlag
    },
    fullScreenFlag() {
      return this.getGuideCache.fullScreenFlag === undefined ? true : this.getGuideCache.fullScreenFlag
    },
    scrollbarFlag() {
      return this.getGuideCache.scrollbarFlag === undefined ? true : this.getGuideCache.scrollbarFlag
    },
    compactView() {
      return this.getGuideCache.compactView === undefined ? false : this.getGuideCache.compactView
    },
    themeView: {
      set: function (value) {
        useGuideStore().toggleThemeView(value)
      },
      get: function () {
        return useGuideStore().getCatchThemeView()
        // return this.getThemeView()
      }
    },
    menuThemeView: {
      set: function (value) {
        useGuideStore().toggleMenuThemeView(value)
      },
      get: function () {
        return this.getMenuThemeView()
      }
    },
    locale: {
      set: function (value) {
        useGuideStore().changeLocale(value)
      },
      get: function () {
        return useGuideStore().getLocale()
      }
    },
    fullscreenViewLog() {
      return !!this.getGuideCache.fullscreenViewLog
    },
    selectCluster: {
      get: function () {
        const temp = this.myClusterList.find((item) => {
          return item.id === this.currentClusterId
        })
        return temp
      }
    },
    inClusterUrl() {
      const data = this.selectCluster
      if (!data || !data.url) {
        // 没有配置集群地址
        return true
      }
      return window.location.href.indexOf(data && data.url) === 0
    }
  },

  created() {
    this.init()
  },
  methods: {
    // Naive n-dropdown 用 on-select（key）而非 props.onClick（Ant 写法）
    handleDropSelect(key) {
      const k = String(key)
      if (k.startsWith('ws-')) {
        const id = k.slice(3)
        for (const group of this.myWorkspaceList || []) {
          for (const item of group.children || []) {
            if (item.id === id) {
              this.handleWorkspaceChange(item)
              return
            }
          }
        }
        return
      }
      if (k.startsWith('cluster-')) {
        const id = k.slice(8)
        const item = (this.myClusterList || []).find((x) => x.id === id)
        if (item) this.handleClusterChange(item)
        return
      }
      switch (k) {
        case 'pwd':
          this.handleUpdatePwd()
          break
        case 'user':
          this.handleUpdateUser()
          break
        case 'userlog':
          this.handleUserlog()
          break
        case 'customize':
          this.customize()
          break
        case 'logout':
          this.logOut()
          break
        case 'logout-swap':
          this.logOutSwap()
          break
        case 'logout-all':
          this.logOutAll()
          break
      }
    },

    customize() {
      this.customizeVisible = true
    },

    init() {
      if (this.mode === 'normal') {
        // 获取工作空间
        myWorkspace().then((res) => {
          if (res.code == 200 && res.data) {
            const tempArray = res.data

            this.myWorkspaceList = itemGroupBy(tempArray, 'group', 'value', 'children')

            let wid = this.$route.query.wid
            wid = wid ? wid : this.getWorkspaceId()
            const existWorkspace = tempArray.find((item) => item.id === wid)

            if (existWorkspace) {
              this.$router.push({
                query: { ...this.$route.query, wid: wid }
              })
              this.selectWorkspace = existWorkspace
            } else {
              this.handleWorkspaceChange(res.data[0])
            }
          }
        })
      }
      // 获取集群
      clusterList().then((res) => {
        if (res.code == 200 && res.data) {
          this.myClusterList = res.data.list || []
          this.currentClusterId = res.data.currentId
        }
      })
    },
    // 切换引导
    toggleGuide() {
      useGuideStore()
        .toggleGuideFlag()
        .then((flag) => {
          if (flag) {
            $notification.success({
              message: this.$t('i18n_fe231ff92f')
            })
          } else {
            $notification.success({
              message: this.$t('i18n_c75d0beca8')
            })
          }
        })
    },
    // 切换菜单打开
    toggleMenuMultiple() {
      useGuideStore()
        .toggleMenuFlag()
        .then((flag) => {
          if (flag) {
            $notification.success({
              message: this.$t('i18n_63c9d63eeb')
            })
          } else {
            $notification.success({
              message: this.$t('i18n_1498557b2d')
            })
          }
        })
    },
    // 页面全屏
    toggleFullScreenFlag() {
      useGuideStore()
        .toggleFullScreenFlag()
        .then((flag) => {
          if (flag) {
            $notification.success({
              message: this.$t('i18n_ef28d3bff2')
            })
          } else {
            $notification.success({
              message: this.$t('i18n_ba6ea3d480')
            })
          }
        })
    },
    // 切换滚动条是否显示
    toggleScrollbarFlag() {
      useGuideStore()
        .toggleScrollbarFlag()
        .then((flag) => {
          if (flag) {
            $notification.success({
              message: this.$t('i18n_af51211a73')
            })
          } else {
            $notification.success({
              message: this.$t('i18n_1afdb4a364')
            })
          }
        })
    },
    // 切换全屏查看日志
    toggleFullscreenViewLog() {
      useGuideStore()
        .toggleFullscreenViewLog()
        .then((fullscreenViewLog) => {
          if (fullscreenViewLog) {
            $notification.success({
              message: this.$t('i18n_82b89bd049')
            })
          } else {
            $notification.success({
              message: this.$t('i18n_57978c11d1')
            })
          }
        })
    },
    toggleCompactView() {
      useGuideStore()
        .toggleCompactView()
        .then((compact) => {
          if (compact) {
            $notification.success({
              message: this.$t('i18n_6e60d2fc75')
            })
          } else {
            $notification.success({
              message: this.$t('i18n_702430b89d')
            })
          }
        })
    },
    restGuide() {
      useGuideStore()
        .restGuide()
        .then(() => {
          $notification.success({
            message: this.$t('i18n_dddf944f5f')
          })
        })
    },
    // 彻底退出登录
    logOutAll() {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_8e38d55231'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return useUserStore()
            .logOut()
            .then(() => {
              $notification.success({
                message: this.$t('i18n_499f058a0b')
              })
              localStorage.clear()
              this.$router.replace({
                path: '/login',
                query: {}
              })
            })
        }
      })
    },
    // 切换账号登录
    logOutSwap() {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_ac783bca36'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return useUserStore()
            .logOut()
            .then(() => {
              $notification.success({
                message: this.$t('i18n_499f058a0b')
              })
              useAppStore().changeWorkspace('')
              this.$router.replace({
                path: '/login',
                query: {}
              })
            })
        }
      })
    },
    // 退出登录
    logOut() {
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: this.$t('i18n_70b9a2c450'),
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return useUserStore()
            .logOut()
            .then(() => {
              $notification.success({
                message: this.$t('i18n_499f058a0b')
              })
              const query = Object.assign({}, this.$route.query)
              this.$router.replace({
                path: '/login',
                query: query
              })
            })
        }
      })
    },
    // 加载修改密码对话框
    handleUpdatePwd() {
      this.updateNameVisible = true
      this.tabChange(1)
    },
    // 修改密码
    handleUpdatePwdOk() {
      // 先执行表单校验（naive n-form 无 finish 事件，需手动 validate）
      this.$refs['pwdForm'].validate().then(() => {
        // 判断两次新密码是否一致
        if (this.temp.newPwd !== this.temp.confirmPwd) {
          $notification.error({
            message: this.$t('i18n_6f15f0beea')
          })
          return
        }
        // 提交修改
        const params = {
          oldPwd: sha1(this.temp.oldPwd),
          newPwd: sha1(this.temp.newPwd)
        }
        this.confirmLoading = true
        updatePwd(params)
          .then((res) => {
            // 修改成功
            if (res.code === 200) {
              // 退出登录
              userStore()
                .logOut()
                .then(() => {
                  $notification.success({
                    message: res.msg
                  })

                  this.updateNameVisible = false
                  this.$router.push('/login')
                })
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    },
    // 加载修改用户资料对话框
    handleUpdateUser() {
      getUserInfo().then((res) => {
        if (res.code === 200) {
          this.temp = res.data
          this.temp.token = this.getToken()
          //this.temp.md5Token = res.data.md5Token;
          this.updateUserVisible = true
        }
      })
    },
    // 发送邮箱验证码
    sendEmailCode() {
      if (!this.temp.email) {
        $notification.error({
          message: this.$t('i18n_2ba4c81587')
        })
        return
      }
      sendEmailCode(this.temp.email).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
        }
      })
    },
    // 修改用户资料
    handleUpdateUserOk() {
      // 检验表单
      this.$refs['userForm'].validate().then(() => {
        const tempData = Object.assign({}, this.temp)
        ;(delete tempData.token, delete tempData.md5Token)
        this.confirmLoading = true
        editUserInfo(tempData)
          .then((res) => {
            // 修改成功
            if (res.code === 200) {
              $notification.success({
                message: res.msg
              })
              // 清空表单校验
              this.$refs['userForm'].restoreValidation()
              this.updateUserVisible = false
              userStore().refreshUserInfo()
            }
          })
          .finally(() => {
            this.confirmLoading = false
          })
      }).catch(() => {})
    },
    // 工作空间切换
    handleWorkspaceChange(item) {
      const cluster = this.myClusterList.find((item2) => {
        return item2.id === item.clusterInfoId
      })
      if (cluster && cluster.url && location.href.indexOf(cluster.url) !== 0) {
        let url = `${cluster.url}/#/${this.$route.fullPath}`.replace(/[\\/]+[\\/]/g, '/').replace(':/', '://')
        url = url.replace(`wid=${this.selectWorkspace.id}`, `wid=${item.id}`)
        // console.log(location.href.indexOf(cluster.url), url);
        location.href = url
      } else {
        appStore()
          .changeWorkspace(item.id)
          .then(() => {
            this.$router
              .push({
                query: { ...this.$route.query, wid: item.id }
              })
              .then(() => {
                useAllMenuStore().restLoadSystemMenus('normal')
                this.reload()
              })
          })
      }
    },
    // 集群切换
    handleClusterChange(item) {
      if (item.url) {
        const url = `${item.url}/#/${this.$route.fullPath}`.replace(/[\\/]+[\\/]/g, '/').replace(':/', '://')
        // console.log(url);
        location.href = url
      } else {
        $notification.error({
          message: this.$t('i18n_db2d99ed33')
        })
      }
    },
    tabChange(key) {
      if (key === 1) {
        this.temp = { tabActiveKey: key }
      }
    },

    handleUserlog() {
      this.viewLogVisible = true
    }
  }
}
</script>
<style scoped>
.btn-group-item {
  padding: 0 5px;
}
.workspace-name {
  min-width: 30px;
  max-width: 200px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-weight: bold;
}

.user-name {
  min-width: 30px;
  max-width: 100px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* 用户名 + 下拉箭头（合并到单个触发按钮） */
.user-name-switch {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
</style>
