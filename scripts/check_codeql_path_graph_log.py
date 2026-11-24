#!/usr/bin/env python3
"""Reject CodeQL SARIF interpretation logs containing CSRF path-graph export errors."""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

_ERROR = re.compile(
    r"ERROR:\s+While processing path-problem rule\s+(?P<rule>[^,\s]+),\s*(?P<detail>.*)"
)


def find_path_graph_errors(log_text: str, rule_id: str) -> list[tuple[int, str]]:
    errors: list[tuple[int, str]] = []
    for line_number, line in enumerate(log_text.splitlines(), start=1):
        match = _ERROR.search(line)
        if match and match.group("rule") == rule_id:
            errors.append((line_number, line.strip()))
    return errors


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--rule", required=True, help="Path-problem rule ID to inspect.")
    parser.add_argument("log_file", type=Path, help="Captured CodeQL interpret-results output.")
    args = parser.parse_args()

    try:
        log_text = args.log_file.read_text(encoding="utf-8", errors="replace")
    except OSError as error:
        print(f"::error::Cannot read CodeQL log {args.log_file}: {error}", file=sys.stderr)
        return 1

    errors = find_path_graph_errors(log_text, args.rule)
    if errors:
        for line_number, line in errors:
            print(f"::error file={args.log_file},line={line_number}::{line}", file=sys.stderr)
        print(f"Found {len(errors)} path-graph export error(s) for {args.rule}.", file=sys.stderr)
        return 1

    print(f"No path-graph export errors found for {args.rule}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
