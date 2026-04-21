import { test, expect } from '@playwright/test';
import { uniqueUser, registerAndSignIn } from './helpers';

test.describe('Main Layout', () => {
  test('top navigation renders with all menu items', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    const nav = page.getByRole('navigation', { name: 'Main navigation' });
    await expect(nav).toBeVisible();

    await expect(nav.getByRole('link', { name: 'ChatApp' })).toBeVisible();
    await expect(nav.getByRole('link', { name: 'Rooms' })).toBeVisible();
    await expect(nav.getByRole('link', { name: 'Contacts' })).toBeVisible();
    await expect(nav.getByRole('link', { name: 'Sessions' })).toBeVisible();
    await expect(nav.getByRole('button', { name: /profile menu/i })).toBeVisible();
  });

  test('right sidebar renders with Rooms and Direct Messages sections', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    const sidebar = page.locator('aside');
    await expect(sidebar).toBeVisible();
    await expect(sidebar.getByRole('heading', { name: 'Rooms' })).toBeVisible();
    await expect(sidebar.getByRole('heading', { name: 'Direct Messages' })).toBeVisible();
  });

  test('sidebar shows empty state when no rooms joined', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    await expect(page.locator('aside')).toContainText('Join rooms from the catalog');
  });

  test('sidebar shows empty state when no friends', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    await expect(page.locator('aside')).toContainText('No friends yet');
  });

  test('room list shows public rooms section', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    // Create a public room via the catalog
    const roomName = `pub-${Date.now()}`;
    await page.goto('/rooms');
    await page.getByRole('button', { name: /create room/i }).click();
    await page.getByLabel('Name').fill(roomName);
    await page.getByRole('button', { name: 'Create', exact: true }).click();
    await page.goto('/');

    // The created public room should appear by name in the sidebar
    await expect(page.locator('aside')).toContainText(`#${roomName}`);
  });

  test('room list shows private rooms with "Private" label', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    await page.goto('/rooms');
    await page.getByRole('button', { name: /create room/i }).click();
    await page.getByLabel('Name').fill(`priv-${Date.now()}`);
    // Uncheck "Public room" to make it private
    const publicCheckbox = page.getByLabel(/public room/i);
    if (await publicCheckbox.isChecked()) {
      await publicCheckbox.uncheck();
    }
    await page.getByRole('button', { name: 'Create', exact: true }).click();
    await page.goto('/');

    await expect(page.locator('aside')).toContainText('Private');
  });

  test('main chat shows "select a conversation" placeholder when nothing selected', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    await expect(page.locator('main')).toContainText('Select a room or friend to start chatting');
  });
});

test.describe('Room Catalog', () => {
  test('create room button is visible on rooms page', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    await page.goto('/rooms');
    await expect(page.getByRole('button', { name: /create room/i })).toBeVisible();
  });

  test('create room modal opens with name, description and visibility fields', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    await page.goto('/rooms');
    await page.getByRole('button', { name: /create room/i }).click();

    await expect(page.getByRole('heading', { name: 'Create Room' })).toBeVisible();
    await expect(page.getByLabel('Name')).toBeVisible();
    await expect(page.getByLabel('Description')).toBeVisible();
    await expect(page.getByLabel(/public room/i)).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create', exact: true })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Cancel' })).toBeVisible();
  });
});
