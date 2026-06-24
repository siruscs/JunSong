import Cookies from 'js-cookie'

export interface ThemePreset {
  key: string
  name: string
  primary: string
  primaryLight: string
  primaryDark: string
  primaryRgb: string
  sidebarBg: string
  sidebarSubmenuBg: string
  sidebarActiveStart?: string
  sidebarActiveEnd?: string
  sidebarActiveText?: string
  sidebarHoverText?: string
  sidebarText?: string
  sidebarHoverBg?: string
  sidebarSubmenuActiveBg?: string
  sidebarActiveShadow?: string
  sidebarLogoText?: string
  appBg?: string
  appBgSoft?: string
  appSurface?: string
  appSurfaceMuted?: string
  appBorder?: string
  appText?: string
  appMuted?: string
  appHeaderBg?: string
  appHeaderShadow?: string
  loginBgStart: string
  loginBgEnd: string
  loginPanelStart: string
  loginPanelMid: string
  loginPanelEnd: string
}

export const themePresets: ThemePreset[] = [
  {
    key: 'green-nature',
    name: '翠绿自然',
    primary: '#2C6975',
    primaryLight: '#68B2A0',
    primaryDark: '#1E4D56',
    primaryRgb: '44, 105, 117',
    sidebarBg: '#1a3d45',
    sidebarSubmenuBg: '#1E4D56',
    sidebarActiveStart: '#68B2A0',
    sidebarActiveEnd: '#2C6975',
    sidebarActiveShadow: '0 10px 22px rgba(44, 105, 117, 0.28)',
    appBg: '#F4F8F9',
    appBgSoft: '#EEF5F6',
    appSurface: '#FFFFFF',
    appSurfaceMuted: '#F8FBFB',
    appBorder: 'rgba(44, 105, 117, 0.12)',
    appText: '#172033',
    appMuted: '#657084',
    appHeaderBg: 'rgba(255, 255, 255, 0.82)',
    appHeaderShadow: '0 8px 24px rgba(31, 77, 86, 0.08)',
    loginBgStart: '#1a3d45',
    loginBgEnd: '#2C4A4F',
    loginPanelStart: '#2C6975',
    loginPanelMid: '#68B2A0',
    loginPanelEnd: '#CDE0C9',
  },
  {
    key: 'blue-tech',
    name: '深蓝科技',
    primary: '#1E293B',
    primaryLight: '#3B82F6',
    primaryDark: '#0F172A',
    primaryRgb: '30, 41, 59',
    sidebarBg: '#0F172A',
    sidebarSubmenuBg: '#1E293B',
    sidebarActiveStart: '#3B82F6',
    sidebarActiveEnd: '#1E293B',
    sidebarActiveShadow: '0 10px 22px rgba(30, 41, 59, 0.28)',
    appBg: '#F5F7FB',
    appBgSoft: '#EEF3FA',
    appSurface: '#FFFFFF',
    appSurfaceMuted: '#F8FAFC',
    appBorder: 'rgba(30, 41, 59, 0.11)',
    appText: '#172033',
    appMuted: '#657084',
    appHeaderBg: 'rgba(255, 255, 255, 0.84)',
    appHeaderShadow: '0 8px 24px rgba(30, 41, 59, 0.08)',
    loginBgStart: '#0a1628',
    loginBgEnd: '#1E293B',
    loginPanelStart: '#1E293B',
    loginPanelMid: '#3B82F6',
    loginPanelEnd: '#93C5FD',
  },
  {
    key: 'citrus-breeze',
    name: '青柠海风',
    primary: '#8A9B2F',
    primaryLight: '#F2DC63',
    primaryDark: '#5F6F22',
    primaryRgb: '138, 155, 47',
    sidebarBg: '#214F52',
    sidebarSubmenuBg: '#1D464A',
    sidebarActiveStart: '#F2DC63',
    sidebarActiveEnd: '#BAC94A',
    sidebarActiveText: '#263C24',
    sidebarHoverText: '#F2DC63',
    sidebarText: '#D7E2CF',
    sidebarHoverBg: 'rgba(242, 220, 99, 0.14)',
    sidebarSubmenuActiveBg: 'rgba(242, 220, 99, 0.18)',
    sidebarActiveShadow: '0 8px 18px rgba(226, 211, 107, 0.34)',
    sidebarLogoText: '#F4F8D7',
    appBg: '#F5F8FB',
    appBgSoft: '#EEF4F6',
    appSurface: '#FFFFFF',
    appSurfaceMuted: '#F8FAF8',
    appBorder: 'rgba(138, 155, 47, 0.13)',
    appText: '#172033',
    appMuted: '#667085',
    appHeaderBg: 'rgba(255, 255, 255, 0.82)',
    appHeaderShadow: '0 8px 24px rgba(33, 79, 82, 0.08)',
    loginBgStart: '#214F52',
    loginBgEnd: '#586A2B',
    loginPanelStart: '#8A9B2F',
    loginPanelMid: '#E2D36B',
    loginPanelEnd: '#96D7C6',
  },
  {
    key: 'hermes-orange',
    name: '爱马仕橙',
    primary: '#B65318',
    primaryLight: '#E88A3A',
    primaryDark: '#6F2D0C',
    primaryRgb: '182, 83, 24',
    sidebarBg: '#FFF7EE',
    sidebarSubmenuBg: '#FCE8D4',
    sidebarActiveStart: '#D46B22',
    sidebarActiveEnd: '#B65318',
    sidebarActiveText: '#FFFFFF',
    sidebarHoverText: '#B65318',
    sidebarText: '#614634',
    sidebarHoverBg: 'rgba(182, 83, 24, 0.09)',
    sidebarSubmenuActiveBg: 'rgba(182, 83, 24, 0.14)',
    sidebarActiveShadow: '0 10px 22px rgba(182, 83, 24, 0.2)',
    sidebarLogoText: '#8A3A12',
    appBg: '#FBF7F3',
    appBgSoft: '#F7EEE5',
    appSurface: '#FFFFFF',
    appSurfaceMuted: '#FFF9F3',
    appBorder: 'rgba(182, 83, 24, 0.12)',
    appText: '#1F2933',
    appMuted: '#75675E',
    appHeaderBg: 'rgba(255, 252, 248, 0.86)',
    appHeaderShadow: '0 8px 24px rgba(111, 45, 12, 0.07)',
    loginBgStart: '#2B160D',
    loginBgEnd: '#7A3413',
    loginPanelStart: '#7A3413',
    loginPanelMid: '#B65318',
    loginPanelEnd: '#E88A3A',
  },
  {
    key: 'indigo-orbit',
    name: '靛蓝星河',
    primary: '#302080',
    primaryLight: '#6C63D9',
    primaryDark: '#21135F',
    primaryRgb: '48, 32, 128',
    sidebarBg: '#F7F5FF',
    sidebarSubmenuBg: '#ECE9FF',
    sidebarActiveStart: '#6C63D9',
    sidebarActiveEnd: '#302080',
    sidebarActiveText: '#FFFFFF',
    sidebarHoverText: '#302080',
    sidebarText: '#4B466E',
    sidebarHoverBg: 'rgba(48, 32, 128, 0.09)',
    sidebarSubmenuActiveBg: 'rgba(48, 32, 128, 0.14)',
    sidebarActiveShadow: '0 10px 22px rgba(48, 32, 128, 0.2)',
    sidebarLogoText: '#302080',
    appBg: '#F7F6FF',
    appBgSoft: '#EEECFF',
    appSurface: '#FFFFFF',
    appSurfaceMuted: '#FAF9FF',
    appBorder: 'rgba(48, 32, 128, 0.12)',
    appText: '#172033',
    appMuted: '#67627F',
    appHeaderBg: 'rgba(255, 255, 255, 0.84)',
    appHeaderShadow: '0 8px 24px rgba(48, 32, 128, 0.07)',
    loginBgStart: '#171039',
    loginBgEnd: '#302080',
    loginPanelStart: '#24185F',
    loginPanelMid: '#3D2B99',
    loginPanelEnd: '#6C63D9',
  },
  {
    key: 'sprout-green',
    name: '青芽翠绿',
    primary: '#8FB657',
    primaryLight: '#C7D99A',
    primaryDark: '#5C7D33',
    primaryRgb: '143, 182, 87',
    sidebarBg: '#26351F',
    sidebarSubmenuBg: '#1E2A19',
    sidebarActiveStart: '#B8D478',
    sidebarActiveEnd: '#7EA247',
    sidebarActiveText: '#FFFFFF',
    sidebarHoverText: '#E9F4D6',
    sidebarText: '#DCE8C4',
    sidebarHoverBg: 'rgba(143, 182, 87, 0.16)',
    sidebarSubmenuActiveBg: 'rgba(143, 182, 87, 0.22)',
    sidebarActiveShadow: '0 8px 18px rgba(143, 182, 87, 0.28)',
    sidebarLogoText: '#F6FAEF',
    appBg: '#F7FAF2',
    appBgSoft: '#EEF5E7',
    appSurface: '#FFFFFF',
    appSurfaceMuted: '#FBFDF8',
    appBorder: 'rgba(143, 182, 87, 0.14)',
    appText: '#172033',
    appMuted: '#65715B',
    appHeaderBg: 'rgba(255, 255, 255, 0.82)',
    appHeaderShadow: '0 8px 24px rgba(92, 125, 51, 0.08)',
    loginBgStart: '#1F2F18',
    loginBgEnd: '#5C7D33',
    loginPanelStart: '#334A24',
    loginPanelMid: '#6F933F',
    loginPanelEnd: '#B8D478',
  },
  {
    key: 'spring-festival',
    name: '瑞春节庆',
    primary: '#C62828',
    primaryLight: '#D6A23A',
    primaryDark: '#7A171A',
    primaryRgb: '198, 40, 40',
    sidebarBg: '#3A0C10',
    sidebarSubmenuBg: '#2B080C',
    sidebarActiveStart: '#D6A23A',
    sidebarActiveEnd: '#C62828',
    sidebarActiveText: '#FFF8E7',
    sidebarHoverText: '#FFD88A',
    sidebarText: '#F6D8C8',
    sidebarHoverBg: 'rgba(214, 162, 58, 0.14)',
    sidebarSubmenuActiveBg: 'rgba(198, 40, 40, 0.22)',
    sidebarActiveShadow: '0 8px 18px rgba(198, 40, 40, 0.3)',
    sidebarLogoText: '#FFE7A8',
    appBg: '#FBF5F2',
    appBgSoft: '#F8EAE3',
    appSurface: '#FFFFFF',
    appSurfaceMuted: '#FFF9F4',
    appBorder: 'rgba(198, 40, 40, 0.13)',
    appText: '#201A1A',
    appMuted: '#775D58',
    appHeaderBg: 'rgba(255, 251, 247, 0.86)',
    appHeaderShadow: '0 8px 24px rgba(122, 23, 26, 0.08)',
    loginBgStart: '#3A0C10',
    loginBgEnd: '#7A171A',
    loginPanelStart: '#4A0F12',
    loginPanelMid: '#9E1F22',
    loginPanelEnd: '#D6A23A',
  },
  {
    key: 'mist-blue',
    name: '霁蓝晨雾',
    primary: '#2A6F97',
    primaryLight: '#8EC8D2',
    primaryDark: '#173B57',
    primaryRgb: '42, 111, 151',
    sidebarBg: '#EEF6F8',
    sidebarSubmenuBg: '#DDECEF',
    sidebarActiveStart: '#5AA6B8',
    sidebarActiveEnd: '#2A6F97',
    sidebarActiveText: '#FFFFFF',
    sidebarHoverText: '#173B57',
    sidebarText: '#405A66',
    sidebarHoverBg: 'rgba(42, 111, 151, 0.09)',
    sidebarSubmenuActiveBg: 'rgba(42, 111, 151, 0.14)',
    sidebarActiveShadow: '0 10px 22px rgba(42, 111, 151, 0.18)',
    sidebarLogoText: '#173B57',
    appBg: '#F3F7F9',
    appBgSoft: '#EAF2F5',
    appSurface: '#FFFFFF',
    appSurfaceMuted: '#F8FBFC',
    appBorder: 'rgba(42, 111, 151, 0.12)',
    appText: '#172433',
    appMuted: '#60717C',
    appHeaderBg: 'rgba(255, 255, 255, 0.86)',
    appHeaderShadow: '0 8px 24px rgba(23, 59, 87, 0.07)',
    loginBgStart: '#10283A',
    loginBgEnd: '#2A6F97',
    loginPanelStart: '#173B57',
    loginPanelMid: '#2A6F97',
    loginPanelEnd: '#8EC8D2',
  },
  {
    key: 'apple-silver',
    name: '苹果银灰',
    primary: '#0071E3',
    primaryLight: '#64B5F6',
    primaryDark: '#004999',
    primaryRgb: '0, 113, 227',
    sidebarBg: '#FBFBFD',
    sidebarSubmenuBg: '#F5F5F7',
    sidebarActiveStart: '#0071E3',
    sidebarActiveEnd: '#0055B3',
    sidebarActiveText: '#FFFFFF',
    sidebarHoverText: '#0071E3',
    sidebarText: '#86868B',
    sidebarHoverBg: 'rgba(0, 113, 227, 0.06)',
    sidebarSubmenuActiveBg: 'rgba(0, 113, 227, 0.08)',
    sidebarActiveShadow: '0 10px 22px rgba(0, 113, 227, 0.18)',
    sidebarLogoText: '#1D1D1F',
    appBg: '#F5F5F7',
    appBgSoft: '#EFEFF1',
    appSurface: '#FFFFFF',
    appSurfaceMuted: '#FAFAFA',
    appBorder: 'rgba(0, 113, 227, 0.09)',
    appText: '#1D1D1F',
    appMuted: '#86868B',
    appHeaderBg: 'rgba(251, 251, 253, 0.88)',
    appHeaderShadow: '0 8px 24px rgba(0, 113, 227, 0.06)',
    loginBgStart: '#F5F5F7',
    loginBgEnd: '#E8E8ED',
    loginPanelStart: '#0071E3',
    loginPanelMid: '#0055B3',
    loginPanelEnd: '#64B5F6',
  },
]

