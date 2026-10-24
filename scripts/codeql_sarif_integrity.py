#!/usr/bin/env python3
"""Conservative CodeQL log/SARIF integrity precheck for GrowLab issue #13.

This is NOT a vulnerability scan, nor a substitute for Code Scanning alert review.
W9 must wire this into CI only after reviewing its artifacts and workflow.
"""
from __future__ import annotations

import argparse
import json
from pathlib import Path
import re
import sys
import tempfile
import unittest

MAX_LOG_BYTES = 20_000_000
MAX_SARIF_BYTES = 30_000_000
CSRF_QUERY = re.compile(r'Interpreted pathproblem query[^\n]*\(java/csrf-unprotected-request-type\)', re.I)
PATH_ERROR = re.compile(r'\bERROR:\s+While processing path-problem rule\b', re.I)
GRAPH_ERROR = re.compile(r'got start node that does not appear in graph', re.I)
SARIF_EXPORTED = re.compile(r'Exported results to SARIF\s*\(', re.I)


def read_bounded(path: Path, limit: int, kind: str) -> tuple[str | None, list[str]]:
    try:
        if path.stat().st_size > limit:
            return None, [f'{kind} exceeds inspection size limit']
        with path.open('rb') as stream:
            raw = stream.read(limit + 1)
        if len(raw) > limit:
            return None, [f'{kind} exceeds inspection size limit']
        return raw.decode('utf-8-sig'), []
    except (OSError, UnicodeError):
        return None, [f'{kind} cannot be read as UTF-8']


def inspect_log(log: str) -> list[str]:
    errors: list[str] = []
    if not CSRF_QUERY.search(log):
        errors.append('CSRF path-query interpretation is not confirmed')
    graph_lines = [line for line in log.splitlines()
                   if PATH_ERROR.search(line) or GRAPH_ERROR.search(line)]
    if graph_lines:
        errors.append(f'{len(graph_lines)} SARIF path-graph export error(s)')
    if not SARIF_EXPORTED.search(log):
        errors.append('SARIF export completion is not confirmed')
    return errors


def inspect_sarif(content: str) -> list[str]:
    try:
        doc = json.loads(content)
    except (ValueError, TypeError):
        return ['SARIF is not valid JSON']
    if not isinstance(doc, dict) or doc.get('version') != '2.1.0':
        return ['SARIF version is missing or unsupported']
    runs = doc.get('runs')
    if not isinstance(runs, list) or not runs:
        return ['SARIF contains no analysis runs']
    errors: list[str] = []
    for index, run in enumerate(runs):
        if not isinstance(run, dict):
            errors.append(f'SARIF run {index} is invalid')
            continue
        tool = run.get('tool')
        driver = tool.get('driver') if isinstance(tool, dict) else None
        name = driver.get('name') if isinstance(driver, dict) else None
        if not isinstance(name, str) or 'codeql' not in name.lower():
            errors.append(f'SARIF run {index} does not identify CodeQL')
        if not isinstance(run.get('results'), list):
            errors.append(f'SARIF run {index} has no results array')
        # Action success alone is not evidence that SARIF contains a completed run.
        invocations = run.get('invocations')
        if not isinstance(invocations, list) or not invocations:
            errors.append(f'SARIF run {index} has no valid invocation evidence')
            continue
        for invocation in invocations:
            if not isinstance(invocation, dict):
                errors.append(f'SARIF run {index} invocation is invalid')
                continue
            if invocation.get('executionSuccessful') is not True:
                errors.append(f'SARIF run {index} lacks successful execution evidence')
            for field in ('toolExecutionNotifications', 'toolConfigurationNotifications'):
                notes = invocation.get(field, [])
                if not isinstance(notes, list):
                    errors.append(f'SARIF run {index} {field} are invalid')
                    continue
                for note in notes:
                    if not isinstance(note, dict):
                        errors.append(f'SARIF run {index} {field} entry is invalid')
                    elif note.get('level') == 'error':
                        errors.append(f'SARIF run {index} contains {field} errors')
                        break
    return errors


def inspect_files(log_path: Path, sarif_path: Path | None) -> list[str]:
    errors: list[str] = []
    log, problems = read_bounded(log_path, MAX_LOG_BYTES, 'CodeQL log')
    errors.extend(problems)
    if log is not None:
        errors.extend(inspect_log(log))
    if sarif_path is None:
        errors.append('SARIF artifact is required for integrity checking')
    else:
        sarif, problems = read_bounded(sarif_path, MAX_SARIF_BYTES, 'SARIF')
        errors.extend(problems)
        if sarif is not None:
            errors.extend(inspect_sarif(sarif))
    return errors


