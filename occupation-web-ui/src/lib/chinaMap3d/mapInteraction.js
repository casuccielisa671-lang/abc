import { Raycaster, Vector2 } from 'three'

/**
 * 省份 hover 上浮（参考 sc-datav city.tsx scale.z 动画）
 */
export function attachRegionHover(renderer, camera, regionGroups) {
  const raycaster = new Raycaster()
  const pointer = new Vector2()
  let hovered = null

  const onMove = (event) => {
    const rect = renderer.domElement.getBoundingClientRect()
    pointer.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
    pointer.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
    raycaster.setFromCamera(pointer, camera)
    const hits = raycaster.intersectObjects(
      regionGroups.map((g) => g.mesh).filter(Boolean),
      false
    )
    const next = hits[0]?.object?.userData?.regionGroup || null
    if (hovered !== next) {
      if (hovered) hovered.setHover(false)
      hovered = next
      if (hovered) {
        hovered.setHover(true)
        renderer.domElement.style.cursor = 'pointer'
      } else {
        renderer.domElement.style.cursor = 'grab'
      }
    }
  }

  const onLeave = () => {
    if (hovered) hovered.setHover(false)
    hovered = null
    renderer.domElement.style.cursor = 'grab'
  }

  renderer.domElement.addEventListener('pointermove', onMove)
  renderer.domElement.addEventListener('pointerleave', onLeave)

  const tick = () => {
    regionGroups.forEach((rg) => rg.tickLift())
  }

  return {
    tick,
    dispose() {
      renderer.domElement.removeEventListener('pointermove', onMove)
      renderer.domElement.removeEventListener('pointerleave', onLeave)
    }
  }
}
