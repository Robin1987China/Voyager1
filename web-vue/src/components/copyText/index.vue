<template>
  <n-button class="copy-text-btn" text size="tiny" @click="doCopy">
    <template #icon>
      <n-icon>
        <svg viewBox="0 0 24 24" width="1em" height="1em" fill="currentColor">
          <path
            d="M16 1H4a2 2 0 0 0-2 2v14h2V3h12V1zm3 4H8a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h11a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2zm0 16H8V7h11v14z"
          />
        </svg>
      </n-icon>
    </template>
  </n-button>
</template>
<script lang="ts" setup>
const props = defineProps<{ text: string }>()

function doCopy() {
  const value = props.text ?? ''
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(value).then(() => {
      $$message.success('复制成功')
    })
    return
  }
  // 兼容非安全上下文（http 局域网部署）
  const textarea = document.createElement('textarea')
  textarea.value = value
  textarea.style.position = 'fixed'
  textarea.style.opacity = '0'
  document.body.appendChild(textarea)
  textarea.select()
  try {
    document.execCommand('copy')
    $$message.success('复制成功')
  } catch (e) {
    $$message.error('复制失败')
  }
  document.body.removeChild(textarea)
}
</script>
<style scoped>
.copy-text-btn {
  vertical-align: middle;
}
</style>
