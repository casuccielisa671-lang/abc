# Frontend Form & Component Pattern Exploration - Findings Report

Explored the occupation-web-ui Vue 3 project to document implementations of:
- Password input patterns (LoginView.vue)
- Custom component styling and Element Plus integration
- Toggle/switch implementations (theme toggle in AppLayout)
- Dark mode state management
- Responsive design patterns
- Global design system architecture

---

## 1. PASSWORD INPUT IMPLEMENTATION

### Current Approach (LoginView.vue, lines 47-49)
Uses Element Plus el-input with type="password" and show-password attribute:

```vue
<el-form-item prop="password" label="密码">
  <el-input 
    v-model="form.password" 
    type="password" 
    placeholder="请输入密码" 
    show-password 
  />
</el-form-item>
```

### Technical Details
- type="password" = native HTML input type
- show-password = Element Plus attribute for built-in toggle
- No custom component needed - Element Plus handles all UX
- Icon toggle managed internally by Element Plus
- Form validation uses :rules prop with validators

### Visual Styling
- Styled via theme.css: .login-card :deep(.el-input__wrapper)
- Background: rgba(255, 255, 255, 0.05) in light mode
- Focus state: border rgba(100, 180, 255, 0.6) with glow
- Uses :deep() CSS combinator to penetrate Element Plus scoping

---

## 2. CUSTOM COMPONENT STYLING

### Design System Architecture
- Location: src/styles/theme.css (415 lines)
- Approach: Global CSS custom properties (--app-*) + Element Plus token overrides

### Theme Token Categories

Surface/Structural:
- --app-canvas: #fbfaf9 (page background: cream)
- --app-surface: #ffffff (card: pure white)
- --app-surface-2: #f6f4ef (secondary: sand)
- --app-stone: #f2f0ed (inner borders)

Text Colors (WCAG compliant):
- --app-ink: #343433 (titles: 11.95:1)
- --app-ink-2: #474645 (body text: 9.03:1)
- --app-ink-3: #6f6e6c (secondary: 4.89:1)

Semantic Colors (all >= 4.5:1):
- --app-money: #00854f (salary)
- --app-score: #9b6800 (match score)
- --app-link: #0073d9 (links)
- --app-ember: #d93500 (secondary action)
- --app-danger: #de2532 (alerts)

Elevation:
- --app-hairline: inset 0 0 0 1px var(--app-stone)
- --app-overlay-shadow: for floats only (dialog/dropdown/tooltip)

### Shared Style Classes

Page Structure:
- .page-head, .page-title, .page-sub

Statistics:
- .stat-grid, .stat-card, .stat-value, .stat-value.accent

Chip (Neutral badge):
- .chip, .chip.learn (amber variant)

Job Cards:
- .job-grid, .job-card, .job-title, .job-salary, etc.

---

## 3. THEME TOGGLE IMPLEMENTATION

### Location: src/components/AppLayout.vue (lines 44-46)

```vue
<el-button 
  text 
  circle 
  :title="appStore.dark ? '切换到浅色模式' : '切换到深色模式'" 
  @click="appStore.toggleTheme()"
>
  <el-icon :size="16">
    <Sunny v-if="appStore.dark" />
    <Moon v-else />
  </el-icon>
</el-button>
```

### State Management (store/app.js)

The store manages theme persistence via localStorage:

```javascript
export const useAppStore = defineStore('app', () => {
  const dark = ref(localStorage.getItem('theme') === 'dark')
  
  function applyTheme() {
    document.documentElement.classList.toggle('dark', dark.value)
  }
  
  function toggleTheme() {
    dark.value = !dark.value
    localStorage.setItem('theme', dark.value ? 'dark' : 'light')
    applyTheme()
  }
  
  applyTheme()
  return { dark, toggleTheme }
})
```

### State Flow
1. Read localStorage on app start
2. Toggle adds/removes 'dark' class to <html>
3. CSS variables in html.dark block apply automatically
4. Preference saved to localStorage
5. No stylesheet switching needed - pure CSS variable approach

---

## 4. DARK MODE APPLICATION

### How It Works

