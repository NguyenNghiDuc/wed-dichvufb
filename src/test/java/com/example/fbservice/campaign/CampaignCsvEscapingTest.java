package com.example.fbservice.campaign;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CampaignCsvEscapingTest {
    @Test
    void escapesCommasQuotesAndFormulaPrefixes() {
        assertThat(CampaignCsvExportService.escape("hello,world")).isEqualTo("\"hello,world\"");
        assertThat(CampaignCsvExportService.escape("A\"B")).isEqualTo("\"A\"\"B\"");
        assertThat(CampaignCsvExportService.escape("=SUM(1,1)")).isEqualTo("\"'=SUM(1,1)\"");
        assertThat(CampaignCsvExportService.escape("+cmd")).isEqualTo("\"'+cmd\"");
        assertThat(CampaignCsvExportService.escape("  @cmd")).isEqualTo("\"'  @cmd\"");
        assertThat(CampaignCsvExportService.escape("normal")).isEqualTo("\"normal\"");
    }
}
