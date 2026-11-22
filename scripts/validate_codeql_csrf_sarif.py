#!/usr/bin/env python3
"""Fail-closed validation for CodeQL SARIF rules required by the CSRF fixture."""

from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Iterable


def _sarif_files(paths: Iterable[str]) -> list[Path]:
    files: list[Path] = []
    for raw_path in paths:
        path = Path(raw_path)
        if path.is_dir():
            files.extend(sorted(path.rglob("*.sarif")))
        elif path.is_file() and path.suffix.lower() == ".sarif":
            files.append(path)
        else:
            raise ValueError(f"SARIF path does not exist or is not a .sarif file/directory: {path}")
    if not files:
        raise ValueError("No SARIF files found in the supplied path(s)")
    return files


def validate_sarif_paths(paths: Iterable[str], required_rules: Iterable[str]) -> dict[str, int]:
    required = tuple(dict.fromkeys(required_rules))
    if not required:
        raise ValueError("At least one --require-rule must be supplied")

    definitions: set[str] = set()
    results: Counter[str] = Counter()
    files = _sarif_files(paths)

    for path in files:
        try:
            document = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            raise ValueError(f"Cannot read SARIF {path}: {error}") from error

        if document.get("version") != "2.1.0":
            raise ValueError(f"Unexpected SARIF version in {path}: {document.get('version')!r}")

        runs = document.get("runs")
        if not isinstance(runs, list):
            raise ValueError(f"SARIF document has no runs array: {path}")

        for run in runs:
            tool = run.get("tool", {})
            driver = tool.get("driver", {})
            driver_rules = driver.get("rules", [])
            components = [driver, *tool.get("extensions", [])]
            for component in components:
                for rule in component.get("rules", []):
                    rule_id = rule.get("id")
                    if isinstance(rule_id, str):
                        definitions.add(rule_id)

            for result in run.get("results", []):
                rule_id = result.get("ruleId")
                if not isinstance(rule_id, str):
                    index = result.get("ruleIndex")
                    if isinstance(index, int) and 0 <= index < len(driver_rules):
                        rule_id = driver_rules[index].get("id")
                if isinstance(rule_id, str):
                    results[rule_id] += 1

    missing_definitions = [rule for rule in required if rule not in definitions]
    missing_results = [rule for rule in required if results[rule] == 0]
    if missing_definitions or missing_results:
        details = []
        if missing_definitions:
            details.append("missing rule definitions: " + ", ".join(missing_definitions))
        if missing_results:
            details.append("zero serialized results: " + ", ".join(missing_results))
        raise ValueError("; ".join(details))

    return {rule: results[rule] for rule in required}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--require-rule",
        action="append",
        dest="required_rules",
        required=True,
        help="SARIF rule ID that must have both a definition and at least one result; repeat as needed.",
    )
    parser.add_argument("paths", nargs="+", help="SARIF file(s) or directories to inspect.")
    args = parser.parse_args()

    try:
        counts = validate_sarif_paths(args.paths, args.required_rules)
    except ValueError as error:
        print(f"::error::{error}", file=sys.stderr)
        return 1

    for rule_id, count in counts.items():
        print(f"Validated {count} SARIF result(s) for {rule_id}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
