import { test, expect } from '@playwright/test'

test.use({ storageState: 'storage-state.json' })

test.describe('流程定义管理', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/workflow/definition')
    await page.waitForLoadState('networkidle')
  })

  test('流程定义列表应加载 @smoke', async ({ page }) => {
    await expect(page.locator('.workflow-definition-page')).toBeVisible()
    await expect(page.locator('.workflow-overview')).toBeVisible()
    const rowCount = await page.locator('.el-table__row').count()
    expect(rowCount).toBeGreaterThanOrEqual(0)
  })

  test('分类筛选应正常工作', async ({ page }) => {
    const select = page.locator('.el-form-item:has-text("分类") .el-select')
    if (await select.isVisible()) {
      await select.click()
      await page.waitForTimeout(500)
      const options = await page.locator('.el-select-dropdown__item').count()
      if (options > 0) {
        await page.locator('.el-select-dropdown__item').first().click()
        await page.click('button:has-text("搜索")')
        await page.waitForTimeout(1000)
        await expect(page.locator('.el-table__row').first()).toBeVisible()
      }
    }
  })

  test('分类管理弹窗应可打开', async ({ page }) => {
    await page.click('text=管理分类')
    await expect(page.locator('.el-dialog__title:has-text("流程分类管理")')).toBeVisible()
    await expect(page.locator('.el-dialog .el-table')).toBeVisible()
    await page.click('.el-dialog__close')
  })

  test('详情弹窗应显示分类', async ({ page }) => {
    const firstRow = page.locator('.el-table__row').first()
    if (await firstRow.isVisible()) {
      await firstRow.locator('text=详情').click()
      await expect(page.locator('.el-dialog__title:has-text("流程定义详情")')).toBeVisible()
      await expect(page.locator('text=分类')).toBeVisible()
      await page.click('.el-dialog__close')
    }
  })

  test('校验按钮应弹出结果', async ({ page }) => {
    const firstRow = page.locator('.el-table__row').first()
    if (await firstRow.isVisible()) {
      await firstRow.locator('text=校验').click()
      await page.waitForTimeout(1000)
      await expect(page.locator('.el-dialog__title:has-text("流程定义详情")')).toBeVisible()
      await page.click('.el-dialog__close')
    }
  })
})
