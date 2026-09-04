<template>
  <div>
    <n-collapse v-model:value="activeKey">
      <n-collapse-item key="1">
        <template #header>
          {{ $t('i18n_0e44ae17ae') }}
          <n-tooltip>
            <template #trigger>
              <QuestionCircleOutlined />
            </template>

            <ul>
              <li>A{{ $t('i18n_8be868ba1b') }}.0.0.0-10.255.255.255</li>
              <li>B{{ $t('i18n_a66644ff47') }}.16.0.0-172.31.255.255</li>
              <li>C{{ $t('i18n_768e843a3e') }}.168.0.0-192.168.255.255</li>
            </ul>
          </n-tooltip>
        </template>
        <n-space direction="vertical" style="width: 100%">
          <template v-for="(item, index) in ipListArray" :key="index">
            <n-list size="small" bordered>
              <template #header>
                <div>
                  {{ item.name }} <n-tag>{{ item.displayName }}</n-tag>
                  <n-tag v-if="item.virtual">{{ $t('i18n_b60352bc4f') }}</n-tag>
                  <n-tag v-if="item.loopback">{{ $t('i18n_4393b5e25b') }}</n-tag>
                </div>
              </template>
              <n-list-item v-for="(ipItem, ipIndex) in item.ips" :key="ipIndex">
                {{ ipItem.ip }}
                <n-tag v-for="(labelItem, labelIdx) in ipItem.labels" :key="labelIdx">{{ labelItem }}</n-tag>
              </n-list-item>
            </n-list>
          </template>
        </n-space>
      </n-collapse-item>
      <n-collapse-item key="ping" :header="$t('i18n_bc4b0fd88a')">
        <n-form ref="form" :model="pingData" :rules="pingRules" @submit.prevent="onPingSubmit">
          <n-form-item :label="$t('i18n_02d9819dda')" path="">
            <n-alert :title="$t('i18n_1dc9514548')" banner />
          </n-form-item>
          <n-form-item label="HOST" path="host">
            <n-input
              v-model:value="pingData.host"
              :placeholder="$t('i18n_49d569f255')"
              @change="
                () => {
                  pingReulst = {}
                }
              "
            />
            <template #help>{{ $t('i18n_d373338541') }} </template>
          </n-form-item>
          <n-form-item :label="$t('i18n_84b28944b7')" path="timeout">
            <n-input-number
              v-model:value="pingData.timeout"
              :min="1"
              :placeholder="$t('i18n_6be30eaad7')"
              style="width: 100%"
            />
          </n-form-item>

          <n-form-item>
            <n-button type="primary" attr-type="submit" :loading="pingLoading">
              {{ $t('i18n_db06c78d1e') }}
            </n-button>
          </n-form-item>
          <template v-if="Object.keys(pingReulst).length">
            <n-form-item :label="$t('i18n_5ad7f5a8b2')" path="result">
              <n-tag v-if="pingReulst.ping" color="green">{{ $t('i18n_330363dfc5') }}</n-tag>
              <n-tag v-else color="red">{{ $t('i18n_acd5cb847a') }}</n-tag>
            </n-form-item>
            <n-form-item :label="$t('i18n_226b091218')" path="labels">
              <n-tag v-for="(item, index) in pingReulst.labels" :key="index">{{ item }}</n-tag>
            </n-form-item>
            <n-form-item v-if="pingReulst.originalIP" :label="$t('i18n_1b5266365f')" path="originalIP">
              {{ pingReulst.originalIP }}
            </n-form-item>
          </template>
        </n-form>
      </n-collapse-item>
      <n-collapse-item key="telnet" :header="$t('i18n_97d08b02e7')">
        <n-form ref="form" :model="telnetData" :rules="telnetRules" @submit.prevent="onTelnetSubmit">
          <n-form-item label="HOST" path="host">
            <n-input
              v-model:value="telnetData.host"
              :placeholder="$t('i18n_49d569f255')"
              @change="
                () => {
                  telnetReulst = {}
                }
              "
            />
            <template #help>{{ $t('i18n_d373338541') }} </template>
          </n-form-item>
          <n-form-item :label="$t('i18n_c76cfefe72')" path="port">
            <n-auto-complete
              v-model:value="telnetData.port"
              :options="UniversalPort"
              @change="
                () => {
                  telnetReulst = {}
                }
              "
            >
              <n-input-number
                v-model:value="telnetData.port"
                :min="0"
                :max="65535"
                :placeholder="$t('i18n_82416714a8')"
                :controls="false"
              />
              <template #option="item"> {{ item.title }} {{ item.value }} </template>
              <template #clearIcon>1</template>
            </n-auto-complete>
          </n-form-item>
          <n-form-item :label="$t('i18n_84b28944b7')" path="timeout">
            <n-input-number
              v-model:value="telnetData.timeout"
              :min="1"
              :placeholder="$t('i18n_6be30eaad7')"
              style="width: 100%"
            />
          </n-form-item>

          <n-form-item>
            <n-button type="primary" attr-type="submit" :loading="telnetLoading">
              {{ $t('i18n_db06c78d1e') }}
            </n-button>
          </n-form-item>
          <template v-if="Object.keys(telnetReulst).length">
            <n-form-item :label="$t('i18n_5ad7f5a8b2')" path="result">
              <n-tag v-if="telnetReulst.open" color="green">{{ $t('i18n_330363dfc5') }}</n-tag>
              <n-tag v-else color="red">{{ $t('i18n_acd5cb847a') }}</n-tag>
            </n-form-item>
            <n-form-item :label="$t('i18n_226b091218')" path="labels">
              <n-tag v-for="(item, index) in telnetReulst.labels" :key="index">{{ item }}</n-tag>
            </n-form-item>
            <n-form-item v-if="telnetReulst.originalIP" :label="$t('i18n_1b5266365f')" path="originalIP">
              {{ telnetReulst.originalIP }}
            </n-form-item>
          </template>
        </n-form>
      </n-collapse-item>
    </n-collapse>
  </div>
