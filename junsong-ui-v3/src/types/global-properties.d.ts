import type { ComponentCustomProperties } from 'vue'

declare module 'vue' {
  interface ComponentCustomProperties {
    money: (value: any) => string
  }
}

export {}
