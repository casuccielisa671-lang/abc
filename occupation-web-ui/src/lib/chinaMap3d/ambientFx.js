import {
  AdditiveBlending,
  CanvasTexture,
  Color,
  DoubleSide,
  Group,
  Mesh,
  PlaneGeometry,
  RingGeometry,
  ShaderMaterial,
  Sprite,
  SpriteMaterial
} from 'three'

/**
 * Demo1 风格环境动效（精简）：
 * 暖色扩散扫光 + 网格呼吸 + 双环缓旋 + 软云精灵慢漂
 * @see https://github.com/knight-L/sc-datav Demo1 map/cloud.tsx · bottom.tsx
 */

function makeCloudTexture() {
  const size = 128
  const canvas = document.createElement('canvas')
  canvas.width = size
  canvas.height = size
  const ctx = canvas.getContext('2d')
  const g = ctx.createRadialGradient(size / 2, size / 2, 8, size / 2, size / 2, size / 2)
  g.addColorStop(0, 'rgba(255,252,245,0.85)')
  g.addColorStop(0.35, 'rgba(255,245,230,0.45)')
  g.addColorStop(0.7, 'rgba(255,236,210,0.12)')
  g.addColorStop(1, 'rgba(255,236,210,0)')
  ctx.fillStyle = g
  ctx.fillRect(0, 0, size, size)
  const tex = new CanvasTexture(canvas)
  tex.needsUpdate = true
  return tex
}

function makeMapAlignedRoot(offsetX = 20) {
  const g = new Group()
  g.rotation.x = -Math.PI / 2
  g.position.x = offsetX
  return g
}

function createScanRing(radius) {
  const uniforms = {
    uTime: { value: 0 },
    uColor: { value: new Color(0xea580c) },
    uMaxR: { value: radius }
  }
  const material = new ShaderMaterial({
    transparent: true,
    depthWrite: false,
    side: DoubleSide,
    blending: AdditiveBlending,
    uniforms,
    vertexShader: `
      varying vec2 vUv;
      void main() {
        vUv = uv;
        gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
      }
    `,
    fragmentShader: `
      uniform float uTime;
      uniform vec3 uColor;
      uniform float uMaxR;
      varying vec2 vUv;
      void main() {
        vec2 p = (vUv - 0.5) * 2.0;
        float d = length(p) * uMaxR;
        float t = mod(uTime * 26.0, uMaxR + 50.0);
        float w = min(30.0, 10.0 + uTime * 3.0);
        float a = 0.0;
        if (d > t && d < t + 2.0 * w) {
          a = d < t + w
            ? (d - t) / w * 0.55
            : (1.0 - (d - t - w) / w) * 0.55;
        }
        if (a < 0.01) discard;
        gl_FragColor = vec4(uColor, a);
      }
    `
  })
  const mesh = new Mesh(new PlaneGeometry(radius * 2.4, radius * 2.4), material)
  mesh.renderOrder = 2
  return {
    mesh,
    uniforms,
    dispose() {
      material.dispose()
      mesh.geometry.dispose()
    }
  }
}

function createSoftGrid(radius) {
  const uniforms = {
    uTime: { value: 0 },
    uColor: { value: new Color(0xf97316) }
  }
  const material = new ShaderMaterial({
    transparent: true,
    depthWrite: false,
    side: DoubleSide,
    uniforms,
    vertexShader: `
      varying vec2 vUv;
      void main() {
        vUv = uv;
        gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
      }
    `,
    fragmentShader: `
      uniform float uTime;
      uniform vec3 uColor;
      varying vec2 vUv;
      void main() {
        vec2 uv = vUv * 36.0;
        vec2 gv = abs(fract(uv) - 0.5);
        float grid = 1.0 - smoothstep(0.0, 0.04, min(gv.x, gv.y));
        float pulse = 0.35 + 0.25 * sin(uTime * 1.2);
        float fade = smoothstep(0.55, 0.15, distance(vUv, vec2(0.5)));
        float a = grid * pulse * fade * 0.22;
        if (a < 0.01) discard;
        gl_FragColor = vec4(uColor, a);
      }
    `
  })
  const mesh = new Mesh(new PlaneGeometry(radius * 2.2, radius * 2.2), material)
  mesh.renderOrder = 1
  return {
    mesh,
    uniforms,
    dispose() {
      material.dispose()
      mesh.geometry.dispose()
    }
  }
}

