package com.example.fbservice.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LegacyOrderCsvEscapingTest {
    @Test void spreadsheetFormulasControlCharactersAndQuotesAreEscaped() {
        assertThat(LegacyOrderCsvExportService.escape("=A1+1")).isEqualTo("\"'=A1+1\"");
        assertThat(LegacyOrderCsvExportService.escape("   @remote")).startsWith("\"'   @");
        assertThat(LegacyOrderCsvExportService.escape("\t=1")).startsWith("\"'\t=1");
        assertThat(LegacyOrderCsvExportService.escape("a\"b")).isEqualTo("\"a\"\"b\"");
    }
}
