# 0002. Redirect with 302, not 301

**Status:** Accepted

**Context:** Short links can redirect permanently (301) or temporarily (302).

**Decision:** Use 302 Found with `Cache-Control: no-store`.

**Consequences:**
- Browsers do not cache the redirect, so features such as click counting and link expiry remain possible later.
- Every visit reaches the application, which is why an LRU cache sits in front of the database.
