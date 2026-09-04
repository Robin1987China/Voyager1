<template>
  <n-layout id="app-layout" has-sider>
    <n-layout-sider v-model:collapsed="collapsed" :theme="menuTheme" :trigger="null" collapsible>
      <n-layout-sider v-model:collapsed="collapsed" class="sider" :theme="menuTheme" :trigger="null" collapsible>
        <div class="sider-content">
          <n-tooltip placement="right">
            <template #trigger>
              <span class="tw">
                <div
                  class="logo text-overflow-hidden"
                  :style="`color:${menuTheme === 'light' && theme === 'light' ? '#000' : '#fff'}`"
                  @click="changeCollapsed()"
                >
                  <img :src="logoUrl || defaultLogo" alt="logo" />
                  {{ !collapsed ? subTitle : '' }}
                </div>
              </span>
            </template>

            {{ subTitle }}
            <div>&nbsp;</div>
            <div>{{ $t('i18n_7548ea6316') }}</div>
          </n-tooltip>
          <div v-if="isSystemUser" class="mode-switch">
            <n-radio-group
              v-if="!collapsed"
              :value="mode"
              button-style="solid"
              class="mode-switch-group"
              @update:value="onModeChange"
            >
              <n-radio-button value="normal">{{ $t('i18n_d9c28e376c') }}</n-radio-button>
              <n-radio-button value="management">{{ $t('i18n_4d85ac1250') }}</n-radio-button>
            </n-radio-group>
            <n-tooltip v-else placement="right">
              <template #trigger>
                <n-button text block class="mode-switch-btn" @click="onToggleMode">
                  <template #icon><swap-outlined /></template>
                </n-button>
              </template>
              {{ mode === 'normal' ? $t('i18n_4d85ac1250') : $t('i18n_d9c28e376c') }}
            </n-tooltip>
          </div>
          <div class="sider-menu">
            <side-menu :mode="mode" :theme="menuTheme" />
          </div>
          <div v-if="version" class="sider-version">
            {{ collapsed ? '' : $t('i18n_fe2df04a16') + ' ' }}{{ version }}
          </div>
        </div>
      </n-layout-sider>
    </n-layout-sider>
    <n-layout>
      <div
        class="app-header"
        :class="{
          'app-header-dark': theme == 'dark'
        }"
        :style="{
          background: theme === 'light' ? '#fff' : '#141414'
        }"
      >
        <n-space direction="vertical" style="width: 100%" :item-style="{ width: '100%' }">
          <n-alert
            v-if="systemNotificationData && systemNotificationData.enabled"
            :type="systemNotificationData.level || 'info'"
            :closable="systemNotificationData.closable"
            banner
            :after-close="notificationAfterClose"
          >
            <template #message> <div v-html="systemNotificationData.title"></div> </template>
            <template #description> <div v-html="systemNotificationData.content"></div> </template>
          </n-alert>
          <div class="header-row">
            <content-tab :mode="mode" class="header-tabs" />
            <UserHeader :mode="mode" class="header-user" />
          </div>
        </n-space>
      </div>
      <n-layout-content
        :style="{
          width: '100%',
          overflowY: 'auto',
          backgroundColor: theme === 'light' ? '#fff' : ''
        }"
        class="layout-content"
      >
        <router-view v-slot="{ Component, route }">
          <keep-alive :include="menuTabKeyList">
            <component
              :is="wrap(String(route.name), Component)"
              v-if="menuTabKeyList.length"
              :key="String(route.name)"
            />
          </keep-alive>
        </router-view>
      </n-layout-content>
      <n-layout-footer v-show="false" style="text-align: center">
        Voyager1 ©{{ new Date().getFullYear() }}
      </n-layout-footer>
    </n-layout>
  </n-layout>
