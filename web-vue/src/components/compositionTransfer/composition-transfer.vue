<template>
  <div class="tree-transfer">
    <div class="tree-transfer__panel">
      <div class="tree-transfer__body">
        <n-tree
          v-if="leftTreeData.length"
          block-node
          checkable
          label-field="title"
          :data="leftTreeData"
          :checked-keys="leftCheckedKey"
          :indeterminate-keys="leftHalfCheckedKeys"
          @update:checked-keys="handleLeftChecked"
          @update:indeterminate-keys="(keys) => (leftHalfCheckedKeys = keys)"
        />
        <n-empty v-else>
          <template #description>{{ $t('i18n_21efd88b67') }}</template>
        </n-empty>
      </div>
    </div>
    <div class="tree-transfer__operations">
      <n-button size="small" type="primary" :disabled="!leftCheckedAllKey.length" @click="onChange(null, 'right')">
        <n-icon><RightOutlined /></n-icon>
      </n-button>
      <n-button size="small" type="primary" :disabled="!rightCheckedKey.length" @click="onChange(null, 'left')">
        <n-icon><LeftOutlined /></n-icon>
      </n-button>
    </div>
    <div class="tree-transfer__panel">
      <div class="tree-transfer__body">
        <n-tree
          v-if="rightTreeData.length"
          block-node
          checkable
          label-field="title"
          :data="rightTreeData"
          :checked-keys="rightCheckedKey"
          :default-expanded-keys="rightExpandedKey"
          @update:checked-keys="handleRightChecked"
        />
        <n-empty v-else>
          <template #description>{{ $t('i18n_21efd88b67') }}</template>
        </n-empty>
      </div>
    </div>
  </div>
</template>
<script>
import { LeftOutlined, RightOutlined } from '@ant-design/icons-vue'
import { cloneDeep, flatten, getTreeKeys, handleLeftTreeData, handleRightTreeData } from './utils'

export default {
  components: {
    LeftOutlined,
    RightOutlined
  },
  props: {
    /** 树数据 */
    treeData: {
      type: Array,
      default: () => []
    },
    /** 编辑 key */
    editKey: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      targetKeys: [], // 显示在右侧框数据的 key 集合
      dataSource: [], // 数据源，其中的数据将会被渲染到左边一栏

      leftCheckedKey: [], // 左侧树选中 key 集合
      leftHalfCheckedKeys: [], // 左侧半选集合
      leftCheckedAllKey: [], // 左侧树选中的 key 集合，包括半选与全选
      leftTreeData: [], // 左侧树

      rightCheckedKey: [], // 右侧树选中集合
      rightCheckedAllKey: [], // 右侧树选中集合，包括半选与全选
      rightExpandedKey: [], // 右侧展开数集合
      rightTreeData: [], // 右侧树

      emitKeys: [], // 往父级组件传递的数据

      deepList: [] // 深层列表
    }
  },
  watch: {
    treeData: {
      deep: true,
      handler() {
        this.processTreeData()
      }
    },
    editKey: {
      deep: true,
      handler() {
        this.processTreeData()
      }
    }
  },
  created() {
    this.processTreeData()
  },
  methods: {
    // 处理树数据
    processTreeData() {
      this.dataSource = []
      flatten(cloneDeep(this.treeData), this.dataSource)
      if (this.editKey.length) {
        this.processEditData()
      } else {
        this.leftTreeData = handleLeftTreeData(cloneDeep(this.treeData), this.leftCheckedKey)
      }
    },
    // 处理编辑数据
    processEditData() {
      this.leftCheckedAllKey = this.editKey
      this.rightExpandedKey = this.editKey
      this.targetKeys = this.editKey
      this.rightTreeData = handleRightTreeData(cloneDeep(this.treeData), this.editKey)

      this.leftCheckedKey = this.editKey
      this.leftHalfCheckedKeys = this.leftCheckedAllKey.filter((item) => this.leftCheckedKey.indexOf(item) === -1)
      this.leftTreeData = handleLeftTreeData(cloneDeep(this.treeData), this.leftCheckedKey)

      this.emitKeys = this.rightExpandedKey
    },
    // 穿梭更改
    onChange(targetKeys, direction) {
      if (direction === 'right') {
        this.targetKeys = this.leftCheckedAllKey
        this.rightCheckedKey = []
        this.rightTreeData = handleRightTreeData(cloneDeep(this.treeData), this.leftCheckedAllKey, 'right')
        this.leftTreeData = handleLeftTreeData(cloneDeep(this.treeData), this.leftCheckedKey, 'right')
      } else if (direction === 'left') {
        this.rightTreeData = handleRightTreeData(this.rightTreeData, this.rightCheckedKey, 'left')
        this.leftTreeData = handleLeftTreeData(this.leftTreeData, this.rightCheckedKey, 'left')
        this.leftCheckedKey = this.leftCheckedKey.filter((item) => this.rightCheckedKey.indexOf(item) === -1)
        this.targetKeys = this.targetKeys.filter((item) => this.rightCheckedKey.indexOf(item) === -1)
        this.leftHalfCheckedKeys = this.leftHalfCheckedKeys.filter((item) => this.rightCheckedKey.indexOf(item) === -1)
        this.rightCheckedKey = []
      }
      this.rightExpandedKey = getTreeKeys(this.rightTreeData)
      this.emitKeys = this.rightExpandedKey
    },
    // 左侧选择
    handleLeftChecked(keys) {
      this.leftCheckedKey = keys
      this.leftCheckedAllKey = [...new Set([...this.leftHalfCheckedKeys, ...keys])]
    },
    // 右侧选择
    handleRightChecked(keys) {
      this.rightCheckedKey = keys
      this.rightCheckedAllKey = [...keys]
    }
  }
}
</script>
<style lang="less" scoped>
.tree-transfer {
  display: flex;
  align-items: stretch;
  width: 100%;
  &__panel {
    flex: 1;
    min-width: 0;
    border: 1px solid var(--n-border-color, rgb(224, 224, 230));
    border-radius: 6px;
    overflow: hidden;
  }
  &__body {
    height: 260px;
    overflow: auto;
    padding: 8px;
  }
  &__operations {
    display: flex;
    flex-direction: column;
    justify-content: center;
    gap: 8px;
    padding: 0 12px;
  }
}
</style>
