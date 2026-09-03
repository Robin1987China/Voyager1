<template>
  <div>
    <!-- 数据表格 -->
    <CustomTable
      is-show-tools
      default-auto-refresh
      :auto-refresh-time="30"
      :active-page="activePage"
      table-name="systemUserLoginLog"
      :empty-description="$t('i18n_ede2c450d1')"
      :loading="loading"
      :data="list"
      :columns="columns"
      :pagination="pagination"
      bordered
      row-key="id"
      :scroll="{
        x: 'max-content'
      }"
      @change="change"
      @refresh="loadData"
    >
      <template #title>
        <n-space>
          <n-input
            v-model:value="listQuery['%modifyUser%']"
            :placeholder="$t('i18n_819767ada1')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['%username%']"
            :placeholder="$t('i18n_9a56bb830e')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-input
            v-model:value="listQuery['%ip%']"
            :placeholder="$t('i18n_b38d6077d6')"
            class="search-input-item"
            @press-enter="loadData"
          />
          <n-date-picker
            class="search-input-item"
            style="width: 220px"
            type="datetimerange"
            format="yyyy-MM-dd HH:mm:ss"
            clearable
            @update:value="onChangeTime"
          />
          <n-tooltip>
            <template #trigger>
              <span class="tw">
                <n-button type="primary" :loading="loading" @click="loadData">{{ $t('i18n_e5f71fc31e') }}</n-button>
              </span>
            </template>
            $t('i18n_4838a3bd20')
          </n-tooltip>
        </n-space>
      </template>
      <template #tableBodyCell="{ column, text }">
        <template v-if="column.dataIndex === 'userAgent'">
          <n-tooltip>
            <template #trigger>
              {{ text }}
            </template>
            text
          </n-tooltip>
        </template>

        <template v-if="column.dataIndex === 'success'">
          <n-tag v-if="text" color="green">{{ $t('i18n_330363dfc5') }}</n-tag>
          <n-tag v-else color="pink">{{ $t('i18n_acd5cb847a') }}</n-tag>
        </template>

        <template v-if="column.dataIndex === 'operateCode'">
          {{ operateCodeMap[text] || $t('i18n_1622dc9b6b') }}
        </template>
      </template>
    </CustomTable>
  </div>
</template>
<script lang="ts" setup>
import { userLoginLgin, operateCodeMap } from '@/api/user/user-login-log'
import { IPageQuery } from '@/interface/common'
import { CustomColumnType } from '@/components/customTable/types'
import { CHANGE_PAGE, COMPUTED_PAGINATION, PAGE_DEFAULT_LIST_QUERY, parseTime } from '@/utils/const'

import { useI18n } from 'vue-i18n'
const { t: $t } = useI18n()

const loading = ref(false)
const list = ref([])
// const operateCode = operateCodeMap;
const listQuery = ref<IPageQuery>({ ...PAGE_DEFAULT_LIST_QUERY })

const route = useRoute()
const attrs = useAttrs()
const activePage = computed(() => {
  return attrs.routerUrl === route.path
})

const columns = ref<CustomColumnType[]>([
  { title: $t('i18n_30acd20d6e'), key: 'modifyUser', width: 100 },
  { title: $t('i18n_9a56bb830e'), key: 'username', width: 120 },
  { title: 'IP', key: 'ip', width: 120 },

  {
    title: $t('i18n_5e9f2dedca'),
    key: 'success',
    width: 90,
    align: 'center'
  },
  {
    title: $t('i18n_64c083c0a9'),
    key: 'operateCode',
    ellipsis: true,
    width: 180
  },
  {
    title: $t('i18n_9fca7c455f'),
    key: 'createTimeMillis',
    sorter: true,
    render: (row) => parseTime(row['createTimeMillis'] || row['optTime']),
    width: '170px'
  },
  { title: $t('i18n_912302cb02'), key: 'userAgent', ellipsis: true, width: 100 }
])

const pagination = computed(() => {
  return COMPUTED_PAGINATION(listQuery.value)
})

const loadData = (pointerEvent?: any) => {
  loading.value = true
  listQuery.value.page = pointerEvent?.altKey || pointerEvent?.ctrlKey ? 1 : listQuery.value.page
  userLoginLgin(listQuery.value)
    .then((res) => {
      if (res.code === 200) {
        list.value = res.data.result
        listQuery.value.total = res.data.total
      }
    })
    .finally(() => {
      loading.value = false
    })
}

const change = (pagination: any, filters: any, sorter: any) => {
  listQuery.value = CHANGE_PAGE(listQuery.value, { pagination, sorter })
  loadData()
}

const onChangeTime = (value, dateString) => {
  listQuery.value.createTimeMillis = `${dateString[0]} ~ ${dateString[1]}`
}

onMounted(() => {
  loadData()
})
</script>
