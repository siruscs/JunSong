import { test, expect } from '@playwright/test'

test.use({ storageState: 'storage-state.json' })

test.describe('工作台首页', () => {
  test('应显示概览卡片 @smoke', async ({ page }) => {
    await page.goto('/index')
    await expect(page.locator('.overview-card')).toHaveCount(4)
    await expect(page.locator('.overview-card__label').first()).toBeVisible()
  })

  test('应显示流程定义统计', async ({ page }) => {
    await page.goto('/index')
    await expect(page.locator('text=流程定义统计')).toBeVisible()
    await expect(page.locator('text=流程实例统计')).toBeVisible()
  })

  test('快捷入口应可点击', async ({ page }) => {
    await page.goto('/index')
    await page.click('text=发起流程')
    await page.waitForURL('**/workflow/start')
    await expect(page.locator('.start-process-container')).toBeVisible()
  })
})
