import { chromium } from '@playwright/test'

async function globalSetup() {
  const browser = await chromium.launch()
  const context = await browser.newContext()
  const page = await context.newPage()

  try {
    await page.goto(`${process.env.BASE_URL || 'http://localhost'}/login`)
    await page.fill('input[placeholder="用户名"]', 'admin')
    await page.fill('input[placeholder="密码"]', 'admin123')
    await page.fill('input[placeholder="验证码"]', '8888')
    await page.click('button:has-text("登 录")')
    await page.waitForURL('**/index', { timeout: 15000 })

    await context.storageState({ path: 'storage-state.json' })
    console.log('[Global Setup] Admin login state saved')
  } catch (e) {
    console.warn('[Global Setup] Login failed, tests may need manual auth:', e)
  } finally {
    await browser.close()
  }
}

export default globalSetup
