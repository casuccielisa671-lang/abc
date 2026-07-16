<template>
  <div class="app-page-background" aria-hidden="true">
    <span class="wash wash--blue" />
    <span class="wash wash--purple" />
    <span class="wash wash--cyan" />

    <svg class="orbit-field" viewBox="0 0 520 420" preserveAspectRatio="xMidYMid meet">
      <g class="orbit-lines">
        <circle cx="250" cy="160" r="86" />
        <circle cx="250" cy="160" r="126" />
        <circle cx="250" cy="160" r="168" />
      </g>
      <g class="orbit-dots">
        <circle v-for="dot in orbitDots" :key="dot.key" :cx="dot.x" :cy="dot.y" :r="dot.r" />
      </g>
    </svg>

    <svg class="wave-field" viewBox="0 0 1600 420" preserveAspectRatio="none">
      <g class="wave-lines">
        <path
          v-for="line in waveLines"
          :key="line"
          :d="`M -120 ${line} C 200 ${line - 118}, 430 ${line + 58}, 680 ${line - 28} C 930 ${line - 112}, 1110 ${line + 48}, 1360 ${line - 22} S 1620 ${line - 70}, 1720 ${line - 20}`"
        />
      </g>
      <g class="wave-dots">
        <circle v-for="dot in waveDots" :key="dot.key" :cx="dot.x" :cy="dot.y" :r="dot.r" />
      </g>
    </svg>
  </div>
</template>

<script setup>
const orbitDots = Array.from({ length: 96 }, (_, index) => {
  const ring = index % 3
  const radius = [88, 126, 168][ring]
  const angle = (index / 96) * Math.PI * 2 + ring * 0.42
  return {
    key: index,
    x: 250 + Math.cos(angle) * radius,
    y: 160 + Math.sin(angle) * radius,
    r: ring === 0 ? 1.45 : 1.05
  }
})

const waveLines = [202, 222, 242, 262, 282, 302, 322, 342, 362, 382, 402]

const waveDots = Array.from({ length: 118 }, (_, index) => ({
  key: index,
  x: -20 + (index % 59) * 29,
  y: 292 + Math.floor(index / 59) * 58 + Math.sin(index * 0.34) * 52 + (index % 6) * 7,
  r: index % 8 === 0 ? 1.25 : 0.9
}))
</script>

<style scoped>
.app-page-background {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(245, 249, 255, 0.8) 46%, rgba(232, 246, 255, 0.72)),
    radial-gradient(ellipse at 36% 22%, rgba(255, 255, 255, 0.9), transparent 48%),
    linear-gradient(135deg, #f8fbff 0%, #eef6ff 48%, #edf9ff 100%);
}

.app-page-background::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 75% 8%, rgba(99, 151, 255, 0.2), transparent 20%),
    radial-gradient(circle at 92% 36%, rgba(168, 85, 247, 0.18), transparent 22%),
    linear-gradient(90deg, rgba(233, 242, 255, 0.68), transparent 34%, transparent 68%, rgba(225, 237, 255, 0.6));
}

.wash {
  position: absolute;
  border-radius: 9999px;
  filter: blur(8px);
  animation: backgroundBreath 14s ease-in-out infinite;
  will-change: transform, opacity;
}

.wash--blue {
  width: 560px;
  height: 560px;
  top: -270px;
  right: 150px;
  background: radial-gradient(circle, rgba(147, 197, 253, 0.48), rgba(191, 219, 254, 0.18) 48%, transparent 70%);
}

.wash--purple {
  width: 520px;
  height: 520px;
  top: 160px;
  right: -180px;
  background: radial-gradient(circle, rgba(196, 181, 253, 0.46), rgba(216, 180, 254, 0.16) 50%, transparent 72%);
  animation-delay: -5s;
}

.wash--cyan {
  width: 420px;
  height: 420px;
  left: -160px;
  bottom: -130px;
  background: radial-gradient(circle, rgba(125, 211, 252, 0.22), rgba(186, 230, 253, 0.12) 52%, transparent 72%);
  animation-delay: -8s;
}

.orbit-field {
  position: absolute;
  top: -34px;
  right: 80px;
  width: 520px;
  height: 420px;
  opacity: 0.62;
  animation: orbitDrift 18s ease-in-out infinite;
}

.orbit-lines {
  fill: none;
  stroke: rgba(96, 165, 250, 0.24);
  stroke-dasharray: 5 8;
  stroke-width: 1;
}

.orbit-lines circle:nth-child(2) {
  stroke: rgba(129, 140, 248, 0.2);
}

.orbit-lines circle:nth-child(3) {
  stroke: rgba(168, 85, 247, 0.16);
}

.orbit-dots {
  fill: rgba(255, 255, 255, 0.72);
}

.wave-field {
  position: absolute;
  left: -3%;
  right: -3%;
  bottom: -36px;
  width: 106%;
  height: 34%;
  opacity: 0.68;
  transform-origin: center bottom;
  animation: waveBreath 13s ease-in-out infinite;
}

.wave-lines {
  fill: none;
  stroke-linecap: round;
  stroke-width: 0.95;
}

.wave-lines path:nth-child(odd) {
  stroke: rgba(79, 140, 245, 0.28);
}

.wave-lines path:nth-child(even) {
  stroke: rgba(14, 189, 218, 0.2);
}

.wave-dots {
  fill: rgba(99, 102, 241, 0.18);
}

html.dark .app-page-background {
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.94), rgba(17, 24, 39, 0.88) 48%, rgba(30, 41, 59, 0.82)),
    radial-gradient(ellipse at 36% 22%, rgba(30, 41, 59, 0.72), transparent 48%),
    linear-gradient(135deg, #0f172a 0%, #111827 48%, #1e1b4b 100%);
}

html.dark .app-page-background::before {
  background:
    radial-gradient(circle at 75% 8%, rgba(59, 130, 246, 0.16), transparent 20%),
    radial-gradient(circle at 92% 36%, rgba(124, 58, 237, 0.18), transparent 22%),
    linear-gradient(90deg, rgba(30, 64, 175, 0.1), transparent 34%, transparent 68%, rgba(91, 33, 182, 0.12));
}

html.dark .orbit-field,
html.dark .wave-field {
  opacity: 0.36;
}

@keyframes backgroundBreath {
  0%,
  100% {
    opacity: 0.76;
    transform: translate3d(0, 0, 0) scale(1);
  }

  50% {
    opacity: 0.92;
    transform: translate3d(12px, 8px, 0) scale(1.035);
  }
}

@keyframes orbitDrift {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }

  50% {
    transform: translate3d(-16px, 10px, 0);
  }
}

@keyframes waveBreath {
  0%,
  100% {
    opacity: 0.58;
    transform: translateY(0) scale(1);
  }

  50% {
    opacity: 0.78;
    transform: translateY(-8px) scale(1.012);
  }
}

@media (max-width: 768px) {
  .orbit-field {
    right: -150px;
    opacity: 0.42;
  }

  .wave-field {
    height: 26%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .wash,
  .orbit-field,
  .wave-field {
    animation: none;
  }
}
</style>
