<template>
  <div class="custom-table">
    <!-- class="custom-table__box" -->
    <div v-if="props.isShowTools">
      <!-- 增加工具栏部分 -->
      <n-card size="small" :body-style="{ padding: '1px 0px 0px 0px' }">
        <template #header>
          <div class="custom-table__toolbar">
            <n-form inline>
              <n-space>
                <template v-if="slots.toolPrefix">
                  <slot name="toolPrefix"></slot>
                </template>
                <n-form-item>
                  <template #label>
                    <span style="font-weight: normal">{{ $t('i18n_891db2373b') }}</span>
                  </template>
                  <n-space v-if="!props.isHideAutoRefresh">
                    <n-switch
                      v-model:value="countdownSwitch"
                      :checked-label="$t('i18n_8493205602')"
                      :unchecked-label="$t('i18n_d58a55bcee')"
                      @change="countDownChange"
                    />
                    <n-divider v-if="countdownSwitch" type="vertical" />
                    <div class="header-statistic">
                      <n-countdown
                        v-if="countdownSwitch"
                        :key="countdownKey"
                        :duration="countdownDuration"
                        :render="countdownRender"
                        @finish="countDownFinish"
                      />
                    </div>
                  </n-space>
                </n-form-item>
                <n-form-item>
                  <n-tooltip v-if="!props.isHideRefresh">
                    <template #trigger>
                      <ReloadOutlined class="table-action__icon" @click="refreshClick" />
                    </template>
                    $t('i18n_694fc5efa9')
                  </n-tooltip>
                </n-form-item>

                <n-form-item>
                  <n-popover trigger="click" placement="bottom-end">
                    <template #trigger>
                      <span class="tw">
                        <n-tooltip>
                          <template #trigger>
                            <ColumnHeightOutlined class="table-action__icon" />
                          </template>
                          $t('i18n_bdd9d38d7e')
                        </n-tooltip>
                      </span>
                    </template>
                    <template #header>{{ $t('i18n_bdd9d38d7e') }}</template>

                    <n-radio-group v-model:value="tableSize" class="custom-size-list">
                      <div v-for="item in tableSizeList" :key="item.value">
                        <n-radio :value="item.value">{{ item.label }}</n-radio>
                      </div>
                    </n-radio-group>
                  </n-popover>
                </n-form-item>

                <n-form-item>
                  <n-popover v-if="props.tableName" trigger="click" placement="bottom-end">
                    <template #trigger>
                      <span class="tw">
                        <n-tooltip>
                          <template #trigger>
                            <SettingOutlined />
                          </template>
                          $t('i18n_949a8b7bd2')
                        </n-tooltip>
                      </span>
                    </template>
                    <template #header>
                      <div class="custom-column-list__title">
                        <div>{{ $t('i18n_949a8b7bd2') }}</div>
                        <n-button text size="small" @click="resetCustomColumn">{{ $t('i18n_4b9c3271dc') }}</n-button>
                      </div>
                    </template>

                    <n-checkbox-group class="custom-column-list" :value="customCheckColumnList" @change="onCheckChange">
                      <Container
                        drag-handle-selector=".custom-column-list__icon"
                        non-drag-area-selector=".not-draggable"
                        orientation="vertical"
                        @drop="onDrop"
                      >
                        <Draggable
                          v-for="(item, index) in customColumnList"
                          :key="index"
                          :class="!!item.fixed ? 'not-draggable' : ''"
                        >
                          <VerticalLeftOutlined v-if="!!item.fixed" class="custom-column-list__icon" />
                          <HolderOutlined v-else class="custom-column-list__icon" />
                          <n-checkbox :value="item.key" :disabled="!!item.fixed">
                            {{ item.title }}
                          </n-checkbox>
                          <n-divider style="margin: 2px 0" />
                        </Draggable>
                      </Container>
                    </n-checkbox-group>
                  </n-popover>
                </n-form-item>

                <n-form-item v-if="canChangeLayout">
                  <n-tooltip>
                    <template #trigger>
                      <span class="tw">
                        <!-- <ReloadOutlined   /> -->
                        <TableOutlined
                          v-if="tableLayout === 'card'"
                          class="table-action__icon"
                          @click="tableLayoutClick"
                        />
                        <LayoutOutlined v-else class="table-action__icon" @click="tableLayoutClick" />
                      </span>
                    </template>
                    $t('i18n_03816381ec')
                  </n-tooltip>
                </n-form-item>

                <template v-if="slots.tableHelp">
                  <n-form-item>
                    <slot name="tableHelp"></slot>
                  </n-form-item>
                </template>
              </n-space>
            </n-form>
          </div>
        </template>

        <n-card :body-style="{ padding: '10px' }" :bordered="false">
          <template #header="slotProps">
            <template v-if="slots.title">
              <slot name="title" v-bind="slotProps || {}"></slot>
            </template>
          </template>
          <template v-if="tableLayout === 'table'">
            <n-data-table
              :data="effectiveDataSource"
              :columns="naiveColumns"
              :pagination="naivePagination"
              :size="tableSize"
              :row-key="rowKeyFn"
              :scroll-x="scrollX"
              :bordered="props.bordered"
              :loading="props.loading"
              :checked-row-keys="checkedRowKeys"
              @update:checked-row-keys="onCheckedRowKeysChange"
            />
          </template>
          <template v-else-if="tableLayout === 'card'">
            <n-space direction="vertical" style="width: 100%">
              <n-grid :x-gap="[16, 16]">
                <template v-if="effectiveDataSource && effectiveDataSource.length">
                  <n-grid-item v-for="(item, index) in effectiveDataSource" :key="item.id" :span="6">
                    <slot name="cardBodyCell" :item="item" :index="index"></slot>
                  </n-grid-item>
                </template>
                <n-grid-item v-else :span="24">
                  <n-empty :description="props.emptyDescription" />
                </n-grid-item>
              </n-grid>
              <div class="card-pagination">
                <n-pagination
                  v-if="naivePagination !== false"
                  v-bind="naivePagination"
                  size="small"
                  @update:page="onPageChange"
                  @update:page-size="onPageSizeChange"
                />
              </div>
            </n-space>
            <!-- <slot name="cardPageTool"></slot> -->
          </template>
          <template v-else>{{ $t('i18n_f4edba3c9d') }}</template>
        </n-card>
      </n-card>
    </div>
  </div>
