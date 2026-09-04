<template>
  <div class="">
    <n-form ref="editForm" :model="temp" :rules="rules">
      <n-form-item :label="$t('i18n_1f130d11d1')" path="host">
        <n-auto-complete v-model:value="temp.host" :options="hostDataSource" :placeholder="$t('i18n_c6209653e4')">
          <template #option="item"> {{ item.title }} {{ item.value }} </template>
        </n-auto-complete>
      </n-form-item>
      <n-form-item :label="$t('i18n_fdcadf68a5')" path="port">
        <n-auto-complete v-model:value="temp.port" :placeholder="$t('i18n_e074f6b6af')" :options="portDataSource">
          <template #option="item"> {{ item.title }} {{ item.value }} </template>
        </n-auto-complete>
      </n-form-item>
      <n-form-item :label="$t('i18n_819767ada1')" path="user">
        <n-input v-model:value="temp.user" type="text" :placeholder="$t('i18n_b85b213579')" />
      </n-form-item>
      <n-form-item :label="$t('i18n_a810520460')" :path="`${temp.type === 'add' ? 'pass' : 'pass-update'}`">
        <n-input v-model:value="temp.pass" type="password" :placeholder="$t('i18n_fc5fb962da')" />
      </n-form-item>
      <n-form-item :label="$t('i18n_5893fa2280')" path="from">
        <!-- <n-input v-model="temp.from" type="text" placeholder="发送方邮箱账号" /> -->
        <n-tooltip>
          <template #trigger>
            <n-auto-complete
              v-model:value="temp.from"
              :options="fromResult"
              :placeholder="$t('i18n_e9bd4484a7')"
              @keyup.enter="handleFromSearch"
            >
              <template #option="{ value: val }">
                {{ val.split('@')[0] }} @
                <span style="font-weight: bold">{{ val.split('@')[1] }}</span>
              </template>
            </n-auto-complete>
          </template>
          {{ $t('i18n_6b4fd0ca47') }}
          <ul>
            <li>1. user@xxx.xx</li>
            <li>2. name &lt;user@xxx.xx&gt;</li>
          </ul>
        </n-tooltip>
      </n-form-item>
      <n-form-item :label="$t('i18n_e95f9f6b6e')" path="sslEnable">
        <n-switch
          v-model:value="temp.sslEnable"
          :checked-label="$t('i18n_7854b52a88')"
          :unchecked-label="$t('i18n_5c56a88945')"
        />
        <!-- <n-input v-show="temp.sslEnable" v-model="temp.socketFactoryPort" type="text" placeholder="SSL 端口" /> -->
      </n-form-item>
      <n-form-item :label="$t('i18n_56071a4fa6')" path="timeout">
        <n-input-number
          v-model:value="temp.timeout"
          style="width: 100%"
          :min="3"
          type="text"
          :placeholder="$t('i18n_50f472ee4e')"
        />
      </n-form-item>

      <n-form-item>
        <n-button type="primary" class="btn" :disabled="submitAble" @click="onSubmit">{{
          $t('i18n_939d5345ad')
        }}</n-button>
      </n-form-item>
    </n-form>
    <n-alert :title="$t('i18n_84597bf5bc')" :description="$t('i18n_fdbac93380')" type="info" show-icon />
    <br />
    <n-alert :title="$t('i18n_8a49e2de39')" type="info" :description="$t('i18n_127de26370')" show-icon />
    <br />
    <n-alert :title="$t('i18n_8be76af198')" :description="$t('i18n_61c0f5345d')" type="info" show-icon />
    <br />
    <n-alert :title="$t('i18n_0a54bd6883')" :description="$t('i18n_197be96301')" type="info" show-icon />
  </div>
</template>
<script>
import { getMailConfigData, editMailConfig } from '@/api/system'

export default {
  data() {
    return {
      temp: {},
      submitAble: false,
      rules: {
        host: [
          {
            required: true,
            message: this.$t('i18n_4b0cb10d18'),
            trigger: 'blur'
          }
        ],

        pass: [{ required: true, message: this.$t('i18n_e39ffe99e9'), trigger: 'blur' }],
        user: [
          {
            required: true,
            message: this.$t('i18n_08b1fa1304'),
            trigger: 'blur'
          }
        ],

        from: [
          {
            required: true,
            message: this.$t('i18n_b39909964f'),
            trigger: 'blur'
          }
        ]
      },
      fromResult: [],
      hostDataSource: [
        {
          title: this.$t('i18n_12dc402a82'),
          options: [
            {
              title: this.$t('i18n_9b7419bc10'),
              value: 'smtp.qq.com'
            },
            {
              title: this.$t('i18n_578ca5bcfd'),
              value: 'smtp.163.com'
            },
            {
              title: this.$t('i18n_3a536dcd7c'),
              value: 'smtp.126.com'
            },
            {
              title: this.$t('i18n_fcb7a47b70'),
              value: 'smtp.mxhichina.com'
            },
            {
              title: this.$t('i18n_edb4275dcd'),
              value: 'smtp.gmail.com'
            }
          ]
        }
      ],

      portDataSource: [
        {
          title: this.$t('i18n_9b7419bc10'),
          options: [
            {
              title: this.$t('i18n_9b7419bc10'),
              value: '587'
            },
            {
              title: this.$t('i18n_e6e5f26c69'),
              value: '465'
            }
          ]
        },
        {
          title: this.$t('i18n_578ca5bcfd'),
          options: [
            {
              title: this.$t('i18n_578ca5bcfd'),
              value: '25'
            },
            {
              title: this.$t('i18n_6b6d6937d7'),
              value: '465'
            }
          ]
        },
        {
          title: this.$t('i18n_fcb7a47b70'),
          options: [
            {
              title: this.$t('i18n_a7ddb00197'),
              value: '465'
            }
          ]
        },
        {
          title: this.$t('i18n_624f639f16'),
          options: [
            {
              title: this.$t('i18n_c96b442dfb'),
              value: '465'
            }
          ]
        }
      ]
    }
  },
  mounted() {
    this.loadData()
  },
  methods: {
    // load data
    loadData() {
      getMailConfigData().then((res) => {
        if (res.code === 200) {
          this.temp = res.data || { type: 'add' }
          if (this.temp.port) {
            this.temp.port = this.temp.port + ''
          }
        }
      })
    },
    // submit
    onSubmit() {
      // disabled submit button
      this.submitAble = true
      // if (this.temp.sslsslEnable === false) {
      //   this.temp.socketFactoryPort = "";
      // }
      editMailConfig(this.temp).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
        }
        // button recover
        this.submitAble = false
      })
    },
    handleFromSearch(val) {
      // let result
      // if (!value || value.indexOf('@') >= 0) {
      //   result = []
      // } else {
      //   result = ['gmail.com', '163.com', 'qq.com'].map((domain) => {
      //     value: `${value}@${domain}`
      //   })
      // }
      // console.log(result)

      let res = []
      if (!val || val.indexOf('@') >= 0) {
        res = []
      } else {
        res = ['gmail.com', '163.com', 'qq.com'].map((domain) => ({ value: `${val}@${domain}` }))
      }

      this.fromResult = res
    }
  }
}
</script>
<style scoped>
.btn {
  margin-left: 20px;
}
</style>
