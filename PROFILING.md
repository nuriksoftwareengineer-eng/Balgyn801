# Production Request Profiler

A built-in, opt-in, APM-style request profiler. When enabled it logs a per-stage timing breakdown
(plus Hikari / JVM / PostgreSQL context) for every HTTP request slower than a threshold. When
disabled it does not exist — no beans, no wrappers, no listeners, no overhead. It is safe to keep in
the codebase permanently and to enable in production for a diagnosis window.

## Purpose

To answer, with measurements rather than guesses, **where** a slow request spends its time:
Spring Security filters, request dispatch, JDBC connection acquisition (Hikari pool wait), SQL
execution, ResultSet reading, entity/DTO mapping, or JSON serialization — and what the pool, heap and
database looked like at that moment.

## Architecture

All classes live in [`com.nurba.java.profiling`](src/main/java/com/nurba/java/profiling). Everything
is created by a single `@ConditionalOnProperty` configuration, so the whole feature is present only
when enabled.

| Component | Role |
|---|---|
| `ProfilingConfig` | `@ConditionalOnProperty("app.profiling.enabled"=true)`. Registers the filters, interceptor, `ProfilingMetrics`, and the DataSource-wrapping `BeanPostProcessor`. Nothing here exists when the flag is off. |
| `OuterProfilingFilter` | Order **-200** (before Spring Security at -100). Starts the per-request timer; on completion, if total ≥ threshold, emits one log line. Flushes the response before profiling so the client is never delayed. |
| `PostSecurityProfilingFilter` | Order **0** (after Security). Marks the security-filters boundary. |
| `ProfilingInterceptor` | `HandlerInterceptor`. Marks controller start (`preHandle`), end before serialization (`postHandle`), and completion (`afterCompletion`). |
| `TimingDataSource` | `DelegatingDataSource` that times `getConnection()` and wraps the connection for SQL timing. |
| `JdbcTiming` | Dynamic proxies (Connection → Statement → ResultSet) timing `execute*()` and `ResultSet.next()`. |
| `ProfilingMetrics` | Collects Hikari / JVM / PostgreSQL context, only for requests past the threshold. |
| `RequestTimings` | Per-request `ThreadLocal` holding the checkpoints and accumulators. |

Flow of one request (single Tomcat worker thread):

```
OuterProfilingFilter.begin()  → Security chain → PostSecurityProfilingFilter (securityDone)
  → DispatcherServlet → ProfilingInterceptor.preHandle (handlerStart)
    → controller → service → TimingDataSource.getConnection() + JdbcTiming (sql, rsRead)
  → ProfilingInterceptor.postHandle (handlerEnd) → Jackson serialization
  → ProfilingInterceptor.afterCompletion (completed)
→ OuterProfilingFilter finally: flush response → report() if slow → clear()
```

## Enabling / disabling

Enable (e.g. in `docker-compose.prod.yml` under the `app` service):

```yaml
environment:
  - APP_PROFILING_ENABLED=true
```

Disable: set `APP_PROFILING_ENABLED=false` or remove it (default is off). No restart-time or
request-time cost when off.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `APP_PROFILING_ENABLED` | `false` | Master switch. Off = no beans, no overhead. |
| `APP_PROFILING_SLOW_REQUEST_THRESHOLD_MS` | `100` | Only requests with total ≥ this are logged. |
| `APP_PROFILING_PG_STATS_ENABLED` | `true` | Collect PostgreSQL stats on slow requests. |
| `APP_PROFILING_PG_STATS_THROTTLE_MS` | `1000` | Minimum interval between PostgreSQL probes. |

## Example log

Logger name `PROFILE`, level `WARN`. Grep for `SLOW`.

```
[SLOW 3987ms] GET /api/v1/catalog/groups -> 200
  stages : security=1 dispatch=0 getConn=3901(1) sql=2(1) rsRead=0(6 rows) mapping=5 serialize=0 total=3987
  hikari : active=10 idle=0 pending=7 total=10 max=10
  jvm    : heapUsed=214MB heapMax=1946MB lastGcPause=8ms/G1 Young Generation
  pg     : active=3 waiting=1 lockWaits=1
```

## Field reference

**Header:** `[SLOW <total>ms] <method> <uri> -> <status>` — request path only (never query string,
headers, cookies or body).

**stages** (milliseconds):

