# 0005. React frontend served by nginx with a reverse proxy

**Status:** Accepted

**Context:** The frontend and the API could be hosted on different origins (which needs CORS) or behind one address.

**Decision:** Build the React app into static files and serve them with an unprivileged nginx container. nginx also forwards `/api/*` and short codes to the Spring Boot container, so visitors use a single address.

**Consequences:**
- No CORS configuration is needed.
- nginx sets `X-Forwarded-For` from the real client address, and the API reads it, so per-IP rate limiting keeps working.
- The same setup works on a single EC2 instance. S3 and CloudFront remain an option for a later iteration.
