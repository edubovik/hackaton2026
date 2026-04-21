import { test, expect, Browser } from '@playwright/test';
import { uniqueUser, registerAndSignIn, createRoom, enterRoom } from './helpers';

async function newUser(browser: Browser) {
  const user = uniqueUser();
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  await registerAndSignIn(page, user);
  return { user, page, ctx };
}

async function openManageModal(page: any, roomName: string) {
  await enterRoom(page, roomName);
  await page.locator('aside').getByTitle('Manage room').click();
}

async function joinRoomFromCatalog(page: any, roomName: string) {
  await page.goto('/rooms');
  await page.getByPlaceholder(/search rooms/i).fill(roomName);
  await page.locator('li').filter({ hasText: roomName }).getByRole('button', { name: 'Join', exact: true }).click();
  await page.waitForURL('/');
}

test.describe('Manage Room Modal — Owner Actions', () => {
  test('owner sees manage room button and can open modal', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    const roomName = `admin-${Date.now()}`;
    await createRoom(page, { name: roomName, isPublic: true });
    await page.goto('/');
    await openManageModal(page, roomName);
    // The modal heading starts with "Manage:"
    await expect(page.getByRole('heading', { name: new RegExp(`Manage.*${roomName}`, 'i') })).toBeVisible();
  });

  test('manage modal shows all tabs: Members, Admins, Banned, Invitations, Settings', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    const roomName = `adm2-${Date.now()}`;
    await createRoom(page, { name: roomName, isPublic: true });
    await page.goto('/');
    await openManageModal(page, roomName);
    for (const tab of ['Members', 'Admins', 'Banned', 'Invitations', 'Settings']) {
      await expect(page.getByRole('button', { name: tab, exact: true })).toBeVisible();
    }
  });

  test('Members tab shows joined members with Ban option', async ({ page, browser }) => {
    const { user: other, page: otherPage, ctx } = await newUser(browser);
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    const roomName = `adm3-${Date.now()}`;
    await createRoom(page, { name: roomName, isPublic: true });

    // Other user joins
    await joinRoomFromCatalog(otherPage, roomName);
    await ctx.close();

    await page.goto('/');
    await openManageModal(page, roomName);
    await page.getByRole('button', { name: 'Members', exact: true }).click();
    // Owner sees other.username
    await expect(page.getByText(other.username).first()).toBeVisible({ timeout: 5000 });
    // Scope Ban button to modal overlay
    const modal = page.locator('[class*="overlay"], [class*="modal"]').last();
    await expect(modal.getByRole('button', { name: 'Ban', exact: true })).toBeVisible();
  });

  test('Settings tab shows room name field and Save/Delete buttons for owner', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    const roomName = `adm4-${Date.now()}`;
    await createRoom(page, { name: roomName, isPublic: true });
    await page.goto('/');
    await openManageModal(page, roomName);
    await page.getByRole('button', { name: 'Settings', exact: true }).click();
    await expect(page.getByLabel('Name')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Save' })).toBeVisible();
    await expect(page.getByRole('button', { name: /delete room/i })).toBeVisible();
  });

  test('Invitations tab shows invite form for private room', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    const roomName = `adm6-${Date.now()}`;
    await createRoom(page, { name: roomName, isPublic: false });
    await page.goto('/');
    await openManageModal(page, roomName);
    await page.getByRole('button', { name: 'Invitations', exact: true }).click();
    await expect(page.getByRole('button', { name: 'Invite' })).toBeVisible();
  });
});

test.describe('Admin Actions', () => {
  test('owner can ban a member from the room', async ({ page, browser }) => {
    const { user: other, page: otherPage, ctx } = await newUser(browser);
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    const roomName = `ban-${Date.now()}`;
    await createRoom(page, { name: roomName, isPublic: true });

    await joinRoomFromCatalog(otherPage, roomName);
    await ctx.close();

    await page.goto('/');
    await openManageModal(page, roomName);
    await page.getByRole('button', { name: 'Members', exact: true }).click();
    const modal = page.locator('[class*="overlay"], [class*="modal"]').last();
    await expect(modal.getByRole('button', { name: 'Ban', exact: true })).toBeVisible({ timeout: 5000 });

    page.on('dialog', dialog => dialog.accept());
    await modal.getByRole('button', { name: 'Ban', exact: true }).click();

    // After ban, member appears in Banned tab
    await page.getByRole('button', { name: 'Banned', exact: true }).click();
    await expect(page.getByText(other.username).first()).toBeVisible({ timeout: 5000 });
  });

  test('non-owner member does not see Manage room button', async ({ page, browser }) => {
    const { user: owner, page: ownerPage } = await newUser(browser);
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    const roomName = `nomgr-${Date.now()}`;
    await createRoom(ownerPage, { name: roomName, isPublic: true });

    await joinRoomFromCatalog(page, roomName);
    await enterRoom(page, roomName);

    await expect(page.locator('aside').getByTitle('Manage room')).not.toBeVisible();
  });
});
