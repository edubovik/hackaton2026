import { test, expect, Browser } from '@playwright/test';
import { uniqueUser, registerAndSignIn } from './helpers';

async function newUser(browser: Browser) {
  const user = uniqueUser();
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  await registerAndSignIn(page, user);
  return { user, page, ctx };
}

async function openContacts(page: any) {
  await page.locator('aside').getByTitle('Manage contacts').click();
}

async function sendRequest(page: any, username: string) {
  await openContacts(page);
  await page.getByPlaceholder('Username').fill(username);
  await page.getByRole('button', { name: 'Send Request' }).click();
  // Wait for the API call to complete (input clears on success)
  await page.getByPlaceholder('Username').waitFor({ state: 'visible' });
  await expect(page.getByPlaceholder('Username')).toHaveValue('', { timeout: 5000 });
}

test.describe('Contacts Panel', () => {
  test('clicking + in DM section opens contacts panel', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    await openContacts(page);
    await expect(page.locator('main').getByRole('heading', { name: /add friend/i })).toBeVisible();
  });

  test('username input and Send Request button visible', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    await openContacts(page);
    await expect(page.getByPlaceholder('Username')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Send Request' })).toBeVisible();
  });

  test('sending a friend request to a real user succeeds', async ({ page, browser }) => {
    const { user: other } = await newUser(browser);
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    await sendRequest(page, other.username);
    // After sending, no error should be shown (basic smoke test)
    await expect(page.getByPlaceholder('Username')).toHaveValue('');
  });

  test('sending a request to unknown user shows error', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);
    await openContacts(page);
    await page.getByPlaceholder('Username').fill(`nobody_${Date.now()}`);
    await page.getByRole('button', { name: 'Send Request' }).click();
    // Expect some error feedback
    await expect(page.locator('main').getByText(/not found|error|failed/i)).toBeVisible({ timeout: 5000 });
  });
});

test.describe('Friend Requests', () => {
  test('recipient sees pending request section with Accept and Reject buttons', async ({ browser }) => {
    const { user: sender, page: senderPage } = await newUser(browser);
    const { user: receiver, page: receiverPage } = await newUser(browser);

    await sendRequest(senderPage, receiver.username);

    await openContacts(receiverPage);
    await expect(receiverPage.getByRole('heading', { name: /pending requests/i })).toBeVisible({ timeout: 5000 });
    await expect(receiverPage.getByRole('button', { name: 'Accept' })).toBeVisible();
    await expect(receiverPage.getByRole('button', { name: 'Reject' })).toBeVisible();
  });

  test('accepting a friend request adds user to Friends list and DM sidebar', async ({ browser }) => {
    const { user: sender, page: senderPage } = await newUser(browser);
    const { user: receiver, page: receiverPage } = await newUser(browser);

    await sendRequest(senderPage, receiver.username);

    await openContacts(receiverPage);
    await expect(receiverPage.getByRole('button', { name: 'Accept' })).toBeVisible({ timeout: 5000 });
    await receiverPage.getByRole('button', { name: 'Accept' }).click();

    // Sender should appear in Friends list and DM sidebar
    await expect(receiverPage.locator('aside').getByText(sender.username)).toBeVisible({ timeout: 5000 });
  });

  test('rejecting a friend request removes it from Pending Requests', async ({ browser }) => {
    const { user: sender, page: senderPage } = await newUser(browser);
    const { user: receiver, page: receiverPage } = await newUser(browser);

    await sendRequest(senderPage, receiver.username);

    await openContacts(receiverPage);
    await expect(receiverPage.getByRole('button', { name: 'Reject' })).toBeVisible({ timeout: 5000 });
    await receiverPage.getByRole('button', { name: 'Reject' }).click();

    await expect(receiverPage.getByRole('button', { name: 'Reject' })).not.toBeVisible({ timeout: 3000 });
    await expect(receiverPage.getByRole('button', { name: 'Accept' })).not.toBeVisible({ timeout: 1000 });
  });
});

test.describe('Presence', () => {
  test('friend appears in DM sidebar after accepting request', async ({ browser }) => {
    const { user: sender, page: senderPage } = await newUser(browser);
    const { user: receiver, page: receiverPage } = await newUser(browser);

    await sendRequest(senderPage, receiver.username);

    await openContacts(receiverPage);
    await expect(receiverPage.getByRole('button', { name: 'Accept' })).toBeVisible({ timeout: 5000 });
    await receiverPage.getByRole('button', { name: 'Accept' }).click();

    // Both users should see each other in their DM sidebars
    await expect(receiverPage.locator('aside').getByText(sender.username)).toBeVisible({ timeout: 5000 });

    // Reload sender's page and check
    await senderPage.reload();
    await expect(senderPage.locator('aside').getByText(receiver.username)).toBeVisible({ timeout: 5000 });
  });

  test('clicking a friend in DM sidebar opens DM chat', async ({ browser }) => {
    const { user: sender, page: senderPage } = await newUser(browser);
    const { user: receiver, page: receiverPage } = await newUser(browser);

    await sendRequest(senderPage, receiver.username);

    await openContacts(receiverPage);
    await expect(receiverPage.getByRole('button', { name: 'Accept' })).toBeVisible({ timeout: 5000 });
    await receiverPage.getByRole('button', { name: 'Accept' }).click();

    // sender should now appear in DM sidebar (already confirmed by previous assertion)
    await receiverPage.locator('aside').getByText(sender.username).click();
    // DM chat area should open with chat title showing sender's username
    await expect(receiverPage.locator('main header').getByText(sender.username)).toBeVisible({ timeout: 5000 });
  });
});
