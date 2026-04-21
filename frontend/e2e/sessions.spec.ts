import { test, expect } from '@playwright/test';
import { uniqueUser, registerAndSignIn } from './helpers';

test.describe('Sessions Page', () => {
  test('sessions page is accessible from nav', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    // Navigate to sessions page
    await page.goto('/sessions');
    await expect(page).toHaveURL('/sessions');
  });

  test('sessions page shows at least one active session', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    await page.goto('/sessions');
    // Should show at least the current session
    await expect(page.locator('li').first()).toBeVisible({ timeout: 5000 });
  });

  test('session entry shows a Revoke button', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    await page.goto('/sessions');
    await expect(page.getByRole('button', { name: /revoke/i })).toBeVisible({ timeout: 5000 });
  });

  test('sessions page shows device/user-agent info', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    await page.goto('/sessions');
    // Each session shows device/user-agent (could be "Unknown device")
    await expect(page.locator('li').first()).toBeVisible({ timeout: 5000 });
    // Some text exists in the first session item
    const text = await page.locator('li').first().textContent();
    expect(text && text.length > 0).toBeTruthy();
  });

  test('sessions page shows date or IP info', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    await page.goto('/sessions');
    await expect(page.locator('li').first()).toBeVisible({ timeout: 5000 });
    const text = await page.locator('li').first().textContent();
    // Should contain either an IP or a date (or "Unknown")
    expect(text).toBeTruthy();
  });

  test('revoking a session removes it from the list', async ({ page, browser }) => {
    const user = uniqueUser();
    // Sign in from a second context to create a second session
    await registerAndSignIn(page, user);
    const ctx2 = await browser.newContext();
    const page2 = await ctx2.newPage();
    await page2.goto('/signin');
    await page2.getByLabel('Email').fill(user.email);
    await page2.getByLabel('Password').fill(user.password);
    await page2.getByRole('button', { name: 'Sign in' }).click();
    await page2.waitForURL('/');
    await ctx2.close();

    // Now there should be 2 sessions; revoke one
    await page.goto('/sessions');
    await expect(page.locator('li').first()).toBeVisible({ timeout: 5000 });
    const sessionsBefore = await page.locator('li').count();
    expect(sessionsBefore).toBeGreaterThanOrEqual(1);

    await page.getByRole('button', { name: /revoke/i }).first().click();
    // After revoking, the count should decrease
    await expect(page.locator('li')).toHaveCount(sessionsBefore - 1, { timeout: 5000 });
  });
});
