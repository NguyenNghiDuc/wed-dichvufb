"""Unit tests for the fail-closed CodeQL SARIF validator."""

from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from validate_codeql_csrf_sarif import validate_sarif_paths


class CodeQLSarifValidatorTest(unittest.TestCase):
    def write_sarif(self, directory: Path, document: dict) -> Path:
        path = directory / "results.sarif"
        path.write_text(json.dumps(document), encoding="utf-8")
        return path

    def document(self, *, version: str = "2.1.0", results: list | None = None) -> dict:
        return {
            "version": version,
            "runs": [
                {
                    "tool": {
                        "driver": {"rules": [{"id": "java/driver-rule"}]},
                        "extensions": [{"rules": [{"id": "growlab/extension-rule"}]}],
                    },
                    "results": results or [],
                }
            ],
        }

    def test_accepts_extension_rule_result(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            path = self.write_sarif(
                Path(temp),
                self.document(results=[{"ruleId": "growlab/extension-rule"}]),
            )
            self.assertEqual(
                validate_sarif_paths([str(path)], ["growlab/extension-rule"]),
                {"growlab/extension-rule": 1},
            )

    def test_resolves_driver_rule_index(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            path = self.write_sarif(
                Path(temp),
                self.document(results=[{"ruleIndex": 0}]),
            )
            self.assertEqual(
                validate_sarif_paths([str(path)], ["java/driver-rule"]),
                {"java/driver-rule": 1},
            )

    def test_rejects_rule_definition_without_result(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            path = self.write_sarif(Path(temp), self.document())
            with self.assertRaisesRegex(ValueError, "zero serialized results"):
                validate_sarif_paths([str(path)], ["growlab/extension-rule"])

    def test_rejects_missing_rule_definition(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            path = self.write_sarif(
                Path(temp),
                self.document(results=[{"ruleId": "unknown/rule"}]),
            )
            with self.assertRaisesRegex(ValueError, "missing rule definitions"):
                validate_sarif_paths([str(path)], ["growlab/extension-rule"])

    def test_rejects_non_sarif_210(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            path = self.write_sarif(
                Path(temp),
                self.document(version="2.0.0", results=[{"ruleId": "growlab/extension-rule"}]),
            )
            with self.assertRaisesRegex(ValueError, "Unexpected SARIF version"):
                validate_sarif_paths([str(path)], ["growlab/extension-rule"])

    def test_rejects_empty_directory(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaisesRegex(ValueError, "No SARIF files found"):
                validate_sarif_paths([temp], ["growlab/extension-rule"])


if __name__ == "__main__":
    unittest.main()
