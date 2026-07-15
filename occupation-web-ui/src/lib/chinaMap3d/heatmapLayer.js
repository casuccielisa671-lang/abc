import {
  CanvasTexture,
  Color,
  DoubleSide,
  Group,
  Mesh,
  PlaneGeometry,
  ShaderMaterial
} from 'three'
import heatmapJs from 'keli-heatmap.js'

/** 低门槛渐变，多城可见 */
const HEAT_GRADIENT = {
  0.05: '#1B4DB3',
  0.2: '#2E86FF',
  0.4: '#28C0D6',
  0.55: '#8BE04E',
  0.7: '#F7C948',
  0.85: '#F97316',
  1.0: '#ff4500'
}

const HEAT_RADIUS = 28
/** 热力凸起高度 */
const HEAT_Z_SCALE = 12
/** 相对地形顶面上浮（过高易被深度测试裁掉） */
const HEAT_FLOAT_ABOVE = 5.5
const TEX_SIZE = 512

function createHeatmapMaterial(maskTexture) {
  return new ShaderMaterial({
    transparent: true,
    side: DoubleSide,
    depthWrite: false,
    // 悬浮热力不写深度也不测深度，避免抬高后被地形/动效层裁没
    depthTest: false,
    uniforms: {
      heatMap: { value: null },
      greyMap: { value: null },
      maskMap: { value: null },
      useMask: { value: 0.0 },
      z_scale: { value: HEAT_Z_SCALE },
      u_color: { value: new Color('#ffffff') },
      u_opacity: { value: 0.92 }
    },
    vertexShader: `
      varying vec2 vUv;
      uniform float z_scale;
      uniform sampler2D greyMap;
      void main() {
        vUv = uv;
        vec4 frgColor = texture2D(greyMap, uv);
        float height = z_scale * frgColor.a;
        vec3 transformed = vec3(position.x, position.y, height);
        gl_Position = projectionMatrix * modelViewMatrix * vec4(transformed, 1.0);
      }
    `,
    fragmentShader: `
      #ifdef GL_ES
      precision highp float;
      #endif
      varying vec2 vUv;
      uniform sampler2D heatMap;
      uniform float u_opacity;
      uniform vec3 u_color;
      void main() {
        vec4 heat = texture2D(heatMap, vUv);
        if (heat.a < 0.012) discard;
        gl_FragColor = vec4(u_color * heat.rgb, heat.a * u_opacity);
      }
    `
  })
}

function renderHeatTextures(points, size, maxVal) {
  const makeCanvasLayer = (gradient) => {
    const container = document.createElement('div')
    container.style.cssText = 'position:absolute;top:-9999px;left:-9999px;'
    document.body.appendChild(container)
    const layer = heatmapJs.create({
      container,
      gradient,
      blur: 0.85,
      radius: HEAT_RADIUS,
      maxOpacity: 1,
      width: size,
      height: size
    })
    layer.setData({ max: Math.max(maxVal, 1), min: 0, data: points })
    const texture = new CanvasTexture(layer._renderer.canvas)
    texture.needsUpdate = true
    document.body.removeChild(container)
    return texture
  }

  return {
    heatTexture: makeCanvasLayer(HEAT_GRADIENT),
    greyTexture: makeCanvasLayer({ 0.0: 'black', 1.0: 'white' })
  }
}

/**
 * 投影到与地形相同的局部坐标 (x, -y)，再按全国 bbox 归一化到热力画布。
 * CanvasTexture 默认 flipY=true：画布自上而下的 cy 应对齐到 UV.v = 1 - cy/size。
 */
function projectHeatPoints(projection, heatPoints, size, bbox) {
  const minX = bbox.min.x
  const minY = bbox.min.y
  const w = Math.max(bbox.max.x - bbox.min.x, 1)
  const h = Math.max(bbox.max.y - bbox.min.y, 1)

  return heatPoints.map((p) => {
    const raw = projection([p.longitude, p.latitude])
    const px = raw?.[0] ?? 0
    const py = raw?.[1] ?? 0
    // 与标签 / 地形一致
    const lx = px
    const ly = -py
    const u = (lx - minX) / w
    const v = (ly - minY) / h
    return {
      x: Math.floor(Math.min(size - 1, Math.max(0, u * size))),
      // flipY：顶边 cy=0 → v=1，故 cy = (1-v)*size
      y: Math.floor(Math.min(size - 1, Math.max(0, (1 - v) * size))),
      value: Number(p.gatherValue ?? p.value) || 0
    }
  })
}

/**
 * 热力面与地形 bbox 对齐，UV 与省界贴图一致，避免「只有北京」错位假象
 */
export function createHeatmapMesh(projection, heatPoints, options = {}) {
  const {
    size = TEX_SIZE,
    depth = 6,
    maskTexture = null,
    bbox = null
  } = options

  if (!bbox) {
    throw new Error('createHeatmapMesh: bbox 必填，否则热力无法与地图对齐')
  }

  const values = heatPoints.map((p) => Number(p.gatherValue ?? p.value) || 0)
  const maxVal = values.length ? Math.max(...values, 1) : 1

  const projected = projectHeatPoints(projection, heatPoints, size, bbox)
  const { heatTexture, greyTexture } = renderHeatTextures(projected, size, maxVal)

  const material = createHeatmapMaterial(null)
  material.uniforms.heatMap.value = heatTexture
  material.uniforms.greyMap.value = greyTexture

  const mapW = Math.max(bbox.max.x - bbox.min.x, 1)
  const mapH = Math.max(bbox.max.y - bbox.min.y, 1)
  const cx = (bbox.min.x + bbox.max.x) / 2
  const cy = (bbox.min.y + bbox.max.y) / 2

  const group = new Group()
  group.position.z = depth + HEAT_FLOAT_ABOVE

  const mesh = new Mesh(new PlaneGeometry(mapW, mapH, 300, 300), material)
  mesh.position.set(cx, cy, 0)
  mesh.renderOrder = 50
  mesh.frustumCulled = false
  group.add(mesh)

  return {
    group,
    mesh,
    dispose() {
      heatTexture.dispose()
      greyTexture.dispose()
      material.dispose()
      mesh.geometry.dispose()
    },
    update(nextPoints) {
      const list = nextPoints || []
      const nextValues = list.map((p) => Number(p.gatherValue ?? p.value) || 0)
      const nMax = nextValues.length ? Math.max(...nextValues, 1) : 1
      const nextProjected = projectHeatPoints(projection, list, size, bbox)
      const canvases = renderHeatTextures(nextProjected, size, nMax)
      material.uniforms.heatMap.value.dispose()
      material.uniforms.greyMap.value.dispose()
      material.uniforms.heatMap.value = canvases.heatTexture
      material.uniforms.greyMap.value = canvases.greyTexture
    }
  }
}
