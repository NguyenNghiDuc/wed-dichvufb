"""Tests for the CodeQL path-graph log guard."""

from __future__ import annotations

import unittest

from check_codeql_path_graph_log import find_path_graph_errors


class CodeQLPathGraphLogTest(unittest.TestCase):
    RULE = "java/csrf-unprotected-request-type"

    def test_detects_missing_start_node_error_with_runner_prefix(self) -> None:
        log = (
            "2026-11-23T05:06:41.551Z ERROR: While processing path-problem rule "
            f"{self.RULE}, got start node that does not appear in graph: loginPage."
        )
        errors = find_path_graph_errors(log, self.RULE)
        self.assertEqual(len(errors), 1)
        self.assertIn("loginPage", errors[0][1])

    def test_ignores_errors_for_other_rules(self) -> None:
        log = (
            "ERROR: While processing path-problem rule java/unvalidated-url-forward, "
            "got start node that does not appear in graph: forward."
        )
        self.assertEqual(find_path_graph_errors(log, self.RULE), [])

    def test_accepts_clean_log(self) -> None:
        log = "Interpreted pathproblem query. Exported results to SARIF."
        self.assertEqual(find_path_graph_errors(log, self.RULE), [])

    def test_detects_multiple_errors(self) -> None:
        log = "\\n".join(
            f"ERROR: While processing path-problem rule {self.RULE}, missing node: {name}."
            for name in ("loginPage", "registerPage", "edit")
        )
        self.assertEqual(len(find_path_graph_errors(log, self.RULE)), 3)


if __name__ == "__main__":
    unittest.main()
