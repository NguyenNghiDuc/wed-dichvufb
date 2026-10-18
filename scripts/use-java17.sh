#!/usr/bin/env bash
# Source this file: changing a child shell cannot change the caller's JAVA_HOME.
if [[ "${BASH_SOURCE[0]}" == "$0" ]]; then
    printf '%s\n' 'Run: source scripts/use-java17.sh [JDK_17_PATH]' >&2
    exit 2
fi

_growlab_select_java17() {
    local candidate properties
    local -a candidates=(
        "${1:-}" "${JAVA_HOME:-}"
        "${SDKMAN_CANDIDATES_DIR:-/usr/local/sdkman/candidates}"/java/*
        /usr/local/sdkman/candidates/java/*
        "${HOME}/.sdkman/candidates/java"/*
        /usr/lib/jvm/* /opt/java/openjdk /usr/local/java17
    )
    for candidate in "${candidates[@]}"; do
        [[ -n "$candidate" && -x "$candidate/bin/java" && -x "$candidate/bin/javac" ]] || continue
        properties=$("$candidate/bin/java" -XshowSettings:properties -version 2>&1) || continue
        if [[ "$properties" =~ java\.specification\.version[[:space:]]*=[[:space:]]*17([[:space:]]|$) ]]; then
            export JAVA_HOME="$candidate"
            export PATH="$JAVA_HOME/bin:$PATH"
            hash -r
            printf 'JAVA_HOME=%s\n' "$JAVA_HOME"
            return 0
        fi
    done
    printf '%s\n' \
        'No JDK 17 found. JAVA_HOME and PATH were not changed.' \
        'In Codespaces: pull the latest main, then run Codespaces: Rebuild Container.' \
        'Or install a Java 17 JDK using SDKMAN (sdk list java), then source this script again.' >&2
    return 1
}

if _growlab_select_java17 "$@"; then
    unset -f _growlab_select_java17
    return 0
else
    unset -f _growlab_select_java17
    return 1
fi
