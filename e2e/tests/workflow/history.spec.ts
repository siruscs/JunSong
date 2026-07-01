import { test, expect } from '@playwright/test'

test.use({ storageState: 'storage-state.json' })

test.describe('流程历史记录', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/workflow/history')
    await page.waitForLoadState('networkidle')
  })

  test('历史记录列表应加载 @smoke', async ({ page }) => {
    await expect(page.locator('.workflow-history-page, .app-container')).toBeVisible()
  })

  test('选中记录应显示流程跟踪图', async ({ page }) => {
    const firstRow = page.locator('.el-table__row').first()
    if (await firstRow.isVisible()) {
      await firstRow.click()
      await page.waitForTimeout(800)
      await expect(page.locator('.workflow-tracking-viewer, .bjs-container')).toBeVisible({ timeout: 10000 })
    }
  })
})
