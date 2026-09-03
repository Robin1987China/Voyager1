<template>
  <div>
    <n-grid type="flex" justify="center">
      <n-grid-item :span="18">
        <n-card :title="$t('i18n_74bdccbb5d')" :bordered="true">
          <Container drag-handle-selector=".move" orientation="vertical" @drop="onDrop">
            <Draggable v-for="(element, index) in myWorkspaceList" :key="index">
              <n-grid class="item-row">
                <n-grid-item :span="18">
                  <template v-if="element.edit">
                    <n-input
                      v-model:value="element.name"
                      :placeholder="$t('i18n_6f32b1077d')"
                      :enter-button="$t('i18n_38cf16f220')"
                      @keyup.enter="editOk(element)"
                    />
                  </template>
                  <template v-else>
                    <n-tooltip>
                      <template #trigger>
                        {{ element.name || element.originalName }}
                      </template>
                      `${$t('i18n_bd4e9d0ee2')}${element.originalName}`
                    </n-tooltip>
                  </template>
                </n-grid-item>
                <n-grid-item :span="2"></n-grid-item>
                <n-grid-item :span="4">
                  <n-space>
                    <n-button :disabled="element.edit" type="primary" size="small" @click="edit(element)">
                      <template #icon><EditOutlined /></template>
                    </n-button>
                    <n-tooltip placement="left" class="move">
                      <template #trigger>
                        <MenuOutlined />
                      </template>
                      `${$t('i18n_181e1ad17d')}`
                    </n-tooltip>
                  </n-space>
                </n-grid-item>
              </n-grid>
            </Draggable>
          </Container>
          <n-grid-item style="margin-top: 10px">
            <n-space>
              <n-button type="primary" @click="save"> {{ $t('i18n_be5fbbe34c') }} </n-button>
              <n-button type="primary" @click="resetDefaultName">
                {{ $t('i18n_b650acd50b') }}
              </n-button>
            </n-space>
          </n-grid-item>
        </n-card>
      </n-grid-item>
    </n-grid>
  </div>
</template>
<script>
import { EditOutlined, MenuOutlined } from '@ant-design/icons-vue'

import { myWorkspace, saveWorkspace } from '@/api/user/user'
import { dropApplyDrag } from '@/utils/const'
import { Container, Draggable } from 'vue3-smooth-dnd'
export default {
  components: {
    Container,
    Draggable
  },
  data() {
    return {
      myWorkspaceList: [],
      loading: true
    }
  },
  created() {
    this.init()
  },
  methods: {
    onDrop(dropResult) {
      this.myWorkspaceList = dropApplyDrag(this.myWorkspaceList, dropResult).map((item, index) => {
        return { ...item, sort: index }
      })
    },
    resetDefaultName() {
      this.myWorkspaceList = this.myWorkspaceList.map((item) => {
        return { ...item, name: '' }
      })
    },
    init() {
      myWorkspace().then((res) => {
        if (res.code == 200 && res.data) {
          this.myWorkspaceList = res.data
        }
        this.loading = false
      })
    },
    edit(editItem) {
      this.myWorkspaceList = this.myWorkspaceList.map((item) => {
        if (item.id === editItem.id) {
          item.edit = true
        }
        return item
      })
    },
    // 编辑 ok
    editOk(editItem) {
      this.myWorkspaceList = this.myWorkspaceList.map((item) => {
        if (item.id === editItem.id) {
          item.edit = false
        }
        return item
      })
    },
    // 保存
    save() {
      saveWorkspace(this.myWorkspaceList).then((res) => {
        if (res.code === 200) {
          $notification.success({
            message: res.msg
          })
        }
      })
    }
  }
}
</script>
<style scoped>
.box-shadow {
  box-shadow: 0 0 10px 5px rgba(223, 222, 222, 0.5);
  border-radius: 5px;
}

.item-row {
  padding: 10px;
  margin: 5px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.8);
}
</style>
