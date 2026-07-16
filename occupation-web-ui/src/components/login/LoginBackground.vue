<template>
  <div class="login-background" aria-hidden="true">
    <span class="corner-wash corner-wash--left" />
    <span class="corner-wash corner-wash--right" />
    <span class="corner-wash corner-wash--bottom" />

    <span class="soft-dot soft-dot--one" />
    <span class="soft-dot soft-dot--two" />
    <span class="soft-dot soft-dot--three" />

    <div class="ghost-bars">
      <span v-for="bar in barHeights" :key="bar.key" :style="{ height: `${bar.height}px` }" />
    </div>

    <svg class="wave-field" viewBox="0 0 1600 420" preserveAspectRatio="none">
      <g class="wave-lines">
        <path
          v-for="line in waveLines"
          :key="line"
          :d="`M -120 ${line} C 170 ${line - 168}, 420 ${line + 64}, 660 ${line - 34} C 920 ${line - 140}, 1080 ${line + 54}, 1340 ${line - 28} S 1640 ${line - 84}, 1720 ${line - 24}`"
        />
      </g>
      <g class="wave-dots">
        <circle v-for="dot in waveDots" :key="dot.key" :cx="dot.x" :cy="dot.y" :r="dot.r" />
      </g>
    </svg>
  </div>
</template>

<script setup>
const barHeights = [72, 108, 142, 120, 86, 154].map((height, index) => ({ key: index, height }))
const waveLines = [216, 236, 256, 276, 296, 316, 336, 356, 376, 396, 416]
const waveDots = Array.from({ length: 132 }, (_, index) => ({
  key: index,
  x: -36 + (index % 66) * 26,
  y: 284 + Math.floor(index / 66) * 62 + Math.sin(index * 0.36) * 62 + (index % 7) * 7,
  r: index % 9 === 0 ? 1.35 : 0.95
}))
</script>

<style scoped>
.login-background {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.88), rgba(245, 249, 255, 0.84) 48%, rgba(232, 246, 255, 0.78)),
    radial-gradient(ellipse at 42% 32%, rgba(255, 255, 255, 0.96), transparent 58%),
    linear-gradient(135deg, #f7faff 0%, #eef5ff 48%, #e8f8ff 100%);
}

.login-background::before {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 50% 18%, rgba(255, 255, 255, 0.9), transparent 48%),
    radial-gradient(circle at 79% 42%, rgba(125, 211, 252, 0.13), transparent 20%),
    linear-gradient(90deg, rgba(226, 237, 255, 0.52), transparent 28%, transparent 72%, rgba(218, 240, 255, 0.72));
  opacity: 0.96;
}

.corner-wash {
  position: absolute;
  border-radius: 9999px;
  filter: blur(10px);
  opacity: 0.54;
  animation: quietBreath 12s ease-in-out infinite;
  will-change: transform, opacity;
}

.corner-wash--left {
  width: 640px;
  height: 640px;
  top: -350px;
  left: -260px;
  background:
    radial-gradient(circle at center, rgba(165, 190, 255, 0.58), rgba(191, 219, 254, 0.3) 43%, transparent 67%);
}

.corner-wash--right {
  width: 660px;
  height: 660px;
  top: -300px;
  right: -170px;
  background:
    radial-gradient(circle at center, rgba(196, 181, 253, 0.56), rgba(177, 167, 255, 0.28) 45%, transparent 69%);
  animation-delay: -4s;
}

.corner-wash--bottom {
  width: 520px;
  height: 520px;
  right: -200px;
  bottom: -150px;
  background:
    radial-gradient(circle at center, rgba(177, 167, 255, 0.3), rgba(125, 211, 252, 0.22) 50%, transparent 72%);
  animation-delay: -7s;
}

.soft-dot {
  position: absolute;
  border-radius: 9999px;
  background: rgba(99, 102, 241, 0.14);
  animation: dotFloat 10s ease-in-out infinite;
}

