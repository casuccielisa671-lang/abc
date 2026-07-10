<template>

  <div class="china-3d-map">

    <div ref="containerRef" class="china-3d-map__canvas" />

    <div v-if="mapLoading" class="china-3d-map__overlay">

      <span>{{ loadingText }}</span>

    </div>

    <div v-else-if="error" class="china-3d-map__overlay china-3d-map__overlay--error">

      <span>{{ error }}</span>

    </div>

    <div v-else-if="textureHint" class="china-3d-map__hint">

      {{ textureHint }}

    </div>

    <div v-if="satelliteBadge" class="china-3d-map__badge">

      {{ satelliteBadge }}

    </div>

  </div>

</template>



<script setup>

import { ref, watch, onMounted, onUnmounted } from 'vue'

import { mountChinaMap3d } from '@/lib/chinaMap3d/chinaMap3d.js'



const props = defineProps({

  heatPoints: {

    type: Array,

    default: () => []

  }

})



const containerRef = ref(null)

const mapLoading = ref(true)

const loadingText = ref('地图加载中…')

const error = ref('')

const textureHint = ref('')

const satelliteBadge = ref('')

let mapInstance = null

let mapReady = false



function normalizePoints(list) {

  return (list || []).map(p => ({

    cityName: p.cityName,

    longitude: p.longitude,

    latitude: p.latitude,

    gatherValue: p.gatherValue

  }))

}



async function bootMap(initialPoints) {

  if (!containerRef.value) return

  mapLoading.value = true

  loadingText.value = '地图加载中…'

  error.value = ''

  textureHint.value = ''

  satelliteBadge.value = ''

  try {

    mapInstance = await mountChinaMap3d(containerRef.value, {

      heatPoints: normalizePoints(initialPoints),

      onSatelliteProgress(ratio) {

        loadingText.value = `正在加载高德卫星影像… ${Math.round(ratio * 100)}%`

      }

    })

    mapReady = true

    if (mapInstance.satelliteSource === 'gaode-live') {

      satelliteBadge.value = '高德卫星影像'

    } else if (mapInstance.satelliteSource === 'static') {

      satelliteBadge.value = '高德卫星影像（离线贴图）'

    }

    if (mapInstance.usingFallbackTextures) {

      textureHint.value =

        '地形高度/轮廓贴图未就绪，可将 sat-hunter 导出的 PNG 放入 public/mapAssets/'

    }

  } catch (e) {

    error.value = e?.message || '地图加载失败'

  } finally {

    mapLoading.value = false

  }

}



function applyHeatPoints(points) {

  if (!mapInstance || !mapReady) return

  mapInstance.updateHeatPoints(normalizePoints(points))

}



watch(

  () => props.heatPoints,

  (pts) => applyHeatPoints(pts),

  { deep: true }

)



onMounted(() => bootMap(props.heatPoints))



onUnmounted(() => {

  mapInstance?.dispose()

  mapInstance = null

  mapReady = false

})



defineExpose({ applyHeatPoints })

</script>



<style scoped>

.china-3d-map {

  position: relative;

  width: 100%;

  height: 100%;

  min-height: 420px;

  border-radius: var(--app-radius-card, 10px);

  overflow: hidden;

  background: radial-gradient(ellipse at 50% 30%, #fff8f0 0%, #f5e8d8 55%, #e8dcc8 100%);

}



.china-3d-map__canvas {

  width: 100%;

  height: 100%;

  touch-action: none;

}



.china-3d-map__overlay {

  position: absolute;

  inset: 0;

  display: flex;

  align-items: center;

  justify-content: center;

  background: rgba(251, 250, 249, 0.72);

  font-size: 14px;

  color: var(--app-ink-2, #474645);

  pointer-events: none;

}



.china-3d-map__overlay--error {

  color: var(--app-danger, #de2532);

}



.china-3d-map__hint {

  position: absolute;

  left: 10px;

  bottom: 10px;

  max-width: 360px;

  padding: 6px 10px;

  font-size: 11px;

  line-height: 1.45;

  color: rgba(120, 80, 40, 0.85);

  background: rgba(255, 255, 255, 0.55);

  border-radius: 6px;

  pointer-events: none;

}



.china-3d-map__badge {

  position: absolute;

  right: 10px;

  bottom: 10px;

  padding: 4px 10px;

  font-size: 11px;

  color: rgba(255, 255, 255, 0.92);

  background: rgba(30, 120, 60, 0.72);

  border-radius: 999px;

  pointer-events: none;

}

</style>


