/**
 * DSH（DeepSeek 风格）暗色主题：全站统一的调色板 / naive 组件令牌 / 字体栈 / CSS 变量。
 *
 * 使用方式：
 * - App.vue：`<n-config-provider :theme="darkTheme" :theme-overrides="dshThemeOverrides">`
 *   并在启动时调用 `installDshCssVars()` 注入 CSS 变量；
 * - 自定义样式（less/style/内联）统一引用 `var(--dsh-*)`，不再硬编码色值。
 */

/** 调色板（唯一权威来源） */
export const dshPalette = {
  /** 背景层次：bg-base / layer-1 / layer-2 / layer-3 */
  bgBase: '#151517',
  bgLayer1: '#1b1b1c',
  bgLayer2: '#232324',
  bgLayer3: '#2c2c2e',
  /** 描边/分隔 */
  border: '#353638',
  divider: '#2c2c2e',
  /** 文字：primary / secondary / tertiary / disabled */
  textPrimary: '#f9fafb',
  textSecondary: '#81858c',
  textTertiary: '#61666b',
  textDisabled: '#43454a',
  /** 品牌蓝 */
  primary: '#4176e6',
  primaryHover: '#5686fe',
  primaryPressed: '#4868b2'
} as const

/** 字体栈（唯一权威来源） */
export const dshFontFamily =
  "-apple-system, BlinkMacSystemFont, 'Segoe UI', 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', 'Helvetica Neue', Helvetica, Arial, sans-serif"
export const dshFontFamilyMono =
  "'SFMono-Regular', 'SF Mono', Menlo, Consolas, 'Liberation Mono', 'Courier New', monospace"

/**
 * naive-ui 全局主题覆盖（配合 darkTheme 使用）。
 * 只写与 naive 默认深色不同的令牌，其余继承。
 */
export const dshThemeOverrides = {
  common: {
    // 背景层次
    bodyColor: dshPalette.bgBase,
    cardColor: dshPalette.bgLayer2,
    modalColor: dshPalette.bgLayer2,
    popoverColor: dshPalette.bgLayer3,
    tableColor: dshPalette.bgBase,
    tableHeaderColor: dshPalette.bgLayer1,
    inputColor: dshPalette.bgLayer1,
    inputColorDisabled: dshPalette.bgLayer2,
    actionColor: dshPalette.bgLayer2,
    hoverColor: '#ffffff14',
    // 描边
    borderColor: dshPalette.border,
    dividerColor: dshPalette.divider,
    // 文字
    textColorBase: dshPalette.textPrimary,
    textColor1: dshPalette.textPrimary,
    textColor2: dshPalette.textSecondary,
    textColor3: dshPalette.textTertiary,
    textColorDisabled: dshPalette.textDisabled,
    // 品牌蓝
    primaryColor: dshPalette.primary,
    primaryColorHover: dshPalette.primaryHover,
    primaryColorPressed: dshPalette.primaryPressed,
    primaryColorSuppl: dshPalette.primary,
    // 圆角与字体
    borderRadius: '6px',
    borderRadiusSmall: '4px',
    fontFamily: dshFontFamily,
    fontFamilyMono: dshFontFamilyMono
  },
  Card: {
    borderColor: dshPalette.divider,
    borderRadius: '8px'
  },
  DataTable: {
    thColor: dshPalette.bgLayer1,
    tdColor: dshPalette.bgBase,
    borderColor: dshPalette.divider,
    thTextColor: dshPalette.textSecondary,
    tdTextColor: dshPalette.textPrimary,
    thFontWeight: '500'
  },
  Dialog: {
    color: dshPalette.bgLayer2
  },
  Input: {
    color: dshPalette.bgLayer1,
    borderColor: dshPalette.border,
    borderRadius: '6px'
  },
  Tabs: {
    tabBorderColor: dshPalette.divider
  },
  Tag: {
    borderColor: dshPalette.border
  },
  Menu: {
    // 侧边深色菜单：与背景层次对齐
    itemColorHover: '#ffffff0f',
    itemColorActive: `${dshPalette.primary}26`,
    itemTextColorActive: dshPalette.primaryHover
  }
}

/**
 * 把调色板注入为 :root CSS 变量（--dsh-*），供自定义样式引用。
 * 在 App 启动时调用一次。
 */
export function installDshCssVars() {
  const style = document.documentElement.style
  style.setProperty('--dsh-bg-base', dshPalette.bgBase)
  style.setProperty('--dsh-bg-layer1', dshPalette.bgLayer1)
  style.setProperty('--dsh-bg-layer2', dshPalette.bgLayer2)
  style.setProperty('--dsh-bg-layer3', dshPalette.bgLayer3)
  style.setProperty('--dsh-border', dshPalette.border)
  style.setProperty('--dsh-divider', dshPalette.divider)
  style.setProperty('--dsh-text-primary', dshPalette.textPrimary)
  style.setProperty('--dsh-text-secondary', dshPalette.textSecondary)
  style.setProperty('--dsh-text-tertiary', dshPalette.textTertiary)
  style.setProperty('--dsh-text-disabled', dshPalette.textDisabled)
  style.setProperty('--dsh-primary', dshPalette.primary)
  style.setProperty('--dsh-primary-hover', dshPalette.primaryHover)
  style.setProperty('--dsh-primary-pressed', dshPalette.primaryPressed)
  style.setProperty('--dsh-font-family', dshFontFamily)
  style.setProperty('--dsh-font-mono', dshFontFamilyMono)
}
