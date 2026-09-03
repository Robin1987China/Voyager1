import { GlobalWindow } from '@/interface/common'
import { createDiscreteApi, darkTheme, lightTheme } from 'naive-ui'
import { computed, ref } from 'vue'
import { useAppStore } from '@/stores/app'
import { useUserStore } from '@/stores/user'
import { useGuideStore } from '@/stores/guide'

// 离散弹层（$message/$notification/$confirm）跟随主主题（深/浅色）切换。
// 注意：本模块加载早于 pinia 安装，不能在此直接访问 store；
// 由 App.vue 在 pinia 就绪后通过 syncDiscreteTheme 同步主题。
const discreteTheme = ref<'light' | 'dark'>('light')
export const syncDiscreteTheme = (theme: 'light' | 'dark') => {
  discreteTheme.value = theme
}
const discreteConfigProviderProps = computed(() => ({
  theme: discreteTheme.value === 'dark' ? darkTheme : lightTheme
}))

const { message, notification, dialog } = createDiscreteApi(['message', 'notification', 'dialog'], {
  configProviderProps: discreteConfigProviderProps
})
export const voyager1Window = () => {
  return window as unknown as GlobalWindow
}

export const $message = message

// Ant notification 用 message/description，Naive 用 title/content；Ant btn -> Naive action
const adaptNotification = (config: any) => {
  if (!config) return config
  const { message: msg, description, btn, ...rest } = config
  return { title: msg, content: description, action: btn, ...rest }
}

// Ant 的 $notification.close(key) 按 key 关闭，Naive 无此 API：记录实例后按 key destroy
const notificationMap = new Map<string, any>()
const trackNotification = (key: string | undefined, instance: any) => {
  if (key) {
    notificationMap.set(key, instance)
  }
  return instance
}

export const $notification = {
  success: (config: any) => trackNotification(config?.key, notification.success(adaptNotification(config))),
  error: (config: any) => trackNotification(config?.key, notification.error(adaptNotification(config))),
  info: (config: any) => trackNotification(config?.key, notification.info(adaptNotification(config))),
  warning: (config: any) => trackNotification(config?.key, notification.warning(adaptNotification(config))),
  // Ant 别名
  warn: (config: any) => trackNotification(config?.key, notification.warning(adaptNotification(config))),
  open: (config: any) => trackNotification(config?.key, notification.open(adaptNotification(config))),
  close: (key?: string) => {
    if (!key) {
      return
    }
    const instance = notificationMap.get(key)
    if (instance && typeof instance.destroy === 'function') {
      instance.destroy()
      notificationMap.delete(key)
    }
  },
  // Ant notification.config 设置默认项；Naive 无等价 API，忽略即可（placement 由 n-notification-provider 控制）
  config: () => {}
}

// Ant Modal.confirm 字段 -> Naive dialog 字段
const adaptDialog = (props: any) => {
  const { onOk, onCancel, okText, cancelText, okButtonProps, cancelButtonProps, zIndex, width, ...rest } = props || {}
  return {
    ...rest,
    positiveText: okText,
    negativeText: cancelText,
    positiveButtonProps: okButtonProps,
    negativeButtonProps: cancelButtonProps,
    onPositiveClick: onOk,
    onNegativeClick: onCancel
  }
}

export const $confirm = (props: any) => dialog.warning(adaptDialog(props))
export const $info = (props: any) => dialog.info(adaptDialog(props))
export const $error = (props: any) => dialog.error(adaptDialog(props))
export const $warning = (props: any) => dialog.warning(adaptDialog(props))
export const $success = (props: any) => dialog.success(adaptDialog(props))

export const appStore = () => {
  return useAppStore()
}

export const userStore = () => {
  return useUserStore()
}

export const guideStore = () => {
  return useGuideStore()
}

export const router = () => {
  return useRouter()
}

export const route = () => {
  return useRoute()
}

// 历史遗留的 $$ 前缀别名（unplugin-auto-import 会扫描本文件导出并全局注入）
export const $$message = message
export const $$notification = $notification
export const $$confirm = $confirm
export const $$info = $info
export const $$error = $error
export const $$warning = $warning
export const $$success = $success
