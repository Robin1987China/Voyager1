<template>
  <defaultBg :show-footer="true">
    <template #content>
      <n-card class="login-card" hoverable>
        <div class="brand">
          <img class="brand-logo" :src="logoImg" alt="旅行者1号" />
          <div class="brand-name">Voyager1 持续交付平台</div>
          <div class="brand-slogan">Voyager1 · 持续交付</div>
        </div>
        <div class="login-title" style="text-align: center">{{ loginTitle }}</div>
        <div class="login-subtitle"></div>
        <br />
        <div>
          <n-form ref="loginFormRef" :model="loginForm" @submit.prevent="submitLogin">
            <n-form-item path="loginName" :rule="[{ required: true, message: $t('i18n_08b1fa1304') }]">
              <n-input v-model:value="loginForm.loginName" autocomplete="true" :placeholder="$t('i18n_819767ada1')" />
            </n-form-item>
            <n-form-item path="userPwd" :rule="[{ required: true, message: $t('i18n_e39ffe99e9') }]">
              <n-input
                v-model:value="loginForm.userPwd"
                type="password"
                autocomplete="true"
                :placeholder="$t('i18n_a810520460')"
              />
            </n-form-item>
            <n-form-item
              v-if="!disabledCaptcha"
              path="code"
              :rule="[{ required: true, message: $t('i18n_d0c06a0df1') }]"
            >
              <n-grid>
                <n-grid-item :span="14">
                  <n-input v-model:value="loginForm.code" :placeholder="$t('i18n_983f59c9d4')" />
                </n-grid-item>
                <n-grid-item :offset="2" :span="8">
                  <div class="rand-code">
                    <img v-if="randCode" :src="randCode" @click="changeCode" />
                    <loading-outlined v-else />
                  </div>
                </n-grid-item>
              </n-grid>
            </n-form-item>
            <n-form-item>
              <n-button type="primary" attr-type="submit" class="btn-login" :loading="loading">
                {{ $t('i18n_402d19e50f') }}
              </n-button>
            </n-form-item>
            <template v-if="enabledOauth2Provides.length">
              <n-divider>{{ $t('i18n_0f004c4cf7') }}</n-divider>
              <n-form-item>
                <n-space :size="20" wrap>
                  <template v-for="(item, index) in oauth2AllProvides">
                    <div v-if="enabledOauth2Provides.includes(item.key)" :key="index" class="oauth2-item">
                      <n-tooltip @click="toOauth2Url(item.key)">
                        <template #trigger>
                          <img :alt="item.name" :src="item.img" />
                        </template>
                        item.name
                      </n-tooltip>
                    </div>
                  </template>
                  <!-- <div v-if="enabledOauth2Provides.includes('maxkey')" class="oauth2-item">
                    <n-tooltip @click="toOauth2Url('maxkey')">
<template #trigger>

                      <img alt="maxkey" :src="maxkeyImg" />
                    
</template>
maxkey
</n-tooltip>
                  </div>
                  <div v-if="enabledOauth2Provides.includes('github')" class="oauth2-item">
                    <n-tooltip @click="toOauth2Url('github')">
<template #trigger>

                      <img alt="github" :src="githubImg" />
                    
</template>
github 账号登录
</n-tooltip>
                  </div>
                  <div v-if="enabledOauth2Provides.includes('dingtalk')" class="oauth2-item">
                    <n-tooltip @click="toOauth2Url('dingtalk')">
<template #trigger>

                      <img alt="dingtalk" :src="dingtalkImg" />
                    
</template>
钉钉账号登录
</n-tooltip>
                  </div>
                  <div v-if="enabledOauth2Provides.includes('feishu')" class="oauth2-item">
                    <n-tooltip @click="toOauth2Url('feishu')">
<template #trigger>

                      <img alt="dingtalk" :src="feishuImg" />
                    
</template>
飞书账号登录
</n-tooltip>
                  </div>
                  <div v-if="enabledOauth2Provides.includes('mygitlab')" class="oauth2-item">
                    <n-tooltip @click="toOauth2Url('mygitlab')">
<template #trigger>

                      <img alt="mygitlab" :src="gitlabImg" />
                    
</template>
自建 Gitlab 账号登录
</n-tooltip>
                  </div> -->
                </n-space>
              </n-form-item>
            </template>
          </n-form>
        </div>
      </n-card>
    </template>
  </defaultBg>
