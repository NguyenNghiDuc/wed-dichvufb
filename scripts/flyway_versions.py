#!/usr/bin/env python3
"""Detect duplicate Flyway versions."""
import re
from pathlib import Path

MIGRATION_PATH = Path('src/main/resources/db/migration')
PATTERN = re.compile(r'^V([0-9]+)__.{1,200}\.sql$')


def migration_files():
    return sorted(MIGRATION_PATH.glob('V*.sql'))
