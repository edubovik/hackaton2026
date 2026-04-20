import { Page } from '@playwright/test';

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
