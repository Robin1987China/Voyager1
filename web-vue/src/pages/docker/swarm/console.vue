<template>
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
    @close="onClose1"
  >
    <template #title>
      <!-- 集群控制台 -->
      <n-menu
        v-model:value="menuKey"
        mode="horizontal"
        class="docker-menu"
        :options="menuOptions"
        @update:value="menuClick"
      />
    </template>

    <!-- <n-layout-header style="height: 48px; padding: 0"> </n-layout-header> -->

    <div class="layout-content">
      <swarm-node v-show="menuKey === 'node'" :id="id" :show="visible" :url-prefix="urlPrefix" />
      <swarm-service v-show="menuKey === 'server'" :id="id" :show="visible" :url-prefix="urlPrefix" />
      <swarm-task v-show="menuKey === 'task'" :id="id" :show="visible" :url-prefix="urlPrefix" />
    </div>
  </CustomDrawer>
</template>
<script>
import SwarmNode from './node'
import SwarmService from './service'
import SwarmTask from './task'
import { mapState } from 'pinia'
import { useGuideStore } from '@/stores/guide'
export default {
  components: {
    SwarmNode,
    SwarmService,
    SwarmTask
  },
  props: {
    id: {
      type: String,
      default: ''
    },
    initMenu: {
      type: String,
      default: 'node'
    },
    visible: {
      type: Boolean,
      default: false
    },
    urlPrefix: {
      type: String,
      default: ''
    }
  },
  emits: ['close'],
  data() {
    return {
      menuKey: ''
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getCollapsed']),
    menuOptions() {
      return [
        { key: 'node', label: this.$t('i18n_957c1b1c50') },
        { key: 'server', label: this.$t('i18n_b5ce5efa6e') },
        { key: 'task', label: this.$t('i18n_8de2137776') }
      ]
    }
  },
  mounted() {
    this.menuKey = this.initMenu
  },
  methods: {
    menuClick(key) {
      this.menuKey = key
    },
    onClose1() {
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
