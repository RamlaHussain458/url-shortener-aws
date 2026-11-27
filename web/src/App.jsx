import { useEffect, useRef, useState } from 'react';
import { ApiError, createShortLink } from './api.js';
import { displayHost, MAX_URL_LENGTH, normalizeUrl } from './url.js';

const STORAGE_KEY = 'url-shortener:recent';
const MAX_RECENT = 5;

function isValidEntry(entry) {
  return (
    entry &&
    typeof entry.shortUrl === 'string' &&
    typeof entry.originalUrl === 'string' &&
    /^https?:\/\//i.test(entry.shortUrl)
  );
}

function loadRecent() {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]');
    return Array.isArray(parsed) ? parsed.filter(isValidEntry).slice(0, MAX_RECENT) : [];
  } catch {
    return [];
  }
}

function saveRecent(items) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  } catch {
    // Storage can be blocked (private mode). The app still works without it.
  }
}

function messageFor(error) {
  if (error instanceof ApiError) {
    switch (error.kind) {
      case 'invalid':
        return 'That doesn’t look like a valid public web address. Check the link and try again.';
      case 'rate-limited':
        return error.retryAfter
          ? `You’re going a bit fast. Please wait ${error.retryAfter} seconds and try again.`
          : 'You’re going a bit fast. Please wait a moment and try again.';
      case 'network':
        return 'We couldn’t reach the server. Check your connection and try again.';
      default:
        break;
    }
  }
  return 'Something went wrong on our side. Please try again in a moment.';
}

async function copyToClipboard(text) {
    // First, try the modern Clipboard API.
    if (navigator.clipboard && window.isSecureContext) {
        try {
            await navigator.clipboard.writeText(text);
            return true;
        } catch {
            // If the modern API fails, try the fallback.
        }
    }

    // Fallback for browsers where Clipboard API is unavailable.
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.setAttribute('readonly', '');

    // Keep the textarea out of sight without removing it from the document.
    textArea.style.position = 'fixed';
    textArea.style.left = '-9999px';
    textArea.style.top = '0';

    document.body.appendChild(textArea);
    textArea.select();
    textArea.setSelectionRange(0, textArea.value.length);

    let copied = false;

    try {
        copied = document.execCommand('copy');
    } catch {
        copied = false;
    } finally {
        document.body.removeChild(textArea);
    }

    return copied;
}

export default function App() {
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [result, setResult] = useState(null);
  const [recent, setRecent] = useState(loadRecent);
  const [copiedText, setCopiedText] = useState('');
  const [copyFailed, setCopyFailed] = useState(false);
  const resultInputRef = useRef(null);
  const copyTimer = useRef(null);

  useEffect(() => () => clearTimeout(copyTimer.current), []);

  async function handleSubmit(event) {
    event.preventDefault();
    if (loading) {
      return;
    }
    const url = normalizeUrl(input);
    if (!url) {
      setError('Please paste a link first.');
      return;
    }
    if (url.length > MAX_URL_LENGTH) {
      setError('That link is too long. Links can be up to 2048 characters.');
      return;
    }

    setLoading(true);
    setError('');
    setResult(null);
    setCopiedText('');
    setCopyFailed(false);
    try {
      const created = await createShortLink(url);
      setResult(created);
      const next = [created, ...recent.filter((item) => item.shortUrl !== created.shortUrl)].slice(0, MAX_RECENT);
      setRecent(next);
      saveRecent(next);
    } catch (err) {
      setError(messageFor(err));
    } finally {
      setLoading(false);
    }
  }

  async function handleCopy(text) {
    const ok = await copyToClipboard(text);
    setCopyFailed(!ok);
    setCopiedText(ok ? text : '');
    if (!ok) {
      resultInputRef.current?.select();
    }
    clearTimeout(copyTimer.current);
    copyTimer.current = setTimeout(() => setCopiedText(''), 2500);
  }

  function clearRecent() {
    setRecent([]);
    saveRecent([]);
  }

  return (
    <main className="page">
      <section className="card" aria-labelledby="title">
        <h1 id="title">Shorten a link</h1>
        <p className="lead">Paste a long web address and get a short one you can share.</p>

        <form onSubmit={handleSubmit} noValidate>
          <label htmlFor="url" className="label">
            Your long link
          </label>
          <div className="row">
            <input
              id="url"
              name="url"
              type="text"
              inputMode="url"
              autoComplete="off"
              autoCapitalize="off"
              spellCheck="false"
              placeholder="https://example.com/a/very/long/address"
              value={input}
              onChange={(event) => setInput(event.target.value)}
              aria-invalid={Boolean(error)}
              aria-describedby={error ? 'form-error' : undefined}
              autoFocus
            />
            <button type="submit" className="primary" disabled={loading}>
              {loading ? 'Shortening…' : 'Shorten'}
            </button>
          </div>
        </form>

        {error && (
          <p id="form-error" className="error" role="alert">
            {error}
          </p>
        )}

        {result && (
          <div className="result" aria-labelledby="result-title">
            <h2 id="result-title">Your short link</h2>
            <div className="row">
              <input
                ref={resultInputRef}
                className="short"
                type="text"
                readOnly
                value={result.shortUrl}
                aria-label="Your short link"
                onFocus={(event) => event.target.select()}
              />
              <button type="button" className="secondary" onClick={() => handleCopy(result.shortUrl)}>
                {copiedText === result.shortUrl ? 'Copied' : 'Copy'}
              </button>
            </div>
            <p className="hint">
              <a href={result.shortUrl} target="_blank" rel="noopener noreferrer">
                Open the link in a new tab
              </a>
            </p>
          </div>
        )}

        {copyFailed && (
          <p className="error" role="alert">
            We couldn’t copy automatically. The link is selected, so press Ctrl+C (or Cmd+C on a Mac) to copy it.
          </p>
        )}

        <p className="visually-hidden" role="status" aria-live="polite">
          {copiedText ? 'Copied to clipboard.' : result ? 'Your short link is ready.' : ''}
        </p>

        {recent.length > 0 && (
          <section className="recent" aria-labelledby="recent-title">
            <div className="recent-head">
              <h2 id="recent-title">Recent links</h2>
              <button type="button" className="link-button" onClick={clearRecent}>
                Clear
              </button>
            </div>
            <ul>
              {recent.map((item) => (
                <li key={item.shortUrl}>
                  <div className="recent-text">
                    <a href={item.shortUrl} target="_blank" rel="noopener noreferrer">
                      {item.shortUrl}
                    </a>
                    <span className="muted" title={item.originalUrl}>
                      {displayHost(item.originalUrl)}
                    </span>
                  </div>
                  <button type="button" className="secondary small" onClick={() => handleCopy(item.shortUrl)}>
                    {copiedText === item.shortUrl ? 'Copied' : 'Copy'}
                  </button>
                </li>
              ))}
            </ul>
            <p className="muted small-print">Saved only in this browser.</p>
          </section>
        )}
      </section>

      <p className="footer">
        Short links are public. Please don’t shorten private or sensitive addresses.
      </p>
    </main>
  );
}