</template>
<script lang="ts" setup>
import { login, loginConfig, oauth2Url, oauth2Login, loginRandCode } from '@/api/user/user'
import { checkSystem } from '@/api/install'
import sha1 from 'js-sha1'
import defaultBg from '@/pages/layout/default-bg.vue'
import logoImg from '@/assets/images/voyager1.svg'
import maxkeyImg from '@/assets/images/maxkey.png'
import giteeImg from '@/assets/images/gitee.svg'
import dingtalkImg from '@/assets/images/dingtalk.svg'
import githubImg from '@/assets/images/github.svg'
import feishuImg from '@/assets/images/feishu.svg'
import gitlabImg from '@/assets/images/gitlab.svg'
import topiamImg from '@/assets/images/topiam.svg'
import qyWeixinImg from '@/assets/images/qyweixin.svg'
import { useGuideStore } from '@/stores/guide'

import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()

const oauth2AllProvides = ref([
  {
    name: $t('i18n_4ba304e77a'),
    key: 'dingtalk',
    img: dingtalkImg
  },
  {
    name: $t('i18n_5516b3130c'),
    key: 'feishu',
    img: feishuImg
  },
  {
    name: $t('i18n_af3a9b6303'),
    key: 'wechat_enterprise',
    img: qyWeixinImg
  },
  {
    name: `gitee ${$t('i18n_efae7764ac')}`,
    key: 'gitee',
    img: giteeImg
  },
  {
    name: `maxkey ${$t('i18n_b6e8fb4106')}`,
    key: 'maxkey',
    img: maxkeyImg
  },
  {
    name: `TOPIAM ${$t('i18n_b6e8fb4106')}`,
    key: 'topiam',
    img: topiamImg
  },
  {
    name: `github ${$t('i18n_efae7764ac')}`,
    key: 'github',
    img: githubImg
  },
  {
    name: $t('i18n_ab13dd3381'),
    key: 'mygitlab',
    img: gitlabImg
  }
])

interface IFormState {
  loginName: string
  userPwd: string
  code: string
}
const guideStore = useGuideStore()

const theme = computed(() => {
  return guideStore.getThemeView()
})

const router = useRouter()
const route = useRoute()

const loginTitle = ref($t('i18n_0de68f5626'))
const loginForm = reactive<IFormState>({
  loginName: '',
  userPwd: '',
  code: ''
})
const loginFormRef = ref()
const loading = ref(false)
const enabledOauth2Provides = ref<string[]>([])

const randCode = ref('')
// const dynamicBg = ref(localStorage.getItem('dynamicBg') === 'true')
const disabledCaptcha = ref(false)

// const backgroundImage = computed(() => {
//   const color =
//     theme.value === 'light' ? 'linear-gradient(#1890ff, #66a9c9)' : 'linear-gradient(rgb(38 46 55), rgb(27 33 36))'
//   // background: linear-gradient(#1890ff, #66a9c9);
//   return { background: color }
// })

// 检查是否需要初始化
const beginCheckSystem = () => {
  checkSystem().then((res) => {
    if (res.code !== 200) {
      $notification.warn({
        message: res.msg
      })
    }
    if (res.code === 999) {
      router.push('/prohibit-access')
    } else if (res.code === 222) {
      router.push('/install')
    }
    if (res.data?.loginTitle) {
      loginTitle.value = res.data.loginTitle
    }

    checkOauth2()
  })
}

const login_tip_key = 'login-tip'

const getLoginConfig = () => {
  loginConfig()
    .then((res) => {
      if (res.data && res.data.demo) {
        const demo = res.data.demo
        const p = h('p', { innerHTML: demo.msg }, [])
        $notification.info({
          message: $t('i18n_947d983961'),
          description: h('div', {}, [p]),
          key: login_tip_key,
          duration: null
        })
        loginForm.loginName = demo.user
      }
      disabledCaptcha.value = !!res.data?.disabledCaptcha
      enabledOauth2Provides.value = res.data?.oauth2Provides || []

      changeCode()
    })
    .catch(() => {
      // 配置接口异常时也要尝试加载验证码，避免登录页永久不可用
      changeCode()
    })
}
// change Code
const changeCode = () => {
  if (disabledCaptcha.value) {
    return
  }
  loginRandCode({ theme: theme.value, t: new Date().getTime() })
    .then((res) => {
      if (res.code === 200) {
        randCode.value = res.data
        loginForm.code = ''
      }
    })
    .catch(() => {
      randCode.value = ''
    })
}
const voyager1Window_ = voyager1Window()

const parseOauth2Provide = () => {
  if (voyager1Window_.oauth2Provide === '<oauth2Provide>') {
    const pathname = location.pathname.substring(1)
    const pathArray = pathname.split('-')
    return pathArray[pathArray.length - 1]
    // console.log(location.pathname.substring(1))
  }
  return voyager1Window_.oauth2Provide
}

