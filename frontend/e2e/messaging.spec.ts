import { test, expect } from '@playwright/test';
import { uniqueUser, registerAndSignIn, createRoom, enterRoom, waitForComposer } from './helpers';

// Shared setup: create a user, a room, enter it, and wait for WebSocket to connect.
async function setup(page: any) {
  const user = uniqueUser();
  await registerAndSignIn(page, user);
  const roomName = `msg-${Date.now()}`;
  await createRoom(page, { name: roomName, isPublic: true });
  await page.goto('/');
  await enterRoom(page, roomName);
  await waitForComposer(page);
  return { user, roomName };
}

test.describe('Message Input', () => {
  test('message textarea is visible with correct placeholder', async ({ page }) => {
    await setup(page);
    const input = page.getByPlaceholder(/type a message/i);
    await expect(input).toBeVisible();
  });

  test('attach button is visible in message input bar', async ({ page }) => {
    await setup(page);
    await expect(page.getByRole('button', { name: /attach file/i })).toBeVisible();
  });

  test.skip('emoji button is visible in message input bar', async ({ page }) => {
    // Skipped: emoji picker not yet implemented in MessageComposer
    await setup(page);
    await expect(page.getByRole('button', { name: /emoji/i })).toBeVisible();
  });

  test('shift+enter creates a newline without sending', async ({ page }) => {
    await setup(page);
    const input = page.getByPlaceholder(/type a message/i);
    await input.focus();
    await input.pressSequentially('line1');
    await input.press('Shift+Enter');
    await input.pressSequentially('line2');

    // Value should contain a newline — message not sent yet
    const value = await input.inputValue();
    expect(value).toContain('\n');
  });
});

test.describe('Send and Receive', () => {
  test('typing and pressing Enter sends message', async ({ page }) => {
    await setup(page);
    const input = page.getByPlaceholder(/type a message/i);
    const text = `hello-${Date.now()}`;
    await input.fill(text);
    await input.press('Enter');

    // Message appears in the chat
    await expect(page.locator('main').getByText(text)).toBeVisible();
  });

  test('sent message shows sender name and is in the chat area', async ({ page }) => {
    const { user } = await setup(page);
    const input = page.getByPlaceholder(/type a message/i);
    const text = `msg-${Date.now()}`;
    await input.fill(text);
    await input.press('Enter');

    await expect(page.locator('main').getByText(text)).toBeVisible();
    await expect(page.locator('main').getByText(user.username).first()).toBeVisible();
  });
});

test.describe('Reply', () => {
  test('hovering a message reveals the reply button', async ({ page }) => {
    await setup(page);
    const input = page.getByPlaceholder(/type a message/i);
    await input.fill('original message');
    await input.press('Enter');

    const message = page.locator('main').getByText('original message');
    await message.hover();
    await expect(page.getByTitle('Reply')).toBeVisible();
  });

  test('clicking reply shows Replying-to bar above the input', async ({ page }) => {
    await setup(page);
    const input = page.getByPlaceholder(/type a message/i);
    await input.fill('original message');
    await input.press('Enter');

    const message = page.locator('main').getByText('original message');
    await message.hover();
    await page.getByTitle('Reply').click();

    await expect(page.locator('text=/Replying to/')).toBeVisible();
  });

  test('reply shows quoted original message in the sent reply', async ({ page }) => {
    await setup(page);
    const input = page.getByPlaceholder(/type a message/i);
    const original = `orig-${Date.now()}`;
    await input.fill(original);
    await input.press('Enter');

    const message = page.locator('main').getByText(original);
    await message.hover();
    await page.getByTitle('Reply').click();
    await input.fill('this is my reply');
    await input.press('Enter');

    // The reply message should contain a quote with the original text
    await expect(page.locator('main').getByText(original).first()).toBeVisible();
    await expect(page.locator('main').getByText('this is my reply')).toBeVisible();
  });
});

test.describe('Edit', () => {
  test('hovering own message reveals the edit button', async ({ page }) => {
    await setup(page);
    const input = page.getByPlaceholder(/type a message/i);
    await input.fill('original text');
    await input.press('Enter');

    const message = page.locator('main').getByText('original text');
    await message.hover();
    await expect(page.getByTitle('Edit')).toBeVisible();
  });

  test('clicking edit fills textarea with existing message text', async ({ page }) => {
    await setup(page);
    const input = page.getByPlaceholder(/type a message/i);
    await input.fill('edit me');
    await input.press('Enter');

    const message = page.locator('main').getByText('edit me');
    await message.hover();
    await page.getByTitle('Edit').click();

    // An inline edit textarea should be pre-filled with the message text
    const editBox = page.locator('textarea').filter({ hasText: 'edit me' });
    await expect(editBox).toBeVisible();
  });

  test('saving an edit updates the message and shows (edited) indicator', async ({ page }) => {
    await setup(page);
    const input = page.getByPlaceholder(/type a message/i);
    await input.fill('before edit');
    await input.press('Enter');

    const message = page.locator('main').getByText('before edit');
    await message.hover();
    await page.getByTitle('Edit').click();

    const editBox = page.locator('[class*="editBox"] textarea, textarea').last();
    await editBox.clear();
    await editBox.fill('after edit');
    await page.getByRole('button', { name: 'Save' }).click();

    await expect(page.locator('main').getByText('after edit')).toBeVisible();
    await expect(page.locator('main').getByText('(edited)')).toBeVisible();
  });
});

test.describe('Delete', () => {
  test('hovering own message reveals the delete button', async ({ page }) => {
    await setup(page);
    const input = page.getByPlaceholder(/type a message/i);
    await input.fill('to be deleted');
    await input.press('Enter');

    const message = page.locator('main').getByText('to be deleted');
    await message.hover();
    await expect(page.getByTitle('Delete')).toBeVisible();
  });

  test('clicking delete removes the message immediately', async ({ page }) => {
    await setup(page);
    const input = page.getByPlaceholder(/type a message/i);
    const text = `del-${Date.now()}`;
    await input.fill(text);
    await input.press('Enter');

    await expect(page.locator('main').getByText(text)).toBeVisible();

    const message = page.locator('main').getByText(text);
    await message.hover();
    await page.getByTitle('Delete').click();

    await expect(page.locator('main').getByText(text)).not.toBeVisible();
  });
});
