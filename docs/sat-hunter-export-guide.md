# sat-hunter 全国地图素材导出指南（Windows）

> 工具仓库：[knight-L/sat-hunter](https://github.com/knight-L/sat-hunter)  
> 用途：离线导出 `china_height.png`（高度灰度图）与 `china_border.png`（全国轮廓图）  
> **不要**把 sat-hunter 源码放进职业能力平台仓库。

---

## 一、工具说明

sat-hunter 是一个 **Vue3 + Leaflet 网页工具**，没有命令行导出脚本。流程是：

1. 本地启动开发服务器  
2. 浏览器里选全国范围、选底图、点下载  
3. 浏览器自动保存 PNG，手动重命名后复制到本项目

内置底图与用途：

| 底图名称 | 内部 ID | 导出文件 |
|----------|---------|----------|
| **地形图** | `esri_hill_shade` | → `china_height.png` |
| **浅色灰底图** | `esri_light_gray` | → `china_border.png`（轮廓遮罩） |

全国行政区划数据来自阿里云 DataV：`geo.datav.aliyun.com`（工具内置，无需配置）。

---

## 二、环境要求

- Windows 10/11  
- **Node.js 18+**（`node -v` 检查）  
- **Git**  
- 可访问 ESRI / DataV 瓦片服务（需网络；部分源需科学上网）

---

## 三、安装与启动（PowerShell）

在**任意工作目录**（不要放在 `abc` 项目里）执行：

```powershell
# 1. 克隆 sat-hunter（仅作离线工具）
cd $env:USERPROFILE\Tools
git clone https://github.com/knight-L/sat-hunter.git
cd sat-hunter

# 2. 安装依赖（任选其一）
npm install
# 或
pnpm install

# 3. 启动本地服务
npm run dev
# 或
pnpm dev
```

终端出现类似：

```text
  ➜  Local:   http://localhost:5173/
```

浏览器打开 **http://localhost:5173**。

---

## 四、可选：修改 sat-hunter 默认全国视图

工具默认中心在北京。若希望打开即看到全国，可编辑克隆目录下的 `src/App.vue`：

### 4.1 默认地图中心改为全国

找到 `onMounted` 里创建地图的代码：

```javascript
// 原代码
const mapInstance = L.map(mapContainer.value).setView([39.9042, 116.4074], 10);

// 改为全国视角
const mapInstance = L.map(mapContainer.value).setView([35.0, 105.0], 4);
```

### 4.2 国家级默认 Zoom（已有逻辑，无需改）

工具在选中「国家级」区域时会自动设 Zoom = 6：

```javascript
const level = { country: 6, province: 10, city: 10, district: 14 }?.[selectedData.value?.level ?? ""];
```

全国导出请保持 **Zoom = 6**（左侧面板滑块或快捷按钮「国家级 (6)」）。

改完后重新 `npm run dev` 刷新浏览器。

---

## 五、导出 china_height.png（高度灰度图）

在浏览器 sat-hunter 页面操作：

| 步骤 | 操作 |
|------|------|
| 1 | 左侧选 **「按区域轮廓下载」** |
| 2 | 搜索框输入 **「中国」**，选择 **中华人民共和国**（adcode `100000`） |
| 3 | 确认 Zoom = **6**（预计瓦片约 80–200 张，远小于 1500 上限） |
| 4 | 底图切换为 **「地形图」**（ESRI World Hillshade） |
| 5 | 地图上出现全国红色轮廓后，点 **「开始下载」** |
| 6 | 等待进度 100%，浏览器下载 `100000_z6.png`（或类似名称） |
| 7 | 重命名为 **`china_height.png`** |

---

## 六、导出 china_border.png（全国轮廓图）

**同范围、同 Zoom**，只换底图：

| 步骤 | 操作 |
|------|------|
| 1–3 | 同上：区域轮廓 → **中国** → Zoom **6** |
| 4 | 底图切换为 **「浅色灰底图」**（`esri_light_gray`） |
| 5 | 点 **「开始下载」** |
| 6 | 重命名为 **`china_border.png`** |

说明：polygon 模式会用 GeoJSON 做 `destination-in` 裁剪，导出图**仅保留国界内像素**，国境外透明，适合作为 3D 地图 alpha 遮罩 / 边界参考。

> sat-hunter **不会**单独导出「省界线条图层」。省界在后续第 3 轮由 GeoJSON + Three.js 绘制；此图主要用于全国外轮廓对齐。

---

## 七、复制到 Vue 项目

在 PowerShell 中（按你的实际下载路径调整 `$Downloads`）：

```powershell
# 创建目标目录（若不存在）
New-Item -ItemType Directory -Force -Path "F:\CSU_Workin\abc\occupation-web-ui\public\mapAssets"

# 复制并重命名后的文件
Copy-Item "$env:USERPROFILE\Downloads\china_height.png" "F:\CSU_Workin\abc\occupation-web-ui\public\mapAssets\china_height.png"
Copy-Item "$env:USERPROFILE\Downloads\china_border.png"  "F:\CSU_Workin\abc\occupation-web-ui\public\mapAssets\china_border.png"
```

最终目录结构：

```text
occupation-web-ui/
└── public/
    └── mapAssets/
        ├── china_height.png   ← 高度灰度贴图
        ├── china_border.png   ← 全国轮廓遮罩
        └── README.md
```

前端静态资源 URL：

- `/mapAssets/china_height.png`
- `/mapAssets/china_border.png`

---

## 八、常见问题

| 问题 | 处理 |
|------|------|
| 瓦片数量超过 1500 | 降低 Zoom（全国用 6，不要用 10+） |
| 下载图片全白/全黑 | 换网络或换底图（ESRI 源可能被墙） |
| 区域搜不到「中国」 | 等左侧「区域数据加载完成」后再搜；或手动矩形框选全国范围 |
| 两张图对不齐 | 必须用**同一 adcode、同一 Zoom、同一下载模式** |
| 浏览器内存不足 | 关闭其他标签页；Zoom 保持 6 |

---

## 九、与职业能力平台的关系

```text
sat-hunter（独立工具，用完可删）
    ↓ 浏览器导出 PNG
occupation-web-ui/public/mapAssets/
    ↓ 第 3 轮 Vue 代码引用
就业分析页 3D 中国地图热力图
```

热力值数据来自 Spark 计算的「城市错配比」，与 sat-hunter 无关；sat-hunter **只负责地形/轮廓贴图素材**。
