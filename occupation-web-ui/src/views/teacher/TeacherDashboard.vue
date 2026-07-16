<template>
  <div class="role-home teacher-home">
    <section class="welcome">
      <div class="hi">
        <span class="welcome-badge">教师驾驶舱</span>
        你好，{{ displayName }}
        <small>教师工作台 · 数据按你的可见范围统计</small>
      </div>
      <el-button type="primary" @click="go('/teacher/students')">学生管理</el-button>
    </section>

    <section class="bento">
      <MapHeroTile class="t-map" />

      <div class="tile kpi clickable" @click="go('/teacher/students')">
        <span class="metric-icon student">生</span>
        <div>
          <div class="eyebrow">学生总数</div>
          <div class="big">{{ ov.totalStudents }}</div>
          <div class="mini-sub">可见范围内学生</div>
        </div>
      </div>
      <div class="tile kpi clickable" @click="go('/teacher/students')">
        <span class="metric-icon profile">像</span>
        <div>
          <div class="eyebrow">已填画像</div>
          <div class="big">{{ ov.withProfile }}<small> / {{ ov.totalStudents }}</small></div>
          <div class="mini-sub">完善率 {{ profileRate }}%</div>
        </div>
      </div>
      <div class="tile kpi">
        <span class="metric-icon apply">投</span>
        <div>
          <div class="eyebrow">累计投递</div>
          <div class="big">{{ ov.totalApplies }}</div>
          <div class="mini-sub">学生站内投递</div>
        </div>
      </div>
      <div class="tile kpi">
        <span class="metric-icon employed">就</span>
        <div>
          <div class="eyebrow">已就业</div>
          <div class="big">{{ ov.employedCount }}<small> / {{ ov.totalStudents }}</small></div>
          <div class="mini-sub">就业率 {{ employmentRate }}%</div>
        </div>
      </div>

      <!-- 技能缺口 Top -->
      <div class="tile t-gap">
        <div class="tile-h section-title">
          <span class="t">技能缺口 Top</span>
          <span class="a" @click="go('/teacher/suggestions')">教学建议 ›</span>
        </div>
        <div v-loading="gapLoading" class="gap-list">
          <div v-for="g in gapsTop" :key="g.skill" class="gap">
            <div class="gap-name">{{ g.skill }}</div>
            <div class="gap-bars">
              <div class="bar"><span class="fill market" :style="{ width: g.marketDemand + '%' }"></span></div>
              <div class="bar"><span class="fill mastery" :style="{ width: g.studentRate + '%' }"></span></div>
            </div>
            <div class="gap-num">缺口 {{ g.gap }}</div>
          </div>
          <el-empty v-if="!gapLoading && !gapsTop.length" description="暂无明显技能缺口" :image-size="50" />
        </div>
        <div v-if="gapsTop.length" class="gap-legend">
          <span><i class="market"></i>市场热度</span><span><i class="mastery"></i>学生掌握率</span>
        </div>
      </div>

      <!-- 快捷入口 -->
      <div class="tile t-quick">
        <div class="tile-h section-title"><span class="t">快捷入口</span></div>
        <div class="quick-grid">
          <div class="qa" @click="go('/teacher/students')">学生管理</div>
          <div class="qa" @click="go('/teacher/suggestions')">教学建议</div>
          <div class="qa" @click="go('/teacher/students')">就业数据</div>
          <div class="qa" @click="exportData">导出 Excel</div>
        </div>
      </div>

      <div class="tile t-msg"><MessageTile /></div>

      <NewsTile class="t-news" />
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getTeacherOverview, getTeacherSuggestions } from '@/api/student'
import MapHeroTile from '@/components/home/MapHeroTile.vue'
import NewsTile from '@/components/home/NewsTile.vue'
import MessageTile from '@/components/home/MessageTile.vue'

const router = useRouter()
const userStore = useUserStore()
function go(p) { router.push(p) }

const ov = ref({ totalStudents: 0, withProfile: 0, totalViews: 0, totalApplies: 0, employedCount: 0 })
const gaps = ref([])
const gapLoading = ref(false)

const displayName = computed(() => userStore.realName || userStore.username || '老师')
const profileRate = computed(() => ov.value.totalStudents ? Math.round(ov.value.withProfile / ov.value.totalStudents * 100) : 0)
const employmentRate = computed(() => ov.value.totalStudents ? Math.round(ov.value.employedCount / ov.value.totalStudents * 100) : 0)
const gapsTop = computed(() => gaps.value.slice(0, 4))

function exportData() {
  // 导出走带鉴权的下载，交给学生管理页的导出按钮；此处直接跳转
  go('/teacher/students')
}

onMounted(() => {
  getTeacherOverview().then(d => { if (d) ov.value = d }).catch(() => {})
  gapLoading.value = true
  getTeacherSuggestions().then(d => { gaps.value = d?.skillGaps || [] }).catch(() => {}).finally(() => { gapLoading.value = false })
})
</script>

<style src="./TeacherDashboard.css"></style>