All pages automatically adapt via CSS variable cascade:

```css
:root {
  --app-canvas: #fbfaf9;
  --app-ink: #343433;
}

html.dark {
  --app-canvas: #121110;
  --app-ink: #f2f0ed;
}

body { 
  background: var(--app-canvas); 
  color: var(--app-ink); 
}
```

### Design Principles

1. Warm near-black canvas (#121110) not pure black
2. Cards lighter than canvas for elevation
3. Main action flips from dark-on-light to light-on-dark
4. No drop shadows - elevation via border/lightness only

### Element Plus Components

- Imports both light and dark CSS from element-plus
- Project overrides --el-* tokens with custom colors
- Automatic adaptation across all components

---

## 5. TOGGLE/SWITCH IMPLEMENTATIONS

### 1. Theme Toggle Button
- Type: Icon button with conditional rendering
- Component: <el-button text circle>
- State: appStore.dark (boolean)
- Callback: appStore.toggleTheme()
- Visual: Icon changes (Sunny <-> Moon)

### 2. Sidebar Collapse Toggle
- Similar pattern: icon button with conditional icon
- Calls: appStore.toggleSidebar()
- Pattern: text variant, conditional icon

### 3. Element Plus Native Switch
- <el-switch> available but NOT used in project
- Project prefers buttons/icons instead
- Could be added for future feature toggles

---

## 6. RESPONSIVE DESIGN PATTERNS

### Media Query Breakpoints Used
- 1200px (tablet large to desktop)
- 1024px (tablet to desktop transition)
- 960px (tablet to narrow desktop)
- 900px (tablet specific)
- 768px (tablet to mobile)
- 640px (mobile specific)

### Desktop-first Approach

Default: side-by-side layout
Tablet (max-width: 1024px): stack vertically
Mobile (max-width: 640px): reduce padding

### Grid/Flex Patterns

.stat-grid: grid-template-columns: repeat(auto-fit, minmax(190px, 1fr))
.job-grid: grid-template-columns: repeat(auto-fill, minmax(340px, 1fr))

---

## 7. FORM VALIDATION PATTERN

Standard Element Plus approach:

```javascript
const rules = {
  tenantName: [{ required: true, message: '...', trigger: 'change' }],
  username: [{ required: true, message: '...', trigger: 'blur' }],
  password: [{ required: true, message: '...', trigger: 'blur' }]
}

async function handleLogin() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
}
```

Triggers: 'blur' (focus loss), 'change' (value change)

---

## 8. PROJECT DEPENDENCIES

Core Stack:
- Vue 3.4.0
- Vue Router 4.3.0
- Pinia 2.1.0
- Element Plus 2.5.0
- @element-plus/icons-vue 2.3.0

Utilities:
- ECharts 5.5.0
- Axios 1.6.0
- Three 0.170.0
- d3-geo 3.1.1

Key Points:
- Element Plus is SOLE UI component library
- Styling: Pure CSS (no CSS-in-JS, Tailwind, etc.)
- Typography: Inter via system font stack
- No external font files loaded

---

## SUMMARY: WHAT EXISTS vs WHAT DOESN'T

EXISTS:
- Password input: el-input type="password" show-password
- Theme toggle: Icon button calling appStore.toggleTheme()
- Dark mode: CSS variables on html.dark
- Global design system: 415-line theme.css
- Responsive breakpoints: 640px-1200px
- Form validation: Element Plus form rules
- Component styling: Pure CSS with :deep()

DOESN'T EXIST:
- Custom toggle/switch components
- CSS-in-JS (styled-components, emotion, etc.)
- Tailwind CSS
- Material-UI or other UI libraries
- Separate dark/light stylesheets
- Per-page dark mode config

---

KEY FILES EXAMINED

src/store/app.js                     # Theme + sidebar toggle
src/views/login/LoginView.vue        # Password input + glass morphism
src/components/AppLayout.vue         # Theme toggle button
src/styles/theme.css                 # Global design system (415 lines)
src/styles/chartTheme.js             # ECharts color themes
src/main.js                          # Vue setup + Element Plus

---

END OF EXPLORATION REPORT
