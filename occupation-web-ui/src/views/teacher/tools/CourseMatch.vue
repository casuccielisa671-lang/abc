<template>
  <div class="tool-page">
    <div class="tool-page-header">
      <el-button text @click="$router.push('/teacher/tools')">
        <el-icon><ArrowLeft /></el-icon> 返回工具箱
      </el-button>
      <h2>课程-岗位匹配</h2>
      <p class="tool-desc">输入课程名称，查看关联岗位的市场需求与技能变化趋势，辅助课程改革决策</p>
    </div>

    <div class="search-bar">
      <el-input v-model="courseName" placeholder="输入课程名称，如：Java程序设计、数据结构、机器学习" size="large" clearable @keyup.enter="search">
        <template #append><el-button type="primary" @click="search" :loading="loading">分析</el-button></template>
      </el-input>
    </div>

    <template v-if="result">
      <div class="section">
        <h3 class="section-title">关联岗位</h3>
        <div class="job-tags">
          <div v-for="job in result.relatedJobs" :key="job.name" class="job-tag">
            <span class="jt-name">{{ job.name }}</span>
            <span class="jt-relevance">相关度 {{ job.relevance }}%</span>
          </div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">市场需求趋势（近 12 个月）</h3>
        <div class="trend-chart">
          <div v-for="(v, i) in result.trend" :key="i" class="trend-col">
            <div class="trend-bar" :style="{ height: (v / maxTrend) * 100 + '%' }"></div>
            <div class="trend-label">{{ result.months[i] }}</div>
          </div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">技能要求变化</h3>
        <div class="skill-changes">
          <div v-for="s in result.skillChanges" :key="s.name" class="sc-item">
            <div class="sc-left">
              <span class="sc-name">{{ s.name }}</span>
              <span class="sc-dir" :class="s.trend === 'up' ? 'up' : 'down'">
                {{ s.trend === 'up' ? '↑' : '↓' }} {{ s.change }}%
              </span>
            </div>
            <div class="sc-bar-track"><div class="sc-bar-fill" :style="{ width: s.current + '%' }"></div></div>
          </div>
        </div>
      </div>

      <div class="section">
        <h3 class="section-title">教学建议</h3>
        <div class="suggestions">
          <div v-for="(s, i) in result.suggestions" :key="i" class="sg-item">
            <span class="sg-idx">{{ i + 1 }}</span>
            <span>{{ s }}</span>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="empty">
      <el-icon :size="48"><Connection /></el-icon>
      <p>输入课程名称，点击"分析"查看岗位匹配结果</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ArrowLeft, Connection } from '@element-plus/icons-vue'
import { courseMatch } from '@/api/student'

const courseName = ref('')
const loading = ref(false)
const result = ref(null)

const maxTrend = computed(() => result.value ? Math.max(...result.value.trend) : 1)

const search = async () => {
  if (!courseName.value.trim()) return
  loading.value = true
  try { result.value = await courseMatch(courseName.value.trim()) } catch { result.value = null }
  loading.value = false
}
</script>

<style scoped>
.tool-page { max-width: 1000px; margin: 0 auto; padding: 16px 0 40px; }
.tool-page-header { margin-bottom: 24px; }
.tool-page-header h2 { font-size: 22px; font-weight: 600; color: var(--app-ink); margin: 12px 0 6px; }
.tool-desc { font-size: 14px; color: var(--app-ink-3); margin: 0; }

.search-bar { margin-bottom: 32px; max-width: 600px; }

.section { margin-bottom: 32px; }
.section-title { font-size: 16px; font-weight: 600; color: var(--app-ink); margin: 0 0 16px; }

.job-tags { display: flex; flex-wrap: wrap; gap: 10px; }
.job-tag {
  display: flex; align-items: center; gap: 8px;
  background: #fff; border: 1px solid #e8eaed; border-radius: 8px; padding: 10px 16px;
}
html.dark .job-tag { background: #1e1f22; border-color: #2e3035; }
.jt-name { font-size: 14px; font-weight: 600; }
.jt-relevance { font-size: 12px; color: #5470c6; }

.trend-chart { display: flex; align-items: flex-end; gap: 6px; height: 180px; padding: 0 4px; }
.trend-col { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; }
.trend-bar { width: 100%; max-width: 32px; background: #5470c6; border-radius: 4px 4px 0 0; min-height: 4px; transition: height 0.6s ease; }
.trend-label { font-size: 11px; color: var(--app-ink-3); margin-top: 6px; }

.skill-changes { display: flex; flex-direction: column; gap: 12px; }
.sc-item { display: flex; align-items: center; gap: 16px; }
.sc-left { display: flex; align-items: center; gap: 8px; width: 160px; flex-shrink: 0; }
.sc-name { font-size: 13px; font-weight: 500; }
.sc-dir { font-size: 12px; font-weight: 600; }
.sc-dir.up { color: #27ae60; }
.sc-dir.down { color: #e74c3c; }
.sc-bar-track { flex: 1; height: 8px; background: #f1f3f4; border-radius: 4px; overflow: hidden; }
html.dark .sc-bar-track { background: #2e3035; }
.sc-bar-fill { height: 100%; background: #5470c6; border-radius: 4px; transition: width 0.6s ease; }

.suggestions { background: #fff; border: 1px solid #e8eaed; border-radius: 10px; padding: 20px; display: flex; flex-direction: column; gap: 12px; }
html.dark .suggestions { background: #1e1f22; border-color: #2e3035; }
.sg-item { display: flex; align-items: flex-start; gap: 10px; font-size: 14px; color: var(--app-ink-2); line-height: 1.6; }
.sg-idx { width: 22px; height: 22px; border-radius: 50%; background: #f1f3f4; font-size: 12px; font-weight: 600; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
html.dark .sg-idx { background: #2e3035; }

.empty { text-align: center; padding: 80px 0; color: var(--app-ink-3); }
.empty p { margin-top: 16px; }
</style>