const checkOauth2 = () => {
  if (route.query.code) {
    loading.value = true
    oauth2Login({
      code: route.query.code,
      state: route.query.state,
      provide: parseOauth2Provide()
    })
      .then((res) => {
        // 删除参数，避免刷新页面 code 已经被使用提示错误信息
        let query = Object.assign({}, route.query)
        ;(delete query.code, delete query.state)
        router.replace({
          query: query
        })
        // 登录不成功，更新验证码
        if (res.code !== 200) {
          changeCode()
        } else {
          startDispatchLogin(res)
        }
      })
      .finally(() => {
        loading.value = false
      })
  }
}
// 跳转到第三方系统
const toOauth2Url = (provide: string) => {
  oauth2Url({ provide: provide }).then((res) => {
    if (res.code === 200 && res.data) {
      $$message.loading({ content: $t('i18n_4c83203419'), key: 'oauth2', duration: 0 })
      location.href = res.data.toUrl
    }
  })
}
const startDispatchLogin = (res: any) => {
  $notification.success({
    message: res.msg
  })
  const existWorkspace = res.data.bindWorkspaceModels.find((item: any) => item.id === appStore().getWorkspaceId())
  if (existWorkspace) {
    // 缓存的还存在
    dispatchLogin(res.data)
  } else {
    // 之前的工作空间已经不存在,切换到当前列表的第一个
    // 还没有选择工作空间，默认选中第一个 用户加载菜单
    let firstWorkspace = res.data.bindWorkspaceModels[0]
    appStore()
      .changeWorkspace(firstWorkspace.id)
      .then(() => {
        dispatchLogin(res.data)
      })
  }
}
const useUserStore = userStore()

const dispatchLogin = (data: any) => {
  // 调用 store action 存储当前登录的用户名和 token
  useUserStore.login({ token: data.token, longTermToken: data.longTermToken }).then(() => {
    // 跳转主页面
    router.push({ path: '/' })
  })
}

const handleLogin = (values: IFormState) => {
  const params = {
    ...values,
    userPwd: sha1(loginForm.userPwd)
  }
  loading.value = true
  login(params)
    .then((res) => {
      // 登录不成功，更新验证码
      if (res.code !== 200) {
        changeCode()
      } else {
        startDispatchLogin(res)
      }
    })
    .finally(() => {
      loading.value = false
    })
}

// Naive 的 n-form 无 @finish 事件（Ant 写法迁移遗漏），需手动 validate
const submitLogin = () => {
  loginFormRef.value
    ?.validate()
    .then(() => {
      handleLogin(loginForm)
    })
    .catch(() => {
      // 校验失败：n-form-item 已展示具体错误提示
    })
}

const tip_has_login_key = `tipHasLoginInfo`

const checkHasLoginInfo = () => {
  if (useUserStore.userInfo && useUserStore.getToken()) {
    const p = h(
      'p',
      {
        innerHTML: `${$t('i18n_cfbb3341d5')}<b>${useUserStore.userInfo.name || ''}</b> ${$t('i18n_17006d4d51')}`
      },
      []
    )
    $notification.open({
      message: $t('i18n_697d60299e'),
      description: h('div', {}, [p]),
      btn: () =>
        h(
          {
            type: 'primary',
            size: 'small',
            onClick: () => {
              $notification.close(tip_has_login_key)
              router.push({ path: '/' })
            }
          },
          { default: () => $t('i18n_7653297de3') }
        ),
      key: tip_has_login_key,
      duration: null
    })
  } else {
    $notification.close(tip_has_login_key)
  }
}

const listener = () => {
  if (document.hidden || document.visibilityState === 'hidden') {
    //this.hidden()
  } else {
    checkHasLoginInfo()
  }
}

onMounted(() => {
  beginCheckSystem()

  getLoginConfig()
  checkHasLoginInfo()
  document.addEventListener('visibilitychange', listener)
  if (/^((?!chrome|android).)*safari/i.test(navigator.userAgent)) {
    window.addEventListener('pageshow', checkHasLoginInfo)
  }
})

onBeforeUnmount(() => {
  $notification.close(tip_has_login_key)
  $notification.close(login_tip_key)
  document.removeEventListener('visibilitychange', listener)
  window.removeEventListener('pageshow', checkHasLoginInfo)
})

