<template>
  <n-drawer
    v-bind="attrs"
    :show="props.open"
    :width="props.width"
    :height="props.height"
    :placement="props.placement"
    :root-style="mergedRootStyle"
    @update:show="onUpdateShow"
  >
    <n-drawer-content
      :title="slots.title ? undefined : props.title"
      :closable="props.closable"
      :header-style="props.headerStyle"
      :body-content-style="props.bodyStyle"
      :footer-style="props.footerStyle"
    >
      <template v-if="slots.title || slots.extra" #header>
        <div class="diy-custom-drawer__header">
          <div class="diy-custom-drawer__header-main">
            <slot v-if="slots.title" name="title"></slot>
          </div>
          <div v-if="slots.extra" class="diy-custom-drawer__header-extra">
            <slot name="extra"></slot>
          </div>
        </div>
      </template>
      <slot name="default"></slot>
      <template v-if="slots.footer" #footer>
        <slot name="footer"></slot>
      </template>
    </n-drawer-content>
  </n-drawer>
</template>
<script lang="ts">
import { CSSProperties, defineComponent } from 'vue'
import { increaseZIndex } from '@/utils/utils'

export default defineComponent({
  name: 'CustomDrawer',
  props: {
    open: { type: Boolean, default: false },
    title: { type: String, default: '' },
    width: { type: [String, Number], default: undefined },
    height: { type: [String, Number], default: undefined },
    placement: { type: String, default: 'right' },
    closable: { type: Boolean, default: true },
    headerStyle: { type: [Object, String], default: undefined },
    // antd 的 body-style 对应 naive n-drawer-content 的 body-content-style
    bodyStyle: { type: [Object, String], default: undefined },
    footerStyle: { type: [Object, String], default: undefined },
    rootStyle: { type: Object, default: () => ({}) }
  },
  emits: ['close', 'update:open'],
  setup(props, { emit, slots, attrs }) {
    const mergedRootStyle: CSSProperties = {
      zIndex: increaseZIndex(),
      ...(props.rootStyle as CSSProperties)
    }
    // 遮罩点击 / Esc / 关闭按钮 -> 同步父组件状态并触发 close（antd 语义）
    const onUpdateShow = (v: boolean) => {
      if (!v) {
        emit('update:open', false)
        emit('close')
      }
    }
    return {
      props,
      attrs,
      slots,
      mergedRootStyle,
      onUpdateShow
    }
  }
})
</script>
<style lang="less" scoped>
.diy-custom-drawer__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  .diy-custom-drawer__header-main {
    flex: 1;
    min-width: 0;
  }
  .diy-custom-drawer__header-extra {
    flex-shrink: 0;
  }
}
</style>
