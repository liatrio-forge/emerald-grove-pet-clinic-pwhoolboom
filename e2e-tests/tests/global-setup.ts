import { mkdirSync } from 'fs';

import { chromium, type FullConfig } from '@playwright/test';

async function waitForHttpOk(url: string, timeoutMs: number): Promise<void> {
  const deadline = Date.now() + timeoutMs;
  let lastError: unknown;

  while (Date.now() < deadline) {
    try {
      const res = await fetch(url, { redirect: 'follow' });
      if (res.ok) {
        return;
      }
      lastError = new Error(`Non-OK response: ${res.status}`);
    } catch (e) {
      lastError = e;
    }

    await new Promise((r) => setTimeout(r, 500));
  }

  throw lastError instanceof Error ? lastError : new Error('Timed out waiting for app');
}

export default async function globalSetup(config: FullConfig): Promise<void> {
  const baseURL = config.projects[0]?.use?.baseURL as string | undefined;
  const url = baseURL ?? 'http://localhost:8080';

  await waitForHttpOk(url, 120_000);

  mkdirSync('test-results/.auth', { recursive: true });

  const browser = await chromium.launch();
  try {
    const context = await browser.newContext();
    const page = await context.newPage();
    await page.setViewportSize({ width: 1280, height: 720 });
    await page.goto(url, { waitUntil: 'domcontentloaded' });
    await page.screenshot({ path: 'test-results/home-page.png', fullPage: true });

    // Log in as admin and persist the authenticated session for all tests
    await page.goto(`${url}/login`, { waitUntil: 'domcontentloaded' });
    await page.locator('input#username').fill('admin@petclinic.com');
    await page.locator('input#password').fill('admin');
    await page.getByRole('button', { name: /Sign In/i }).click();
    await page.waitForURL((u) => !u.pathname.startsWith('/login'));

    await context.storageState({ path: 'test-results/.auth/admin.json' });
  } finally {
    await browser.close();
  }
}
