#!/usr/bin/env python3
"""Fail-closed validation for required CodeQL SARIF rules and analysis metadata."""

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


def _validate_invocations(run: dict, path: Path, run_index: int) -> None:
    invocations = run.get("invocations")
    if not isinstance(invocations, list) or not invocations:
        raise ValueError(f"SARIF run {run_index} has no invocation metadata: {path}")

    for invocation_index, invocation in enumerate(invocations):
        if not isinstance(invocation, dict):
            raise ValueError(
                f"SARIF run {run_index} has malformed invocation {invocation_index}: {path}"
            )
        if invocation.get("executionSuccessful") is not True:
            raise ValueError(
                f"SARIF run {run_index} invocation {invocation_index} is not confirmed successful: {path}"
            )

        for notification_field in ("toolExecutionNotifications", "toolConfigurationNotifications"):
            notifications = invocation.get(notification_field, [])
            if not isinstance(notifications, list):
                raise ValueError(
                    f"SARIF run {run_index} has malformed {notification_field}: {path}"
                )
            for notification in notifications:
                if not isinstance(notification, dict):
                    raise ValueError(
                        f"SARIF run {run_index} has malformed notification in {notification_field}: {path}"
                    )
                if notification.get("level") == "error":
                    message = notification.get("message", {})
                    text = message.get("text") if isinstance(message, dict) else None
                    detail = f": {text}" if text else ""
                    raise ValueError(
                        f"SARIF run {run_index} contains an error-level {notification_field}{detail}: {path}"
                    )


def parse_expected_rule_counts(values: Iterable[str]) -> dict[str, int]:
    """Parse repeatable RULE_ID=COUNT CLI arguments without accepting negatives or conflicts."""
    expected: dict[str, int] = {}
    for raw_value in values:
        rule_id, separator, raw_count = raw_value.partition("=")
        if not separator or not rule_id or not raw_count.isdecimal():
            raise ValueError(
                f"Invalid --expect-rule-count value {raw_value!r}; expected RULE_ID=NON_NEGATIVE_INTEGER"
            )
        count = int(raw_count)
        previous = expected.get(rule_id)
        if previous is not None and previous != count:
            raise ValueError(
                f"Conflicting expected counts for {rule_id}: {previous} and {count}"
            )
        expected[rule_id] = count
    return expected


