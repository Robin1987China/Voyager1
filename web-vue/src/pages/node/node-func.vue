<template>
  <CustomDrawer
    destroy-on-close
    placement="right"
    :width="`${getCollapsed ? 'calc(100vw - 80px)' : 'calc(100vw - 200px)'}`"
    :header-style="{
      padding: '0 10px'
    }"
    :body-style="{
      padding: '0'
    }"
    :open="true"
    @close="
      () => {
        $emit('close')
      }
    "
  >
    <template #title>
      <n-space>
        <!-- {{ name }} -->
        <div>
          <n-tabs
            v-model:value="current"
            :tab-bar-style="{
              margin: '0'
            }"
          >
            <n-tab-pane v-if="tabs.includes('project')" name="project" :tab="$t('i18n_436367b066')"></n-tab-pane>
            <n-tab-pane v-if="tabs.includes('scripct')" name="scripct" :tab="$t('i18n_a1fb7f1606')"></n-tab-pane>
            <n-tab-pane
              v-if="tabs.includes('scripct-log')"
              name="scripct-log"
              :tab="$t('i18n_7370bdf0d2')"
            ></n-tab-pane>
          </n-tabs>
        </div>
      </n-space>
    </template>
    <div class="layout-content">
      <project-search v-if="current === 'project'" :node-id="id" />
      <script-list v-else-if="current === 'scripct'" :node-id="id"></script-list>
      <script-log v-else-if="current === 'scripct-log'" :node-id="id"></script-log>
    </div>
  </CustomDrawer>
</template>
<script>
import { mapState } from 'pinia'
import ScriptList from '@/pages/node/script-list'
import ScriptLog from '@/pages/node/node-layout/other/script-log'
import { useGuideStore } from '@/stores/guide'
export default {
  components: {
    ScriptList,
    ScriptLog,
    projectSearch: defineAsyncComponent(() => import('@/pages/node/search'))
  },
  props: {
    name: {
      type: String,
      default: ''
    },
    id: {
      type: String,
      default: ''
    },
    tabs: {
      type: Array,
      default: function () {
        return ['project', 'scripct', 'scripct-log']
      }
    }
  },
  emits: ['close'],
  data() {
    return {
      current: null
    }
  },
  computed: {
    ...mapState(useGuideStore, ['getCollapsed'])
  },
  created() {
    //
    this.current = this.tabs[0]
  },
  methods: {}
}
</script>
<style scoped>
.layout-content {
  padding: 0;
  margin: 15px;
}
:deep(.n-tabs-nav--line-type) {
  box-shadow: none;
}
</style>
