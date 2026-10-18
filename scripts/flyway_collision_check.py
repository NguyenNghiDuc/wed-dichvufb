#!/usr/bin/env python3
"""Reject duplicate Flyway migration versions, including numeric aliases."""
from collections import defaultdict
from pathlib import Path
import re
import sys

MIGRATIONS = Path("src/main/resources/db/migration")
PATTERN = re.compile(r"^V([0-9]+(?:[._][0-9]+)*)__.+[.]sql$")


def version(name):
    match = PATTERN.fullmatch(name)
    if match is None:
        return None
    return tuple(int(x) for x in re.split(r"[._]", match.group(1)))


def collisions(paths):
    versions = defaultdict(list)
    for path in paths:
        number = version(Path(path).name)
        if number is not None:
            versions[number].append(str(path))
    return {key: sorted(set(value)) for key, value in versions.items()
            if len(set(value)) > 1}


def main():
    if not MIGRATIONS.is_dir():
        print("Missing Flyway migration directory", file=sys.stderr)
        return 2
    paths = list(MIGRATIONS.glob("V*.sql"))
    duplicates = collisions(paths)
    for number, filenames in sorted(duplicates.items()):
        print("Duplicate Flyway V" + ".".join(map(str, number)) +
              ": " + ", ".join(filenames), file=sys.stderr)
    if duplicates:
        return 1
    print("Flyway version check passed: " + str(len(paths)) + " migrations")
    return 0


if __name__ == "__main__":
    sys.exit(main())
