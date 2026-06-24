import type { App } from 'vue'
import { vHasPermi } from './hasPermi'
import { vHasRole } from './hasRole'
import { vDrag } from './drag'
import { vClipboard } from './clipboard'

export function setupDirectives(app: App) {
  app.directive('hasPermi', vHasPermi)
  app.directive('hasRole', vHasRole)
  app.directive('drag', vDrag)
  app.directive('clipboard', vClipboard)
}