</template>
<script lang="ts">
import {
  ColumnHeightOutlined,
  HolderOutlined,
  LayoutOutlined,
  ReloadOutlined,
  SettingOutlined,
  TableOutlined,
  VerticalLeftOutlined
} from '@ant-design/icons-vue'

import { useUserStore } from '@/stores/user'
import { Container, Draggable } from 'vue3-smooth-dnd'
import { CatchStorageType, CustomColumnType, CustomTableSlotsType, TableLayoutType } from './types'
import { compareArrays } from './utils'
import { tableSizeList } from './dict'
import { customTableProps } from './props'
import { StorageService } from './utils/StorageService'
import { dropApplyDrag } from '@/utils/const'
import { t } from '@/i18n'

export default defineComponent({
  name: 'CustomTable',
  components: {
    Container,
    Draggable,
    ReloadOutlined,
    ColumnHeightOutlined,
    SettingOutlined,
    TableOutlined,
    LayoutOutlined,
    HolderOutlined,
    VerticalLeftOutlined
  },
  inheritAttrs: false,
  props: customTableProps,
  slots: Object as CustomTableSlotsType,
  emits: ['refresh', 'change', 'changeTableLayout'],
  setup(props, { attrs, slots, emit }) {
    const userStore = useUserStore()
    const storageService: StorageService = new StorageService(props.tableName, {
      provide: 'localStorage',
      prefix: 'table:catch__' + userStore?.userInfo?.id,
      // 存储前拦截器
      beforeStorage(storageObject, defaultConfig) {
        // 判断components.customTable.index.b76d94e0是否为默认
        const defaultAutoRefresh: number = props.defaultAutoRefresh ? 1 : 0
        if (defaultAutoRefresh === storageObject?.refresh?.isAutoRefresh) {
          storageObject.refresh = defaultConfig.refresh
        }
        // 判断表格大小是否为默认
        if (storageObject?.tableSize === props?.size) {
          storageObject.tableSize = defaultConfig?.tableSize
        }
        // layout
        if (storageObject?.layout === props?.layout) {
          storageObject.layout = defaultConfig?.layout
        }
        if (
          storageObject?.column?.length === 0 ||
          JSON.stringify(storageObject?.column?.filter((item) => item.checked)?.map((item) => item.key) || []) ===
            JSON.stringify(props.columns.map((item) => item.key))
        ) {
          storageObject.column = defaultConfig.column
        }
        return storageObject
      }
    })

    const { autoRefreshTime, isAutoRefresh } = storageService.getRefreshConfig()
    // 倒计时（naive n-countdown 使用 duration 毫秒，重置通过 remount 实现）
    const getCountdownDuration = () => {
      return 1000 * (autoRefreshTime !== -1 ? autoRefreshTime : props.autoRefreshTime + 0)
    }
    const countdownSwitch = ref(isAutoRefresh !== -1 ? isAutoRefresh : props.defaultAutoRefresh)
    const countdownDuration = ref(0)
    const countdownKey = ref(0)
    const restartCountdown = () => {
      countdownDuration.value = getCountdownDuration()
      countdownKey.value++
    }
    const countdownRender = ({ hours, minutes, seconds }: any) => {
      const total = hours * 3600 + minutes * 60 + seconds
      return `${t('i18n_14feaa5b3a')} ${total} ${t('i18n_ee6ce96abb')}`
    }
    const countDownFinish = () => {
      if (props.activePage) {
        // 仅当页面处于活跃才components.customTable.index.b76d94e0
        emit('refresh', 'silence')
      }
      restartCountdown()
    }
    const countDownChange = () => {
      if (countdownSwitch.value) {
        restartCountdown()
      } else {
        countdownDuration.value = 0
      }
      storageService.setRefreshConfig({
        isAutoRefresh: countdownSwitch.value ? 1 : 0,
        autoRefreshTime: props.autoRefreshTime
      })
    }
    onMounted(() => {
      if (countdownSwitch.value && !props.isHideAutoRefresh) {
        restartCountdown()
      }
    })

    /** 获取缓存key */
    const refreshClick = () => {
      restartCountdown()
      emit('refresh', 'click')
    }
    // 表格components.customTable.index.7c3745c1调整hooks
    const tableSize = ref<string>('middle')
    watch(
      () => tableSize.value,
      (val) => {
        if (!storageService.exitOpenStorage()) return
        storageService.setTableSizeConfig(val)
      }
    )
    // 组件加载 从存储中读取
    onMounted(() => {
      // 判断是否需要存储
      const size = storageService.getTableSizeConfig()
      tableSize.value = size ? size : props.size || 'middle'
    })

    // 视图模式
    const tableLayout = ref<TableLayoutType>('table')
    const canChangeLayout = computed(() => {
      if (props.layout) {
        return false
      }
      return Object.keys(slots).filter((key) => key === 'cardBodyCell').length > 0
    })
    const sizeOptions = computed(() => {
      if (tableLayout.value === 'card') {
        return ['8', '12', '16', '20', '24']
      }
      return ['5', '10', '15', '20', '25', '30', '35', '40', '50']
    })
    const paginationByLayout = computed(() => {
      if (props.pagination === false) {
        return false
      }
      return { ...props.pagination, pageSizeOptions: sizeOptions.value }
    })

    /** 数据源：优先 dataSource，页面误用 :data 时回退到 data，保证列表有数据 */
    const effectiveDataSource = computed(() => {
      if (Array.isArray(props.dataSource)) return props.dataSource
      if (Array.isArray(props.data)) return props.data
      return []
    })

    // row-key 兼容：Ant 传字符串("id")，Naive 的 n-data-table 要求函数，否则构建行树时报 "getKey is not a function"。
    const rowKeyFn = (row: any) => {
      const rk = props.rowKey
      if (typeof rk === 'function') return rk(row)
      if (typeof rk === 'string' && rk) return row[rk]
      return (row as any)?.id
    }

    // Ant columns -> Naive columns 适配层
    const toNaiveColumn = (col: CustomColumnType) => {
      // 迁移时列定义已从 dataIndex 改为 key，但页面 slot 仍按 column.dataIndex 判断。
      // 这里统一合成 dataIndex，保证 slot 中的 column.dataIndex === 'xxx' 能命中。
      const dataIndex = String(col.dataIndex ?? col.key ?? '')
      const key = String(col.key ?? col.dataIndex ?? '')
      // 传给 slot 的 column 需同时携带 key 与 dataIndex（以及页面自定义字段）。
      const columnForSlot = { ...col, key, dataIndex }
      return {
        key,
        title: col.title,
        width: col.width,
        fixed: col.fixed,
        ellipsis: col.ellipsis,
        align: col.align,
        sorter: col.sorter,
        defaultSortOrder: col.defaultSortOrder,
        render: (row: any, index: number) => {
          const text = row[dataIndex]
          // Ant 风格列级 render: (row, index) => ...
          if (typeof col.render === 'function') {
            return col.render(row, index)
          }
          if (typeof col.customRender === 'function') {
            return col.customRender({ text, record: row, index, column: columnForSlot })
          }
          // 页面同时存在 #tableBodyCell 与 #bodyCell 两种 slot 命名，均需兼容
          const cellSlot = slots.tableBodyCell || slots.bodyCell
          if (cellSlot) {
            // slot 内的 v-if/v-else-if 可能没有命中该列（如普通文本列），
            // Vue 会把未命中的分支渲染成注释节点（<!---->）而非空数组，
            // 旧逻辑 length===0 判断失效导致普通文本列整列空白。
            // 过滤注释/空节点后若确无真实内容，则回退为纯文本。
            const slotResult = cellSlot({ text, value: text, record: row, index, column: columnForSlot })
            const list = (Array.isArray(slotResult) ? slotResult : [slotResult]).filter((vnode: any) => vnode != null)
            const hasRealContent = list.some(
              (vnode: any) =>
                // 注释节点 type 为 Symbol（如 Comment），过滤掉
                !(typeof vnode.type === 'symbol') &&
                // 无 type 且无 children 的空 VNode 也过滤
                !(vnode.type === undefined && !vnode.children)
            )
            if (slotResult === undefined || slotResult === null || !hasRealContent) return text
            return slotResult
          }
          return text
        }
      }
    }

    const naiveColumns = computed(() => {
      const cols = customColumn.value.map((col: CustomColumnType) => toNaiveColumn(col))
      // 行选择（多选）列：页面传 :row-selection 时展示 checkbox 列并绑定受控选中值
      if (props.rowSelection) {
        const rd = props.rowSelection as any
        cols.unshift({
          type: 'selection',
          fixed: 'left',
          disabled:
            typeof rd.getCheckboxProps === 'function' ? (row: any) => !!rd.getCheckboxProps(row)?.disabled : undefined
        })
      }
      return cols
    })
    // 受控选中行 key，与 rowSelection.selectedRowKeys 同步
    const checkedRowKeys = ref<Array<any>>([])
    watch(
      () => (props.rowSelection as any)?.selectedRowKeys,
      (val) => {
        if (Array.isArray(val)) checkedRowKeys.value = val
      },
      { immediate: true }
    )
    const onCheckedRowKeysChange = (keys: Array<any>) => {
      checkedRowKeys.value = keys
      const rc = (props.rowSelection as any)?.onChange
      if (typeof rc === 'function') rc(keys, [])
    }

    const naivePagination = computed(() => {
      if (props.pagination === false) {
        return false
      }
      const p = (props.pagination as any) || {}
      return {
        page: p.current || 1,
        pageSize: p.pageSize || 10,
        itemCount: p.total || 0,
        showSizePicker: p.showSizeChanger !== false,
        pageSizes: sizeOptions.value
      }
    })

    // Ant Table 的 scroll.x 常用 'max-content'，但 Naive 的 n-data-table scroll-x 需要数字（像素）。
    // 传 'max-content' 会让 Naive 把表格宽度设成 1000000px 且 fixed 布局列宽失效，
    // 导致所有非 fixed 列被撑到几万像素、被推到视口外（只剩 fixed 操作列可见）。
    // 这里把这类非法值归一化：非数字（如 'max-content'）时不设 scroll-x，让 Naive 自适应列宽。
    const scrollX = computed(() => {
      const x = (props.scroll as any)?.x
      if (typeof x === 'number') return x
      return undefined
    })

    const onPageChange = (page: number) => {
      emit('change', { ...(props.pagination as any), current: page }, {}, {})
    }
    const onPageSizeChange = (size: number) => {
      emit('change', { ...(props.pagination as any), current: 1, pageSize: size }, {}, {})
    }

    onMounted(() => {
      // 判断是否需要存储
      if (props.layout) {
        tableLayout.value = props.layout as TableLayoutType
      } else {
        const layout = storageService.getLayoutConfig()
        tableLayout.value = (layout || 'table') as TableLayoutType
      }
    })
    const tableLayoutClick = () => {
      tableLayout.value = tableLayout.value === 'card' ? 'table' : 'card'
      emit('changeTableLayout', tableLayout.value)
    }
    watch(
      () => tableLayout.value,
      (val) => {
        if (!storageService.exitOpenStorage()) return
        // 判断是否需要存储
        storageService.setLayoutConfig(val)
      }
    )
    let customColumnList = ref<CustomColumnType[]>([])
    const customCheckColumnList = computed(() => {
      return customColumnList.value
        .filter((item: CustomColumnType) => item.checked)
        .map((item: CustomColumnType) => String(item.key))
    })
    const customColumn = computed(() => {
      if (!storageService.exitOpenStorage()) return props.columns
      return customColumnList.value.filter((item: CustomColumnType) => item.checked)
    })
    const resetCustomColumn = () => {
      customColumnList.value = props.columns.map((item: CustomColumnType) => ({ ...item, checked: true }))
    }
    const onCheckChange = (checkedValues: (string | number)[]) => {
      customColumnList.value = customColumnList.value.map((item: CustomColumnType) => ({
        ...item,
        checked: checkedValues.includes(String(item.key))
      }))
    }
    const onDrop = (dropResult: any) => {
      customColumnList.value = dropApplyDrag<CustomColumnType>(customColumnList.value, dropResult)
    }
    /** 设置默认列 */
    const setDefaultCustomColumnList = () => {
      customColumnList.value = props.columns.map((item: CustomColumnType) => ({ ...item, checked: true }))
    }
    // 监听列变化,同步至缓存customColumnList中
    watch(
      () => props.columns,
      (val) => {
        if (!storageService.exitOpenStorage()) return
        const catchStorage = storageService.getColumnConfig() || []
        if (
          catchStorage.length == 0 ||
          (catchStorage.length > 0 && catchStorage.some((key) => typeof key === 'string')) ||
          !compareArrays(
            val.map((item: CustomColumnType) => String(item.key)),
            catchStorage.filter((item) => item.key).map((item) => String(item.key))
          )
        ) {
          return setDefaultCustomColumnList()
        } else {
          const tmpObj: { [key: string]: CustomColumnType } = {}
          val.forEach((item: CustomColumnType) => {
            tmpObj[String(item.key || '_d')] = item
          })
          customColumnList.value = catchStorage.map((item) => {
            const key = item.key
            return {
              ...tmpObj[String(key)],
              checked: item.checked
            }
          })
        }
      },
      {
        immediate: true
      }
    )
    // 监听列展示变化,持久化存储
    watch(
      () => customColumnList.value,
      (val) => {
        if (!storageService.exitOpenStorage()) return

        if (JSON.stringify(val) !== JSON.stringify(props.columns)) {
          storageService.setColumnConfig(
            customColumnList.value.map((item) => {
              return {
                key: item.key,
                checked: item.checked
              } as CatchStorageType
            })
          )
        } else {
          storageService.setColumnConfig([])
        }
      },
      {
        immediate: true
      }
    )

    return {
      onDrop,
      countdownSwitch,
      countDownFinish,
      countdownDuration,
      countdownKey,
      countdownRender,
      countDownChange,
      attrs,
      props,
      slots,
      effectiveDataSource,
      rowKeyFn,
      refreshClick,
      // otherSlots,
      tableLayoutClick,
      canChangeLayout,
      tableLayout,
      tableSizeList,
      tableSize,
      customColumnList,
      customColumn,
      customCheckColumnList,
      resetCustomColumn,
      onCheckChange,
      naiveColumns,
      naivePagination,
      checkedRowKeys,
      onCheckedRowKeysChange,
      scrollX,
      onPageChange,
      onPageSizeChange
    }
  }
})
</script>
<style lang="less" scoped>
.custom-table {
  // position: relative;
  // padding-top: 36px;
  &__toolbar {
    display: flex;
    justify-content: flex-end;
    width: 100%;
  }
  &__box {
    padding: 0 px;
    // height: 36px;
    box-sizing: border-box;
    border-radius: 4px 4px 0 0px;
    position: absolute;
    right: 0px;
    top: 0;
    //background: #fff;
    // border: 1px solid rgb(240, 240, 240);
    border-bottom: 0px;
    display: flex;
    align-items: center;
    // box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }
}

.table-action {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  &__icon {
    // font-size: 18px;
  }
}
.custom-column-list {
  display: block;
  &__title {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  &__icon {
    // color: rgba(0, 0, 0, 0.6);
    margin-right: 10px;
  }
}
.custom-size-list {
  display: block;
}
.card-pagination {
  display: flex;
  justify-content: flex-end;
}
</style>
<style scoped>
:deep(.n-form-item) {
  margin-inline-end: 0;
}
.header-statistic :deep(.n-countdown) {
  line-height: 1;
  font-size: 16px;
}
</style>
