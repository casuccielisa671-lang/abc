import {
  AmbientLight,
  Color,
  DirectionalLight,
  Group,
  PerspectiveCamera,
  Scene,
  WebGLRenderer
} from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'
import { MAP_ASSET_PATHS, CHINA_GEOJSON_URL } from './mapAssets.js'
import { loadMapTextures } from './textureLoader.js'
import { createMercatorProjection, parseGeoJsonRegions } from './geoProjection.js'
import { buildTerrainGroup } from './terrainMesh.js'
import { createHeatmapMesh } from './heatmapLayer.js'
import { buildProvinceLabels, buildCityHeatLabels } from './mapLabels.js'
import { attachRegionHover } from './mapInteraction.js'
import { fitCameraToMap, refitCameraOnResize } from './cameraFit.js'

/** 初始相机边距（越小地图越大） */
const MAP_CAMERA_PADDING = 1.38 / 1.65

async function fetchGeoJson(url) {
  const res = await fetch(url)
  if (!res.ok) throw new Error(`GeoJSON 加载失败: ${res.status}`)
  return res.json()
}

function setupLights(scene) {
  scene.add(new AmbientLight(0xffffff, 0.85))
  const dir = new DirectionalLight(0xffffff, 1.1)
  dir.position.set(120, 180, 140)
  scene.add(dir)
}

/** sc-datav index.tsx — OrbitControls 配置 + 滚轮缩放修复 */
function setupOrbitControls(camera, domElement, container) {
  const controls = new OrbitControls(camera, domElement)
  controls.enablePan = true
  controls.enableZoom = true
  controls.enableRotate = true
  controls.zoomSpeed = 0.3
  controls.maxPolarAngle = 1.5
  // target / minDistance / maxDistance 在 fitCameraToMap 中按包围盒计算

  const blockScroll = (e) => e.preventDefault()
  domElement.addEventListener('wheel', blockScroll, { passive: false })
  container.addEventListener('wheel', blockScroll, { passive: false })

  return { controls, blockScroll }
}

/**
 * 挂载全国 3D 地图（对齐 sc-datav Demo1 原生 Three.js 逻辑）
 */
export async function mountChinaMap3d(container, options = {}) {
  const {
    heatPoints = [],
    assetPaths = MAP_ASSET_PATHS,
    geoJsonUrl = CHINA_GEOJSON_URL,
    depth = 6
  } = options

  const width = container.clientWidth || 800
  const height = container.clientHeight || 420

  const scene = new Scene()
  scene.background = new Color('#fff5e8')

  const camera = new PerspectiveCamera(50, width / height, 0.1, 5000)

  const renderer = new WebGLRenderer({ antialias: true, alpha: true, powerPreference: 'high-performance' })
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.5))
  renderer.setSize(width, height)
  // 阴影未实际使用，关闭省 GPU。黑屏多因反复开关地图累积/丢失 WebGL 上下文，
  // 关键缓解在 dispose() 里的 renderer.forceContextLoss()（立即释放上下文，防止累积超过浏览器上限）。
  renderer.shadowMap.enabled = false
  container.appendChild(renderer.domElement)
  renderer.domElement.style.touchAction = 'none'
  // 上下文丢失时阻止默认行为，允许浏览器尝试恢复，避免直接永久黑屏
  const onContextLost = (e) => e.preventDefault()
  renderer.domElement.addEventListener('webglcontextlost', onContextLost, false)

  const { controls, blockScroll } = setupOrbitControls(camera, renderer.domElement, container)

  setupLights(scene)

  const geoJson = await fetchGeoJson(geoJsonUrl)
  const textures = await loadMapTextures(assetPaths, {
    geoJson,
    onSatelliteProgress: options.onSatelliteProgress
  })

  if (textures.warnings?.length) {
    console.warn('[China3DMap] 贴图排查:\n', textures.warnings.join('\n'))
  }

  const projection = createMercatorProjection(geoJson, { scale: 720 })
  const { regions, bbox } = parseGeoJsonRegions(geoJson, projection, depth)

  const terrainGroup = buildTerrainGroup(regions, bbox, textures, depth)

  buildProvinceLabels(regions, depth).forEach((s) => terrainGroup.add(s))

  const labelGroup = new Group()
  buildCityHeatLabels(projection, heatPoints, depth).forEach((s) => labelGroup.add(s))
  terrainGroup.add(labelGroup)

  let heatLayer = null
  if (heatPoints.length > 0) {
    heatLayer = createHeatmapMesh(projection, heatPoints, {
      depth,
      maskTexture: textures.borderTex || null
    })
    terrainGroup.add(heatLayer.group)
  }

  scene.add(terrainGroup)

  fitCameraToMap(camera, controls, terrainGroup, { padding: MAP_CAMERA_PADDING })

  const hoverCtrl = attachRegionHover(renderer, camera, terrainGroup.regionGroups || [])

  let frameId = null
  const renderLoop = () => {
    frameId = requestAnimationFrame(renderLoop)
    hoverCtrl.tick()
    controls.update()
    renderer.render(scene, camera)
  }
  renderLoop()

  const onResize = () => {
    const w = container.clientWidth
    const h = container.clientHeight
    if (!w || !h) return
    camera.aspect = w / h
    camera.updateProjectionMatrix()
    renderer.setSize(w, h)
    refitCameraOnResize(camera, controls, terrainGroup, MAP_CAMERA_PADDING)
  }
  window.addEventListener('resize', onResize)

  function refreshCityLabels(points) {
    while (labelGroup.children.length) {
      const child = labelGroup.children[0]
      child.material?.map?.dispose()
      child.material?.dispose()
      labelGroup.remove(child)
    }
    buildCityHeatLabels(projection, points, depth).forEach((s) => labelGroup.add(s))
  }

  return {
    textureWarnings: textures.warnings || [],
    usingFallbackTextures: textures.fallback,
    satelliteSource: textures.satelliteSource || '',
    updateHeatPoints(points) {
      if (!points?.length) return
      refreshCityLabels(points)
      if (heatLayer) {
        heatLayer.update(points)
      } else {
        heatLayer = createHeatmapMesh(projection, points, {
          depth,
          maskTexture: textures.borderTex || null
        })
        terrainGroup.add(heatLayer.group)
      }
    },
    dispose() {
      if (frameId) cancelAnimationFrame(frameId)
      window.removeEventListener('resize', onResize)
      renderer.domElement.removeEventListener('wheel', blockScroll)
      container.removeEventListener('wheel', blockScroll)
      renderer.domElement.removeEventListener('webglcontextlost', onContextLost)
      hoverCtrl.dispose()
      controls.dispose()
      heatLayer?.dispose()
      scene.traverse((obj) => {
        if (obj.geometry) obj.geometry.dispose()
        if (obj.material) {
          if (Array.isArray(obj.material)) obj.material.forEach((m) => m.dispose())
          else obj.material.dispose()
        }
      })
      renderer.dispose()
      // 立即释放 WebGL 上下文，避免反复打开地图累积上下文导致后续黑屏
      renderer.forceContextLoss()
      if (renderer.domElement.parentNode === container) {
        container.removeChild(renderer.domElement)
      }
    }
  }
}