function createOrbitRings(radius) {
  const group = new Group()
  const configs = [
    { r: radius * 0.92, color: '#fb923c', speed: 0.12, opacity: 0.35 },
    { r: radius * 1.08, color: '#fdba74', speed: -0.07, opacity: 0.25 }
  ]
  configs.forEach((cfg) => {
    const mat = new ShaderMaterial({
      transparent: true,
      depthWrite: false,
      side: DoubleSide,
      uniforms: {
        uColor: { value: new Color(cfg.color) },
        uOpacity: { value: cfg.opacity },
        uTime: { value: 0 }
      },
      vertexShader: `
        varying vec2 vUv;
        void main() {
          vUv = uv;
          gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 1.0);
        }
      `,
      fragmentShader: `
        uniform vec3 uColor;
        uniform float uOpacity;
        uniform float uTime;
        varying vec2 vUv;
        void main() {
          float pulse = 0.65 + 0.35 * sin(uTime * 1.8 + vUv.x * 12.0);
          gl_FragColor = vec4(uColor, uOpacity * pulse);
        }
      `
    })
    const mesh = new Mesh(new RingGeometry(cfg.r * 0.98, cfg.r, 96), mat)
    mesh.userData.speed = cfg.speed
    mesh.userData.uniforms = mat.uniforms
    group.add(mesh)
  })
  return {
    group,
    tick(dt, elapsed) {
      group.children.forEach((m) => {
        m.rotation.z += m.userData.speed * dt
        m.userData.uniforms.uTime.value = elapsed
      })
    },
    dispose() {
      group.children.forEach((m) => {
        m.geometry.dispose()
        m.material.dispose()
      })
    }
  }
}

function createCloudLayer(bbox, cloudTex) {
  const group = makeMapAlignedRoot()
  const midX = (bbox.min.x + bbox.max.x) / 2
  const midY = (bbox.min.y + bbox.max.y) / 2
  const spanX = Math.max(bbox.max.x - bbox.min.x, 80)
  const spanY = Math.max(bbox.max.y - bbox.min.y, 80)
  const sprites = []

  for (let i = 0; i < 14; i++) {
    const mat = new SpriteMaterial({
      map: cloudTex,
      transparent: true,
      depthWrite: false,
      depthTest: false,
      opacity: 0.18,
      color: new Color('#fff6ea')
    })
    const sp = new Sprite(mat)
    const sx = 55 + (i % 4) * 28
    sp.scale.set(sx, sx * (0.45 + (i % 3) * 0.08), 1)
    sp.renderOrder = 5
    sp.position.set(
      midX + (Math.sin(i * 1.7) * 0.55) * spanX * 0.55,
      midY + (Math.cos(i * 2.1) * 0.5) * spanY * 0.5,
      28 + (i % 4) * 8
    )
    sp.userData = { baseZ: sp.position.z, phase: i * 0.7, spin: 0.08 + (i % 5) * 0.02 }
    group.add(sp)
    sprites.push(sp)
  }

  return {
    group,
    tick(elapsed) {
      group.rotation.z = Math.sin(elapsed / 2.2) * 0.08
      sprites.forEach((sp) => {
        const u = sp.userData
        sp.material.rotation += 0.0015 * u.spin
        sp.position.z = u.baseZ + Math.sin(elapsed * 0.6 + u.phase) * 3.5
        sp.material.opacity = 0.12 + 0.1 * (0.5 + 0.5 * Math.sin(elapsed * 0.5 + u.phase))
      })
    },
    dispose() {
      sprites.forEach((sp) => sp.material.dispose())
    }
  }
}

/** @param {import('three').Box2} bbox */
export function createAmbientFx(bbox) {
  const midX = (bbox.min.x + bbox.max.x) / 2
  const midY = (bbox.min.y + bbox.max.y) / 2
  const radius = Math.max(bbox.max.x - bbox.min.x, bbox.max.y - bbox.min.y) * 0.55

  const fxRoot = makeMapAlignedRoot()
  fxRoot.position.z = -1.5

  const cloudTex = makeCloudTexture()
  const clouds = createCloudLayer(bbox, cloudTex)

  const scan = createScanRing(radius)
  scan.mesh.position.set(midX, midY, 0)
  fxRoot.add(scan.mesh)

  const grid = createSoftGrid(radius)
  grid.mesh.position.set(midX, midY, -0.3)
  fxRoot.add(grid.mesh)

  const rings = createOrbitRings(radius)
  rings.group.position.set(midX, midY, 0.2)
  fxRoot.add(rings.group)

  let elapsed = 0

  return {
    attach(scene) {
      scene.add(fxRoot)
      scene.add(clouds.group)
    },
    tick(dt) {
      elapsed += dt
      scan.uniforms.uTime.value = elapsed
      grid.uniforms.uTime.value = elapsed
      rings.tick(dt, elapsed)
      clouds.tick(elapsed)
    },
    dispose() {
      scan.dispose()
      grid.dispose()
      rings.dispose()
      clouds.dispose()
      cloudTex.dispose()
      fxRoot.parent?.remove(fxRoot)
      clouds.group.parent?.remove(clouds.group)
    }
  }
}
