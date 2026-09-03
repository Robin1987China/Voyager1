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

  app.mount('#app')
})
//console.log('app done', new Date().getTime())
