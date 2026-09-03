<template>
  <div>
    <n-tabs default-active-key="1">
      <n-tab-pane name="1" :tab="$t('i18n_ffa9fd37b5')"><workspaceList></workspaceList> </n-tab-pane>
      <n-tab-pane name="2" :tab="$t('i18n_74ea72bbd6')">
        <clusterList></clusterList>
      </n-tab-pane>
    </n-tabs>
  </div>
</template>
<script>
import workspaceList from './workspace-list.vue'
import clusterList from './cluster-list.vue'
export default {
  components: {
    workspaceList,
    clusterList
  },
  data() {
    return {}
  },
  computed: {},
  created() {},
  methods: {}
}
</script>
