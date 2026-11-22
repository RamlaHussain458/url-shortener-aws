import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, createShortLink, parseRetryAfter } from './api.js';

function mockFetch(response) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue(response));
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('createShortLink', () => {
  it('returns the short and original URL on success', async () => {
    mockFetch(
      new Response(JSON.stringify({ shortCode: 'abc1234', shortUrl: 'http://x/abc1234', originalUrl: 'https://example.com' }), {
        status: 201,
        headers: { 'Content-Type': 'application/json' },
      }),
    );
    await expect(createShortLink('https://example.com')).resolves.toEqual({
      shortUrl: 'http://x/abc1234',
      originalUrl: 'https://example.com',
    });
  });

  it('sends the URL as JSON to the API', async () => {
    mockFetch(new Response(JSON.stringify({ shortUrl: 'a', originalUrl: 'b' }), { status: 201 }));
    await createShortLink('https://example.com');
    const [path, options] = fetch.mock.calls[0];
    expect(path).toBe('/api/v1/urls');
    expect(options.method).toBe('POST');
    expect(JSON.parse(options.body)).toEqual({ url: 'https://example.com' });
  });

  it('maps a 400 response to an invalid error', async () => {
    mockFetch(new Response('{}', { status: 400 }));
    await expect(createShortLink('x')).rejects.toMatchObject({ kind: 'invalid' });
  });

  it('maps a 429 response to a rate limited error with the wait time', async () => {
    mockFetch(new Response('{}', { status: 429, headers: { 'Retry-After': '7' } }));
    await expect(createShortLink('x')).rejects.toMatchObject({ kind: 'rate-limited', retryAfter: 7 });
  });

  it('maps other failures to a server error', async () => {
    mockFetch(new Response('{}', { status: 503 }));
    await expect(createShortLink('x')).rejects.toMatchObject({ kind: 'server' });
  });

  it('maps a broken success body to a server error', async () => {
    mockFetch(new Response('not json', { status: 201 }));
    await expect(createShortLink('x')).rejects.toBeInstanceOf(ApiError);
  });

  it('maps a network failure to a network error', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    await expect(createShortLink('x')).rejects.toMatchObject({ kind: 'network' });
  });
});

describe('parseRetryAfter', () => {
  it('parses positive whole seconds only', () => {
    expect(parseRetryAfter('5')).toBe(5);
    expect(parseRetryAfter('0')).toBeNull();
    expect(parseRetryAfter('abc')).toBeNull();
    expect(parseRetryAfter(null)).toBeNull();
  });
});
