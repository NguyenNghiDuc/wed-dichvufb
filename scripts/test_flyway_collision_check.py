"""Regression tests for Flyway duplicate version rejection."""
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

from flyway_collision_check import check, collisions, version

SCRIPT = Path(__file__).with_name("flyway_collision_check.py")


class FlywayVersionTests(unittest.TestCase):
    def test_standard_versions(self):
        self.assertEqual((10,), version("V10__customers.sql"))
        self.assertEqual((10, 1), version("V10_1__customers.sql"))
        self.assertEqual((10, 1), version("V10.1__customers.sql"))

    def test_version_aliases(self):
        self.assertEqual(version("V10__a.sql"), version("V10_0__b.sql"))
        self.assertEqual(version("V10__a.sql"), version("V10.0.0__b.sql"))
        self.assertEqual(version("V010__a.sql"), version("V10__b.sql"))

    def test_invalid_names(self):
        for name in ("V10_bad.sql", "V__missing.sql", "V1.2.__broken.sql",
                     "V1__bad.txt", "V1__x.SQL"):
            with self.subTest(name=name):
                self.assertIsNone(version(name))

    def test_collisions(self):
        self.assertEqual(
            {(10,): ["V10_0__b.sql", "V10__a.sql"]},
            collisions(["V10__a.sql", "V10_0__b.sql", "V11__ok.sql"]),
        )

    def test_valid(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            (directory / "V1__init.sql").write_text("select 1;")
            (directory / "V2__next.sql").write_text("select 2;")
            (directory / "R__view.sql").write_text("select 3;")
            self.assertEqual(([], 2), check(directory))

    def test_invalid_and_duplicate_are_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            for name in ("V10__one.sql", "V10.0__two.sql", "V_bad.sql"):
                (directory / name).write_text("select 1;")
            errors, count = check(directory)
            self.assertEqual(3, count)
            self.assertTrue(any("Duplicate Flyway V10:" in e for e in errors))
            self.assertTrue(any("Invalid versioned Flyway filename" in e for e in errors))

    def test_missing_or_empty_fails_closed(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            self.assertTrue(check(directory)[0])
            self.assertTrue(check(directory / "missing")[0])

    def test_cli_result(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            (directory / "V1__okay.sql").write_text("select 1;")
            ok = subprocess.run([sys.executable, str(SCRIPT), "--migration-dir", str(directory)],
                                capture_output=True, text=True, check=False)
            self.assertEqual(ok.returncode, 0, ok.stderr)
            (directory / "V1_0__duplicate.sql").write_text("select 1;")
            bad = subprocess.run([sys.executable, str(SCRIPT), "--migration-dir", str(directory)],
                                 capture_output=True, text=True, check=False)
            self.assertEqual(bad.returncode, 1)
            self.assertIn("Duplicate Flyway V1", bad.stderr)


if __name__ == "__main__":
    unittest.main()
