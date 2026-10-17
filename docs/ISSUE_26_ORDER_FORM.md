# Customer order form UX and cancellation feedback (#26)

- On invalid service/channel URL or brief, `POST /campaigns` preserves the platform, service, URL and brief on re-render. Thymeleaf `th:text` and `th:value` escape user input rather than placing raw HTML into the response.
- Price remains server-derived; no client price field is added.
- Customer cancel still succeeds only for PENDING_REVIEW; success/error now follows a post/redirect/get flash banner, preventing hidden error messages and accidental repeated POST on reload.
- Missing CSRF still rejects all state-changing form requests.
- Integration checks cover field preservation, XSS escaping, invalid target URL, and cancellation feedback.
- Requires browser/mobile UAT before release.
