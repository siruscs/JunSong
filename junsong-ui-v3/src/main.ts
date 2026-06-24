import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import Cookies from 'js-cookie'

import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

import 'virtual:svg-icons-register'
import '@/assets/styles/index.scss'
import { setupDirectives } from './directives'
import { parseTime } from '@/utils/junsong'
import { download } from '@/api/request'
import { initTheme } from '@/utils/theme'

const app = createApp(App)

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
  size: (Cookies.get('size') as any) || 'default',
})

for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

setupDirectives(app)

app.config.globalProperties.parseTime = parseTime
app.config.globalProperties.download = download
app.config.globalProperties.resetForm = function (refName: string) {
  const refs = (this as any).$refs || {}
  const formRef = refs[refName]
  if (formRef) {
    formRef.resetFields?.()
  }
}

initTheme()

app.mount('#app')
