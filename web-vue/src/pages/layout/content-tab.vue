<template>
  <n-tabs v-model:value="activeTabKey" class="my-tabs" hide-add type="editable-card" style="width: 100%" @edit="onEdit">
    <n-tab-pane v-for="(tab, index) in tabList" :key="tab.key" :name="tab.key" :closable="tabList.length > 1">
      <template #tab>
        <n-dropdown
          :trigger="['contextmenu']"
          :options="[
            {
              label: $t('i18n_6816da19f3'),
              key: '0',
              disabled: tabList.length <= 1,
              props: {
                onClick: () =>
                  closeTabs({
                    key: tab.key
                  })
              }
            },
            {
              label: $t('i18n_e9290eaaae'),
              key: '1',
              disabled: tabList.length <= 1 || index === 0,
              props: {
                onClick: () =>
                  closeTabs({
                    key: tab.key,
                    position: 'left'
                  })
              }
            },
            {
              label: $t('i18n_649d90ab3c'),
              key: '2',
              disabled: tabList.length <= 1 || index === tabList.length - 1,
              props: {
                onClick: () =>
                  closeTabs({
                    key: tab.key,
                    position: 'right'
                  })
              }
            }
          ]"
        >
          <span style="display: inline-table">{{ tab.title }}</span>
        </n-dropdown>
      </template>
    </n-tab-pane>
  </n-tabs>
</template>
<script lang="ts" setup>
import { h } from 'vue'
import { NIcon } from 'naive-ui'

import userHeader from './user-header.vue'
import { useAllMenuStore } from '@/stores/menu2'
import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()
const router = useRouter()
const route = useRoute()

const props = defineProps<{
  mode: string
}>()

const menuStore = useAllMenuStore()
const tabList = computed(() => {
  return menuStore.getTabList(props.mode)
})
const activeTabKey = computed({
  get() {
    return menuStore.getActiveTabKey(props.mode)
  },
  set(value) {
    activeTab(value)
  }
})

const activeTab = (key?: string) => {
  key = key || activeTabKey.value
  const index = tabList.value.findIndex((ele: any) => ele.key === key)
  const activeTab = tabList.value[index]
  if (!activeTab) {
    return
  }
  router.push({
    query: { ...route.query, sPid: activeTab.parentId, sId: activeTab.id },
    path: activeTab.path
  })

  menuStore.activeMenu(props.mode, activeTab.id)

  return activeTab
}

const onEdit = (key: string | number | MouseEvent | KeyboardEvent, action: 'add' | 'remove') => {
  if (action === 'remove') {
    if (tabList.value.length === 1) {
      $notification.warn({
        message: $t('i18n_b953d1a8f1')
      })
      return
    }
    menuStore.removeTab(props.mode, key).then(() => {
      activeTab()
    })
  }
}

// 关闭 tabs
const closeTabs = (data: any) => {
  $notification.success({
    message: $t('i18n_33130f5c46')
  })
  menuStore.clearTabs(props.mode, data).then(() => {
    activeTab()
  })
}
</script>
<style scoped>
.my-tabs {
  width: 100%;
  height: 40px;
}
</style>
