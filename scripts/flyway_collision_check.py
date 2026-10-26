#!/usr/bin/env python3
"""Reject Flyway migration collisions, including semantic version aliases.

Flyway treats trailing zero components as equal (V10 = V10_0 = V10.0).
This checks the merged checkout; parallel PRs still require coordination.
"""
from collections import defaultdict
from pathlib import Path
import argparse
import re
import sys

MIGRATIONS = Path(__file__).resolve().parents[1] / "src/main/resources/db/migration"
VERSIONED = re.compile(r"^V(?P<version>[0-9]+(?:[._][0-9]+)*)__[^/]+[.]sql$")


def version(name):
    matched = VERSIONED.fullmatch(name)
    if matched is None:
        return None
    parts = [int(part) for part in re.split(r"[._]", matched.group("version"))]
    while len(parts) > 1 and parts[-1] == 0:
        parts.pop()
    return tuple(parts)


def collisions(paths):
    versions = defaultdict(set)
    for path in paths:
        key = version(Path(path).name)
        if key is not None:
            versions[key].add(str(path))
    return {key: sorted(paths) for key, paths in versions.items() if len(paths) > 1}


def check(directory):
    if not directory.is_dir():
        return [f"Missing Flyway migration directory: {directory}"], 0
    candidates = sorted(path for path in directory.iterdir()
                        if path.is_file() and path.suffix.lower() == ".sql")
    versioned = [path for path in candidates if path.name.startswith("V")]
    errors = []
    if not versioned:
        errors.append(f"No versioned Flyway migrations found in {directory}")
    for path in versioned:
        if version(path.name) is None:
            errors.append(f"Invalid versioned Flyway filename: {path.name}")
    for number, paths in sorted(collisions(versioned).items()):
        errors.append(f"Duplicate Flyway V{'.'.join(map(str, number))}: " + ", ".join(paths))
    return errors, len(versioned)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--migration-dir", type=Path, default=MIGRATIONS)
    args = parser.parse_args(argv)
    errors, count = check(args.migration_dir)
    if errors:
        for error in errors:
            print(error, file=sys.stderr)
        return 1
    print(f"Flyway version check passed: {count} versioned migrations")
    return 0


if __name__ == "__main__":
    sys.exit(main())
