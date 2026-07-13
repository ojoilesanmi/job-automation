import { test, expect } from '@playwright/test';

test.describe('Navigation', () => {
  test('should show landing page', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('body')).toBeVisible();
  });

  test('should have sign in link on landing page', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('a[href*="login"]')).toBeVisible();
  });
});