.soft-dot--one {
  width: 18px;
  height: 18px;
  top: 45px;
  left: 19.5%;
}

.soft-dot--two {
  width: 6px;
  height: 6px;
  top: 36%;
  right: 5%;
  background: rgba(56, 189, 248, 0.2);
  animation-delay: -3s;
}

.soft-dot--three {
  width: 4px;
  height: 4px;
  top: 47%;
  right: 15%;
  background: rgba(251, 191, 36, 0.2);
  animation-delay: -6s;
}

.ghost-bars {
  position: absolute;
  left: 41%;
  bottom: 150px;
  display: flex;
  align-items: flex-end;
  gap: 12px;
  opacity: 0.2;
  transform: translateX(-50%);
}

.ghost-bars span {
  width: 22px;
  border-radius: 4px 4px 0 0;
  background: linear-gradient(180deg, rgba(99, 102, 241, 0.26), rgba(147, 197, 253, 0.08));
}

.wave-field {
  position: absolute;
  left: -3%;
  right: -3%;
  bottom: -28px;
  width: 106%;
  height: 48%;
  opacity: 0.86;
  transform-origin: center bottom;
  animation: waveBreath 13s ease-in-out infinite;
}

.wave-lines {
  fill: none;
  stroke-linecap: round;
  stroke-width: 1.05;
}

.wave-lines path:nth-child(odd) {
  stroke: rgba(79, 140, 245, 0.34);
}

.wave-lines path:nth-child(even) {
  stroke: rgba(14, 189, 218, 0.25);
}

.wave-dots {
  fill: rgba(99, 102, 241, 0.24);
}

html.dark .login-background {
  background:
    linear-gradient(180deg, rgba(15, 23, 42, 0.9), rgba(17, 24, 39, 0.86) 46%, rgba(30, 41, 59, 0.72)),
    radial-gradient(ellipse at 42% 32%, rgba(30, 41, 59, 0.76), transparent 58%),
    linear-gradient(135deg, #0f172a 0%, #111827 47%, #1e1b4b 100%);
}

html.dark .login-background::before {
  background:
    radial-gradient(ellipse at 50% 16%, rgba(30, 41, 59, 0.42), transparent 46%),
    linear-gradient(90deg, rgba(30, 64, 175, 0.12), transparent 28%, transparent 72%, rgba(88, 28, 135, 0.14));
}

html.dark .corner-wash--left {
  background:
    radial-gradient(circle at center, rgba(96, 165, 250, 0.2), rgba(30, 64, 175, 0.12) 42%, transparent 66%);
}

html.dark .corner-wash--right,
html.dark .corner-wash--bottom {
  background:
    radial-gradient(circle at center, rgba(129, 140, 248, 0.18), rgba(91, 33, 182, 0.12) 45%, transparent 68%);
}

html.dark .ghost-bars {
  opacity: 0.1;
}

html.dark .wave-field {
  opacity: 0.32;
}

@keyframes quietBreath {
  0%,
  100% {
    opacity: 0.44;
    transform: translate3d(0, 0, 0) scale(1);
  }

  50% {
    opacity: 0.62;
    transform: translate3d(12px, 8px, 0) scale(1.035);
  }
}

@keyframes waveBreath {
  0%,
  100% {
    opacity: 0.66;
    transform: translateY(0) scale(1);
  }

  50% {
    opacity: 0.92;
    transform: translateY(-8px) scale(1.012);
  }
}

@keyframes dotFloat {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }

  50% {
    transform: translate3d(8px, -10px, 0);
  }
}

@media (max-width: 768px) {
  .ghost-bars {
    display: none;
  }

  .wave-field {
    height: 30%;
  }
}

@media (prefers-reduced-motion: reduce) {
  .corner-wash,
  .soft-dot,
  .wave-field {
    animation-duration: 24s;
  }
}
</style>
