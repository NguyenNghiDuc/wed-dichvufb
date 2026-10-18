package com.example.fbservice.campaign;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CampaignHistoryCsvEscapingTest {
    @Test void dangerousFormulasAreNotPreservedAsActiveSpreadsheetExpressions() {
        assertThat(CampaignCsvExportService.escape("=SUM(1,1)")).isEqualTo("\"'=SUM(1,1)\"");
        assertThat(CampaignCsvExportService.escape(" +cmd")).startsWith("\"' +");
        assertThat(CampaignCsvExportService.escape("a\"b")).isEqualTo("\"a\"\"b\"");
    }
}
