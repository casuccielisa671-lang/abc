import {
  CanvasTexture,
  Color,
  DoubleSide,
  Group,
  Mesh,
  PlaneGeometry,
  ShaderMaterial
} from 'three'
// sc-datav 同款热力库
import heatmapJs from 'keli-heatmap.js'

/** sc-datav Demo1 完全一致的热力渐变 */
const SC_DATAV_GRADIENT = {
  0.5: '#1fc2e1',
  0.6: '#24d560',
  0.7: '#9cd522',
  0.8: '#f1e12a',
  0.9: '#ffbf3a',
  1.0: '#ff0000'
}

function createHeatmapMaterial(maskTexture) {
  return new ShaderMaterial({
    transparent: true,
    side: DoubleSide,
    depthWrite: false,
    uniforms: {
      heatMap: { value: null },
      greyMap: { value: null },
      maskMap: { value: maskTexture || null },
      useMask: { value: maskTexture ? 1.0 : 0.0 },
      z_scale: { value: 4.0 },
      u_color: { value: new Color('#ffffff') },
      u_opacity: { value: 1.0 }
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
      uniform sampler2D maskMap;
      uniform float useMask;
      uniform vec3 u_color;
      uniform float u_opacity;
      void main() {
        vec4 heat = texture2D(heatMap, vUv);
        if (heat.a < 0.02) discard;
        float alpha = heat.a * u_opacity;
        if (useMask > 0.5) {
          float maskAlpha = texture2D(maskMap, vUv).a;
          if (maskAlpha < 0.08) discard;
          alpha *= maskAlpha;
        }
        gl_FragColor = vec4(u_color * heat.rgb, alpha);
      }
    `
  })
}

/** sc-datav heatmap.tsx — keli-heatmap.js 双通道绘制 */
function renderScDatavHeatTextures(points, size, minVal, maxVal) {
  const container = document.createElement('div')
  container.style.cssText = 'position:absolute;top:-9999px;left:-9999px;'
  document.body.appendChild(container)

  const radius = 10
  const heatmap = heatmapJs.create({
    container,
    gradient: SC_DATAV_GRADIENT,
    blur: 1,
    radius,
    maxOpacity: 1,
    width: size,
    height: size
  })

  const greymap = heatmapJs.create({
    container,
    gradient: { 0.0: 'black', 1.0: 'white' },
    radius,
    maxOpacity: 1,
    width: size,
    height: size
  })

  heatmap.setData({ max: maxVal, min: minVal, data: points })
  greymap.setData({ max: maxVal, min: minVal, data: points })

  const heatTexture = new CanvasTexture(heatmap._renderer.canvas)
  heatTexture.needsUpdate = true
  const greyTexture = new CanvasTexture(greymap._renderer.canvas)
  greyTexture.needsUpdate = true

  document.body.removeChild(container)

  return { heatTexture, greyTexture }
}

function projectHeatPoints(projection, heatPoints, size) {
  return heatPoints.map((p) => {
    const [x = 0, y = 0] = projection([p.longitude, p.latitude]) ?? [0, 0]
    return {
      x: Math.floor(x + size / 2),
      y: Math.floor(y + size / 2),
      value: Number(p.gatherValue ?? p.value) || 0
    }
  })
}

/**
 * sc-datav 结构：热力 mesh 放在 map group 内 position-z = depth+1，无额外 rotation
 */
export function createHeatmapMesh(projection, heatPoints, options = {}) {
  const { size = 500, depth = 6, maskTexture = null } = options
  const values = heatPoints.map((p) => Number(p.gatherValue ?? p.value) || 0)
  const minVal = values.length ? Math.min(...values) : 0
  const maxVal = values.length ? Math.max(...values, minVal + 1) : 1

  const projected = projectHeatPoints(projection, heatPoints, size)
  const { heatTexture, greyTexture } = renderScDatavHeatTextures(
    projected,
    size,
    minVal,
    maxVal
  )

  const material = createHeatmapMaterial(maskTexture)
  material.uniforms.heatMap.value = heatTexture
  material.uniforms.greyMap.value = greyTexture

  const group = new Group()
  group.position.z = depth + 1

  const mesh = new Mesh(new PlaneGeometry(size, size, 300, 300), material)
  mesh.renderOrder = 11
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
      const nextValues = nextPoints.map((p) => Number(p.gatherValue ?? p.value) || 0)
      const nMin = Math.min(...nextValues)
      const nMax = Math.max(...nextValues, nMin + 1)
      const nextProjected = projectHeatPoints(projection, nextPoints, size)
      const canvases = renderScDatavHeatTextures(nextProjected, size, nMin, nMax)
      material.uniforms.heatMap.value.dispose()
      material.uniforms.greyMap.value.dispose()
      material.uniforms.heatMap.value = canvases.heatTexture
      material.uniforms.greyMap.value = canvases.greyTexture
    }
  }
}
