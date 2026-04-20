import { test, expect } from '@playwright/test';
import { uniqueUser, registerUser, signIn, registerAndSignIn } from './helpers';

// ─── Registration ────────────────────────────────────────────────────────────

test.describe('Registration', () => {
  test('register page renders all fields', async ({ page }) => {
    await page.goto('/register');
    await expect(page.getByRole('heading', { name: 'Register' })).toBeVisible();
    await expect(page.getByLabel('Email')).toBeVisible();
    await expect(page.getByLabel('Username')).toBeVisible();
    await expect(page.getByLabel('Password', { exact: true })).toBeVisible();
    await expect(page.getByLabel('Confirm password')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Create account' })).toBeVisible();
  });

  test('successful registration redirects to sign-in page', async ({ page }) => {
    const user = uniqueUser();
    await registerUser(page, user);
    await expect(page).toHaveURL('/signin');
  });

  test('shows error when email is already taken', async ({ page }) => {
    await page.route('**/api/v1/auth/register', (route) =>
      route.fulfill({ status: 400, contentType: 'application/json', body: JSON.stringify({ error: 'Email already in use' }) })
    );
    const user = uniqueUser();
    await page.goto('/register');
    await page.getByLabel('Email').fill(user.email);
    await page.getByLabel('Username').fill(user.username);
    await page.getByLabel('Password', { exact: true }).fill(user.password);
    await page.getByLabel('Confirm password').fill(user.password);
    await page.getByRole('button', { name: 'Create account' }).click();
    await expect(page.locator('text=Email already in use')).toBeVisible();
  });

  test('shows error when username is already taken', async ({ page }) => {
    await page.route('**/api/v1/auth/register', (route) =>
      route.fulfill({ status: 400, contentType: 'application/json', body: JSON.stringify({ error: 'Username already taken' }) })
    );
    const user = uniqueUser();
    await page.goto('/register');
    await page.getByLabel('Email').fill(user.email);
    await page.getByLabel('Username').fill(user.username);
    await page.getByLabel('Password', { exact: true }).fill(user.password);
    await page.getByLabel('Confirm password').fill(user.password);
    await page.getByRole('button', { name: 'Create account' }).click();
    await expect(page.locator('text=Username already taken')).toBeVisible();
  });

  test('shows validation error when passwords do not match', async ({ page }) => {
    await page.goto('/register');
    await page.getByLabel('Email').fill('test@example.com');
    await page.getByLabel('Username').fill('testuser');
    await page.getByLabel('Password', { exact: true }).fill('Password123!');
    await page.getByLabel('Confirm password').fill('DifferentPassword!');
    await page.getByRole('button', { name: 'Create account' }).click();
    await expect(page.locator('text=Passwords do not match')).toBeVisible();
  });

  test('shows validation error when fields are empty', async ({ page }) => {
    await page.goto('/register');
    await page.getByRole('button', { name: 'Create account' }).click();
    await expect(page.locator('text=All fields are required')).toBeVisible();
  });
});

// ─── Sign In ─────────────────────────────────────────────────────────────────

test.describe('Sign In', () => {
  test('sign in page renders all fields', async ({ page }) => {
    await page.goto('/signin');
    await expect(page.getByRole('heading', { name: 'Sign In' })).toBeVisible();
    await expect(page.getByLabel('Email')).toBeVisible();
    await expect(page.getByLabel('Password')).toBeVisible();
    await expect(page.getByRole('checkbox', { name: /keep me signed in/i })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Sign in' })).toBeVisible();
    await expect(page.getByRole('link', { name: /forgot password/i })).toBeVisible();
  });

  test('successful sign in redirects to main chat', async ({ page }) => {
    const user = uniqueUser();
    await registerUser(page, user);
    await page.goto('/signin');
    await signIn(page, user.email, user.password);
    await expect(page).toHaveURL('/');
  });

  test('shows error for wrong credentials', async ({ page }) => {
    await page.goto('/signin');
    await signIn(page, 'nobody@example.com', 'wrongpassword');
    await expect(page.locator('[class*="error"]')).toBeVisible();
  });

  test('forgot password link navigates to forgot password page', async ({ page }) => {
    await page.goto('/signin');
    await page.getByRole('link', { name: /forgot password/i }).click();
    await expect(page).toHaveURL('/forgot-password');
  });
});

// ─── Sign Out ────────────────────────────────────────────────────────────────

test.describe('Sign Out', () => {
  test('sign out redirects to sign-in page', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    await page.getByRole('button', { name: /profile menu/i }).click();
    await page.getByRole('menuitem', { name: /sign out/i }).click();
    await expect(page).toHaveURL('/signin');
  });

  test('navigating to protected route after sign out redirects to sign-in', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    await page.getByRole('button', { name: /profile menu/i }).click();
    await page.getByRole('menuitem', { name: /sign out/i }).click();
    await expect(page).toHaveURL('/signin');

    await page.goto('/');
    await expect(page).toHaveURL('/signin');
  });
});

// ─── Persistent Login ────────────────────────────────────────────────────────

test.describe('Persistent Login', () => {
  test('reloading page keeps user logged in', async ({ page }) => {
    const user = uniqueUser();
    await registerAndSignIn(page, user);

    await page.reload();
    await expect(page).toHaveURL('/');
    await expect(page.getByRole('button', { name: /profile menu/i })).toBeVisible();
  });
});

// ─── Forgot Password ─────────────────────────────────────────────────────────

test.describe('Forgot Password', () => {
  test('forgot password page renders correctly', async ({ page }) => {
    await page.goto('/forgot-password');
    await expect(page.getByRole('heading', { name: 'Forgot Password' })).toBeVisible();
    await expect(page.getByLabel('Email')).toBeVisible();
    await expect(page.getByRole('button', { name: 'Send reset link' })).toBeVisible();
    await expect(page.getByRole('link', { name: /back to sign in/i })).toBeVisible();
  });

  test('submitting a valid email shows confirmation message', async ({ page }) => {
    await page.route('**/api/v1/auth/forgot-password', (route) =>
      route.fulfill({ status: 200, contentType: 'application/json', body: '{}' })
    );
    await page.goto('/forgot-password');
    await page.getByLabel('Email').fill('someone@example.com');
    await page.getByRole('button', { name: 'Send reset link' }).click();
    await expect(page.locator('text=reset link has been sent')).toBeVisible();
  });

  test('submitting empty email shows validation error', async ({ page }) => {
    await page.goto('/forgot-password');
    await page.getByRole('button', { name: 'Send reset link' }).click();
    await expect(page.locator('text=Email is required')).toBeVisible();
  });
});
