<template>
  <div>
    <n-tag
      v-for="(data, key) in parameterMap"
      :key="key"
      closable
      @close="delteParameters(key)"
      @click="handleEdit(key)"
    >
      <n-tooltip>
        <template #trigger>
          {{ key }}
        </template>
        {{ parameterMap[key] }}
      </n-tooltip>
    </n-tag>
    <n-tag
      :style="{
        borderStyle: 'dashed'
      }"
      @click="handleAdd"
    >
      <PlusOutlined />{{ $t('i18n_7e1b283c57') }}</n-tag
    >

    <!-- 编辑区 -->
    <CustomModal
      v-if="editVisible"
      v-model:open="editVisible"
      destroy-on-close
      :title="$t('i18n_71a2c432b0')"
      :mask-closable="false"
      @ok="handleEditOk"
    >
      <n-form ref="editForm" :model="temp" :rules="rules">
        <n-form-item label="key" path="key">
          <n-input v-model:value="temp.key" :disabled="!!temp.oldKey" :placeholder="$t('i18n_c0d19bbfb3')" />
        </n-form-item>
        <n-form-item label="value" path="value">
          <n-input v-model:value="temp.value" :placeholder="$t('i18n_24384dab27')" />
        </n-form-item>
      </n-form>
    </CustomModal>
  </div>
</template>
<script lang="ts" setup>
import { PlusOutlined } from '@ant-design/icons-vue'

import Qs from 'qs'
import { useI18n } from 'vue-i18n'
const props = withDefaults(
  defineProps<{
    value?: string
  }>(),
  {
    value: ''
  }
)
const emit = defineEmits<{ (e: 'update:value', value: object): void }>()
const useData = ref(props.value)
const { t: $t } = useI18n()
const parameterMap = ref<Record<string, any>>({})

// 删除
const delteParameters = (key: string) => {
  delete parameterMap.value[key]
  useData.value = Qs.stringify(parameterMap.value)
}

watch(
  () => useData.value,
  (val) => {
    emit('update:value', val as any)
  },
  {
    immediate: false
  }
)
watch(
  () => props.value,
  (val) => {
    useData.value = val
    parameterMap.value = Qs.parse(props.value)
  },
  {
    immediate: true
  }
)

// 监听变量变化
watch(
  () => parameterMap.value,
  () => {
    useData.value = Qs.stringify(parameterMap.value)
  },
  {
    deep: true,
    immediate: false
  }
)

const rules = ref<Record<string, any[]>>({
  key: [{ required: true, message: $t('i18n_c0d19bbfb3') as string, trigger: 'blur' }]
})

const editVisible = ref(false)
const editForm = ref()
const temp = ref<{
  key: string
  value: string
  oldKey?: string
}>({
  key: '',
  value: ''
})

const handleAdd = () => {
  editVisible.value = true
  temp.value = { key: '', value: '' }
}

const handleEdit = (key: string) => {
  editVisible.value = true
  temp.value = { key: key, value: parameterMap.value[key], oldKey: key }
}

const handleEditOk = () => {
  editForm.value.validate().then(() => {
    editVisible.value = false
    parameterMap.value[temp.value.key] = temp.value.value
    if (temp.value.key !== temp.value.oldKey && temp.value.oldKey) {
      delete parameterMap.value[temp.value.oldKey]
    }
  }).catch(() => {})
}
</script>
