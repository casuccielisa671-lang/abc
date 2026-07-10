import { geoMercator } from 'd3-geo'
import { Box2, Vector2 } from 'three'

/**
 * 创建墨卡托投影（全国中心）
 */
export function createMercatorProjection(geoJson, options = {}) {
  const {
    center = [104.0, 35.0],
    scale = 720,
    translate = [0, 0]
  } = options

  const first = geoJson?.features?.[0]
  const centroid = first?.properties?.centroid || first?.properties?.center || center

  return geoMercator()
    .center(centroid)
    .scale(scale)
    .translate(translate)
}

export function lonLatToVector2(projection, coord, flipY = true) {
  const projected = projection(coord)
  if (!projected) return new Vector2(0, 0)
  const [x, y] = projected
  return new Vector2(x, flipY ? -y : y)
}

/**
 * GeoJSON MultiPolygon → 区域列表 + 全局 bbox
 */
export function parseGeoJsonRegions(geoJson, projection, depth = 5) {
  const regions = []
  const bbox = new Box2()

  const toV2 = (coord) => {
    const v = lonLatToVector2(projection, coord)
    bbox.expandByPoint(v)
    return v
  }

  geoJson.features.forEach((feature) => {
    const coords = feature.geometry?.coordinates
    if (!coords) return

    const rings = []
    if (feature.geometry.type === 'Polygon') {
      rings.push(...coords.map((ring) => ring.map(toV2)))
    } else if (feature.geometry.type === 'MultiPolygon') {
      coords.forEach((poly) => {
        poly.forEach((ring) => rings.push(ring.map(toV2)))
      })
    }

    const center = feature.properties?.centroid || feature.properties?.center
    let cityId = [0, 0, depth + 0.1]
    if (center) {
      const [x, y] = projection(center)
      cityId = [x, -y, depth + 0.1]
    }

    regions.push({
      name: feature.properties?.name || '未知',
      cityId,
      points: rings
    })
  })

  return { regions, bbox }
}
