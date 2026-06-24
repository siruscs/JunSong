/// <reference types="vite/client" />
/// <reference types="vite-plugin-svg-icons/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, any>
  export default component
}

declare module 'virtual:svg-icons-register' {
  const content: any
  export default content
}

declare module 'virtual:*' {
  const content: any
  export default content
}

interface ImportMetaEnv {
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_BASE_API: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
