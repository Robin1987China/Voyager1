<template>
  <defaultBg>
    <template #content>
      <n-card hoverable class="card">
        <div>
          <n-result status="error" :title="$t('i18n_2245cf01a3')">
            <template #extra>
              <n-button> {{ $t('i18n_bb9ef827bf') }} </n-button>
            </template>
          </n-result>
        </div>
      </n-card>
    </template>
  </defaultBg>
</template>
<script>
import defaultBg from '@/pages/layout/default-bg.vue'
export default {
  components: {
    defaultBg
  },
  data() {
    return {}
  },
  methods: {}
}
</script>
