import { CanvasTexture, RepeatWrapping } from 'three'

/** 从灰度高度图生成法线贴图（增强山脉起伏视觉） */
export function heightCanvasToNormalMap(canvas, strength = 2.8) {
  const ctx = canvas.getContext('2d')
  const { width, height } = canvas
  const src = ctx.getImageData(0, 0, width, height).data
  const out = ctx.createImageData(width, height)
  const dst = out.data

  const sample = (x, y) => {
    const cx = Math.max(0, Math.min(width - 1, x))
    const cy = Math.max(0, Math.min(height - 1, y))
    return src[(cy * width + cx) * 4] / 255
  }

  for (let y = 0; y < height; y++) {
    for (let x = 0; x < width; x++) {
      const l = sample(x - 1, y)
      const r = sample(x + 1, y)
      const d = sample(x, y - 1)
      const u = sample(x, y + 1)
      let nx = (l - r) * strength
      let ny = (d - u) * strength
      const nz = 1
      const len = Math.hypot(nx, ny, nz) || 1
      nx = nx / len * 0.5 + 0.5
      ny = ny / len * 0.5 + 0.5
      const nzNorm = nz / len * 0.5 + 0.5
      const i = (y * width + x) * 4
      dst[i] = nx * 255
      dst[i + 1] = ny * 255
      dst[i + 2] = nzNorm * 255
      dst[i + 3] = 255
    }
  }

  const normalCanvas = document.createElement('canvas')
  normalCanvas.width = width
  normalCanvas.height = height
  normalCanvas.getContext('2d').putImageData(out, 0, 0)
  return normalCanvas
}

/** 贴图缺失时的程序化地形（避免扁平灰底） */
export function createProceduralTerrainTextures() {
  const size = 512
  const heightCanvas = document.createElement('canvas')
  heightCanvas.width = size
  heightCanvas.height = size
  const ctx = heightCanvas.getContext('2d')

  const grad = ctx.createLinearGradient(0, 0, size, size)
  grad.addColorStop(0, '#8a7f6e')
  grad.addColorStop(0.5, '#c4b8a4')
  grad.addColorStop(1, '#9a8f7e')
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, size, size)

  for (let i = 0; i < 9000; i++) {
    const x = Math.random() * size
    const y = Math.random() * size
    const g = 120 + Math.random() * 100
    ctx.fillStyle = `rgba(${g},${g - 10},${g - 20},${0.04 + Math.random() * 0.08})`
    ctx.fillRect(x, y, 1 + Math.random() * 2, 1 + Math.random() * 2)
  }

  const borderCanvas = document.createElement('canvas')
  borderCanvas.width = size
  borderCanvas.height = size
  const bctx = borderCanvas.getContext('2d')
  bctx.fillStyle = '#e8dcc8'
  bctx.fillRect(0, 0, size, size)

  const normalCanvas = heightCanvasToNormalMap(heightCanvas)
  const toTex = (canvas) => {
    const tex = new CanvasTexture(canvas)
    tex.wrapS = tex.wrapT = RepeatWrapping
    return tex
  }

  return {
    heightTex: toTex(heightCanvas),
    borderTex: toTex(borderCanvas),
    normalTex: toTex(normalCanvas),
    fallback: true
  }
}

export async function imageUrlToCanvas(url) {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.crossOrigin = 'anonymous'
    img.onload = () => {
      const canvas = document.createElement('canvas')
      canvas.width = img.width
      canvas.height = img.height
      canvas.getContext('2d').drawImage(img, 0, 0)
      resolve(canvas)
    }
    img.onerror = () => reject(new Error(`无法加载: ${url}`))
    img.src = url
  })
}
