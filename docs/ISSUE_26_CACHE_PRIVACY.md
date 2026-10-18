# Issue #26 — cache control for sensitive customer order API

All private customer order JSON responses now include `Cache-Control: no-store`: legacy list, summary, paged filters, individual order, history, create and cancellation. This reduces the risk of a shared proxy/browser retaining responses for another account.

Owner isolation and CSRF remain unchanged. No new public endpoints; no changes to administrator, staff, payment or support code. MockMvc regression tests cover success paths and foreign order 404. Human security review and browser/cache validation remain acceptance gates.
