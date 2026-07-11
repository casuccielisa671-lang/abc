## 职业能力大数据服务平台 UI 优化方案

### 一、项目现状分析

**当前问题：**
- 整体配色不统一（白色导航 + 紫色背景 + 多种卡片透明效果）
- 缺乏统一的颜色规范和使用指南
- 页面视觉层次不够清晰（特别是匹配分、薪资、标签）

**现有优势：**
- 已有完整的设计系统框架（theme.css）
- 深浅主题系统已完善
- 玻璃态设计手法成熟

---

### 二、新设计系统架构

#### 1. 核心色系

**主品牌色：科技蓝**
```
--color-primary: #2563EB        主色（按钮、链接、强调）
--color-primary-light: #DBEAFE  极浅（背景/悬停）
--color-primary-dark: #1E40AF   深色（活跃态）
```

**辅助色：AI紫**
```
--color-secondary: #6366F1      AI、推荐、高级功能
--color-secondary-light: #E0E7FF
```

**中性色体系：蓝灰系**
```
背景：#F8FAFC → #F1F5F9 → #E2E8F0
文字：#0F172A → #475569 → #64748B
边框：#E2E8F0
```

**语义色：**
- 成功/薪资：#10B981（绿）
- 警告/推荐：#F59E0B（琥珀）
- 危险/失败：#EF4444（红）
- 信息：#3B82F6（蓝）

#### 2. 设计令牌优先级

| 优先级 | 用途 | 示例 |
|-------|------|------|
| 最高 | 主操作、核心功能 | 按钮、重要链接、活跃菜单 |
| 高 | 二级操作、强调 | 副按钮、标签、卡片边框 |
| 中 | 容器、背景 | 卡片、表格、输入框 |
| 低 | 文字、图标、分隔 | 正文、辅助文字、边框 |

---

### 三、实施清单

#### 第1阶段：颜色系统集成

- [x] 创建 `tech-colors.css`（全局颜色变量）
- [ ] 更新 `theme.css`（集成新色系）
- [ ] 更新 Element Plus CSS 变量覆写
- [ ] 验证浅色/深色模式对比度

#### 第2阶段：关键页面适配

**导航层：**
- [ ] MainLayout.vue - 背景改为科技蓝渐变
- [ ] 菜单色调与主题对齐
- [ ] 顶部按钮/标签规范化

**内容层：**
- [ ] Dashboard 看板 - 统计卡片规范
- [ ] 卡片组件 - 统一阴影和边框
- [ ] 按钮组件 - 主/副操作色分离
- [ ] 表格 - 表头背景和行分隔

**数据展示：**
- [ ] 匹配分 - 统一使用琥珀黄
- [ ] 薪资数字 - 统一使用成功绿
- [ ] 技能标签 - 多色分类标签
- [ ] 状态标签 - 语义色规范

#### 第3阶段：全局优化

- [ ] 图表主题更新（ECharts）
- [ ] 表单组件样式统一
- [ ] 弹窗/Popover 样式统一
- [ ] 响应式适配检查

---

### 四、具体改动指南

#### MainLayout.vue 背景改动

**改前：**
```css
background: linear-gradient(135deg, #f0f8ff 0%, #dce9ff 50%, #e8dff5 100%);
```

**改后（日光模式）：**
```css
background: linear-gradient(135deg, #F8FAFC 0%, #EFF6FF 50%, #F0F4FF 100%);
```

**深色模式：**
```css
background: linear-gradient(135deg, #0F172A 0%, #1E293B 50%, #1F2937 100%);
```

#### 卡片样式统一

**原则：**
- 所有卡片背景统一为 `--color-surface: #FFFFFF`
- 边框统一为 `--color-border: #E2E8F0` 1px
- 阴影统一为 `--shadow-md`（不再用玻璃态模糊）
- 悬停态背景改为 `--color-bg-tertiary: #E2E8F0`

**示例：**
```css
.card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  transition: all var(--transition-fast);
}

.card:hover {
  box-shadow: var(--shadow-lg);
  background: var(--color-surface-hover);
}
```

#### 按钮规范

**主操作按钮：**
```css
background: var(--color-primary);      /* #2563EB */
color: var(--color-text-inverse);      /* 白色 */
```

**副操作按钮：**
```css
background: var(--color-bg-tertiary);  /* 浅灰 */
color: var(--color-text-primary);      /* 深灰黑 */
border: 1px solid var(--color-border);
```

**危险按钮：**
```css
background: var(--color-danger);       /* #EF4444 */
color: var(--color-text-inverse);
```

#### 标签分类

| 用途 | 颜色 | 背景 |
|------|------|------|
| 默认/中性 | #475569 | #F1F5F9 |
| 成功/绿 | #10B981 | #ECFDF5 |
| 警告/琥珀 | #F59E0B | #FFFBEB |
| 危险/红 | #EF4444 | #FEE2E2 |
| 信息/蓝 | #3B82F6 | #EFF6FF |

---

### 五、检查清单

实施完成后，需要验证：

#### 视觉一致性
- [ ] 所有页面背景保持统一渐变
- [ ] 所有卡片边框和阴影一致
- [ ] 所有按钮颜色符合规范

#### 对比度合规（WCAG AA）
- [ ] 文字对卡片背景 ≥ 4.5:1
- [ ] 匹配分（琥珀）对浅色背景 ≥ 4.5:1
- [ ] 薪资（绿）对浅色背景 ≥ 4.5:1

#### 浅色/深色模式
- [ ] 所有语义色在深色模式下仍可见
- [ ] 深色背景配浅文字对比充分
- [ ] 过渡动画平滑（0.3s）

#### 页面覆盖
- [ ] 学生端：首页、职位推荐、个人画像、我的投递、收藏、报告、顾问
- [ ] 教师端：首页、学生管理、教学建议、数据看板
- [ ] HR端：首页、职位管理、投递列表、人才浏览
- [ ] 管理端：首页、数据看板、就业分析、采集管理、报告中心、用户管理

---

### 六、后续维护

#### 设计变更流程
1. 修改 `tech-colors.css` 中的颜色值
2. 在浅色/深色模式下同步更新
3. 对比度检查（使用在线工具：webaim.org/resources/contrastchecker）
4. 全量视觉回归测试

#### 新增组件规范
- 优先使用 `--color-primary` 系列作主色
- 强调/次级功能用 `--color-secondary` 系列
- 文字严格遵循 `--color-text-*` 体系
- 所有阴影统一用 `--shadow-*` 变量

#### 文档更新
- 后续维护者请更新此文件的「实施清单」
- 记录每次设计变更的原因和日期
- 保留旧配色方案的注释以备回滚

---

### 七、相关文件

**新增文件：**
- `src/styles/tech-colors.css` - 科技蓝色系统

**需要修改文件：**
- `src/styles/theme.css` - 集成新色系
- `src/components/MainLayout.vue` - 背景渐变
- `src/views/Home/HomeIndex.vue` - 背景适配
- `src/views/*/Dashboard.vue` 及其它看板类页面
- `src/views/*/Profile.vue` 等内容页

**参考文档：**
- `CLAUDE.md` - 项目架构说明
- `DESIGN.md` - 原设计规范
- `theme.css` - Element Plus 主题定制

