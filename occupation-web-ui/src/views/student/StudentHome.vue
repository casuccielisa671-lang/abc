<template>
  <div class="student-home">
    <div class="page-head">
      <h2 class="page-title">职位推荐</h2>
      <p class="page-sub">根据你的画像智能匹配，得分越高越适合你</p>
    </div>

    <div v-loading="loading">
      <el-empty v-if="!loading && !list.length" description="暂无推荐职位，请先完善个人画像" />

      <!-- ============ 可投递岗位：企业在本平台发布，投递后 HR 能看到你的简历 ============ -->
      <template v-if="applicable.length">
        <div class="section-head">
          <h3 class="section-title">可投递岗位</h3>
          <span class="section-sub">企业在本平台发布，投递后可在「我的投递」看到处理进度</span>
        </div>
        <div class="job-grid">
          <JobCard
            v-for="item in applicable" :key="item.job.id" :item="item"
            @open="goDetail(item.job.id)"
          >
            <el-button type="primary" text size="small" @click.stop="goDetail(item.job.id)">
              查看详情 →
            </el-button>
          </JobCard>
        </div>
      </template>

      <!-- ============ 市场参考：采集来的岗位，平台上没有招聘方 ============ -->
      <template v-if="reference.length">
        <div class="section-head" :class="{ spaced: applicable.length }">
          <h3 class="section-title">市场参考</h3>
          <span class="section-sub">
            采集自外部招聘渠道，本平台没有对应的招聘方。可自主联系，也用于技能热度与薪资趋势统计
          </span>
        </div>
        <div class="job-grid">
          <JobCard
            v-for="item in reference" :key="item.job.id" :item="item"
            @open="goDetail(item.job.id)"
          >
            <el-button
              text size="small" :loading="contacting === item.job.id" :disabled="empStore.employed"
              @click.stop="handleContact(item.job)"
            >{{ empStore.employed ? '已入职' : '自主联系' }}</el-button>
          </JobCard>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getRecommend, contactJob } from '@/api/student'
import JobCard from '@/components/JobCard.vue'
import { toList } from '@/utils/list'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useEmploymentStore } from '@/store/employment'

const empStore = useEmploymentStore()

/** 取多一些：可投递岗位远少于采集岗位，取 20 条常常一条可投的都排不进来 */
const TOP_N = 30

const router = useRouter()
const list = ref([])
const loading = ref(false)
const contacting = ref(null)

const applicable = computed(() => list.value.filter(i => i.job?.applicable))
const reference = computed(() => list.value.filter(i => !i.job?.applicable))

function goDetail(id) {
  router.push(`/student/job/${id}`)
}

/**
 * 自主联系：记录求职意向，并在有真实链接时打开原招聘页面。
 *
 * 平台只能记录「你表达过意向」—— 之后你在原网站上做了什么，我们无从得知，
 * 所以这里没有投递那样的状态跟踪。这个意向会计入教师端的自主求职统计。
 */
async function handleContact(job) {
  try {
    await ElMessageBox.confirm(
      `将记录你对「${job.company} · ${job.title}」的求职意向，并为你打开原招聘页面（如果有）。`,
      '自主联系',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

  contacting.value = job.id
  try {
    const { sourceUrl } = await contactJob(job.id)
    if (sourceUrl) {
      // noopener：防止新页面通过 window.opener 反向操作本页
      window.open(sourceUrl, '_blank', 'noopener,noreferrer')
      ElMessage.success('已记录求职意向，正在为你打开原招聘页面')
    } else {
      ElMessage.success('已记录求职意向。该职位为模拟数据，没有可跳转的外部页面')
    }
    // 已联系过的职位不再出现在推荐里，重新拉一次列表
    await load()
  } catch {
    /* 拦截器已提示 */
  } finally {
    contacting.value = null
  }
}

async function load() {
  loading.value = true
  try {
    // 未填画像时后端抛业务异常，拦截器已提示，这里保持空列表并展示引导文案
    list.value = toList(await getRecommend(TOP_N))
  } catch {
    list.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => { load(); empStore.refresh() })
</script>

<style scoped>
.section-head { display: flex; align-items: baseline; gap: 12px; margin: 0 0 14px; flex-wrap: wrap; }
.section-head.spaced { margin-top: 32px; }
.section-title { font-size: 16px; font-weight: 600; color: var(--app-ink); margin: 0; }
.section-sub { font-size: 12px; color: var(--app-ink-3); }
</style>
