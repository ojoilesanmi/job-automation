import { test, expect } from '@playwright/test';

test.describe('Authentication', () => {
  test('should show login page', async ({ page }) => {
    await page.goto('/auth/login');
    await expect(page.locator('h1, h2')).toContainText(/sign in|login/i);
  });

  test('should show register page', async ({ page }) => {
    await page.goto('/auth/register');
    await expect(page.locator('h1, h2')).toContainText(/register|sign up/i);
  });
});

test.describe('Dashboard', () => {
  test('should redirect to login when not authenticated', async ({ page }) => {
    await page.goto('/dashboard/overview');
    await expect(page).toHaveURL(/\/auth\/login/);
  });
});
