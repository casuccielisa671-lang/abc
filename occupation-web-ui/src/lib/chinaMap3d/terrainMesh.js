import {
  DoubleSide,
  EdgesGeometry,
  ExtrudeGeometry,
  Float32BufferAttribute,
  Group,
  LineBasicMaterial,
  LineSegments,
  Mesh,
  MeshStandardMaterial,
  Shape,
  ShapeGeometry
} from 'three'

export function computeShapeUV(geometry, bbox) {
  const pos = geometry.attributes.position
  const width = bbox.max.x - bbox.min.x || 1
  const height = bbox.max.y - bbox.min.y || 1
  const uv = []
  for (let i = 0; i < pos.count; i++) {
    uv.push(
      (pos.getX(i) - bbox.min.x) / width,
      (pos.getY(i) - bbox.min.y) / height
    )
  }
  geometry.setAttribute('uv', new Float32BufferAttribute(uv, 2))
}

/** sc-datav city.tsx — map + normalMap 顶面，extrude 侧面 */
export function createTerrainMaterial(textures) {
  const { borderTex, normalTex, heightTex, satelliteTex } = textures
  return new MeshStandardMaterial({
    map: satelliteTex || borderTex || null,
    normalMap: normalTex || heightTex || null,
    normalScale: { x: 1.8, y: 1.8 },
    displacementMap: heightTex || null,
    displacementScale: heightTex ? 2.2 : 0,
    metalness: 0.2,
    roughness: 0.55,
    side: DoubleSide,
    color: '#ffffff'
  })
}

function buildRegionGroup(region, bbox, topMaterial, sideMaterial, depth) {
  const root = new Group()
  root.userData.regionName = region.name

  const shapes = region.points.map((ring) => new Shape(ring))
  const shapeGeo = new ShapeGeometry(shapes)
  computeShapeUV(shapeGeo, bbox)

  const top = new Mesh(shapeGeo, topMaterial.clone())
  top.position.z = depth + 0.1
  top.castShadow = true
  top.receiveShadow = true
  top.userData.regionGroup = root
  root.add(top)

  const sideGeo = new ExtrudeGeometry(shapes, { depth, steps: 1, bevelEnabled: false })
  computeShapeUV(sideGeo, bbox)
  const side = new Mesh(sideGeo, sideMaterial.clone())
  side.castShadow = true
  side.receiveShadow = true
  root.add(side)

  const edges = new LineSegments(
    new EdgesGeometry(shapeGeo),
    new LineBasicMaterial({ color: '#ffffff', transparent: true, opacity: 0.32 })
  )
  edges.position.z = depth + 0.2
  root.add(edges)

  let currentLift = 1
  let targetLift = 1

  root.setHover = (active) => {
    targetLift = active ? 1.5 : 1
  }
  root.tickLift = () => {
    currentLift += (targetLift - currentLift) * 0.1
    root.scale.z = currentLift
  }
  root.mesh = top

  return root
}

export function buildTerrainGroup(regions, bbox, textures, depth = 6) {
  const topMaterial = createTerrainMaterial(textures)
  const sideMaterial = new MeshStandardMaterial({
    color: '#f9f3e7',
    metalness: 0.15,
    roughness: 0.65,
    side: DoubleSide
  })

  const root = new Group()
  root.rotation.x = -Math.PI / 2
  root.position.x = 20

  const regionGroups = []
  regions.forEach((region) => {
    const rg = buildRegionGroup(region, bbox, topMaterial, sideMaterial, depth)
    regionGroups.push(rg)
    root.add(rg)
  })

  root.regionGroups = regionGroups
  root.bbox = bbox
  return root
}
