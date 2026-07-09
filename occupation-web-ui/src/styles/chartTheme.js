import * as echarts from 'echarts'

/**
 * 统一 ECharts 主题
 *
 * 分类色板：八个色相族与 DESIGN.md 的强调色一一对应（blue→Link Blue、
 * aqua→Grass Green、yellow→Honey、green→Mint、violet→Plum、red→Alert、
 * magenta→Coral、orange→Ember），但取的是各族中**通过校验**的那一档 ——
 * DESIGN.md 的原始亮色是插画填充值，作为图表标记时亮度/对比度不达标。
 *
 * 已用色板校验器逐项验证（浅色底 #ffffff、深色底 #1b1a18）：
 *   浅色：亮度带 PASS，色度下限 PASS，色盲相邻 ΔE 24.2 PASS，
 *         其中 aqua/yellow/magenta 对比度 < 3:1 → 必须配可见标签或 tooltip（本项目图表均有）
 *   深色：亮度带 PASS，色度下限 PASS，对比度 PASS，
 *         色盲相邻最差 ΔE 10.3（8–12 兜底带）→ 同样依赖直接标签作为二次编码
 *
 * 使用规则（勿违反，否则色板的安全性失效）：
 *   · 槽位按固定顺序分配，绝不循环复用；第 9 个系列请归入「其他」
 *   · 单系列图表统一用槽位 1，不要按数值给柱子上色
 *   · 禁止双 Y 轴：数量与薪资拆成两张图（参考 Dashboard.vue）
 *   · 文字（数值、图例、坐标轴）一律用文字色，不要染成系列色
 */

/** 分类色板：八槽固定顺序 */
export const CHART_COLORS = {
  light: ['#2a78d6', '#1baf7a', '#eda100', '#008300', '#4a3aa7', '#e34948', '#e87ba4', '#eb6834'],
  dark: ['#3987e5', '#199e70', '#c98500', '#008300', '#9085e9', '#e66767', '#d55181', '#d95926']
}

/** 单系列图表（柱/折线）统一用槽位 1 */
export function primarySeriesColor(dark) {
  return dark ? CHART_COLORS.dark[0] : CHART_COLORS.light[0]
}

/** 薪资类图表用「钱」的语义色，与 .salary-text 保持一致 */
export function moneySeriesColor(dark) {
  return dark ? '#35d98f' : '#00854f'
}

function buildTheme(dark) {
  const ink = dark ? '#f2f0ed' : '#343433'
  const inkMuted = dark ? '#9d978e' : '#6f6e6c'
  const grid = dark ? '#2e2b28' : '#f2f0ed'
  const axis = dark ? '#3a3733' : '#e5e1da'
  const surface = dark ? '#1b1a18' : '#ffffff'

  return {
    color: dark ? CHART_COLORS.dark : CHART_COLORS.light,
    backgroundColor: 'transparent',
    textStyle: { color: ink, fontFamily: 'Inter, "PingFang SC", "Microsoft YaHei", sans-serif' },
    title: { textStyle: { color: ink, fontSize: 14, fontWeight: 600 } },
    legend: { textStyle: { color: inkMuted, fontSize: 12 }, itemWidth: 14, itemHeight: 8 },
    tooltip: {
      backgroundColor: surface,
      borderColor: axis,
      textStyle: { color: ink, fontSize: 12 },
      // 十字准星：折线/面积图的默认悬浮层
      axisPointer: { lineStyle: { color: axis }, crossStyle: { color: axis } }
    },
    categoryAxis: {
      axisLine: { lineStyle: { color: axis } },
      axisTick: { show: false },
      axisLabel: { color: inkMuted, fontSize: 11 },
      splitLine: { show: false }
    },
    valueAxis: {
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: inkMuted, fontSize: 11 },
      splitLine: { lineStyle: { color: grid } },
      nameTextStyle: { color: inkMuted }
    },
    line: {
      lineStyle: { width: 2 },
      symbol: 'circle',
      symbolSize: 8,
      // 标记描一圈底色，重叠时仍能分辨
      itemStyle: { borderWidth: 2, borderColor: surface }
    },
    bar: {
      barMaxWidth: 24,
      // 数据端 4px 圆角，锚在基线上
      itemStyle: { borderRadius: [4, 4, 0, 0] }
    },
    pie: {
      label: { color: inkMuted, fontSize: 12 },
      labelLine: { lineStyle: { color: axis } },
      // 2px 底色间隙，扇区之间不直接相接
      itemStyle: { borderColor: surface, borderWidth: 2 }
    }
  }
}

let registered = false

export function registerChartThemes() {
  if (registered) return
  echarts.registerTheme('app-light', buildTheme(false))
  echarts.registerTheme('app-dark', buildTheme(true))
  registered = true
}

/** 当前应使用的主题名 */
export function chartThemeName(dark) {
  return dark ? 'app-dark' : 'app-light'
}
