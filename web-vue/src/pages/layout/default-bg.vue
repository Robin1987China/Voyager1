<template>
  <n-config-provider :theme="darkTheme">
    <div class="init-wrapper" :style="backgroundImage">
      <!-- 舱内环境（极暗） -->
      <div class="cabin"></div>

      <!-- 圆形舷窗（悬窗透视深空） -->
      <div class="viewport">
        <div class="viewport-frame">
          <div class="space">
            <!-- 深空星点 -->
            <svg class="stars" viewBox="0 0 400 400">
              <circle cx="60" cy="50" r="0.8" fill="#E2E8F0" opacity="0.5" />
              <circle cx="140" cy="30" r="0.5" fill="#CBD5E1" opacity="0.4" />
              <circle cx="230" cy="80" r="0.9" fill="#F1F5F9" opacity="0.55" />
              <circle cx="320" cy="45" r="0.6" fill="#94A3B8" opacity="0.4" />
              <circle cx="370" cy="120" r="0.7" fill="#E2E8F0" opacity="0.45" />
              <circle cx="30" cy="160" r="0.5" fill="#CBD5E1" opacity="0.35" />
              <circle cx="110" cy="210" r="0.8" fill="#E2E8F0" opacity="0.5" />
              <circle cx="250" cy="200" r="0.5" fill="#94A3B8" opacity="0.35" />
              <circle cx="350" cy="260" r="0.7" fill="#F1F5F9" opacity="0.45" />
              <circle cx="200" cy="320" r="0.6" fill="#CBD5E1" opacity="0.4" />
              <circle cx="90" cy="330" r="0.9" fill="#E2E8F0" opacity="0.5" />
              <circle cx="320" cy="350" r="0.5" fill="#94A3B8" opacity="0.35" />
              <circle cx="180" cy="90" r="0.4" fill="#94A3B8" opacity="0.3" />
              <circle cx="290" cy="150" r="0.4" fill="#CBD5E1" opacity="0.3" />
              <circle cx="45" cy="270" r="0.6" fill="#E2E8F0" opacity="0.4" />
              <circle cx="160" cy="370" r="0.5" fill="#CBD5E1" opacity="0.35" />
            </svg>
            <!-- 遥远星球 -->
            <div class="planet-shadow"></div>
            <div class="planet"></div>
            <div class="planet-ring"></div>
            <!-- 旅行者1号探测器（掠过舷窗） -->
            <div class="probe-trail"></div>
            <img class="probe" :src="probeImg" alt="旅行者1号" />
            <!-- 舷窗玻璃反射 -->
            <div class="viewport-glass"></div>
          </div>
        </div>
      </div>

      <!-- 舱内面板 + 指示灯 -->
      <div class="cabin-panel"></div>
      <div class="cabin-light cabin-light-1"></div>
      <div class="cabin-light cabin-light-2"></div>

      <div class="box">
        <slot name="content" />
      </div>

      <div v-show="showFooter" class="footer">
        <n-space>
          <template #split>
            <n-divider type="vertical" />
          </template>
          <n-button text>
            <span>Voyager1 ©{{ new Date().getFullYear() }}</span>
          </n-button>
          <n-dropdown :options="langOptions">
            <span class="tw"
              ><n-button text>
                {{
                  supportLang.find((item) => {
                    return item.value === nowLang
                  })?.label
                }}
                <DownOutlined /> </n-button
            ></span>
          </n-dropdown>
        </n-space>
      </div>
    </div>
  </n-config-provider>
</template>
<script lang="ts" setup>
import { darkTheme } from 'naive-ui'
import { DownOutlined } from '@ant-design/icons-vue'

import { supportLang } from '@/i18n'
import probeImg from '@/assets/images/voyager-probe.svg'

const useGuideStore = guideStore()

const themeValue = computed(() => {
  return useGuideStore.getCatchThemeView()
})

const nowLang = computed({
  get() {
    return useGuideStore.getLocale()
  },
  set(newValue) {
    useGuideStore.changeLocale(newValue)
  }
})

const langOptions = computed(() =>
  supportLang.map((item) => ({
    label: item.label,
    key: item.value,
    disabled: nowLang.value === item.value,
    props: { onClick: () => useGuideStore.changeLocale(item.value) }
  }))
)

defineProps({
  showFooter: {
    type: Boolean,
    default: true
  }
})

const backgroundImage = computed(() => {
  // 舱内环境：极暗深黑（2001 太空漫游基调）
  const color = 'radial-gradient(1400px 900px at 50% 40%, #0a0f1c 0%, #05070d 60%, #020409 100%)'
  return {
    background: color
  }
})
</script>
<style scoped>
.init-wrapper {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 16px;
  overflow: auto;
  position: relative;
}

.box {
  position: absolute;
  left: 50%;
  top: 68%;
  transform: translate(-50%, -50%);
  animation: boxFadeIn 0.8s ease-out both;
  z-index: 5;
}

@keyframes boxFadeIn {
  from {
    opacity: 0;
    transform: translate(-50%, -45%);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%);
  }
}

.cabin {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 0;
}

