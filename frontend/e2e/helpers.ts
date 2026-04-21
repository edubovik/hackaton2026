import { Page, expect } from '@playwright/test';

export function uniqueUser() {
  const ts = Date.now();
  return {
    email: `testuser_${ts}@example.com`,
    username: `testuser_${ts}`,
    password: 'TestPass123!',
  };
}

/** Fills and submits the registration form. Lands on /signin on success. */
export async function registerUser(page: Page, user: { email: string; username: string; password: string }) {
  await page.goto('/register');
  await page.getByLabel('Email').fill(user.email);
  await page.getByLabel('Username').fill(user.username);
  await page.getByLabel('Password', { exact: true }).fill(user.password);
  await page.getByLabel('Confirm password').fill(user.password);
  await page.getByRole('button', { name: 'Create account' }).click();
  await page.waitForURL('/signin');
}

/** Fills and submits the sign-in form. Lands on / on success. */
export async function signIn(page: Page, email: string, password: string) {
  await page.goto('/signin');
  await page.getByLabel('Email').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Sign in' }).click();
}

/** Registers a new user then signs them in. Lands on / (main chat). */
export async function registerAndSignIn(page: Page, user: { email: string; username: string; password: string }) {
  await registerUser(page, user);
  await signIn(page, user.email, user.password);
  await page.waitForURL('/');
}

/** Creates a room via the catalog page. Returns the room name used. */
export async function createRoom(page: Page, opts: { name: string; isPublic?: boolean; description?: string }) {
  await page.goto('/rooms');
  await page.getByRole('button', { name: /create room/i }).click();
  await page.getByLabel('Name').fill(opts.name);
  if (opts.description) await page.getByLabel('Description').fill(opts.description);
  const checkbox = page.getByLabel(/public room/i);
  const checked = await checkbox.isChecked();
  const wantPublic = opts.isPublic !== false;
  if (checked && !wantPublic) await checkbox.uncheck();
  if (!checked && wantPublic) await checkbox.check();
  await page.getByRole('button', { name: 'Create', exact: true }).click();
  // Wait for modal to close — signals the API call completed and room is persisted
  await page.getByRole('heading', { name: 'Create Room' }).waitFor({ state: 'hidden' });
  return opts.name;
}

/** Clicks a room in the sidebar by name (without # prefix) and waits for the chat to open. */
export async function enterRoom(page: Page, roomName: string) {
  await page.locator('aside').getByText(`#${roomName}`).click();
}

/** Waits for the WebSocket to connect (message textarea becomes enabled). */
export async function waitForComposer(page: Page) {
  await expect(page.getByPlaceholder(/type a message/i)).toBeEnabled({ timeout: 15_000 });
}
