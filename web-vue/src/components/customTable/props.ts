import { t } from '@/i18n'
import type { CustomColumnType } from './types'

export const customTableProps = {
  // 页面通用传 :data="list"，组件内部以 dataSource 为准；此处兼容 data 别名。
  dataSource: {
    type: Array,
    default: undefined
  },
  data: {
    type: Array,
    default: () => []
  },
  columns: {
    type: Array as () => CustomColumnType[],
    default: () => []
  },
  pagination: {
    type: [Object, Boolean],
    default: undefined
  },
  rowKey: {
    type: [String, Function],
    default: 'id'
  },
  size: {
    type: String,
    default: 'middle'
  },
  scroll: {
    type: Object,
    default: undefined
  },
  bordered: {
    type: Boolean,
    default: false
  },
  loading: {
    type: [Boolean, Object],
    default: false
  },
  /** 是否显示工具栏 */
  isShowTools: Boolean,
  /** 是否显示刷新按钮 */
  isHideRefresh: Boolean,
  /** 行选择（多选）配置，传给 n-data-table 的 type=checkbox 列 */
  rowSelection: {
    type: Object,
    default: undefined
  },
  /** tableName 全局唯一值，存储需要 * */
  tableName: {
    type: String,
    required: true
  },
  /** 是否隐藏自动刷新 */
  isHideAutoRefresh: {
    type: Boolean,
    default: false
  },
  /** 默认自动刷新 */
  defaultAutoRefresh: {
    type: Boolean,
    default: false
  },
  /** 自动刷新时间 s 秒，不建议小于 10 秒 */
  autoRefreshTime: {
    type: Number,
    default: 10
  },
  /**
   * 页面布局方式
   */
  layout: {
    type: String,
    default: null
  },
  /**
   * 当前页面是否激活
   */
  activePage: {
    type: Boolean,
    default: false
  },
  // 空数据时显示内容
  emptyDescription: {
    type: String,
    default: t('i18n_807ed6f5a6')
  }
}