</template>
<script lang="ts" setup>
import SideMenu from './side-menu.vue'
import UserHeader from './user-header.vue'
import ContentTab from './content-tab.vue'
import { checkSystem, loadingLogo } from '@/api/install'
import defaultLogo from '@/assets/images/voyager1-mark.svg'
import { useAllMenuStore } from '@/stores/menu2'
import { UserNotificationType, systemNotification } from '@/api/user/user-notification'
import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()
const props = defineProps({
  mode: {
    type: String,
    required: true
  }
})
const useUserStore2 = userStore()
// 页面缓存对象
const wrapperMap = shallowRef(new Map())
// 组件套壳，动态添加name属性
const wrap = (name: string, component: any) => {
  let wrapper
  const wrapperName = name
  if (wrapperMap.value.has(wrapperName)) {
    wrapper = wrapperMap.value.get(wrapperName)
  } else {
    //包裹组件
    wrapper = {
      name: wrapperName,
      render() {
        return h('div', component)
      }
    }
    wrapperMap.value.set(wrapperName, wrapper)
  }
  return h(wrapper)
}
const menuStore = useAllMenuStore()
// 获取两个菜单中的tab key
const menuTabKeyList = computed(() => {
  return [...menuStore.normal_tabList, ...menuStore.management_tabList].map((item: { key: string }) => item.key)
})
// 监听menuTabKeyList变化
watch(
  menuTabKeyList,
  (newKeys, oldKeys) => {
    if (!useUserStore2.getToken()) {
      // 登录登录会触发 tab 变化，这里不改变路由缓存。避免重新加载路由触发请求接口
      // 已经由 v-if="menuTabKeyList.length" 实现
      // return
    } // 获取已被删除的key
    oldKeys
      ?.filter((key) => {
        return !newKeys.includes(key)
      })
      .forEach((key) => {
        // 删除缓存
        wrapperMap.value.delete(key)
      })
  },
  {
    immediate: true
  }
)

const collapsed = ref(false)
const subTitle = ref($t('i18n_03d9de2834'))
const logoUrl = ref('')
const version = ref('')

const _appStore = appStore()
const _guideStore = guideStore()
onMounted(() => {
  checkSystemHannder()

  collapsed.value = _appStore.getCollapsed
})

const router = useRouter()
// const route = useRoute()

// 功能管理 / 系统管理 切换（仅超级管理员可见）
const _userStore = userStore()
const { userInfo } = storeToRefs(_userStore)
const isSystemUser = computed(() => !!userInfo.value?.systemUser)
const onModeChange = (value: string) => {
  if (value === props.mode) {
    return
  }
  router.push({ path: value === 'normal' ? '/overview' : '/system/overview' })
}
const onToggleMode = () => {
  router.push({ path: props.mode === 'normal' ? '/system/overview' : '/overview' })
}

const menuTheme = computed(() => {
  return _guideStore.getMenuThemeView()
})

const theme = computed(() => {
  return _guideStore.getThemeView()
})
const voyager1Window_ = voyager1Window()

const systemNotificationData = ref<UserNotificationType>({})
// 检查是否需要初始化
const checkSystemHannder = () => {
  checkSystem().then((res) => {
    if (res.data) {
      voyager1Window_.routerBase = res.data.routerBase || ''
      if (res.data.subTitle) {
        subTitle.value = res.data.subTitle
      }
      if (res.data.version) {
        version.value = res.data.version
      }

      // 禁用导航
      _guideStore.commitGuide({
        disabledGuide: res.data.disabledGuide ? true : false,
        extendPlugins: res.data.extendPlugins as string[]
      })

      $notification.config({
        placement: res.data.notificationPlacement ? res.data.notificationPlacement : 'topRight'
      })
    }
    if (res.code !== 200) {
      $notification.warn({
        message: res.msg
      })
    }
    if (res.code === 999) {
      router.push('/prohibit-access')
    } else if (res.code === 222) {
      router.push('/install')
    } else {
      // 加载公告信息
      systemNotification().then((res) => {
        if (res.code === 200) {
          systemNotificationData.value = res.data || {}
        }
      })
    }
  })

  loadingLogo().then((res) => {
    logoUrl.value = res.data || ''
  })
}

// const headerNotificationSize = ref<SpaceSize>('small')

