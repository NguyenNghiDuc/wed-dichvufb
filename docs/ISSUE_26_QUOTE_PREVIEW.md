# Customer quote preview, Issue #26

The order request form loads a same-origin external script under the existing CSP, rather than using inline JavaScript. Changing service type fetches the authenticated read-only quote endpoint, shows formatted VND value and explains it remains provisional.

The script uses `textContent` rather than `innerHTML`, encodes the service identifier, aborts stale requests, validates the returned service type/amount, and fails safely when offline or disabled. The form still works without JavaScript and the server always recalculates the price during order creation.

MockMvc tests cover HTML script/ARIA integration and static resource availability. Human mobile/UAT and offline testing remain release gates.
