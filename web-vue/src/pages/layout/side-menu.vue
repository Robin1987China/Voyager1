<template>
  <div>
    <n-menu
      v-model:value="selectedKeys"
      :theme="theme"
      :options="menuOptions"
      class="menu"
      :expanded-keys="getMenuOpenKeys2"
      @update:expanded-keys="openChange"
      @update:value="handleMenuClick"
    />
  </div>
</template>
<script>
import { h } from 'vue'
import { mapState } from 'pinia'
import Icon from '@/components/Icon'
import { useAllMenuStore } from '@/stores/menu2'
import { useGuideStore } from '@/stores/guide'
import { useAppStore } from '@/stores/app'
export default {
  components: {
    Icon
  },
  props: {
    mode: {
      type: String,
      default: ''
    },
    theme: {
      type: String,
      default: ''
    }
  },
  data() {
    return {}
  },
  computed: {
    ...mapState(useGuideStore, ['getGuideCache']),
    ...mapState(useAppStore, ['getCollapsed']),
    selectedKeys: {
      get() {
        // naive n-menu 的 value 是单值（string | number | null），不是数组
        return useAllMenuStore().getActiveMenuKey(this.mode)
      },
      set() {}
    },
    getMenus() {
      return useAllMenuStore().getMenus(this.mode)
    },
    getMenuOpenKeys2() {
      if (this.getCollapsed) {
        // 折叠态下 n-menu 以浮层展示子菜单，expanded-keys 无意义
        return []
      }
      // 时候全局缓存的菜单
      return useAllMenuStore().getMenuOpenKeys(this.mode)
    },
    menuMultipleFlag() {
      return this.getGuideCache.menuMultipleFlag === undefined ? true : this.getGuideCache.menuMultipleFlag
    },
    menuOptions() {
      const toOption = (menu) => {
        const children =
          menu.childs && menu.childs.length
            ? menu.childs.map((sub) => {
                sub.parent = menu
                return { key: String(sub.id), label: sub.title }
              })
            : undefined
        return {
          key: String(menu.id),
          label: menu.title,
          icon: menu.icon_v3 ? () => h(Icon, { type: menu.icon_v3, style: { fontSize: '18px' } }) : undefined,
          children
        }
      }
      return this.getMenus.map(toOption)
    }
  },
  created() {
    useAllMenuStore().menuOpenKeys(this.mode, this.$route.query.sPid || '')
  },
  beforeUnmount() {},
  methods: {
    // 点击菜单（n-menu @update:value 传 key，映射回菜单项）
    handleMenuClick(key) {
      const findMenu = (menus, id) => {
        for (const m of menus) {
          if (String(m.id) === String(key)) return m
          if (m.childs && m.childs.length) {
            const found = findMenu(m.childs, id)
            if (found) return found
          }
        }
        return null
      }
      const menu = findMenu(this.getMenus, key)
      if (menu) this.handleClick(menu)
    },
    // 菜单打开
    openChange(keys) {
      if (keys.length && !this.menuMultipleFlag) {
        // 保留一个打开
        keys = [keys[keys.length - 1]]
      }

      useAllMenuStore().menuOpenKeys(this.mode, keys)
    },
    // 点击菜单
    handleClick(subMenu) {
      // 如果路由不存在
      if (!subMenu.path) {
        $notification.error({
          message: this.$t('i18n_130318a2a1')
        })
        return false
      }
      // 如果跳转路由跟当前一致
      if (this.$route.path === subMenu.path) {
        // $notification({
        //   message: "已经在当前页面了",
        // });
        return false
      }
      // 跳转路由
      this.$router.push({
        query: {
          ...this.$route.query,
          sPid: subMenu.parent?.id,
          sId: subMenu.id
        },
        path: subMenu.path
      })
      // this.$router.push()
    }
  }
}
</script>
<style scoped>
.menu {
  border-inline-end: 0 !important;
}
</style>
