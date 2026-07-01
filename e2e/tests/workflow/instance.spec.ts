import { test, expect } from '@playwright/test'

test.use({ storageState: 'storage-state.json' })

test.describe('流程实例管理', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/workflow/instance')
    await page.waitForLoadState('networkidle')
  })

  test('流程实例列表应加载 @smoke', async ({ page }) => {
    await expect(page.locator('.workflow-instance-page')).toBeVisible()
  })

  test('应能按流程标识筛选', async ({ page }) => {
    await page.fill('input[placeholder*="流程标识"]', 'test')
    await page.click('button:has-text("搜索")')
    await page.waitForTimeout(1000)
    await expect(page.locator('.el-table')).toBeVisible()
  })
})
