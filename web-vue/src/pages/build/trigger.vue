<template>
  <div>
    <div v-if="triggerVisible">
      <n-form ref="editTriggerForm" :model="temp">
        <n-tabs default-active-key="1" type="card">
          <template #rightExtra>
            <n-tooltip>
              <template #trigger>
                <span class="tw">
                  <n-button type="primary" size="small" @click="resetTrigger">{{ $t('i18n_4b9c3271dc') }}</n-button>
                </span>
              </template>
              $t('i18n_01ad26f4a9')
            </n-tooltip>
          </template>
          <n-tab-pane name="1" :tab="$t('i18n_3d3b918f49')">
            <n-space direction="vertical" style="width: 100%">
              <n-alert :title="$t('i18n_947d983961')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('i18n_314f5aca4e') }}</li>
                    <li>
                      {{ $t('i18n_e319a2a526') }}
                    </li>
                    <li>{{ $t('i18n_3a3c5e739b') }} BODY json： [ { "id":"1", "token":"a", "delay":"0" } ]</li>
                    <li>
                      {{ $t('i18n_3baa9f3d72') }}
                    </li>
                    <li>
                      {{ $t('i18n_dcc846e420') }} BODY json： [ { "id":"1", "token":"a",
                      "delay":"0","branchName":"test","branchTagName":"1.*","script":"mvn clean
                      package","resultDirFile":"/target/","webhook":"http://test.com/webhook" } ]
                    </li>
                    <li>
                      {{ $t('i18n_2adbfb41e9') }} useQueue=true
                      {{ $t('i18n_0c1e9a72b7') }}
                    </li>
                    <li>{{ $t('i18n_68a1faf6e2') }}</li>
                  </ul>
                </template>
              </n-alert>
              <n-alert type="info" :title="`${$t('i18n_de78b73dab')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <n-p style="margin-bottom: 0">
                    <n-tag>GET</n-tag> <span>{{ temp.triggerBuildUrl }} </span>
                    <copy-text :text="temp.triggerBuildUrl" />
                  </n-p>
                </template>
              </n-alert>
              <n-alert type="info" :title="`${$t('i18n_8d202b890c')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <n-p style="margin-bottom: 0">
                    <n-tag>POST</n-tag>
                    <span>{{ temp.batchTriggerBuildUrl }} </span>
                    <copy-text :text="temp.batchTriggerBuildUrl" />
                  </n-p>
                </template>
              </n-alert>
            </n-space>
          </n-tab-pane>
          <n-tab-pane name="2" :tab="$t('i18n_251a89efa9')">
            <n-space direction="vertical" style="width: 100%">
              <n-alert :title="$t('i18n_947d983961')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('i18n_3c48d9b970') }}</li>
                    <li>{{ $t('i18n_c34f1dc2b9') }}</li>
                    <li>
                      <n-tag>No(0, "{{ $t('i18n_d30b8b0e43') }}")</n-tag>,
                      <n-tag>Ing(1, "{{ $t('i18n_32493aeef9') }}")</n-tag>,
                      <n-tag>Success(2, "{{ $t('i18n_7f3809d36b') }}")</n-tag>,
                      <n-tag>Error(3, "{{ $t('i18n_41298f56a3') }}")</n-tag>,
                      <n-tag>PubIng(4, "{{ $t('i18n_0baa0e3fc4') }}")</n-tag>,
                      <n-tag>PubSuccess(5, "{{ $t('i18n_2fff079bc7') }}")</n-tag>,
                      <n-tag>PubError(6, "{{ $t('i18n_250688d7c9') }}")</n-tag>,
                      <n-tag>Cancel(7, "{{ $t('i18n_b4fc1ac02c') }}")</n-tag>,
                    </li>
                  </ul>
                </template>
              </n-alert>
              <n-alert type="info" :title="`${$t('i18n_dcd72e6014')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <n-p style="margin-bottom: 0">
                    <n-tag>GET</n-tag>
                    <span>{{ temp.batchBuildStatusUrl2 }} </span>
                    <copy-text :text="temp.batchBuildStatusUrl2" />
                  </n-p>
                </template>
              </n-alert>
              <n-alert type="info" :title="`${$t('i18n_cac6ff1d82')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <n-p style="margin-bottom: 0">
                    <n-tag>POST</n-tag>
                    <span>{{ temp.batchBuildStatusUrl }} </span>
                    <copy-text :text="temp.batchBuildStatusUrl" />
                  </n-p>
                </template>
              </n-alert>
            </n-space>
          </n-tab-pane>
          <n-tab-pane name="3" :tab="$t('i18n_d3ded43cee')">
            <n-space direction="vertical" style="width: 100%">
              <n-alert :title="$t('i18n_947d983961')" type="warning" show-icon>
                <template #description>
                  <ul>
                    <li>{{ $t('i18n_178ad7e9bc') }}</li>
                    <li>{{ $t('i18n_0215b91d97') }}</li>
                  </ul>
                </template>
              </n-alert>
              <n-alert type="info" :title="`${$t('i18n_2f5e885bc6')}(${$t('i18n_00a070c696')})`">
                <template #description>
                  <n-p style="margin-bottom: 0">
                    <n-tag>GET</n-tag> <span>{{ temp.buildLogUrl }} </span>
                    <copy-text :text="temp.buildLogUrl" />
                  </n-p>
                </template>
              </n-alert>
            </n-space>
          </n-tab-pane>
        </n-tabs>
      </n-form>
    </div>
    <template v-else>
      <n-result :title="$t('i18n_475a349f32')">
        <template #extra>
          <n-button key="console" type="primary" @click="handleTrigger">
            {{ $t('i18n_a1a3a7d853') }}
          </n-button>
        </template>
      </n-result>
    </template>
  </div>
</template>
<script>
import { getTriggerUrl } from '@/api/build-info'
export default {
  props: {
    id: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      temp: {},

      triggerVisible: false
    }
  },
  created() {
    if (this.id) {
      this.handleTrigger()
    }
  },
  methods: {
    // 触发器
    handleTrigger() {
      this.temp = {}

      getTriggerUrl({
        id: this.id
      }).then((res) => {
        if (res.code === 200) {
          this.fillTriggerResult(res)
          this.triggerVisible = true
        }
      })
    },
    // 重置触发器
    resetTrigger() {
      getTriggerUrl({
        id: this.id,
        rest: 'rest'
      }).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
          this.fillTriggerResult(res)
        }
      })
    },
    fillTriggerResult(res) {
      this.temp.triggerBuildUrl = `${location.protocol}//${location.host}${res.data.triggerBuildUrl}`
      this.temp.batchTriggerBuildUrl = `${location.protocol}//${location.host}${res.data.batchTriggerBuildUrl}`
      this.temp.batchBuildStatusUrl = `${location.protocol}//${location.host}${res.data.batchBuildStatusUrl}`
      this.temp.buildLogUrl = `${location.protocol}//${location.host}${res.data.buildLogUrl}`
      // this.temp.id = res.data.id;
      // this.temp.token = res.data.token;
      this.temp.batchBuildStatusUrl2 = `${this.temp.batchBuildStatusUrl}?id=${res.data.id}&token=${res.data.token}`
      this.temp.buildLogUrl = `${this.temp.buildLogUrl}?id=${res.data.id}&token=${res.data.token}&buildNumId=0`
      this.temp = { ...this.temp }
    }
  }
}
</script>