export const defaultThemeKey = 'apple-silver'

export function getThemePreset(key: string): ThemePreset {
  return themePresets.find(t => t.key === key) || themePresets.find(t => t.key === defaultThemeKey)!
}

/** 简单的颜色混合：将主色与白色按比例混合 */
function mixWithWhite(hex: string, ratio: number): string {
  const r = parseInt(hex.slice(1, 3), 16)
  const g = parseInt(hex.slice(3, 5), 16)
  const b = parseInt(hex.slice(5, 7), 16)
  const nr = Math.round(r + (255 - r) * ratio)
  const ng = Math.round(g + (255 - g) * ratio)
  const nb = Math.round(b + (255 - b) * ratio)
  return `#${nr.toString(16).padStart(2, '0')}${ng.toString(16).padStart(2, '0')}${nb.toString(16).padStart(2, '0')}`
}

export function applyTheme(preset: ThemePreset): void {
  const root = document.documentElement
  root.style.setProperty('--theme-primary', preset.primary)
  root.style.setProperty('--theme-primary-light', preset.primaryLight)
  root.style.setProperty('--theme-primary-dark', preset.primaryDark)
  root.style.setProperty('--theme-primary-rgb', preset.primaryRgb)
  root.style.setProperty('--theme-sidebar-bg', preset.sidebarBg)
  root.style.setProperty('--theme-sidebar-submenu-bg', preset.sidebarSubmenuBg)
  root.style.setProperty('--theme-sidebar-active-start', preset.sidebarActiveStart || preset.primary)
  root.style.setProperty('--theme-sidebar-active-end', preset.sidebarActiveEnd || preset.primaryDark)
  root.style.setProperty('--theme-sidebar-active-shadow', preset.sidebarActiveShadow || `0 4px 12px rgba(${preset.primaryRgb}, 0.4)`)
  root.style.setProperty('--theme-sidebar-logo-text', preset.sidebarLogoText || '#FFFFFF')
  root.style.setProperty('--theme-login-bg-start', preset.loginBgStart)
  root.style.setProperty('--theme-login-bg-end', preset.loginBgEnd)
  root.style.setProperty('--theme-login-panel-start', preset.loginPanelStart)
  root.style.setProperty('--theme-login-panel-mid', preset.loginPanelMid)
  root.style.setProperty('--theme-login-panel-end', preset.loginPanelEnd)
  root.style.setProperty('--theme-link-color', preset.primary)
  root.style.setProperty('--theme-link-hover', preset.primaryDark)
  root.style.setProperty('--theme-focus-shadow', `rgba(${preset.primaryRgb}, 0.15)`)
  root.style.setProperty('--theme-hover-bg', mixWithWhite(preset.primary, 0.88))
  root.style.setProperty('--theme-hover-bg-light', `rgba(${preset.primaryRgb}, 0.12)`)
  root.style.setProperty('--theme-sidebar-hover', preset.sidebarHoverBg || `rgba(${preset.primaryRgb}, 0.15)`)
  root.style.setProperty('--theme-sidebar-submenu-active-bg', preset.sidebarSubmenuActiveBg || `rgba(${preset.primaryRgb}, 0.2)`)
  root.style.setProperty('--theme-sidebar-active-text', preset.sidebarActiveText || '#FFFFFF')
  root.style.setProperty('--theme-sidebar-hover-text', preset.sidebarHoverText || '#FFFFFF')
  root.style.setProperty('--theme-sidebar-text', preset.sidebarText || '#94A3B8')
  root.style.setProperty('--theme-app-bg', preset.appBg || '#F5F8FB')
  root.style.setProperty('--theme-app-bg-soft', preset.appBgSoft || mixWithWhite(preset.primary, 0.93))
  root.style.setProperty('--theme-app-surface', preset.appSurface || '#FFFFFF')
  root.style.setProperty('--theme-app-surface-muted', preset.appSurfaceMuted || '#F8FAFC')
  root.style.setProperty('--theme-app-border', preset.appBorder || `rgba(${preset.primaryRgb}, 0.12)`)
  root.style.setProperty('--theme-app-text', preset.appText || '#172033')
  root.style.setProperty('--theme-app-muted', preset.appMuted || '#667085')
  root.style.setProperty('--theme-app-header-bg', preset.appHeaderBg || 'rgba(255, 255, 255, 0.82)')
  root.style.setProperty('--theme-app-header-shadow', preset.appHeaderShadow || `0 8px 24px rgba(${preset.primaryRgb}, 0.08)`)
  root.style.setProperty('--theme-card-shadow', `0 18px 50px rgba(${preset.primaryRgb}, 0.1)`)

  // Element Plus CSS 变量覆盖
  root.style.setProperty('--el-color-primary', preset.primary)
  root.style.setProperty('--el-color-primary-light-3', preset.primaryLight)
  root.style.setProperty('--el-color-primary-light-5', mixWithWhite(preset.primary, 0.5))
  root.style.setProperty('--el-color-primary-light-7', mixWithWhite(preset.primary, 0.7))
  root.style.setProperty('--el-color-primary-light-9', mixWithWhite(preset.primary, 0.9))
  root.style.setProperty('--el-color-primary-dark-2', preset.primaryDark)
}

/** 从 localStorage 读取保存的主题 key */
export function getSavedThemeKey(): string {
  return localStorage.getItem('theme-preset') || Cookies.get('theme-preset') || defaultThemeKey
}

/** 保存主题 key 到 localStorage + Cookie */
export function saveThemeKey(key: string): void {
  localStorage.setItem('theme-preset', key)
  Cookies.set('theme-preset', key, { expires: 365 })
}

/** 初始化主题（应用启动时调用） */
export function initTheme(): void {
  const key = getSavedThemeKey()
  const preset = getThemePreset(key)
  applyTheme(preset)
}
