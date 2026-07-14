<template>
  <canvas ref="canvasRef" class="water-ripple-canvas" />
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useAppStore } from '@/store/app'

const appStore = useAppStore()
const canvasRef = ref(null)

let canvasW = 0
let canvasH = 0
let dpr = 1

// ==================== 涟漪类 ====================
class Ripple {
  constructor(cx, cy, maxR, isDark) {
    this.cx = cx
    this.cy = cy
    this.maxR = maxR
    this.isDark = isDark
    this.reset()
  }

  reset() {
    // 5-8 层同心圆环，模拟真实水面
    const count = 5 + Math.floor(Math.random() * 4)
    this.rings = []
    for (let i = 0; i < count; i++) {
      this.rings.push({
        r: 2 + Math.random() * 5,
        speed: 0.3 + Math.random() * 0.5,
        maxR: this.maxR * (0.4 + i * 0.12),
        alpha: 0.15 + Math.random() * 0.12,
        width: 0.8 + Math.random() * 1.2
      })
    }
    this.life = 400 + Math.random() * 300
    this.age = 0
  }

  update() {
    this.age++
    for (const ring of this.rings) {
      if (ring.r < ring.maxR) {
        ring.r += ring.speed
      }
      // 平滑淡出
      const fadeStart = this.life * 0.35
      if (this.age > fadeStart) {
        const p = (this.age - fadeStart) / (this.life - fadeStart)
        ring.alpha = Math.max(0, ring.alpha * (1 - p * p * 0.85))
      }
    }
    return this.age >= this.life
  }

  draw(ctx) {
    for (const ring of this.rings) {
      if (ring.alpha <= 0.004 || ring.r <= 0) continue

      const r = ring.r
      const w = ring.width
      const a = ring.alpha

      // 画细线圆环（stroke），模拟水面波纹
      ctx.beginPath()
      ctx.arc(this.cx, this.cy, r, 0, Math.PI * 2)

      if (this.isDark) {
        // 深色模式：淡蓝白波纹
        ctx.strokeStyle = `rgba(180, 210, 255, ${a})`
      } else {
        // 浅色模式：深蓝波纹
        ctx.strokeStyle = `rgba(30, 80, 170, ${a})`
      }
      ctx.lineWidth = w
      ctx.stroke()
    }
  }
}

// ==================== 动画状态 ====================
let ripples = []
let animationId = null
let spawnTimer = 0
let isDark = false

function resize() {
  const canvas = canvasRef.value
  if (!canvas) return
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  canvasW = window.innerWidth
  canvasH = window.innerHeight
  canvas.width = canvasW * dpr
  canvas.height = canvasH * dpr
  canvas.style.width = canvasW + 'px'
  canvas.style.height = canvasH + 'px'
  const ctx = canvas.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
}

function spawnRipple() {
  if (canvasW <= 0 || canvasH <= 0) return
  const margin = 150
  const cx = margin + Math.random() * (canvasW - margin * 2)
  const cy = margin + Math.random() * (canvasH - margin * 2)
  // 涟漪覆盖更大面积
  const maxR = Math.min(canvasW, canvasH) * (0.5 + Math.random() * 0.35)
  ripples.push(new Ripple(cx, cy, maxR, isDark))
}

function animate() {
  const canvas = canvasRef.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')

  // 完全清空
  ctx.clearRect(0, 0, canvasW, canvasH)

  // 生成新涟漪：每 2-4 秒一个
  spawnTimer++
  const spawnInterval = 120 + Math.floor(Math.random() * 120)
  if (spawnTimer >= spawnInterval) {
    spawnTimer = 0
    if (ripples.length < 4) {
      spawnRipple()
    }
  }

  ripples = ripples.filter(r => !r.update())
  ripples.forEach(r => r.draw(ctx))

  animationId = requestAnimationFrame(animate)
}

watch(() => appStore.dark, (val) => {
  isDark = val
  ripples = []
  spawnTimer = 0
})

onMounted(() => {
  isDark = appStore.dark
  resize()
  spawnRipple()
  setTimeout(() => spawnRipple(), 800)
  animate()
  window.addEventListener('resize', resize)
})

onUnmounted(() => {
  window.removeEventListener('resize', resize)
  if (animationId) cancelAnimationFrame(animationId)
})
</script>

<style scoped>
.water-ripple-canvas {
  position: fixed;
  top: 0;
  left: 0;
  pointer-events: none;
  z-index: 2;
}
</style>
