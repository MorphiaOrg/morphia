#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Fetching MongoDB versions..."
raw=$( ${SCRIPT_DIR}/.github/BuildMatrix.java all )

# Parse the output: ['ver1', 'ver2', ...] -> array of versions
versions=$(echo "$raw" | tr -d "[]'" | tr ',' '\n' | tr -d ' ')

if [[ -z "$versions" ]]; then
    echo "ERROR: No versions returned from BuildMatrix.java" >&2
    exit 1
fi

echo "Testing against versions:"
echo "$versions" | sed 's/^/  /'
echo

pass=()
fail=()

for version in $versions; do
    echo "======================================"
    echo "Running tests with MongoDB $version"
    echo "======================================"
    if "${SCRIPT_DIR}/mvnw" test \
        -pl :morphia-core \
        -Dmongodb="$version" \
        -Ddeploy.skip=true \
        -Dinvoker.skip=true \
        -e surefire:test \
        -o \
        2>&1 | tee "core/target/morphia-test-${version}.log"; then
        pass+=("$version")
    else
        fail+=("$version")
        echo "FAILED for MongoDB $version (log: core/target/morphia-test-${version}.log)"
    fi
    echo
done

echo "======================================"
echo "Results"
echo "======================================"
echo "Passed (${#pass[@]}): ${pass[*]:-none}"
echo "Failed (${#fail[@]}): ${fail[*]:-none}"

[[ ${#fail[@]} -eq 0 ]]
