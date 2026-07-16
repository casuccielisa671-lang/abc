<template>
  <section class="intro-section" aria-label="平台介绍">
    <div class="intro-content">
      <span class="decoration-line" />
      <div class="intro-copy">
        <h1 class="site-name">
          <span class="site-name__accent">职业能力</span>大数据<br>
          服务平台
        </h1>
        <p class="bottom-hint">登录以解锁职位推荐、能力画像与就业分析</p>

        <div class="feature-list" aria-label="平台能力">
          <div
            v-for="feature in features"
            :key="feature.label"
            class="feature-item"
            :data-tone="feature.tone"
          >
            <span class="feature-icon">
              <el-icon><component :is="feature.icon" /></el-icon>
            </span>
            <span class="feature-label">{{ feature.label }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { Aim, DataAnalysis, TrendCharts } from '@element-plus/icons-vue'

const features = [
  { label: '数据驱动', icon: DataAnalysis, tone: 'blue' },
  { label: '智能分析', icon: TrendCharts, tone: 'indigo' },
  { label: '精准匹配', icon: Aim, tone: 'cyan' }
]
</script>

<style scoped>
.intro-section {
  flex: 1;
  min-width: 420px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding-right: 56px;
}

.intro-content {
  display: flex;
  align-items: flex-start;
  gap: 28px;
  opacity: 0;
  animation: contentFadeIn 0.8s cubic-bezier(0.16, 1, 0.3, 1) 0.25s forwards;
}

.decoration-line {
  width: 4px;
  height: 154px;
  margin-top: 8px;
  border-radius: 2px;
  flex: none;
  background: linear-gradient(180deg, #38bdf8 0%, #6678f6 54%, #a855f7 100%);
  box-shadow: 0 0 22px rgba(99, 102, 241, 0.18);
}

.intro-copy {
  min-width: 0;
}

.site-name {
  margin: 0;
  color: #111827;
  font-family: 'Noto Serif SC', 'Songti SC', 'SimSun', 'Georgia', serif;
  font-size: 60px;
  font-weight: 700;
  line-height: 1.08;
  letter-spacing: 0;
}

.site-name__accent {
  color: transparent;
  background: linear-gradient(135deg, #4f7cff 0%, #6c63f6 58%, #8b5cf6 100%);
  -webkit-background-clip: text;
  background-clip: text;
  text-shadow: 0 12px 34px rgba(79, 124, 255, 0.14);
}

.bottom-hint {
  margin: 28px 0 0;
  color: #52617a;
  font-size: 15px;
  line-height: 1.6;
}

.feature-list {
  display: flex;
  gap: 56px;
  margin-top: 46px;
}

.feature-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  min-width: 62px;
}

.feature-icon {
  width: 48px;
  height: 48px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  color: #4f6df5;
  background: rgba(255, 255, 255, 0.52);
  box-shadow: 0 14px 30px rgba(79, 109, 245, 0.09);
}

.feature-icon .el-icon {
  font-size: 24px;
}

.feature-item[data-tone="indigo"] .feature-icon {
  color: #5578f0;
}

.feature-item[data-tone="cyan"] .feature-icon {
  color: #38c9d7;
}

.feature-label {
  color: #354056;
  font-size: 14px;
  font-weight: 600;
}

html.dark .site-name {
  color: #edf2ff;
}

html.dark .bottom-hint,
html.dark .feature-label {
  color: #c7d2fe;
}

html.dark .feature-icon {
  background: rgba(30, 41, 59, 0.58);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.22);
}

@keyframes contentFadeIn {
  from {
    opacity: 0;
    transform: translateX(-28px);
  }
  to {
    opacity: 1;
    transform: none;
  }
}

@media (max-width: 1200px) {
  .site-name {
    font-size: 54px;
  }

  .feature-list {
    gap: 42px;
  }
}

@media (max-width: 1024px) {
  .intro-section {
    padding-right: 24px;
  }

  .site-name {
    font-size: 48px;
  }

  .feature-list {
    margin-top: 36px;
  }
}

@media (max-width: 768px) {
  .intro-section {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .intro-content {
    animation: none;
    opacity: 1;
  }
}
</style>
