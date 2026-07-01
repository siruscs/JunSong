import { test, expect } from '@playwright/test'

test.use({ storageState: 'storage-state.json' })

test.describe('低代码-业务对象管理', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/lowcode/admin')
    await page.waitForLoadState('networkidle')
  })

  test('业务对象列表应加载 @smoke', async ({ page }) => {
    await expect(page.locator('.app-container')).toBeVisible()
    await expect(page.locator('.el-table')).toBeVisible()
  })
})
