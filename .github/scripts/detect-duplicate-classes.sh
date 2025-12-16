#!/bin/bash

# Script to detect duplicate classes in a JAR file
# Usage: ./detect-duplicate-classes.sh <path-to-jar>

set -euo pipefail

if [ $# -ne 1 ]; then
    echo "Usage: $0 <path-to-jar>" >&2
    exit 1
fi

JAR_FILE="$1"

if [ ! -f "$JAR_FILE" ]; then
    echo "Error: File '$JAR_FILE' not found" >&2
    exit 1
fi

echo "Analyzing JAR: $JAR_FILE"
echo "Searching for duplicate classes..."
echo ""

jar tf "$JAR_FILE" | grep '\.class$' | sort > /tmp/classes.txt

DUPLICATES=$(awk '{
    full_path = $0
    count[full_path]++
}
END {
    found = 0
    for (path in count) {
        if (count[path] > 1) {
            if (found == 0) found = 1
            print path " (appears " count[path] " times)"
        }
    }
    if (found == 0) print ""
}' /tmp/classes.txt)

if [ -z "$DUPLICATES" ]; then
    echo "✓ No duplicate classes found"
else
    echo "✗ Duplicate classes detected:"
    echo ""
    echo "$DUPLICATES"
    exit 1
fi
