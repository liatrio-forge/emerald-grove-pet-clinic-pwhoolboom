import { test, expect } from '@fixtures/base-test';

test.describe('Owner CSV Export', () => {
  test('GET /owners.csv returns text/csv with header row', async ({ page }) => {
    // Act — use page.request so the authenticated browser cookies are included
    const response = await page.request.get('/owners.csv');
    const body = await response.text();

    // Assert
    expect(response.status()).toBe(200);
    expect(response.headers()['content-type']).toContain('text/csv');
    expect(body).toContain('id,firstName,lastName,address,city,telephone');
  });
});
