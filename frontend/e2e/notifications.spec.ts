import { test, expect, Browser } from '@playwright/test';
import { uniqueUser, registerAndSignIn, createRoom, enterRoom, waitForComposer } from './helpers';

async function newUser(browser: Browser) {
  const user = uniqueUser();
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  await registerAndSignIn(page, user);
  return { user, page, ctx };
}

async function joinRoomFromCatalog(page: any, roomName: string) {
  await page.goto('/rooms');
  await page.getByPlaceholder(/search rooms/i).fill(roomName);
  // Click Join — the handler awaits the API then navigates to /
  await page.locator('li').filter({ hasText: roomName }).getByRole('button', { name: 'Join', exact: true }).click();
  // Wait until we're on the main page (navigation happens inside handleJoin)
  await page.waitForURL('/');
}

test.describe('Unread Badges — Rooms', () => {
  test('unread badge appears on room sidebar entry when message received while away', async ({ browser }) => {
    const { user: userA, page: pageA } = await newUser(browser);
    const { user: userB, page: pageB } = await newUser(browser);

    const roomName = `notif-${Date.now()}`;
    await createRoom(pageA, { name: roomName, isPublic: true });

    // User B joins the room — navigated to / automatically after join
    await joinRoomFromCatalog(pageB, roomName);
    // Verify B can see the room in their sidebar
    await expect(pageB.locator('aside').getByText(`#${roomName}`)).toBeVisible({ timeout: 8000 });

    // User A sends a message to the room
    await pageA.goto('/');
    await enterRoom(pageA, roomName);
    await waitForComposer(pageA);
    await pageA.getByPlaceholder(/type a message/i).fill(`badge-msg-${Date.now()}`);
    await pageA.getByPlaceholder(/type a message/i).press('Enter');

    // User B (not in the room) should see a badge on the room sidebar entry
    await expect(
      pageB.locator('aside').locator('span').filter({ hasText: /^\d+$/ }).first()
    ).toBeVisible({ timeout: 15_000 });
  });

  test('unread badge clears when user enters the room', async ({ browser }) => {
    const { user: userA, page: pageA } = await newUser(browser);
    const { user: userB, page: pageB } = await newUser(browser);

    const roomName = `notif2-${Date.now()}`;
    await createRoom(pageA, { name: roomName, isPublic: true });

    await joinRoomFromCatalog(pageB, roomName);
    await expect(pageB.locator('aside').getByText(`#${roomName}`)).toBeVisible({ timeout: 8000 });

    // User A sends a message
    await pageA.goto('/');
    await enterRoom(pageA, roomName);
    await waitForComposer(pageA);
    await pageA.getByPlaceholder(/type a message/i).fill(`badge-clear-${Date.now()}`);
    await pageA.getByPlaceholder(/type a message/i).press('Enter');

    // Wait for badge on B
    await expect(
      pageB.locator('aside').locator('span').filter({ hasText: /^\d+$/ }).first()
    ).toBeVisible({ timeout: 15_000 });

    // B enters the room — badge should disappear
    await enterRoom(pageB, roomName);
    await expect(
      pageB.locator('aside').locator('span').filter({ hasText: /^\d+$/ })
    ).not.toBeVisible({ timeout: 5000 });
  });
});

test.describe('Unread Badges — DMs', () => {
  async function becomeFriends(pageA: any, userB: { username: string }, pageB: any) {
    await pageA.locator('aside').getByTitle('Manage contacts').click();
    await pageA.getByPlaceholder('Username').fill(userB.username);
    await pageA.getByRole('button', { name: 'Send Request' }).click();
    await pageA.locator('aside').getByTitle('Manage contacts').click();

    await pageB.locator('aside').getByTitle('Manage contacts').click();
    await expect(pageB.getByRole('button', { name: 'Accept' })).toBeVisible({ timeout: 5000 });
    await pageB.getByRole('button', { name: 'Accept' }).click();
    await pageB.locator('aside').getByTitle('Manage contacts').click();
  }

  test('unread badge appears in DM sidebar when friend sends a message', async ({ browser }) => {
    const { user: userA, page: pageA } = await newUser(browser);
    const { user: userB, page: pageB } = await newUser(browser);

    await becomeFriends(pageA, userB, pageB);
    // Both pages reload to show friends in sidebar
    await pageA.reload();
    await pageB.reload();
    await expect(pageB.locator('aside').getByText(userA.username)).toBeVisible({ timeout: 5000 });
    await expect(pageA.locator('aside').getByText(userB.username)).toBeVisible({ timeout: 5000 });

    // User B opens DM with A and sends a message
    await pageB.locator('aside').getByText(userA.username).click();
    await waitForComposer(pageB);
    await pageB.getByPlaceholder(/type a message/i).fill('dm badge msg');
    await pageB.getByPlaceholder(/type a message/i).press('Enter');
    // Wait for message to appear in B's chat (confirms it was persisted)
    await expect(pageB.getByText('dm badge msg')).toBeVisible({ timeout: 5000 });

    // DM unread counts update on page load (not real-time); reload pageA to pick up the new count
    await pageA.reload();
    // Wait for friend to load in sidebar first, then check for badge
    await expect(pageA.locator('aside').getByText(userB.username)).toBeVisible({ timeout: 5000 });
    await expect(
      pageA.locator('aside').locator('span').filter({ hasText: /^\d+$/ }).first()
    ).toBeVisible({ timeout: 5000 });
  });
});

test.describe('Page Title', () => {
  test('page has a non-empty title', async ({ page }) => {
    await page.goto('/');
    const title = await page.title();
    expect(title.length).toBeGreaterThan(0);
  });
});
