<template>
  <div>
    <n-timeline>
      <n-timeline-item>
        <span>
          {{ $t('i18n_0c5c8d2d11') }}{{ temp.name }} - {{ temp.osType }} - {{ temp.operatingSystem }} -
          <n-tag>{{ temp.architecture }} </n-tag>
          <n-tag>{{ temp.id }}</n-tag>
        </span>
      </n-timeline-item>
      <n-timeline-item>
        <span>
          {{ $t('i18n_2684c4634d') }}<n-tag>{{ temp.serverVersion }}</n-tag>
          <n-tag>{{ temp.kernelVersion }}</n-tag>
        </span>
      </n-timeline-item>
      <n-timeline-item>
        <span
          >{{ $t('i18n_3d06693eb5') }} <n-tag>cpu:{{ temp.nCPU || temp.NCPU }}</n-tag>
          <n-tag>{{ $t('i18n_af708b659f') }}{{ renderSize(temp.memTotal) }}</n-tag>

          <n-tag>{{ $t('i18n_6b189bf02d') }}{{ temp.containers }}</n-tag>
          <n-tag>{{ $t('i18n_897d865225') }}{{ temp.images }}</n-tag>
        </span>
      </n-timeline-item>
      <n-timeline-item>
        <span>{{ $t('i18n_089a88ecee') }}{{ temp.systemTime }} </span>
      </n-timeline-item>
      <n-timeline-item>
        <span>{{ $t('i18n_b6728e74a4') }}{{ temp.dockerRootDir }} </span>
      </n-timeline-item>
      <template v-if="temp.swarm">
        <n-timeline-item>
          <div>
            {{ $t('i18n_e414392917') }}
            <div style="padding-left: 10px">
              <n-space direction="vertical" style="width: 100%">
                <div>
                  {{ $t('i18n_1862c48f72') }}<n-tag v-if="temp.swarm.nodeAddr">{{ temp.swarm.nodeAddr }}</n-tag>
                  <n-tag>{{ temp.swarm.localNodeState }}</n-tag>
                </div>
                <div v-if="temp.swarm.remoteManagers">
                  {{ $t('i18n_2f6989595f') }}
                  <n-tag v-for="(item, index) in temp.swarm.remoteManagers" :key="index">{{ item.addr }}</n-tag>
                </div>
                <div>
                  {{ $t('i18n_47072e451e')
                  }}{{ temp.swarm.controlAvailable ? $t('i18n_0a60ac8f02') : $t('i18n_c9744f45e7') }}
                </div>
              </n-space>
            </div>
          </div>
        </n-timeline-item>
      </template>
      <n-timeline-item v-if="temp.plugins">
        <div>
          {{ $t('i18n_b9bcb4d623') }}

          <n-list item-layout="horizontal" :data="Object.keys(temp.plugins)" size="small">
            <template #renderItem="{ item }">
              <n-list-item>
                {{ item }}
                <n-tag v-for="(item1, index) in temp.plugins[item]" :key="index">{{ item1 }}</n-tag>
              </n-list-item>
            </template>
          </n-list>
        </div>
      </n-timeline-item>
      <n-timeline-item v-if="temp.registryConfig">
        <div>
          {{ $t('i18n_92f3fdb65f') }}
          <n-list item-layout="horizontal" :data="Object.keys(temp.registryConfig.indexConfigs)" size="small">
            <template #renderItem="{ item }">
              <n-list-item>
                {{ item }}
                <n-tag v-if="temp.registryConfig.indexConfigs[item].official" color="green">{{
                  $t('i18n_f5c3795be5')
                }}</n-tag
                ><n-tag v-if="temp.registryConfig.indexConfigs[item].secure" color="green">{{
                  $t('i18n_fdbc77bd19')
                }}</n-tag>
                <n-tag v-for="(item1, index) in temp.registryConfig.indexConfigs[item].mirrors" :key="index">{{
                  item1
                }}</n-tag>
              </n-list-item>
            </template>
          </n-list>
        </div>
      </n-timeline-item>
    </n-timeline>
  </div>
</template>
<script>
import { dockerInfo } from '@/api/docker-api'
import { renderSize } from '@/utils/const'
export default {
  props: {
    id: {
      type: String,
      default: ''
    },
    urlPrefix: {
      type: String,
      default: ''
    },
    machineDockerId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      temp: {},

      rules: {}
    }
  },
  computed: {
    reqDataId() {
      return this.id || this.machineDockerId
    }
  },
  mounted() {
    this.loadData()
    // console.log(Comparator);
  },
  methods: {
    renderSize,
    // load data
    loadData() {
      dockerInfo(this.urlPrefix, {
        id: this.reqDataId
      }).then((res) => {
        if (res.code === 200) {
          this.temp = res.data
        }
      })
    }
  }
}
</script>