# Embedded unit tests permit one reviewed, atomic commit without changing W9 workflows.
class IntegrityTests(unittest.TestCase):
    LOG = ('Interpreted pathproblem query "HTTP request type unprotected from CSRF" '
           '(java/csrf-unprotected-request-type) at path /tmp/results.bqrs.\n'
           'Exported results to SARIF (274ms).\n')
    SARIF = json.dumps({'version': '2.1.0', 'runs': [
        {'tool': {'driver': {'name': 'CodeQL'}}, 'results': [],
         'invocations': [{'executionSuccessful': True}]}]})

    def test_clean_pair(self):
        self.assertEqual(inspect_log(self.LOG), [])
        self.assertEqual(inspect_sarif(self.SARIF), [])

    def test_actual_three_errors_even_when_export_succeeded(self):
        errors = ''.join('ERROR: While processing path-problem rule '
                         'java/csrf-unprotected-request-type, got start node that '
                         f'does not appear in graph: {node}.\n'
                         for node in ('edit', 'registerPage', 'loginPage'))
        self.assertIn('3 SARIF path-graph export error(s)', inspect_log(self.LOG + errors))

    def test_missing_query(self):
        self.assertTrue(inspect_log('Exported results to SARIF (100ms).'))

    def test_missing_export(self):
        self.assertTrue(inspect_log(self.LOG.split('Exported results')[0]))

    def test_unrelated_error_does_not_create_false_positive(self):
        self.assertEqual(inspect_log(self.LOG + 'ERROR: Maven dependency warning\n'), [])

    def test_invalid_json(self):
        self.assertIn('SARIF is not valid JSON', inspect_sarif('{'))

    def test_missing_runs(self):
        self.assertTrue(inspect_sarif('{"version":"2.1.0","runs":[]}'))

    def test_wrong_tool(self):
        self.assertTrue(inspect_sarif('{"version":"2.1.0","runs":[{"tool":{"driver":{"name":"other"}},"results":[]}]}'))

    def test_failed_execution(self):
        doc = json.loads(self.SARIF)
        doc['runs'][0]['invocations'][0]['executionSuccessful'] = False
        self.assertTrue(inspect_sarif(json.dumps(doc)))

    def test_tool_notification_error(self):
        doc = json.loads(self.SARIF)
        doc['runs'][0]['invocations'][0]['toolExecutionNotifications'] = [{'level': 'error'}]
        self.assertTrue(inspect_sarif(json.dumps(doc)))

    def test_missing_or_empty_invocations_fail_closed(self):
        doc = json.loads(self.SARIF)
        del doc['runs'][0]['invocations']
        self.assertTrue(inspect_sarif(json.dumps(doc)))
        doc['runs'][0]['invocations'] = []
        self.assertTrue(inspect_sarif(json.dumps(doc)))

    def test_missing_success_marker_fails_closed(self):
        doc = json.loads(self.SARIF)
        del doc['runs'][0]['invocations'][0]['executionSuccessful']
        self.assertTrue(inspect_sarif(json.dumps(doc)))

    def test_malformed_tool_notifications_fail_closed(self):
        doc = json.loads(self.SARIF)
        doc['runs'][0]['invocations'][0]['toolExecutionNotifications'] = 'error'
        self.assertTrue(inspect_sarif(json.dumps(doc)))

    def test_configuration_notification_errors_fail_closed(self):
        doc = json.loads(self.SARIF)
        doc['runs'][0]['invocations'][0]['toolConfigurationNotifications'] = [{'level': 'error'}]
        self.assertTrue(inspect_sarif(json.dumps(doc)))

    def test_invalid_notification_entry_fails_closed(self):
        doc = json.loads(self.SARIF)
        doc['runs'][0]['invocations'][0]['toolExecutionNotifications'] = [42]
        self.assertTrue(inspect_sarif(json.dumps(doc)))

    def test_no_artifact_fails_closed(self):
        with tempfile.TemporaryDirectory() as tmp:
            log = Path(tmp) / 'log.txt'
            log.write_text(self.LOG, encoding='utf-8')
            self.assertTrue(inspect_files(log, None))

    def test_bounded_log(self):
        with tempfile.TemporaryDirectory() as tmp:
            log = Path(tmp) / 'log.txt'
            with log.open('wb') as out:
                out.truncate(MAX_LOG_BYTES + 1)
            self.assertTrue(read_bounded(log, MAX_LOG_BYTES, 'CodeQL log')[1])

    def test_invalid_utf8(self):
        with tempfile.TemporaryDirectory() as tmp:
            log = Path(tmp) / 'log.txt'
            log.write_bytes(b'\xff\xfe')
            self.assertTrue(read_bounded(log, MAX_LOG_BYTES, 'CodeQL log')[1])

    def test_end_to_end_valid_pair(self):
        with tempfile.TemporaryDirectory() as tmp:
            log, sarif = Path(tmp) / 'log.txt', Path(tmp) / 'out.sarif'
            log.write_text(self.LOG, encoding='utf-8')
            sarif.write_text(self.SARIF, encoding='utf-8')
            self.assertEqual(inspect_files(log, sarif), [])


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--self-test', action='store_true', help='run embedded unit tests')
    parser.add_argument('--log', type=Path, help='decoded CodeQL job log')
    parser.add_argument('--sarif', type=Path, help='actual SARIF artifact')
    args = parser.parse_args(argv)
    if args.self_test:
        suite = unittest.defaultTestLoader.loadTestsFromTestCase(IntegrityTests)
        return 0 if unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful() else 1
    if args.log is None:
        parser.error('--log is required (or use --self-test)')
    errors = inspect_files(args.log, args.sarif)
    for error in errors:
        print('::error::CodeQL integrity: ' + error)
    if errors:
        return 1
    print('Log and SARIF structure checks passed; CSRF coverage and security approval still require independent review.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
