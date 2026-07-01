import { test, expect } from '@playwright/test'

test.use({ storageState: 'storage-state.json' })

test.describe('发起新流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/workflow/start')
    await page.waitForLoadState('networkidle')
  })

  test('页面应正常加载 @smoke', async ({ page }) => {
    await expect(page.locator('.start-process-container')).toBeVisible()
    await expect(page.locator('.start-title:has-text("发起新流程")')).toBeVisible()
  })

  test('分类应正确显示（非编码）', async ({ page }) => {
    await page.waitForTimeout(1500)
    const tags = await page.locator('.category-header .el-tag').allTextContents()
    for (const text of tags) {
      expect(text).not.toMatch(/^[a-z]+$/)
      expect(text.length).toBeGreaterThan(1)
    }
  })

  test('搜索流程应过滤结果', async ({ page }) => {
    await page.fill('.start-header input', '不存在测试')
    await page.waitForTimeout(800)
    const cards = await page.locator('.process-card').count()
    expect(cards).toBe(0)
  })
})