/* ===== 圆形舷窗 ===== */
.viewport {
  position: fixed;
  left: 50%;
  top: 38%;
  transform: translate(-50%, -50%);
  width: min(520px, 56vh);
  height: min(520px, 56vh);
  pointer-events: none;
  z-index: 1;
}
.viewport-frame {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  box-sizing: border-box;
  border: 3px solid rgba(148, 163, 184, 0.14);
  box-shadow:
    inset 0 0 60px rgba(2, 6, 23, 0.9),
    inset 0 0 12px rgba(148, 163, 184, 0.12),
    0 0 0 14px rgba(15, 23, 42, 0.55),
    0 0 0 16px rgba(148, 163, 184, 0.08),
    0 0 0 30px rgba(15, 23, 42, 0.35),
    0 0 0 31px rgba(148, 163, 184, 0.05),
    0 40px 120px rgba(0, 0, 0, 0.8);
  overflow: hidden;
  position: relative;
}
.space {
  position: absolute;
  inset: 0;
  background: radial-gradient(120% 120% at 50% 45%, #0b1222 0%, #060a14 55%, #020409 100%);
}
.stars {
  position: absolute;
  inset: 8%;
  width: 84%;
  height: 84%;
}
.planet {
  position: absolute;
  left: 50%;
  top: 46%;
  width: 150px;
  height: 150px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  background:
    radial-gradient(circle at 32% 30%, rgba(212, 175, 55, 0.55) 0%, transparent 60%),
    radial-gradient(circle at 40% 45%, #c9a24b 0%, #8a6530 38%, #4a3016 62%, #1c1006 100%);
  box-shadow:
    inset -20px -16px 40px rgba(0, 0, 0, 0.65),
    inset 6px 8px 20px rgba(255, 236, 179, 0.25),
    0 0 60px rgba(201, 162, 75, 0.12);
}
.planet-shadow {
  position: absolute;
  left: 50%;
  top: 46%;
  width: 150px;
  height: 150px;
  transform: translate(-50%, -50%);
  border-radius: 50%;
  box-shadow: inset -30px -22px 60px rgba(0, 0, 0, 0.9);
}
.planet-ring {
  position: absolute;
  left: 50%;
  top: 46%;
  width: 240px;
  height: 96px;
  transform: translate(-50%, -50%) rotate(-18deg);
  border: 1.5px solid rgba(201, 162, 75, 0.28);
  border-radius: 50%;
  box-shadow:
    0 0 12px rgba(201, 162, 75, 0.1),
    inset 0 0 12px rgba(201, 162, 75, 0.08);
}
.planet-ring::before {
  content: '';
  position: absolute;
  inset: 26% 4%;
  border: 1px solid rgba(201, 162, 75, 0.14);
  border-radius: 50%;
}
.viewport-glass {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: linear-gradient(
    115deg,
    rgba(148, 197, 255, 0.05) 0%,
    transparent 32%,
    transparent 60%,
    rgba(148, 197, 255, 0.03) 78%,
    transparent 100%
  );
  pointer-events: none;
}
/* ===== 旅行者1号探测器（掠过舷窗） ===== */
.probe {
  position: absolute;
  left: 62%;
  top: 20%;
  width: 92px;
  height: 92px;
  opacity: 0.95;
  filter: drop-shadow(0 0 10px rgba(242, 206, 107, 0.25));
  animation: probeDrift 14s ease-in-out infinite;
}
.probe-trail {
  position: absolute;
  left: 30%;
  top: 42%;
  width: 46%;
  height: 1.5px;
  transform: rotate(-32deg);
  transform-origin: right center;
  background: linear-gradient(90deg, transparent, rgba(242, 206, 107, 0.55));
  animation: trailPulse 4s ease-in-out infinite;
}
@keyframes probeDrift {
  0%,
  100% {
    transform: translate(0, 0) rotate(0deg);
  }
  50% {
    transform: translate(-10px, 8px) rotate(-3deg);
  }
}
@keyframes trailPulse {
  0%,
  100% {
    opacity: 0.35;
  }
  50% {
    opacity: 0.8;
  }
}
.cabin-panel {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 18vh;
  pointer-events: none;
  z-index: 2;
  background: linear-gradient(180deg, transparent 0%, rgba(10, 15, 26, 0.5) 55%, rgba(6, 9, 16, 0.85) 100%);
  border-top: 1px solid rgba(148, 163, 184, 0.07);
  box-shadow: 0 -30px 60px rgba(0, 0, 0, 0.5);
}
.cabin-light {
  position: fixed;
  bottom: 3.5vh;
  width: 3px;
  height: 3px;
  border-radius: 50%;
  pointer-events: none;
  z-index: 3;
  animation: lightPulse 5s ease-in-out infinite;
}
.cabin-light-1 {
  left: calc(50% - 90px);
  background: #38bdf8;
  box-shadow: 0 0 6px rgba(56, 189, 248, 0.6);
}
.cabin-light-2 {
  right: calc(50% - 90px);
  background: #f59e0b;
  box-shadow: 0 0 6px rgba(245, 158, 11, 0.5);
  animation-delay: 2.2s;
}
@keyframes lightPulse {
  0%,
  100% {
    opacity: 0.15;
  }
  50% {
    opacity: 0.7;
  }
}

.footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 6;
}

:global(.n-result-content) {
  background-color: unset !important;
}
</style>