| Field | Meaning |
|---|---|
| `security` | Time inside the Spring Security filter chain. |
| `dispatch` | Security boundary → controller start (DispatcherServlet routing). |
| `getConn(n)` | Accumulated `DataSource.getConnection()` time; `n` = number of acquisitions. High = Hikari pool wait. |
| `sql(n)` | Accumulated JDBC `execute*()` time; `n` = statements. |
| `rsRead(k rows)` | Accumulated `ResultSet.next()` time; `k` = rows iterated. |
| `mapping` | Handler time minus getConn/sql/rsRead ≈ entity hydration + DTO mapping + business logic. |
| `serialize` | Controller return → response written (Jackson serialization + commit). |
| `total` | Whole request wall time. |

**hikari:** `active` (in use), `idle` (free), `pending` (threads **waiting** for a connection —
`>0` means pool starvation), `total`, `max`.

**jvm:** `heapUsed`, `heapMax`, `lastGcPause` (duration + collector of the most recent GC, captured
passively via a GC notification listener).

**pg:** `active` (backends running a query), `waiting` (backends blocked on a lock),
`lockWaits` (ungranted locks). `disabled` / `n/a(...)` if unavailable.

`n/a` appears for any stage whose checkpoint wasn't reached (e.g. a request rejected inside Security
never reaches the handler).

## Limitations

- **SQL vs fetch split** is driver-dependent. With the PostgreSQL driver's default fetch (no cursor)
  the whole result is materialised during `execute*()`, so `rsRead` is typically ~0; it becomes
  meaningful only with server-side cursors / streaming.
- **`mapping`** is a derived value (handler − getConn − sql − rsRead), not independently timed.
- **`lastGcPause`** uses `com.sun.management.GarbageCollectionNotificationInfo` (HotSpot / Amazon
  Corretto). On a non-HotSpot JVM it reads `none`.
- The **PostgreSQL probe** reflects a throttled snapshot (default once/second), so its numbers are
  near-real-time, not exact per-request.
- **Servlet-async endpoints** (`Callable`/`DeferredResult`/streaming): timing is measured per
  dispatch, and the DB work runs on a separate async thread whose `ThreadLocal` the profiler does not
  span, so `getConn`/`sql` on the async portion are not captured. This is safe (no leak, no crash —
  each dispatch cleans up its own `ThreadLocal`) but the numbers for such endpoints are partial. The
  app's storefront endpoints are all synchronous.

## Performance impact

- **Disabled:** zero. `@ConditionalOnProperty` means none of the beans are created, the DataSource is
  not wrapped, no GC listener is registered, and no per-request `ThreadLocal` is allocated. Proven by
  [`ProfilingConfigConditionalTest`](src/test/java/com/nurba/java/profiling/ProfilingConfigConditionalTest.java).
- **Enabled, fast requests (< threshold):** a handful of `System.nanoTime()` reads and the JDBC
  proxies; no log output, no metric collection, no database probe.
- **Enabled, slow requests (≥ threshold):** one log line + in-process MXBean reads (microseconds) +
  at most one throttled PostgreSQL probe (bounded to ~1s, on a dedicated short-lived connection, not
  the Hikari pool). The response is flushed to the client **before** any of this, so client-visible
  latency is never affected.
- JDBC proxying adds per-statement / per-row reflection **only while enabled**.

## Production recommendations

- Enable it for a **diagnosis window** when investigating latency, then turn it off (or leave it on
  with a conservative threshold — the cost on fast requests is negligible).
- Keep the threshold at or above your p99 so the log stays signal, not noise.
- Read `getConn` together with `hikari.pending` and `pg.waiting`: `getConn` high + `pending>0`
  ⇒ pool starvation; `sql` high + `pg.lockWaits>0` ⇒ database lock contention; all stages small while
  the client still sees a delay ⇒ the latency is **outside** the app (reverse proxy / TLS / network).
- If the PostgreSQL probe is ever undesirable, set `APP_PROFILING_PG_STATS_ENABLED=false`.

## Safety guarantees (tested)

The profiler never breaks a request: any internal error is swallowed and logged at DEBUG, the
business exception always propagates unchanged, and the `ThreadLocal` is always cleared. See
[`OuterProfilingFilterTest`](src/test/java/com/nurba/java/profiling/OuterProfilingFilterTest.java)
for threshold gating, exception-during-request, exception-inside-profiler, concurrency isolation, and
async-request coverage.
