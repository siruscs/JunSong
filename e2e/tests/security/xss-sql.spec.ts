import { test, expect } from '@playwright/test'

test.use({ storageState: 'storage-state.json' })

test.describe('安全防御-XSS与SQL注入', () => {
  test('搜索框应过滤XSS脚本', async ({ page }) => {
    await page.goto('/system/user')
    await page.waitForLoadState('networkidle')
    const xssPayload = '<script>alert(1)</script>'
    await page.fill('input[placeholder*="用户"]', xssPayload)
    await page.click('button:has-text("搜索")')
    await page.waitForTimeout(1000)
    const pageContent = await page.content()
    expect(pageContent).not.toContain('<script>alert(1)</script>')
  })

  test('搜索框应过滤SQL注入', async ({ page }) => {
    await page.goto('/system/user')
    await page.waitForLoadState('networkidle')
    const sqlPayload = "' OR '1'='1"
    await page.fill('input[placeholder*="用户"]', sqlPayload)
    await page.click('button:has-text("搜索")')
    await page.waitForTimeout(1000)
    await expect(page.locator('.app-container')).toBeVisible()
  })
})
