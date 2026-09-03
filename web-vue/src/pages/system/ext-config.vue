<template>
  <div>
    <n-grid>
      <n-grid-item span="6" style="">
        <n-grid>
          <n-space style="display: inline">
            <n-input v-model:value="addName" :placeholder="$t('i18n_cfeea27648')" style="width: 100%">
              <template #suffix>
                <n-button type="primary" size="small" :disabled="!addName" @click="addItemHander"
                  >{{ $t('i18n_e83a256e4f') }}
                </n-button>
              </template>
            </n-input>
          </n-space>
        </n-grid>
        <n-tree
          v-model:expanded-keys="expandedKeys"
          v-model:selected-keys="selectedKeys"
          :data="treeData"
          :label-field="'name'"
          :key-field="'id'"
          :children-field="'children'"
          @update:selected-keys="select"
        ></n-tree>
      </n-grid-item>
      <n-grid-item span="18" style="padding-left: 5px">
        <n-space direction="vertical" style="display: flex">
          <code-editor
            v-model:content="temp.content"
            height="calc(100vh - 170px)"
            :show-tool="true"
            :file-suffix="temp.name"
          >
            <template #tool_before>
              <div v-show="temp.name">
                {{ $t('i18n_5b47861521') }} <n-tag color="red">{{ temp.name }}</n-tag>
              </div>
            </template>
          </code-editor>

          <n-grid type="flex" justify="center">
            <n-space>
              <n-button type="primary" danger :disabled="!temp || !temp.name" @click="saveData">{{
                $t('i18n_be5fbbe34c')
              }}</n-button>
              <n-button v-if="temp.hasDefault" type="primary" :disabled="!temp || !temp.name" @click="readeDefault">
                {{ $t('i18n_3306c2a7c7') }}
              </n-button>
            </n-space>
          </n-grid>
        </n-space>
      </n-grid-item>
    </n-grid>
  </div>
</template>
<script>
import codeEditor from '@/components/codeEditor'
import { addItem, listExtConf, getItem, saveItem, getDefaultItem } from '@/api/ext-config'

export default {
  components: {
    codeEditor
  },
  data() {
    return {
      loading: false,
      treeData: [],
      expandedKeys: [],
      selectedKeys: [],
      editVisible: false,
      temp: {},

      replaceFields: {
        children: 'children',
        title: 'name',
        key: 'id'
      },
      addName: ''
    }
  },
  computed: {},
  created() {
    this.loadData()
  },
  methods: {
    // 加载数据
    loadData() {
      this.loading = true
      listExtConf().then((res) => {
        if (res.code === 200) {
          this.treeData = res.data?.children
        }
        this.loading = false
      })
    },
    // 选择
    select(selectedKeys, { node }) {
      if (this.temp?.name === node.dataRef?.name) {
        return
      }
      if (!node.dataRef.isLeaf) {
        return
      }
      this.temp = {}
      getItem({ name: node.dataRef?.id }).then((res) => {
        if (res.code === 200) {
          this.temp = {
            name: node.dataRef?.id,
            content: res.data,
            hasDefault: node.dataRef?.hasDefault
          }
        }
      })
    },
    readeDefault() {
      getDefaultItem({ name: this.temp.name }).then((res) => {
        if (res.code === 200) {
          this.temp = { ...this.temp, content: res.data }
          $$message.success({ content: this.$t('i18n_335258331a') })
        }
      })
    },
    addItemHander() {
      const title = this.$t('i18n_ef016ab402')
      const other = this.$t('i18n_956ab8a9f7')
      const content = `${title} 【${this.addName}】 ${other}`
      $confirm({
        title: this.$t('i18n_c4535759ee'),
        zIndex: 1009,
        content: content,
        okText: this.$t('i18n_e83a256e4f'),
        cancelText: this.$t('i18n_625fb26b4b'),
        onOk: () => {
          return addItem({ name: this.addName }).then((res) => {
            if (res.code === 200) {
              // 成功
              $notification.success({
                message: res.msg
              })
              this.addName = ''
              this.loadData()
            }
          })
        }
      })
    },
    saveData() {
      saveItem(this.temp).then((res) => {
        if (res.code === 200) {
          // 成功
          $notification.success({
            message: res.msg
          })
        }
      })
    }
  }
}
</script>
