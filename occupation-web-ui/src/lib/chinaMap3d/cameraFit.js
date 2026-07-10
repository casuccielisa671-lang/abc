import { Box3, Vector3 } from 'three'

function computeMapBounds(terrainGroup) {
  const box = new Box3()
  const regions = terrainGroup?.regionGroups
  if (regions?.length) {
    regions.forEach((rg) => box.expandByObject(rg))
  }
  if (!box.isEmpty()) return box
  return new Box3().setFromObject(terrainGroup)
}

/**
 * 根据地形包围盒自动适配相机，保证全国地图初始完整可见
 */
export function fitCameraToMap(camera, controls, terrainGroup, options = {}) {
  const { padding = 1.35, viewAngle = { x: 0.55, y: 0.62, z: 0.58 } } = options

  const box = computeMapBounds(terrainGroup)
  if (box.isEmpty()) return null

  const center = new Vector3()
  const size = new Vector3()
  box.getCenter(center)
  box.getSize(size)

  const fovRad = (camera.fov * Math.PI) / 180
  const fitHeight = size.y / (2 * Math.tan(fovRad / 2))
  const fitWidth = size.x / (2 * Math.tan(fovRad / 2) * camera.aspect)
  const fitDepth = size.z / (2 * Math.tan(fovRad / 2))
  const distance = Math.max(fitHeight, fitWidth, fitDepth) * padding

  const viewDir = new Vector3(viewAngle.x, viewAngle.y, viewAngle.z).normalize()

  controls.target.copy(center)
  camera.position.copy(center).add(viewDir.clone().multiplyScalar(distance))

  camera.near = Math.max(0.1, distance / 200)
  camera.far = Math.max(2000, distance * 20)
  camera.updateProjectionMatrix()

  controls.minDistance = distance * 0.45
  controls.maxDistance = distance * 2.6
  controls.update()

  return { center, distance, size }
}

export function refitCameraOnResize(camera, controls, terrainGroup, padding = 1.35) {
  if (!terrainGroup) return
  fitCameraToMap(camera, controls, terrainGroup, { padding })
}