def validate_sarif_paths(
    paths: Iterable[str],
    required_rules: Iterable[str],
    required_definitions: Iterable[str] = (),
    expected_result_counts: dict[str, int] | None = None,
) -> dict[str, int]:
    required = tuple(dict.fromkeys(required_rules))
    definitions_required = tuple(dict.fromkeys(required_definitions))
    expected_counts = dict(expected_result_counts or {})
    for rule_id, count in expected_counts.items():
        if not isinstance(rule_id, str) or not rule_id:
            raise ValueError("Expected result-count rule IDs must be non-empty strings")
        if isinstance(count, bool) or not isinstance(count, int) or count < 0:
            raise ValueError(f"Expected result count for {rule_id} must be a non-negative integer")
    if not required and not definitions_required and not expected_counts:
        raise ValueError(
            "At least one --require-rule, --require-rule-definition, or --expect-rule-count must be supplied"
        )

    definitions: set[str] = set()
    results: Counter[str] = Counter()
    files = _sarif_files(paths)

    for path in files:
        try:
            document = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as error:
            raise ValueError(f"Cannot read SARIF {path}: {error}") from error

        if not isinstance(document, dict) or document.get("version") != "2.1.0":
            version = document.get("version") if isinstance(document, dict) else None
            raise ValueError(f"Unexpected SARIF version in {path}: {version!r}")

        runs = document.get("runs")
        if not isinstance(runs, list) or not runs:
            raise ValueError(f"SARIF document has no non-empty runs array: {path}")

        for run_index, run in enumerate(runs):
            if not isinstance(run, dict):
                raise ValueError(f"SARIF run {run_index} is malformed: {path}")
            _validate_invocations(run, path, run_index)

            tool = run.get("tool", {})
            if not isinstance(tool, dict):
                raise ValueError(f"SARIF run {run_index} has malformed tool metadata: {path}")
            driver = tool.get("driver", {})
            if not isinstance(driver, dict):
                raise ValueError(f"SARIF run {run_index} has malformed driver metadata: {path}")
            driver_rules = driver.get("rules", [])
            extensions = tool.get("extensions", [])
            if not isinstance(driver_rules, list) or not isinstance(extensions, list):
                raise ValueError(f"SARIF run {run_index} has malformed rule metadata: {path}")

            components = [driver]
            for extension in extensions:
                if not isinstance(extension, dict):
                    raise ValueError(f"SARIF run {run_index} has malformed extension metadata: {path}")
                components.append(extension)

            for component in components:
                component_rules = component.get("rules", [])
                if not isinstance(component_rules, list):
                    raise ValueError(f"SARIF run {run_index} has malformed rule list: {path}")
                for rule in component_rules:
                    if isinstance(rule, dict):
                        rule_id = rule.get("id")
                        if isinstance(rule_id, str):
                            definitions.add(rule_id)

            run_results = run.get("results", [])
            if not isinstance(run_results, list):
                raise ValueError(f"SARIF run {run_index} has malformed results: {path}")
            for result in run_results:
                if not isinstance(result, dict):
                    raise ValueError(f"SARIF run {run_index} has malformed result: {path}")
                rule_id = result.get("ruleId")
                if not isinstance(rule_id, str):
                    index = result.get("ruleIndex")
                    if isinstance(index, int) and 0 <= index < len(driver_rules):
                        rule = driver_rules[index]
                        if isinstance(rule, dict):
                            rule_id = rule.get("id")
                if isinstance(rule_id, str):
                    results[rule_id] += 1

    all_required_definitions = tuple(
        dict.fromkeys((*required, *definitions_required, *expected_counts))
    )
    missing_definitions = [rule for rule in all_required_definitions if rule not in definitions]
    missing_results = [rule for rule in required if results[rule] == 0]
    count_mismatches = [
        f"{rule} expected exactly {expected}, got {results[rule]}"
        for rule, expected in expected_counts.items()
        if results[rule] != expected
    ]
    if missing_definitions or missing_results or count_mismatches:
        details = []
        if missing_definitions:
            details.append("missing rule definitions: " + ", ".join(missing_definitions))
        if missing_results:
            details.append("zero serialized results: " + ", ".join(missing_results))
        if count_mismatches:
            details.append("unexpected result counts: " + "; ".join(count_mismatches))
        raise ValueError("; ".join(details))

    returned_rules = tuple(dict.fromkeys((*required, *expected_counts)))
    return {rule: results[rule] for rule in returned_rules}


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--require-rule",
        action="append",
        dest="required_rules",
        default=[],
        help="SARIF rule ID that must have both a definition and at least one result; repeat as needed.",
    )
    parser.add_argument(
        "--require-rule-definition",
        action="append",
        dest="required_definitions",
        default=[],
        help="SARIF rule ID whose definition must be present; zero findings are allowed.",
    )
    parser.add_argument(
        "--expect-rule-count",
        action="append",
        dest="expected_rule_counts",
        default=[],
        metavar="RULE_ID=COUNT",
        help="Require an exact non-negative result count for a SARIF rule; repeat as needed.",
    )
    parser.add_argument("paths", nargs="+", help="SARIF file(s) or directories to inspect.")
    args = parser.parse_args()

    try:
        expected_counts = parse_expected_rule_counts(args.expected_rule_counts)
        counts = validate_sarif_paths(
            args.paths,
            args.required_rules,
            args.required_definitions,
            expected_counts,
        )
    except ValueError as error:
        print(f"::error::{error}", file=sys.stderr)
        return 1

    for rule_id, count in counts.items():
        print(f"Validated {count} SARIF result(s) for {rule_id}.")
    for rule_id in args.required_definitions:
        print(f"Validated SARIF rule definition for {rule_id}; zero findings are allowed.")
    for rule_id, expected in parse_expected_rule_counts(args.expected_rule_counts).items():
        print(f"Validated exactly {expected} SARIF result(s) for {rule_id}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
