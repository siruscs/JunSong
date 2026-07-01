import { test, expect } from '@playwright/test'

test.describe('认证与登录', () => {
  test('登录页应正常加载 @smoke', async ({ page }) => {
    await page.goto('/login')
    await expect(page.locator('input[placeholder="用户名"]')).toBeVisible()
    await expect(page.locator('input[placeholder="密码"]')).toBeVisible()
    await expect(page.locator('input[placeholder="验证码"]')).toBeVisible()
    await expect(page.locator('button:has-text("登 录")')).toBeVisible()
  })

  test('错误密码应提示登录失败', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[placeholder="用户名"]', 'admin')
    await page.fill('input[placeholder="密码"]', 'wrongpassword')
    await page.fill('input[placeholder="验证码"]', '8888')
    await page.click('button:has-text("登 录")')
    await expect(page.locator('.el-message--error')).toContainText('用户不存在/密码错误')
  })

  test('正确凭据应跳转首页', async ({ page }) => {
    await page.goto('/login')
    await page.fill('input[placeholder="用户名"]', 'admin')
    await page.fill('input[placeholder="密码"]', 'admin123')
    await page.fill('input[placeholder="验证码"]', '8888')
    await page.click('button:has-text("登 录")')
    await page.waitForURL('**/index', { timeout: 15000 })
    await expect(page.locator('.app-container')).toBeVisible()
  })
})
