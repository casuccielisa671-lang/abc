<template>
  <div class="role-home">
    <section class="welcome">
      <div class="hi">
        你好，{{ displayName }} 👋
        <small>教师工作台 · 数据按你的可见范围统计</small>
      </div>
      <el-button type="primary" @click="go('/teacher/students')">学生管理</el-button>
    </section>

    <section class="bento">
      <MapHeroTile class="t-map" />

      <div class="tile kpi clickable" @click="go('/teacher/students')">
        <div class="eyebrow">学生总数</div>
        <div class="big">{{ ov.totalStudents }}</div>
        <div class="mini-sub">可见范围内学生</div>
      </div>
      <div class="tile kpi clickable" @click="go('/teacher/students')">
        <div class="eyebrow">已填画像</div>
        <div class="big">{{ ov.withProfile }}<small> / {{ ov.totalStudents }}</small></div>
        <div class="mini-sub">完善率 {{ profileRate }}%</div>
      </div>
      <div class="tile kpi">
        <div class="eyebrow">累计投递</div>
        <div class="big">{{ ov.totalApplies }}</div>
        <div class="mini-sub">学生站内投递</div>
      </div>
      <div class="tile kpi">
        <div class="eyebrow">累计浏览</div>
        <div class="big">{{ ov.totalViews }}</div>
        <div class="mini-sub">职位浏览次数</div>
      </div>

      <!-- 技能缺口 Top -->
      <div class="tile t-gap">
        <div class="tile-h">
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
        <div class="tile-h"><span class="t">快捷入口</span></div>
        <div class="quick-grid">
          <div class="qa" @click="go('/teacher/students')">学生管理</div>
          <div class="qa" @click="go('/teacher/suggestions')">教学建议</div>
          <div class="qa" @click="go('/teacher/students')">就业数据</div>
          <div class="qa" @click="exportData">导出 Excel</div>
        </div>
      </div>

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

const router = useRouter()
const userStore = useUserStore()
function go(p) { router.push(p) }

const ov = ref({ totalStudents: 0, withProfile: 0, totalViews: 0, totalApplies: 0 })
const gaps = ref([])
const gapLoading = ref(false)

const displayName = computed(() => userStore.realName || userStore.username || '老师')
const profileRate = computed(() => ov.value.totalStudents ? Math.round(ov.value.withProfile / ov.value.totalStudents * 100) : 0)
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

<style scoped>
.role-home { max-width: 1200px; margin: 0 auto; }
.welcome { display: flex; align-items: center; gap: 18px; flex-wrap: wrap; padding: 16px 20px; border-radius: 14px; margin-bottom: 16px;
  background: linear-gradient(100deg, color-mix(in srgb, var(--color-primary) 12%, var(--color-surface)), var(--color-surface)); border: 1px solid var(--color-border); }
.welcome .hi { font-size: 19px; font-weight: 700; }
.welcome .hi small { display: block; font-size: 12.5px; font-weight: 500; color: var(--color-text-tertiary); margin-top: 3px; }
.welcome > .el-button { margin-left: auto; }

.bento { display: grid; grid-template-columns: repeat(4, 1fr); grid-auto-rows: 158px; gap: 14px; }
.tile { background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 14px; padding: 16px; overflow: hidden; display: flex; flex-direction: column; box-shadow: var(--shadow-sm); }
.clickable { cursor: pointer; transition: transform .15s, box-shadow .15s, border-color .15s; }
.clickable:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); border-color: color-mix(in srgb, var(--color-primary) 40%, var(--color-border)); }
.tile-h { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.tile-h .t { font-size: 14px; font-weight: 650; }
.tile-h .a { font-size: 12px; color: var(--color-primary); cursor: pointer; }
.eyebrow { font-size: 11px; letter-spacing: .4px; text-transform: uppercase; color: var(--color-text-tertiary); margin-bottom: 6px; }

.t-map { grid-column: span 2; grid-row: span 2; }
.t-gap { grid-column: span 2; grid-row: span 2; }
.t-quick { grid-column: span 2; grid-row: span 2; }
.t-news { grid-column: span 4; grid-row: span 2; background: var(--color-surface); border: 1px solid var(--color-border); border-radius: 14px; padding: 16px; box-shadow: var(--shadow-sm); }

.kpi .big { font-size: 30px; font-weight: 750; letter-spacing: -.5px; margin-top: auto; }
.kpi .big small { font-size: 14px; font-weight: 500; color: var(--color-text-tertiary); }
.mini-sub { font-size: 12px; color: var(--color-text-tertiary); margin-top: 4px; }

/* 技能缺口 */
.gap-list { flex: 1; display: flex; flex-direction: column; gap: 10px; overflow: hidden; }
.gap { display: grid; grid-template-columns: 68px 1fr 56px; align-items: center; gap: 10px; }
.gap-name { font-size: 13px; font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.gap-bars { display: flex; flex-direction: column; gap: 3px; }
.bar { height: 6px; border-radius: 4px; background: var(--color-bg-tertiary); overflow: hidden; }
.fill { display: block; height: 100%; border-radius: 4px; }
.fill.market { background: var(--color-primary); }
.fill.mastery { background: var(--color-warning); }
.gap-num { font-size: 12px; color: var(--color-warning); font-weight: 600; text-align: right; }
.gap-legend { display: flex; gap: 16px; font-size: 11.5px; color: var(--color-text-tertiary); margin-top: 10px; }
.gap-legend i { display: inline-block; width: 10px; height: 6px; border-radius: 3px; margin-right: 5px; vertical-align: middle; }
.gap-legend i.market { background: var(--color-primary); }
.gap-legend i.mastery { background: var(--color-warning); }

/* 快捷入口 */
.quick-grid { flex: 1; display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.qa { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; border-radius: 11px;
  background: var(--color-bg-secondary); border: 1px solid var(--color-border); cursor: pointer; font-size: 13px; font-weight: 600; transition: all .15s; }
.qa:hover { border-color: var(--color-primary); color: var(--color-primary); transform: translateY(-1px); }
.qi { font-size: 20px; }

@media (max-width: 900px) {
  .bento { grid-template-columns: repeat(2, 1fr); }
  .t-map, .t-gap, .t-quick { grid-column: span 2; }
}
</style>
