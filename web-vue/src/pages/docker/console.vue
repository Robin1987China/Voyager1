<template>
  <div>
    <!-- 控制台 -->
    <CustomDrawer
      destroy-on-close
      placement="right"
      :width="`${getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
      :open="true"
      :body-style="{
        padding: '0'
      }"
      :header-style="{
        padding: '0 10px'
      }"
      @close="onClose"
    >
      <template #title>
        <n-menu
          v-model:value="menuKeyArray"
          mode="horizontal"
          class="docker-menu"
          :options="[
            { label: $t('i18n_2b0623dab9'), key: 'containers' },
            { label: 'docker-compose', key: 'docker-compose' },
            { label: $t('i18n_3477228591'), key: 'images' },
            { label: $t('i18n_7088e18ac9'), key: 'volumes' },
            { label: $t('i18n_7ddbe15c84'), key: 'networks' },
            { label: $t('i18n_d8c7e04c8e'), key: 'info' },
            { label: $t('i18n_293cafbbd3'), key: 'prune' }
          ]"
          @update:value="menuClick"
        />
      </template>

      <div class="layout-content">
        <!-- <n-layout-content> -->
        <container
          v-show="menuKey === 'containers'"
          :id="id"
          type="container"
          :machine-docker-id="machineDockerId"
          :show="visible"
          :url-prefix="urlPrefix"
        />
        <container
          v-show="menuKey === 'docker-compose'"
          :id="id"
          type="compose"
          :machine-docker-id="machineDockerId"
          :show="visible"
          :url-prefix="urlPrefix"
        />
        <images
          v-show="menuKey === 'images'"
          :id="id"
          :machine-docker-id="machineDockerId"
          :show="visible"
          :url-prefix="urlPrefix"
        />
        <volumes
          v-if="menuKey === 'volumes'"
          :id="id"
          :machine-docker-id="machineDockerId"
          :show="visible"
          :url-prefix="urlPrefix"
        />
        <info
          v-show="menuKey === 'info'"
          :id="id"
          :machine-docker-id="machineDockerId"
          :show="visible"
          :url-prefix="urlPrefix"
        />
        <networks
          v-show="menuKey === 'networks'"
          :id="id"
          :machine-docker-id="machineDockerId"
          :show="visible"
          :url-prefix="urlPrefix"
        />
        <prune
          v-show="menuKey === 'prune'"
          :id="id"
          :machine-docker-id="machineDockerId"
          :show="visible"
          :url-prefix="urlPrefix"
        />
        <!-- </n-layout-content> -->
      </div>
    </CustomDrawer>
  </div>
</template>
<script>
import Container from './container'
import Images from './images'
import Volumes from './volumes'
import Info from './info'
import Networks from './networks'
import Prune from './prune'
import { mapState } from 'pinia'
import { useGuideStore } from '@/stores/guide'
export default {
  components: {
    Container,
    Images,
    Volumes,
    Info,
    Networks,
    Prune
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    visible: {
      type: Boolean,
      default: false
    },
    machineDockerId: {
      type: String,
      default: ''
    },
    urlPrefix: {
      type: String,
      default: ''
    }
  },
  emits: ['close'],
  data() {
    return {
      menuKeyArray: ['containers'],
      menuKey: 'containers'
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getCollapsed'])
  },
  mounted() {},
  methods: {
    menuClick(item) {
      this.menuKey = item.key
    },
    onClose() {
      this.$emit('close')
    }
  }
}
</script>
<style scoped>
.docker-menu {
  border-bottom: 0;
}

.layout-content {
  padding: 0;
  margin: 15px;
}
</style>
