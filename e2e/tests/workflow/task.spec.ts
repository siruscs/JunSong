import { test, expect } from '@playwright/test'

test.use({ storageState: 'storage-state.json' })

test.describe('流程任务中心', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/workflow/task')
    await page.waitForLoadState('networkidle')
  })

  test('任务列表应加载 @smoke', async ({ page }) => {
    await expect(page.locator('.workflow-task-page, .app-container')).toBeVisible()
  })
})
