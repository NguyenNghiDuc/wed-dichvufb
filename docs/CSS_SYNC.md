# CSS synchronization across GrowLab's Java 25 workspace

Purpose: align the visual tokens of the current public landing (dark pink/cyan), authenticated customer pages, public service detail, and legacy light help/admin/auth pages without overwriting W1/W3/UI work.

## Strategy
- Source of truth: `src/main/resources/static/style.css` from **main** commit `6290d3632633801f48ee236110d9efef6a4eb182`.
- Destination: this CSS-only branch builds **on current main** `6290d3632633801f48ee236110d9efef6a4eb182`, preserving the newest home page and service detail. The Java 25 PR #52 was forked before those pages were integrated; do not overwrite newer pages with its older CSS. This branch appends a **namespaced** design contract without replacing the public landing templates.
- Shared `--gl-brand-*` design tokens make light utility pages, public landing and customer workspace consistent while retaining dark vs light readability.
- Preserve current layout and JS/Thymeleaf: `.boost-page` public home, `.account-page` service/campaign pages, and light utility pages that have no body class.
- Mobile/tablet fixes: stacked landing at tablet widths, scannable customer navigation, scrollable data tables, single-column checkout and FAQ, keyboard focus indicators, reduced motion.
- The `.boost-faq` markup inside the account service page is styled as a dark card; on the public home page existing FAQ remains unchanged.
- No external assets or CDN references; no application, authentication, migrations, payments or scripts modified.

## Automated verification
`mvn -Dtest=GrowLabCssSynchronizationTest test` — static CSS contract test and 7 shared stylesheet/template references. On this CSS branch use Java 17 (current main baseline); the Java 25 upgrade is isolated in PR #52. Full project `mvn clean verify` requires local PostgreSQL 16 `growlab_ci` database (same as Java CI). On GitHub, Java CI and CodeQL must PASS for the exact CSS branch HEAD.

## UAT and integration
- Verify desktop/tablet/mobile for `/`, `/dich-vu/tiktok/profile-audit`, `/login`, `/register`, `/campaigns`, `/campaigns/new`, `/admin/**` and `/help`.
- Review all branding, contrast, keyboard focus, dropdowns, modal overlays, price cards, table horizontal scroll and FAQ.
- Not all templates use `.account-page`; light pages intentionally remain light but inherit the same pink accent. Avoid forcing dark backgrounds on them without markup adjustments.
- **Avoid merging PR #51 wholesale**: its old service-detail and shared stylesheet are from a different snapshot, while the main branch already contains a newer homepage and service page.
- Coordinate with W3 PR #35, whose `style.css` version is older and should not replace current main's styles.
- First integrate this CSS PR into main after full CI and browser review. Then update/rebase Java 25 PR #52 onto current main while preserving the added service detail pages and design tokens; rerun Java CI + CodeQL on its new exact head. Do not use wholesale CSS replacement.
