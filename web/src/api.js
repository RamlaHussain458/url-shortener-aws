/** Error with a simple category the UI can turn into a friendly message. */
export class ApiError extends Error {
  constructor(kind, { retryAfter = null } = {}) {
    super(kind);
    this.name = 'ApiError';
    this.kind = kind; // 'invalid' | 'rate-limited' | 'server' | 'network'
    this.retryAfter = retryAfter;
  }
}

export function parseRetryAfter(value) {
  const seconds = Number.parseInt(value ?? '', 10);
  return Number.isFinite(seconds) && seconds > 0 ? seconds : null;
}

export async function createShortLink(url, { signal } = {}) {
  let response;
  try {
    response = await fetch('/api/v1/urls', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ url }),
      signal,
    });
  } catch (error) {
    if (error?.name === 'AbortError') {
      throw error;
    }
    throw new ApiError('network');
  }

  if (response.ok) {
    let data;
    try {
      data = await response.json();
    } catch {
      throw new ApiError('server');
    }
    if (typeof data?.shortUrl !== 'string' || typeof data?.originalUrl !== 'string') {
      throw new ApiError('server');
    }
    return { shortUrl: data.shortUrl, originalUrl: data.originalUrl };
  }

  if (response.status === 400) {
    throw new ApiError('invalid');
  }
  if (response.status === 429) {
    throw new ApiError('rate-limited', { retryAfter: parseRetryAfter(response.headers.get('Retry-After')) });
  }
  throw new ApiError('server');
}
