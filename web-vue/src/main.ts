import '@/assets/style.less'
import '@/assets/reset.less'
import App from './App.vue'
import router from './router'
import '@/router/auth'
import i18n from './i18n'
import { changeLang, defaultLocale } from './i18n'
import { $notification } from '@/d.ts/global/global'

changeLang(defaultLocale).then(() => {
  //console.log('defaultLocale done', new Date().getTime())
  const pinia = createPinia()

  const app = createApp(App)

  app.use(router)
  app.use(pinia)
  app.use(i18n)

  // 全局错误兜底：组件渲染/生命周期异常时给出提示而不是静默白屏
  let errorNotified = false
  app.config.errorHandler = (err, instance, info) => {
    console.error('[global-error]', err, info)
    try {
      if (!errorNotified) {
        errorNotified = true
        $notification.error({
          message: '页面出现异常',
          description: String((err as Error)?.message || err),
          duration: 5000,
          onClose: () => {
            errorNotified = false
          }
        })
      }
    } catch (e) {
      // 通知组件自身异常时忽略
    }
  }

  // 兜底捕获未处理的 Promise 拒绝与运行时异常，记录完整堆栈便于定位（不中断应用）
  window.addEventListener('unhandledrejection', (event) => {
    const reason = (event as PromiseRejectionEvent).reason
    console.error('[unhandledrejection]', reason)
    ;(window as any).__voyager1_lastError = {
      time: new Date().toISOString(),
      type: 'unhandledrejection',
      message: String((reason as Error)?.message || reason),
      stack: String((reason as Error)?.stack || '')
    }
  })
  window.addEventListener('error', (event) => {
    console.error('[window-error]', event.error || event.message)
    ;(window as any).__voyager1_lastError = {
      time: new Date().toISOString(),
      type: 'error',
      message: String(event.message || ''),
      stack: String((event.error as Error)?.stack || event.message || '')
    }
  })

  app.mount('#app')
})
//console.log('app done', new Date().getTime())
