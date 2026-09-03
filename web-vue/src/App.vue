<template>
  <n-config-provider :theme="naiveTheme">
    <n-message-provider>
      <n-dialog-provider>
        <n-notification-provider>
          <n-spin
            :show="globalLoadingProps.show"
            :description="globalLoadingProps.description"
            :size="globalLoadingProps.size"
            :delay="globalLoadingProps.delay"
            content-class="globalLoading"
          >
            <router-view v-if="routerActivation" />
            <div v-if="pageloading" class="pageLoading"></div>
            <n-back-top />
          </n-spin>
        </n-notification-provider>
      </n-dialog-provider>
    </n-message-provider>
  </n-config-provider>
</template>
<script lang="ts" setup>
import {
  NConfigProvider,
  NMessageProvider,
  NDialogProvider,
  NNotificationProvider,
  darkTheme,
  lightTheme
} from 'naive-ui'
import { onMounted, onUnmounted } from 'vue'
import { changeLang } from './i18n'
import { useI18n } from 'vue-i18n'
import { syncDiscreteTheme } from '@/d.ts/global/global'
const routerActivation = ref(true)
const useGuideStore = guideStore()
const i18nHook = useI18n()
const t = i18nHook.t

// 主题（light/dark/auto），绑定到 Naive n-config-provider
const naiveTheme = computed(() => {
  const theme = useGuideStore.getThemeView()
  return theme === 'dark' ? darkTheme : lightTheme
})

// 同步主题到离散弹层（$message/$notification/$confirm）
watch(
  naiveTheme,
  (t) => {
    syncDiscreteTheme(t === darkTheme ? 'dark' : 'light')
  },
  { immediate: true }
)

const nowLang = computed(() => {
  return useGuideStore.getLocale()
})

// 监听系统主题模式
const onMatchMediaChange = (e: MediaQueryListEvent) => {
  useGuideStore.setSystemIsDark(e.matches)
}
const changeI18n = (lang: string) => {
  changeLang(lang)
}
onMounted(() => {
  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', onMatchMediaChange)
  changeI18n(nowLang.value)
})

onUnmounted(() => {
  window.matchMedia('(prefers-color-scheme: dark)').removeEventListener('change', onMatchMediaChange)
})

const pageloading = ref(true)
const pageLoadingTimeout = ref()

const useAppStore = appStore()

const pageLoadingStore = computed(() => {
  return useAppStore.loading
})
watch(pageLoadingStore, (newValue) => {
  //
  if (newValue === 2) {
    clearTimeout(pageLoadingTimeout.value)
    globalLoading(false)
    pageloading.value = false
  } else {
    pageLoadingTimeout.value = setTimeout(() => {
      pageloading.value = true
      globalLoading({
        spinning: true,
        tip: t('i18n_6ad02e7a1b')
      })
    }, 500)
  }
})
// 打包后无效
// useAppStore.$subscribe((mutation, state) => {
//   const events: any = mutation.events
//   if (events && events.key === 'loading') {
//     if (events.newValue === 2) {
//       clearTimeout(pageLoadingTimeout.value)
//       globalLoading(false)
//       pageloading.value = false
//     } else {
//       pageLoadingTimeout.value = setTimeout(() => {
//         pageloading.value = true
//         globalLoading({
//           spinning: true,
//           tip: '页面资源加载中....'
//         })
//       }, 500)
//     }
//   }
// })

const reload = () => {
  routerActivation.value = false
  nextTick(() => {
    // const menuStore = useMenuStore()
    // 刷新菜单
    // menuStore.restLoadSystemMenus()
    routerActivation.value = true
  })
}

const globalLoadingProps = ref({
  show: false,
  description: t('i18n_26b5bd4947'),
  size: 'large' as const,
  delay: 500
})

/**
 * 全局 loading（兼容 Ant 的 spinning/tip 参数）
 * @param props 参数
 */
const globalLoading = (props: boolean | string | Record<string, any>) => {
  let newProps: any = {}
  if (typeof props === 'boolean') {
    newProps = { show: props }
  } else if (typeof props === 'string') {
    newProps = { description: props }
  } else if (Object.prototype.toString.call(props) === '[object Object]') {
    // 适配 Ant 的 spinning/tip 字段
    if ('spinning' in props) newProps.show = props.spinning
    if ('tip' in props) newProps.description = props.tip
    if ('size' in props) newProps.size = props.size
    if ('delay' in props) newProps.delay = props.delay
  } else {
    console.error('不支持的类型', props, Object.prototype.toString.call(props))
  }
  globalLoadingProps.value = { ...globalLoadingProps.value, ...newProps }
}

provide('reload', reload)
provide('globalLoading', globalLoading)
</script>
<style lang="less">
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  // color: #2c3e50;
  margin: 0;
  padding: 0;
}

.globalLoading {
  z-index: 99999;
  // background-color: #1f1f1f;
  // background-color: rgba(0, 0, 0, 0.7);
  background-color: rgba(140, 140, 140, 0.2);
  opacity: 0.8;
  height: 100vh;
}
</style>
<style scoped>
.pageLoading {
  position: absolute;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  min-height: 100vh;
  height: 100%;
  flex: 1;
}
</style>
