export const MAX_URL_LENGTH = 2048;

const HAS_SCHEME = /^[a-z][a-z0-9+.-]*:\/\//i;

/**
 * Cleans up what a person pasted. Non-technical users often leave out "https://", so it is added
 * for them. The backend still validates everything, this is only a convenience.
 */
export function normalizeUrl(input) {
  const value = (input ?? '').trim();
  if (!value) {
    return '';
  }
  if (value.startsWith('//')) {
    return `https:${value}`;
  }
  return HAS_SCHEME.test(value) ? value : `https://${value}`;
}

/** Short, readable label for a long URL (just the host name). */
export function displayHost(url) {
  try {
    return new URL(url).host;
  } catch {
    return url;
  }
}