// export default {
//   data() {
//     return {
//       loginForm: {
//         loginName: '',
//         userPwd: '',
//         code: '',
//       },
//       randCode: 'randCode.png',
//       dynamicBg: localStorage.getItem('dynamicBg') === 'true',
//       loginTitle: '登录VOYAGER1',
//       rules: {
//         loginName: [{ required: true, message: '请输入用户名' }],
//         userPwd: [{ required: true, message: '请输入密码' }],
//         code: [{ required: true, message: '请输入验证码' }],
//       },
//       disabledCaptcha: false,
//       enabledOauth2Provides: [],
//       maxkeyImg: require(`@/assets/images/maxkey.png`),
//       giteeImg: require(`@/assets/images/gitee.svg`),
//       githubImg: require(`@/assets/images/github.png`),
//     }
//   },
//   created() {
//     this.checkSystem()
//     //this.getBg();

//     this.changeCode()
//     this.getLoginConfig()
//   },
//   computed: {
//     ...mapGetters(['getWorkspaceId']),
//     backgroundImage: function () {
//       if (this.dynamicBg) {
//         return {
//           backgroundImage: `url(https://picsum.photos/${screen.width}/${screen.height}/?random)`,
//         }
//       }
//       return {}
//     },
//   },
//   methods: {
//     // Get background pic
//     // getBg() {},
//     //
//   },
// }
</script>
<style scoped>
.login-card {
  min-width: 380px;
  max-width: 400px;
  border-radius: 16px;
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%);
  background: rgba(13, 23, 42, 0.55);
  backdrop-filter: blur(24px) saturate(1.2);
  -webkit-backdrop-filter: blur(24px) saturate(1.2);
  border: 1px solid rgba(148, 163, 184, 0.18);
  box-shadow:
    0 24px 64px rgba(2, 8, 23, 0.55),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
  animation: cardRise 0.7s ease-out both;
}

@keyframes cardRise {
  from {
    opacity: 0;
    transform: translate(-50%, -44%);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%);
  }
}

.login-subtitle {
  width: 48px;
  height: 3px;
  border-radius: 2px;
  margin: 10px auto 0;
  background: linear-gradient(90deg, #d9a93f, #8a5e1b);
  opacity: 0.85;
}

/* ===== 品牌区（旅行者1号 · 深空探索风） ===== */
.brand {
  text-align: center;
  margin-bottom: 14px;
}
.brand-logo {
  width: 68px;
  height: 68px;
  filter: drop-shadow(0 6px 18px rgba(217, 169, 63, 0.35));
  animation: logoFloat 6s ease-in-out infinite;
}
@keyframes logoFloat {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-5px);
  }
}
.brand-name {
  margin-top: 8px;
  font-size: 30px;
  font-weight: 600;
  letter-spacing: 8px;
  text-indent: 8px;
  background: linear-gradient(135deg, #f4e2ae 0%, #d9a93f 55%, #b0802a 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.brand-slogan {
  margin-top: 6px;
  font-size: 13px;
  letter-spacing: 3px;
  color: rgba(148, 163, 184, 0.85);
}

/* 深色玻璃卡片上的标题文字（darkTheme 不作用于普通 div） */
.login-title {
  color: #e5e9f0;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 1px;
}

.rand-code {
  width: 100%;
  height: 32px;
}

.rand-code img {
  width: 100%;
  height: 100%;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  display: inherit;
}

.btn-login {
  width: 100%;
  margin: 10px 0;
  background: linear-gradient(135deg, #e8c97e, #c99b3f);
  border: none;
  color: #231603;
  font-weight: 600;
  box-shadow: 0 8px 24px rgba(217, 169, 63, 0.35);
  letter-spacing: 4px;
}
.btn-login:hover,
.btn-login:focus {
  background: linear-gradient(135deg, #f4e2ae, #d9a93f);
  color: #1c1006;
  box-shadow: 0 10px 28px rgba(217, 169, 63, 0.5);
}

/* 深色玻璃卡片内的 naive 组件适配 */
:deep(.n-card__content) {
  color: #e2e8f0;
  padding: 30px;
}
:deep(.n-input) {
  background: rgba(15, 27, 48, 0.6);
  border-radius: 8px;
}
:deep(.n-input .n-input__input-el),
:deep(.n-input .n-input__password-el) {
  color: #f1f5f9;
}
:deep(.n-input .n-input__input-el::placeholder),
:deep(.n-input .n-input__password-el::placeholder) {
  color: rgba(148, 163, 184, 0.6);
}
:deep(.n-divider .n-divider__title) {
  color: rgba(148, 163, 184, 0.8);
}
:deep(.n-divider:not(.n-divider--vertical)) {
  border-color: rgba(148, 163, 184, 0.2) !important;
}

.oauth2-item {
  width: 40px;
  height: 40px;
}

.oauth2-item img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
</style>
