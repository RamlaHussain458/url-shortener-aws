# 0001. Random Base62 short codes

**Status:** Accepted

**Context:** Each link needs a short, unique code. Options were a counter encoded in Base62, a hash of the URL, or random codes.

**Decision:** Generate 7 random characters from `0-9A-Za-z` using `SecureRandom`. Rely on a unique database index and retry on collision.

**Consequences:**
- Codes cannot be guessed, so links cannot be enumerated.
- There are 62^7 (about 3.5 trillion) possible codes, so collisions are rare and handled by a retry loop.
- Inserting first and catching the constraint error avoids a race between "check" and "insert".
- The same URL submitted twice gets two different codes.
