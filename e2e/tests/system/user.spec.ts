import { test, expect } from '@playwright/test'

test.use({ storageState: 'storage-state.json' })

test.describe('系统管理-用户', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/system/user')
    await page.waitForLoadState('networkidle')
  })

  test('用户列表应加载 @smoke', async ({ page }) => {
    await expect(page.locator('.el-table')).toBeVisible()
  })

  test('应能搜索用户', async ({ page }) => {
    await page.fill('input[placeholder*="用户"]', 'admin')
    await page.click('button:has-text("搜索")')
    await page.waitForTimeout(1000)
    await expect(page.locator('.el-table__row').first()).toBeVisible()
  })
})
