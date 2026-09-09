<template>
  <div>
    <n-card :bordered="false" class="about-card">
      <div class="about-logo">
        <n-h2 style="margin: 0">Voyager1</n-h2>
        <n-tag type="info" size="small">Java Project Online Manage</n-tag>
      </div>
      <n-divider />
      <n-descriptions :column="1" label-placement="left" size="small">
        <n-descriptions-item label="版本">{{ info.v || '-' }}</n-descriptions-item>
        <n-descriptions-item label="构建时间">{{ info.t || '-' }}</n-descriptions-item>
        <n-descriptions-item label="构建环境">{{ info.e || '-' }}</n-descriptions-item>
      </n-descriptions>
      <n-divider />
      <n-text depth="3" style="font-size: 12px">
        轻量级一体化运维与持续交付平台（在线构建 · 自动部署 · 日常运维 · 项目监控）
      </n-text>
      <br />
      <n-text depth="3" style="font-size: 12px">Licensed under the Apache License, Version 2.0</n-text>
    </n-card>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import { pageBuildInfo } from '@/utils/const'

const info = ref<Record<string, any>>({})

onMounted(() => {
  info.value = pageBuildInfo()
})
</script>

<style scoped>
.about-card {
  max-width: 520px;
  margin: 24px auto;
}
.about-logo {
  text-align: center;
  padding: 12px 0;
}
</style>
