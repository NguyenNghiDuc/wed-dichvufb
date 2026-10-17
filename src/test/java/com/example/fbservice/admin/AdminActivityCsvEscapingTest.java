package com.example.fbservice.admin;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AdminActivityCsvEscapingTest {
    @Test
    void escapesFormulaPrefixesQuotesAndLineBreaks() {
        assertThat(AdminActivityCsvController.cell("=1+2")).isEqualTo("\"'=1+2\"");
        assertThat(AdminActivityCsvController.cell(" \t@SUM(1)")).isEqualTo("\"' \t@SUM(1)\"");
        assertThat(AdminActivityCsvController.cell("\uFEFF-3+4"))
            .isEqualTo("\"'\uFEFF-3+4\"");
        assertThat(AdminActivityCsvController.cell("a\"b\r\nc"))
            .isEqualTo("\"a\"\"b\r\nc\"");
        assertThat(AdminActivityCsvController.cell(null)).isEqualTo("\"\"");
        assertThat(AdminActivityCsvController.cell("ADMIN")).isEqualTo("\"ADMIN\"");
    }
}
