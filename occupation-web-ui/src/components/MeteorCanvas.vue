<template>
  <canvas
    ref="canvasRef"
    class="meteor-canvas"
    :width="width"
    :height="height"
  />
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useAppStore } from '@/store/app'

const canvasRef = ref(null)
const width = ref(0)
const height = ref(0)
const appStore = useAppStore()

class Meteor {
  constructor(width, height, isDark) {
    this.width = width
    this.height = height
    this.isDark = isDark
    this.reset()
  }

  reset() {
    this.x = Math.random() * this.width
    this.y = -10
    this.vx = (Math.random() - 0.5) * 4
    this.vy = Math.random() * 3 + 2
    this.length = Math.random() * 80 + 40
    this.opacity = Math.random() * 0.5 + 0.5
    this.color = this.isDark
      ? `rgba(100, 180, 255, ${this.opacity})`
      : `rgba(139, 92, 246, ${this.opacity})`
    this.trailOpacity = this.opacity * 0.3
  }

  update() {
    this.x += this.vx
    this.y += this.vy
    this.opacity *= 0.96  // 更快消退
  }

  draw(ctx) {
    ctx.save()
    ctx.strokeStyle = this.color
    ctx.lineWidth = 2.5  // 加粗流星线
    ctx.globalAlpha = this.opacity
    ctx.beginPath()
    ctx.moveTo(this.x, this.y)
    ctx.lineTo(
      this.x - this.vx * (this.length / Math.sqrt(this.vx ** 2 + this.vy ** 2)),
      this.y - this.vy * (this.length / Math.sqrt(this.vx ** 2 + this.vy ** 2))
    )
    ctx.stroke()

    // 增强光晕效果
    ctx.strokeStyle = this.color.replace(/[\d.]+\)/, `${this.trailOpacity * 1.5})`)
    ctx.lineWidth = 4
    ctx.lineCap = 'round'
    ctx.beginPath()
    ctx.moveTo(this.x, this.y)
    ctx.lineTo(this.x - this.vx * 40, this.y - this.vy * 40)
    ctx.stroke()

    // 流星头部光圈
    ctx.fillStyle = this.color.replace(/[\d.]+\)/, `${this.opacity * 0.8})`)
    ctx.beginPath()
    ctx.arc(this.x, this.y, 3, 0, Math.PI * 2)
    ctx.fill()

    ctx.restore()
  }

  isOutOfBounds() {
    return this.y > this.height + 50 || this.x < -100 || this.x > this.width + 100 || this.opacity < 0.02
  }
}

let meteors = []
let animationId = null
let lastMeteorTime = 0

function updateCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return

  const ctx = canvas.getContext('2d')
  const isDark = appStore.dark

  ctx.fillStyle = 'transparent'
  ctx.fillRect(0, 0, width.value, height.value)

  const now = Date.now()
  if (now - lastMeteorTime > 1500) {
    meteors.push(new Meteor(width.value, height.value, isDark))
    lastMeteorTime = now
  }

  meteors = meteors.filter(m => !m.isOutOfBounds())

  meteors.forEach(meteor => {
    meteor.update()
    meteor.draw(ctx)
  })

  animationId = requestAnimationFrame(updateCanvas)
}

function resizeCanvas() {
  const canvas = canvasRef.value
  if (!canvas) return

  const rect = canvas.parentElement.getBoundingClientRect()
  width.value = rect.width
  height.value = rect.height

  canvas.width = width.value
  canvas.height = height.value
}

onMounted(() => {
  resizeCanvas()
  updateCanvas()
  window.addEventListener('resize', resizeCanvas)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeCanvas)
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
})
</script>

<style scoped>
.meteor-canvas {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}
</style>
