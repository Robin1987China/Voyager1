<template>
  <defaultBg>
    <template #content>
      <n-card v-if="canInstall" style="width: 550px" hoverable>
        <template #title>
          {{ $t('i18n_c2f11fde3a') }}
          <span>{{ $t('i18n_620efec150') }}</span>
        </template>
        <n-grid type="flex" justify="center">
          <n-grid-item :span="16">
            <div>
              <h3>{{ $t('i18n_08ab230290') }}</h3>
              <ol>
                <li>{{ $t('i18n_2953a9bb97') }}</li>
                <li style="color: red">
                  {{ $t('i18n_e09d0d8c41') }}
                  <b>admin{{ $t('i18n_cb93a1f4a5') }}</b
                  >{{ $t('i18n_2b788a077e') }}
                </li>
              </ol>
            </div>
            <br />
            <n-form
              ref="loginFormRef"
              :model="loginForm"
              name="login"
              class="init-form"
              @submit.prevent="submitInstall"
            >
              <n-form-item
                class="init-user-name"
                path="userName"
                :rule="[{ required: true, message: $t('i18n_ea7fbabfa1') }]"
              >
                <n-input v-model:value="loginForm.userName" :placeholder="$t('i18n_fec6151b49')" />
              </n-form-item>
              <n-form-item
                class="init-user-password"
                path="userPwd"
                :rule="[
                  { required: true, message: $t('i18n_e39ffe99e9') },
                  {
                    pattern: /^(?![\d]+$)(?![a-zA-Z]+$)(?![^\da-zA-Z]+$).{6,18}$/,
                    message: $t('i18n_974be6600d')
                  }
                ]"
              >
                <n-input v-model:value="loginForm.userPwd" type="password" :placeholder="$t('i18n_efafd0cbd4')" />
              </n-form-item>
              <n-form-item>
                <n-button type="primary" attr-type="submit" block :loading="loading">
                  {{ $t('i18n_94d4fcca1b') }}
                </n-button>
              </n-form-item>
            </n-form>
          </n-grid-item>
        </n-grid>
      </n-card>
      <div v-else>
        <n-result status="warning" :title="$t('i18n_65cf4248a8')" :sub-title="$t('i18n_70a6bc1e94')">
          <template #extra>
            <n-button type="primary" @click="goHome"> {{ $t('i18n_0bbc7458b4') }} </n-button>
          </template>
        </n-result>
      </div>
    </template>
  </defaultBg>
</template>
<script lang="ts" setup>
import sha1 from 'js-sha1'
import { checkSystem } from '@/api/install'
import { initInstall } from '@/api/install'
// import { onMounted, reactive, ref } from 'vue'

import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import defaultBg from '@/pages/layout/default-bg.vue'

import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()
const router = useRouter()

const loginForm = reactive({
  userName: '',
  userPwd: ''
})
const loginFormRef = ref()
const canInstall = ref(true)

const loading = ref(false)

// Naive 的 n-form 无 @finish 事件（Ant 写法迁移遗漏），需手动 validate
const submitInstall = () => {
  loginFormRef.value
    ?.validate()
    .then(() => {
      handleLogin(loginForm)
    })
    .catch(() => {
      // 校验失败：n-form-item 已展示具体错误提示
    })
}

// install
const handleLogin = (values: any) => {
  const params = {
    ...values,
    userPwd: sha1(values.userPwd)
  }
  loading.value = true
  initInstall(params)
    .then((res) => {
      const userStore = useUserStore()
      const appStore = useAppStore()
      // 登录不成功，更新验证码
      if (res.code === 200) {
        $notification.success({
          message: res.msg
        })
        const tokenData = res.data.tokenData
        userStore.login({ token: tokenData.token, longTermToken: tokenData.longTermToken })

        const firstWorkspace = tokenData.bindWorkspaceModels[0]
        appStore.changeWorkspace(firstWorkspace.id)
        router.push({ path: '/' })
      }
    })
    .finally(() => {
      loading.value = false
    })
}

const goHome = () => {
  router.replace({ path: '/' })
}

onMounted(() => {
  checkSystem().then((res) => {
    if (res.code === 222) {
      canInstall.value = true
    } else {
      canInstall.value = false
    }
  })
})
</script>

<style scoped>
/* 深色玻璃卡片上的自定义文字（darkTheme 不作用于普通 HTML 元素） */
:deep(h3),
:deep(ol),
:deep(li) {
  color: #cdd5e1;
}
:deep(li b) {
  color: #fbbf24;
}
</style>
