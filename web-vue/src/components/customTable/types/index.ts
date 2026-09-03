export type CustomColumnType = Record<string, any> & {
  dataIndex?: string
  key?: string
  title?: any
  width?: number | string
  fixed?: string | boolean
  ellipsis?: boolean
  align?: string
  sorter?: any
  defaultSortOrder?: any
  customRender?: (opt: { text: any; record: any; index: number; column: any }) => any
  checked?: boolean
}
export type CatchStorageType = {
  key: string
  checked: boolean
}

export type TableLayoutType = 'table' | 'card' | undefined

export type CustomTableType = {
  columns: CustomColumnType[]
  storageKey: string
}

export type CustomTableSlotsType = Record<string, any> & {
  tableBodyCell?: (props: {
    text: any
    value: any
    record: Record<string, any>
    index: number
    column: CustomColumnType
  }) => void
  cardBodyCell?: any
  default: any
}