</template>
<script setup>
import { QuestionCircleOutlined } from '@ant-design/icons-vue'

import { ipList, netPing, netTelnet } from '@/api/tools'

import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()
const ipListArray = ref([])
const activeKey = ref(['ping', 'telnet'])

const UniversalPort = ref([
  {
    title: $t('i18n_6a922e0fb6'),
    value: '2123'
  },
  {
    title: $t('i18n_9af372557e'),
    value: '2122'
  },
  {
    title: `SSH${$t('i18n_4722bc0c56')}`,
    value: '22'
  },
  {
    title: 'Docker HTTP',
    value: '2375'
  },

  {
    title: 'HTTP',
    value: '80'
  },
  {
    title: 'HTTPS',
    value: '443'
  }
])

const pingData = ref({
  timeout: 1
})
const pingRules = ref({
  host: [
    {
      required: true,
      message: $t('i18n_49d569f255'),
      trigger: 'blur'
    }
  ]
})
const pingLoading = ref(false)
const pingReulst = ref({})

const onPingSubmit = () => {
  pingLoading.value = true
  pingReulst.value = {}
  netPing(pingData.value)
    .then((res) => {
      if (res.code !== 200) {
        $notification.warn({
          message: res.msg
        })
        return
      }
      pingReulst.value = res.data
    })
    .finally(() => {
      pingLoading.value = false
    })
}

//
const telnetData = ref({
  timeout: 1
})
const telnetRules = ref({
  host: [
    {
      required: true,
      message: $t('i18n_49d569f255'),
      trigger: 'blur'
    }
  ],

  port: [
    {
      required: true,
      message: $t('i18n_9302bc7838'),
      trigger: 'blur'
    }
  ]
})
const telnetLoading = ref(false)
const telnetReulst = ref({})

const onTelnetSubmit = () => {
  telnetLoading.value = true
  telnetReulst.value = {}
  netTelnet(telnetData.value)
    .then((res) => {
      if (res.code !== 200) {
        $notification.warn({
          message: res.msg
        })
        return
      }
      telnetReulst.value = res.data
    })
    .finally(() => {
      telnetLoading.value = false
    })
}

onMounted(() => {
  ipList().then((res) => {
    ipListArray.value = res.data || []
  })
})
</script>
