import { CanvasTexture, SRGBColorSpace } from 'three'
import { getGeoJsonBounds } from './geoBounds.js'

const TILE_SIZE = 256

function lon2tileX(lon, z) {
  return Math.floor(((lon + 180) / 360) * 2 ** z)
}

function lat2tileY(lat, z) {
  const rad = (lat * Math.PI) / 180
  return Math.floor(((1 - Math.log(Math.tan(rad) + 1 / Math.cos(rad)) / Math.PI) / 2) * 2 ** z)
}

function tileCount(bounds, z) {
  const xMin = lon2tileX(bounds.minLon, z)
  const xMax = lon2tileX(bounds.maxLon, z)
  const yMin = lat2tileY(bounds.maxLat, z)
  const yMax = lat2tileY(bounds.minLat, z)
  return (xMax - xMin + 1) * (yMax - yMin + 1)
}

function pickZoom(bounds) {
  for (const z of [6, 5, 4]) {
    if (tileCount(bounds, z) <= 180) return z
  }
  return 4
}

function tileUrl(x, y, z) {
  const host = `webst0${((x + y) % 4) + 1}.is.autonavi.com`
  const path = `/appmaptile?style=6&x=${x}&y=${y}&z=${z}`
  // 开发环境走 Vite 代理；生产环境直连高德（国内网络通常可用）
  if (import.meta.env.DEV) {
    return `/amap-tile/appmaptile?style=6&x=${x}&y=${y}&z=${z}`
  }
  return `https://${host}${path}`
}

function loadTileImage(url) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => resolve(img)
    img.onerror = () => reject(new Error(`瓦片加载失败: ${url}`))
    img.src = url
  })
}

/**
 * 拼接高德卫星瓦片为 Three.js 贴图
 * @param {object} geoJson 全国 GeoJSON
 * @param {{ onProgress?: (ratio:number)=>void }} options
 */
export async function loadGaodeSatelliteTexture(geoJson, options = {}) {
  const bounds = getGeoJsonBounds(geoJson)
  const z = pickZoom(bounds)
  const xMin = lon2tileX(bounds.minLon, z)
  const xMax = lon2tileX(bounds.maxLon, z)
  const yMin = lat2tileY(bounds.maxLat, z)
  const yMax = lat2tileY(bounds.minLat, z)

  const cols = xMax - xMin + 1
  const rows = yMax - yMin + 1
  const canvas = document.createElement('canvas')
  canvas.width = cols * TILE_SIZE
  canvas.height = rows * TILE_SIZE
  const ctx = canvas.getContext('2d')

  const jobs = []
  for (let x = xMin; x <= xMax; x++) {
    for (let y = yMin; y <= yMax; y++) {
      jobs.push({ x, y, dx: (x - xMin) * TILE_SIZE, dy: (y - yMin) * TILE_SIZE })
    }
  }

  let done = 0
  const batchSize = 8
  for (let i = 0; i < jobs.length; i += batchSize) {
    const batch = jobs.slice(i, i + batchSize)
    await Promise.all(
      batch.map(async (job) => {
        try {
          const img = await loadTileImage(tileUrl(job.x, job.y, z))
          ctx.drawImage(img, job.dx, job.dy, TILE_SIZE, TILE_SIZE)
        } catch {
          ctx.fillStyle = '#3a4a3a'
          ctx.fillRect(job.dx, job.dy, TILE_SIZE, TILE_SIZE)
        } finally {
          done += 1
          options.onProgress?.(done / jobs.length)
        }
      })
    )
  }

  const texture = new CanvasTexture(canvas)
  texture.colorSpace = SRGBColorSpace
  texture.needsUpdate = true
  return {
    texture,
    meta: { zoom: z, tileCount: jobs.length, source: 'gaode-live' }
  }
}
