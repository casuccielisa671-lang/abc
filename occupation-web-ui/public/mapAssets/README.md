# 地图素材目录

本目录存放 **离线导出的静态图片**，供就业分析页 3D 中国地图使用。

| 文件名 | 用途 | 来源 |
|--------|------|------|
| `china_height.png` | 全国高度灰度贴图（heightMap） | sat-hunter + ESRI 地形图（Hill Shade） |
| `china_border.png` | 全国轮廓遮罩 / 边界参考图 | sat-hunter + 浅色底图 + 区域轮廓裁剪 |
| `china_satellite.png` | 高德卫星影像底图（可选，优先于在线瓦片） | sat-hunter + 底图选「高德 卫星影像」 |

> sat-hunter 仓库仅作**离线导出工具**，代码不集成进本项目。  
> 完整导出步骤见：`docs/sat-hunter-export-guide.md`

## 前端引用示例（第 3 轮开发时使用）

```js
const heightUrl = '/mapAssets/china_height.png'
const borderUrl = '/mapAssets/china_border.png'
```

## 注意事项

- 两张 PNG 需**同范围、同 Zoom 级别**导出，否则 3D 贴图会对不齐。
- 建议全国范围统一使用 **Zoom = 6**。
- 文件较大时可压缩，但需保持宽高比一致。
