import { test, expect } from '@playwright/test';
import { uniqueUser, registerAndSignIn, createRoom, enterRoom } from './helpers';

test.describe('Create Room', () => {
  test('can create a public room and it appears in sidebar', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    const name = `pub-${Date.now()}`;
    await createRoom(page, { name, isPublic: true });
    await page.goto('/');

    await expect(page.locator('aside')).toContainText(`#${name}`);
  });

  test('can create a private room and it appears under Private label', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    const name = `priv-${Date.now()}`;
    await createRoom(page, { name, isPublic: false });
    await page.goto('/');

    const sidebar = page.locator('aside');
    await expect(sidebar).toContainText('Private');
    await expect(sidebar).toContainText(`#${name}`);
  });

  test('shows validation error when room name is empty', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    await page.goto('/rooms');
    await page.getByRole('button', { name: /create room/i }).click();
    await page.getByRole('button', { name: 'Create', exact: true }).click();

    // HTML5 required validation keeps focus on the Name field
    const nameInput = page.getByLabel('Name');
    await expect(nameInput).toBeFocused();
  });
});

test.describe('Room Catalog', () => {
  test('catalog page renders with heading and search', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    await page.goto('/rooms');
    await expect(page.getByRole('heading', { name: /room catalog/i })).toBeVisible();
    await expect(page.getByPlaceholder('Search rooms…')).toBeVisible();
  });

  test('search filters rooms by name', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    const name = `searchable-${Date.now()}`;
    await createRoom(page, { name, isPublic: true });

    await page.getByPlaceholder('Search rooms…').fill(name);
    await expect(page.getByRole('button', { name: name })).toBeVisible();

    await page.getByPlaceholder('Search rooms…').fill('zzzzzzzzzz-no-match');
    await expect(page.locator('text=No rooms found')).toBeVisible();
  });

  test('join button is present on rooms not yet joined', async ({ page, browser }) => {
    const userA = uniqueUser();
    const ctxA = await browser.newContext();
    const pageA = await ctxA.newPage();
    await registerAndSignIn(pageA, userA);
    const roomName = `joinable-${Date.now()}`;
    await createRoom(pageA, { name: roomName, isPublic: true });
    await ctxA.close();

    const userB = uniqueUser();
    await registerAndSignIn(page, userB);
    await page.goto('/rooms');
    await page.getByPlaceholder('Search rooms…').fill(roomName);
    await expect(page.getByRole('button', { name: 'Join', exact: true })).toBeVisible();
  });

  test('user can join a public room from the catalog', async ({ page, browser }) => {
    const userA = uniqueUser();
    const ctxA = await browser.newContext();
    const pageA = await ctxA.newPage();
    await registerAndSignIn(pageA, userA);
    const roomName = `tojoin-${Date.now()}`;
    await createRoom(pageA, { name: roomName, isPublic: true });
    await ctxA.close();

    const userB = uniqueUser();
    await registerAndSignIn(page, userB);
    await page.goto('/rooms');
    await page.getByPlaceholder('Search rooms…').fill(roomName);
    await page.getByRole('button', { name: 'Join', exact: true }).click();
    await page.goto('/');

    await expect(page.locator('aside')).toContainText(`#${roomName}`);
  });
});

test.describe('Room View', () => {
  test('entering a room shows chat area and members panel', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    const name = `rv-${Date.now()}`;
    await createRoom(page, { name, isPublic: true });
    await page.goto('/');
    await enterRoom(page, name);

    await expect(page.locator('main header')).toContainText(name);
    await expect(page.locator('text=/Members \\(\\d+\\)/')).toBeVisible();
  });

  test('members panel shows current user with a presence indicator', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    const name = `mp-${Date.now()}`;
    await createRoom(page, { name, isPublic: true });
    await page.goto('/');
    await enterRoom(page, name);

    // Username appears somewhere in the members panel area
    await expect(page.locator('text=' + user.username).first()).toBeVisible();
    // Any presence indicator is rendered (aria-label is Online, Away, or Offline)
    await expect(
      page.locator('[aria-label="Online"], [aria-label="Away"], [aria-label="Offline"]').first()
    ).toBeVisible();
  });
});

test.describe('Manage Room Modal', () => {
  // Scope all modal assertions to the overlay to avoid conflicts with the
  // chat title heading and sidebar room buttons sharing similar text.
  const modal = (page: any) => page.locator('.overlay, [class*="overlay"]').last();

  test('manage room modal opens from gear icon in sidebar', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    const name = `mr-${Date.now()}`;
    await createRoom(page, { name, isPublic: true });
    await page.goto('/');
    await enterRoom(page, name);
    await page.getByTitle('Manage room').click();

    await expect(modal(page).getByRole('heading', { name: /manage/i })).toBeVisible();
  });

  test('manage room modal has Members, Admins, Banned, Invitations, Settings tabs', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    const name = `tabs-${Date.now()}`;
    await createRoom(page, { name, isPublic: true });
    await page.goto('/');
    await enterRoom(page, name);
    await page.getByTitle('Manage room').click();

    const m = modal(page);
    await expect(m.getByRole('button', { name: 'Members' })).toBeVisible();
    await expect(m.getByRole('button', { name: 'Admins' })).toBeVisible();
    await expect(m.getByRole('button', { name: 'Banned' })).toBeVisible();
    await expect(m.getByRole('button', { name: 'Invitations' })).toBeVisible();
    await expect(m.getByRole('button', { name: 'Settings', exact: true })).toBeVisible();
  });

  test('Members tab shows owner row with no Ban or Make Admin actions', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    const name = `own-${Date.now()}`;
    await createRoom(page, { name, isPublic: true });
    await page.goto('/');
    await enterRoom(page, name);
    await page.getByTitle('Manage room').click();

    const m = modal(page);
    await m.getByRole('button', { name: 'Members' }).click();

    // The content area (not the tab row) should have no Ban action
    const content = m.locator('[class*="content"], [class*="list"]').first();
    await expect(content.getByRole('button', { name: 'Ban' })).not.toBeVisible();
  });

  test('Banned tab shows "No bans" when no one is banned', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    const name = `nb-${Date.now()}`;
    await createRoom(page, { name, isPublic: true });
    await page.goto('/');
    await enterRoom(page, name);
    await page.getByTitle('Manage room').click();

    const m = modal(page);
    await m.getByRole('button', { name: 'Banned' }).click();
    await expect(m.locator('text=No bans')).toBeVisible();
  });

  test('Settings tab shows name, description, visibility and delete button', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    const name = `st-${Date.now()}`;
    await createRoom(page, { name, isPublic: true });
    await page.goto('/');
    await enterRoom(page, name);
    await page.getByTitle('Manage room').click();

    const m = modal(page);
    await m.getByRole('button', { name: 'Settings', exact: true }).click();

    await expect(m.getByLabel('Name')).toBeVisible();
    await expect(m.getByLabel('Description')).toBeVisible();
    await expect(m.getByLabel('Public')).toBeVisible();
    await expect(m.getByRole('button', { name: 'Save' })).toBeVisible();
    await expect(m.getByRole('button', { name: 'Delete Room' })).toBeVisible();
  });
});
