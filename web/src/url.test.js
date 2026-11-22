import { describe, expect, it } from 'vitest';
import { displayHost, normalizeUrl } from './url.js';

describe('normalizeUrl', () => {
  it('trims whitespace', () => {
    expect(normalizeUrl('  https://example.com/a  ')).toBe('https://example.com/a');
  });

  it('adds https when the scheme is missing', () => {
    expect(normalizeUrl('example.com/page')).toBe('https://example.com/page');
  });

  it('keeps an existing http or https scheme', () => {
    expect(normalizeUrl('http://example.com')).toBe('http://example.com');
    expect(normalizeUrl('HTTPS://example.com')).toBe('HTTPS://example.com');
  });

  it('turns protocol relative links into https links', () => {
    expect(normalizeUrl('//example.com/x')).toBe('https://example.com/x');
  });

  it('returns an empty string for empty input', () => {
    expect(normalizeUrl('   ')).toBe('');
    expect(normalizeUrl(undefined)).toBe('');
  });

  it('leaves other schemes alone so the server can reject them', () => {
    expect(normalizeUrl('ftp://example.com/file')).toBe('ftp://example.com/file');
  });
});

describe('displayHost', () => {
  it('returns the host name', () => {
    expect(displayHost('https://www.example.com/a/b?c=1')).toBe('www.example.com');
  });

  it('falls back to the input when it is not a URL', () => {
    expect(displayHost('not a url')).toBe('not a url');
  });
});
