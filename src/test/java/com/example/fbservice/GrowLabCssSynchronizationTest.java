package com.example.fbservice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guard the shared public CSS contract without requiring a database or a browser.
 * Browser/mobile UAT is still required before a production integration merge.
 */
class GrowLabCssSynchronizationTest {

    private String resource(String classpath) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(classpath)) {
            assertThat(stream).as("Classpath resource %s", classpath).isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void sharedStylesheetKeepsBothPageThemesAndAccessibleBreakpoints() throws Exception {
        String css = resource("static/style.css");

        assertThat(css).contains("GrowLab CSS synchronization (2026-10-18)");
        assertThat(css).contains("--gl-brand-primary:", "--gl-brand-cyan:", "--gl-brand-focus:");
        assertThat(css).contains("body:not(.boost-page):not(.account-page)");
        assertThat(css).contains(".boost-page .boost-register");
        assertThat(css).contains(".account-page .boost-faq");
        assertThat(css).contains(".account-page .checkout-grid > *{min-width:0}");
        assertThat(css).contains("@media(max-width:980px)", "@media(max-width:600px)");
        assertThat(css).contains("@media(prefers-reduced-motion:reduce)");
        assertThat(css).doesNotContain("@import url(");
    }

    @Test
    void activeTemplatesUseTheSinglePublicStylesheet() throws Exception {
        String[] templates = {
            "templates/home.html", "templates/service_detail.html",
            "templates/campaign_form.html", "templates/campaign_list.html",
            "templates/login.html", "templates/admin_dashboard.html",
            "templates/help.html"
        };
        for (String template : templates) {
            assertThat(resource(template)).as(template).contains("@{/style.css}");
        }
        assertThat(resource("templates/home.html")).contains("class=\"boost-page\"");
        assertThat(resource("templates/service_detail.html")).contains("class=\"account-page\"");
        assertThat(resource("templates/campaign_form.html")).contains("class=\"account-page\"");
    }
}
