import { CanvasTexture, RepeatWrapping, TextureLoader } from 'three'
import {
  createProceduralTerrainTextures,
  heightCanvasToNormalMap,
  imageUrlToCanvas
} from './heightMapUtils.js'
import { loadGaodeSatelliteTexture } from './gaodeSatelliteLoader.js'

/**
 * 加载贴图；失败时使用程序化地形并输出排查提示
 * @param {object} paths 静态资源路径
 * @param {{ geoJson?: object, onSatelliteProgress?: (n:number)=>void }} options
 */
export async function loadMapTextures(paths, options = {}) {
  const { geoJson, onSatelliteProgress } = options
  const warnings = []
  let heightTex = null
  let borderTex = null
  let satelliteTex = null
  let normalTex = null
  let fallback = false
  let satelliteSource = ''

  const loader = new TextureLoader()

  const loadUrl = (url, label) =>
    new Promise((resolve) => {
      loader.load(
        url,
        (tex) => {
          tex.wrapS = tex.wrapT = RepeatWrapping
          resolve(tex)
        },
        undefined,
        () => {
          warnings.push(`${label} 加载失败: ${url}`)
          resolve(null)
        }
      )
    })

  heightTex = await loadUrl(paths.heightMap, '高度图 china_height.png')
  borderTex = await loadUrl(paths.borderMap, '轮廓图 china_border.png')
  satelliteTex = await loadUrl(paths.satelliteMap, '卫星图 china_satellite.png')

  if (satelliteTex?.image) {
    satelliteSource = 'static'
  } else if (geoJson) {
    try {
      const { texture, meta } = await loadGaodeSatelliteTexture(geoJson, {
        onProgress: onSatelliteProgress
      })
      satelliteTex = texture
      satelliteSource = meta.source
    } catch (e) {
      warnings.push(`高德卫星影像在线拼接失败: ${e.message}`)
    }
  }

  if (heightTex?.image) {
    try {
      const canvas = await imageUrlToCanvas(paths.heightMap)
      const normalCanvas = heightCanvasToNormalMap(canvas)
      normalTex = new CanvasTexture(normalCanvas)
      normalTex.wrapS = normalTex.wrapT = RepeatWrapping
    } catch (e) {
      warnings.push(`高度图灰度解析失败: ${e.message}`)
    }
  }

  if (!heightTex || !borderTex) {
    fallback = true
    const proc = createProceduralTerrainTextures()
    heightTex = heightTex || proc.heightTex
    borderTex = borderTex || proc.borderTex
    normalTex = normalTex || proc.normalTex
    if (!satelliteTex) {
      warnings.push(
        '未检测到 public/mapAssets/china_height.png 或 china_border.png。',
        '可放入 china_satellite.png（sat-hunter 高德卫星导出），或依赖在线高德瓦片。'
      )
    }
  }

  return {
    heightTex,
    borderTex,
    satelliteTex,
    normalTex,
    fallback,
    satelliteSource,
    warnings
  }
}
