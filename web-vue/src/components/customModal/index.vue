<template>
  <n-modal
    v-model:show="show"
    preset="card"
    :title="slots.title ? undefined : title"
    :mask-closable="maskClosable"
    :close-on-esc="keyboard"
    :style="mergedStyle"
    :z-index="zIndex"
    :class="'diy-custom-modal'"
  >
    <template v-if="slots.title" #header>
      <slot name="title"></slot>
    </template>
    <slot name="default"></slot>
    <template v-if="slots.footer || realShowFooter" #footer>
      <slot v-if="slots.footer" name="footer"></slot>
      <template v-else>
        <n-space justify="end">
          <n-button @click="cancel">{{ cancelText }}</n-button>
          <n-button type="primary" :loading="confirmLoading" @click="confirm">{{ okText }}</n-button>
        </n-space>
      </template>
    </template>
  </n-modal>
</template>
<script lang="ts">
import { CSSProperties, computed, defineComponent } from 'vue'
import { increaseZIndex } from '@/utils/utils'

export default defineComponent({
  name: 'CustomModal',
  props: {
    open: { type: Boolean, default: false },
    bodyStyle: { type: Object, default: () => ({}) },
    title: { type: String, default: '' },
    // antd 的 width -> naive 需要写到 style 上
    width: { type: [String, Number], default: undefined },
    maskClosable: { type: Boolean, default: true },
    // antd 的 keyboard=false -> naive close-on-esc=false
    keyboard: { type: Boolean, default: true },
    okText: { type: String, default: '确 定' },
    cancelText: { type: String, default: '取 消' },
    confirmLoading: { type: Boolean, default: false },
    showFooter: { type: Boolean, default: true }
  },
  emits: ['ok', 'cancel', 'close', 'update:open'],
  setup(props, { emit, slots, attrs }) {
    const diyBodyStyle: CSSProperties = {
      maxHeight: 'calc(100vh - 240px)',
      overflowY: 'auto',
      padding: '8px 0px'
    }
    if ((props.bodyStyle as any)?.height) {
      delete diyBodyStyle.maxHeight
    }
    const bodyStyle: CSSProperties = {
      ...diyBodyStyle,
      ...(props.bodyStyle as CSSProperties)
    }
    // 合并 antd width 到 style（naive n-modal 没有 width prop）
    const mergedStyle = computed<CSSProperties>(() => {
      const style: CSSProperties = { ...bodyStyle }
      const w = props.width
      if (w !== undefined && w !== null && w !== '') {
        style.width = typeof w === 'number' ? `${w}px` : String(w)
      }
      return style
    })
    // antd 写法 :footer="null" / :footer="false" -> 隐藏 footer
    const footerAttrHidden = 'footer' in attrs && (attrs.footer === null || attrs.footer === false)
    const realShowFooter = computed(() => !footerAttrHidden && props.showFooter)
    // v-model:open / v-model:show -> Naive n-modal show
    // 该 setter 只会被 naive 内部交互触发（遮罩/Esc/关闭按钮），父组件改 open 不会经过这里
    const show = computed({
      get: () => props.open,
      set: (v: boolean) => {
        emit('update:open', v)
        if (!v) {
          // antd 语义：用户主动关闭（遮罩/Esc/X）触发 cancel
          emit('cancel')
          emit('close')
        }
      }
    })
    // Ant 写法 @ok / @cancel 映射到按钮点击
    const confirm = () => emit('ok')
    const cancel = () => {
      emit('cancel')
      emit('update:open', false)
    }
    return {
      props,
      attrs,
      slots,
      show,
      confirm,
      cancel,
      realShowFooter,
      mergedStyle,
      zIndex: increaseZIndex()
    }
  }
})
</script>
<style lang="less">
.diy-custom-modal {
  .n-card-header {
    padding-bottom: 8px;
  }
}
</style>
