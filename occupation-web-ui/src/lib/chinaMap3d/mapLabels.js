import { CanvasTexture, Sprite, SpriteMaterial } from 'three'

function createLabelSprite(text) {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  const fontSize = 20
  ctx.font = `600 ${fontSize}px "Microsoft YaHei", sans-serif`
  const w = ctx.measureText(text).width + 20
  canvas.width = w
  canvas.height = fontSize + 14
  ctx.font = `600 ${fontSize}px "Microsoft YaHei", sans-serif`
  ctx.fillStyle = 'rgba(255,255,255,0.95)'
  ctx.fillRect(2, 2, canvas.width - 4, canvas.height - 4)
  ctx.strokeStyle = 'rgba(220,140,60,0.9)'
  ctx.lineWidth = 2
  ctx.strokeRect(2, 2, canvas.width - 4, canvas.height - 4)
  ctx.fillStyle = 'rgba(210,90,30,0.95)'
  ctx.fillText(text, 10, fontSize + 2)

  const texture = new CanvasTexture(canvas)
  const sprite = new Sprite(
    new SpriteMaterial({
      map: texture,
      transparent: true,
      depthTest: false,
      depthWrite: false
    })
  )
  const scale = 0.042
  sprite.scale.set(canvas.width * scale, canvas.height * scale, 1)
  return sprite
}

/** 省会/直辖市标签 */
export function buildProvinceLabels(regions, depth) {
  return regions
    .filter((r) => r.name && r.name.length <= 8)
    .map((region) => {
      const [x, y, z] = region.cityId
      const sprite = createLabelSprite(region.name)
      sprite.position.set(x, y, z + 3)
      sprite.renderOrder = 20
      return sprite
    })
}

/** 热力城市标签（API 返回的 cityName） */
export function buildCityHeatLabels(projection, heatPoints, depth) {
  return (heatPoints || []).map((p) => {
    const [x = 0, y = 0] = projection([p.longitude, p.latitude]) ?? [0, 0]
    const sprite = createLabelSprite(p.cityName || '')
    sprite.position.set(x, -y, depth + 8)
    sprite.renderOrder = 25
    return sprite
  })
}