const notificationAfterClose = () => {
  systemNotificationData.value = { ...systemNotificationData.value, enabled: false }
}

const changeCollapsed = () => {
  collapsed.value = !collapsed.value

  _appStore.collapsed(collapsed.value)
}
</script>
<style lang="less" scoped>
#app-layout {
  min-height: 100vh;
}

// #app-layout .icon-btn {
//   float: left;
//   font-size: 18px;
//   line-height: 64px;
//   padding: 0 160px;
//   cursor: pointer;
//   transition: color 0.3s;
// }
// #app-layout .trigger:hover {
//   color: #1890ff;
// }
#app-layout .logo {
  flex-shrink: 0;
  width: 100%;
  cursor: pointer;
  height: 48px;
  margin: 20px 0 12px;
  font-size: 17px;

  font-weight: bold;
  overflow: hidden;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  white-space: nowrap;
}
#app-layout .logo img {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: 9px;
  vertical-align: middle;
  /* 小尺寸扁平徽章：自带高对比，无需描边/底色，仅保留轻微投影与文字拉开层级 */
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.28);
}

.app-header {
  display: flex;
  /* background: #fff; */
  padding: 10px 10px 0;
  height: auto;
  position: sticky;
  top: 0;
  z-index: 10;
  // 背景色由计算属性实现
  // background: #fff;
  // border-bottom: 1px solid #eee;
  // box-shadow: 0 0px 8px 0px rgba(0, 0, 0, 0.18);
}

.sider {
  border-inline-end: 1px solid rgba(5, 5, 5, 0.06);
  position: fixed;
  left: 0;
  top: 0;
  bottom: 0;
}
.sider-content {
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.mode-switch {
  flex-shrink: 0;
  padding: 4px 12px 8px;
}
.mode-switch-group {
  display: flex;
  width: 100%;
}
.mode-switch-group :deep(.n-radio-button) {
  flex: 1;
  text-align: center;
  padding-inline: 0;
}
.mode-switch-btn {
  width: 100%;
}
.sider-menu {
  flex: 1;
  overflow-y: auto;
}
.sider-version {
  flex-shrink: 0;
  padding: 8px 16px 12px;
  font-size: 12px;
  color: rgba(148, 163, 184, 0.7);
  text-align: center;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}
/* 深色主题下的菜单/内容区滚动条（避免渲染成默认亮色/绿色块） */
.sider-menu::-webkit-scrollbar,
.layout-content::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
.sider-menu::-webkit-scrollbar-thumb,
.layout-content::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.35);
  border-radius: 4px;
}
.sider-menu::-webkit-scrollbar-thumb:hover,
.layout-content::-webkit-scrollbar-thumb:hover {
  background: rgba(148, 163, 184, 0.5);
}
.sider-menu::-webkit-scrollbar-track,
.layout-content::-webkit-scrollbar-track {
  background: transparent;
}
.sider-menu,
.layout-content {
  scrollbar-width: thin;
  scrollbar-color: rgba(148, 163, 184, 0.35) transparent;
}
.layout-content {
  overflow-x: auto;
  padding: 10px;
  /* margin: 15px; */
}
/*
.sider-scroll {
  min-height: 100vh;
  overflow-y: auto;
}

.sider-full-screen {
  height: 100vh;
  overflow-y: scroll;
}

.layout-content-scroll {
  overflow-y: auto;
}

.layout-content-full-screen {
  height: calc(100vh - 120px);
  overflow-y: scroll;
} */
</style>
<style>
.header-row {
  display: flex;
  align-items: center;
  width: 100%;
}
.header-tabs {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}
.header-user {
  flex-shrink: 0;
  margin-left: 8px;
}

/* .layout-content { */
/* margin: 0; */
/* padding: 15px 15px 0; */
/* background: #fff; */
/* min-height: 280px; */
/* } */

/* .drawer-layout-content { */
/* min-height: calc(100vh - 85px); */
/* overflow-y: auto; */
/* } */
</style>
