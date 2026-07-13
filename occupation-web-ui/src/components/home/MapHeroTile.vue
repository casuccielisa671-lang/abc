<template>
  <div class="map-hero" @click="openMap">
    <div class="map-stage">
      <div class="map-grid"></div>
      <div class="map-top">
        <div class="map-title">全国岗位分布<small>3D 城市热力 · 点击进入互动大图</small></div>
      </div>
      <span v-for="d in mapDots" :key="d.name" class="dot"
            :style="{ left: d.x + '%', top: d.y + '%', width: d.s + 'px', height: d.s + 'px' }">
        <span class="core"></span><span class="lab">{{ d.name }}</span>
      </span>
      <div class="map-cta"><el-button type="primary" size="small">进入大图 ↗</el-button></div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()
const mapDots = [
  { name: '北京', x: 66, y: 34, s: 26 }, { name: '上海', x: 74, y: 50, s: 30 },
  { name: '深圳', x: 71, y: 70, s: 24 }, { name: '杭州', x: 69, y: 57, s: 20 },
  { name: '成都', x: 48, y: 55, s: 16 }, { name: '武汉', x: 58, y: 52, s: 15 }
]

function openMap() {
  const rp = { STUDENT: 'student', TEACHER: 'teacher', HR: 'hr', ADMIN: 'admin' }[userStore.role] || 'student'
  router.push(`/${rp}/map`)
}
</script>

<style scoped>
.map-hero { padding: 0; border: none; border-radius: 14px; overflow: hidden; cursor: pointer;
  transition: transform .15s, box-shadow .15s; box-shadow: var(--shadow-sm); height: 100%; }
.map-hero:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
.map-stage { height: 100%; border-radius: 14px; position: relative; overflow: hidden;
  background: radial-gradient(600px 300px at 70% 30%, rgba(43,107,251,.20), transparent 60%), linear-gradient(160deg,#0c1526,#101d38 60%,#0b1830); }
.map-grid { position: absolute; inset: 0; background-image: linear-gradient(rgba(120,160,255,.06) 1px, transparent 1px), linear-gradient(90deg, rgba(120,160,255,.06) 1px, transparent 1px); background-size: 26px 26px; }
.map-top { position: absolute; top: 14px; left: 16px; z-index: 3; }
.map-title { color: #eaf1ff; font-weight: 650; font-size: 14px; }
.map-title small { display: block; color: #9fb4de; font-size: 11.5px; font-weight: 500; margin-top: 2px; }
.dot { position: absolute; border-radius: 50%; transform: translate(-50%,-50%); z-index: 2; }
.dot .core { display: block; width: 100%; height: 100%; border-radius: 50%; background: radial-gradient(circle,#ffd36b,#ff8a3d 60%,rgba(255,120,50,0) 72%); }
.dot .lab { position: absolute; left: 50%; top: 100%; transform: translateX(-50%); margin-top: 3px; white-space: nowrap; font-size: 10.5px; color: #dfe8ff; text-shadow: 0 1px 3px #000; }
.map-cta { position: absolute; bottom: 12px; right: 16px; z-index: 3; }
</style>
