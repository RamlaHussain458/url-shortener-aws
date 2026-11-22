# 0004. One shared MySQL Testcontainer for all integration tests

**Status:** Accepted

**Context:** Integration tests run against a real MySQL. A container per test class failed on CI because Spring cached the application context, which still pointed at a stopped container.

**Decision:** Start one container in a static initializer of the shared base class and let Testcontainers remove it when the JVM exits.

**Consequences:**
- All test classes share one database, so tests must use unique data and not depend on an empty database.
- Integration tests need Docker running. Without it they fail instead of being skipped.
- The test run is faster because MySQL starts once.
