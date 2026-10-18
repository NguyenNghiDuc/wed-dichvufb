# W1: Session lifecycle and authentication checks

- Session inactivity timeout: 30 minutes, set in application.properties, shared by both dev and prod unless explicitly overridden by deployment.
- Session cookie: HttpOnly and SameSite=Lax by default. The production profile separately requires Secure=true and a trusted TLS proxy.
- Spring Security session fixation protection changes the session ID upon successful form authentication; tested with MockMvc.
- Logout is POST /logout and requires CSRF. The old authenticated session must not be trusted after logout.
- Protected routes require a current DB-backed principal; password change and revoke-all session functions require current-password proof and CSRF.
- Run staging tests over HTTPS in Codespaces or a production-like reverse proxy, including cookie Secure/SameSite attributes and different browsers. MockMvc does not replace real browser/UAT.
- The current in-memory authenticated-session store is not shared across multiple nodes; clustered production needs a central session store and distributed abuse prevention.
