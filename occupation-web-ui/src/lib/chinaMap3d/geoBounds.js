/** 从 GeoJSON 计算经纬度包围盒 */
export function getGeoJsonBounds(geoJson, padding = 0.4) {
  let minLon = Infinity
  let minLat = Infinity
  let maxLon = -Infinity
  let maxLat = -Infinity

  const visit = (coord) => {
    const [lon, lat] = coord
    if (typeof lon !== 'number' || typeof lat !== 'number') return
    minLon = Math.min(minLon, lon)
    maxLon = Math.max(maxLon, lon)
    minLat = Math.min(minLat, lat)
    maxLat = Math.max(maxLat, lat)
  }

  const walkCoords = (coords, depth) => {
    if (depth === 0) {
      visit(coords)
      return
    }
    coords.forEach((c) => walkCoords(c, depth - 1))
  }

  geoJson?.features?.forEach((feature) => {
    const type = feature.geometry?.type
    const coords = feature.geometry?.coordinates
    if (!coords) return
    if (type === 'Point') visit(coords)
    else if (type === 'MultiPoint' || type === 'LineString') coords.forEach(visit)
    else if (type === 'MultiLineString' || type === 'Polygon') walkCoords(coords, 2)
    else if (type === 'MultiPolygon') walkCoords(coords, 3)
  })

  return {
    minLon: minLon - padding,
    minLat: minLat - padding,
    maxLon: maxLon + padding,
    maxLat: maxLat + padding
  }
}
