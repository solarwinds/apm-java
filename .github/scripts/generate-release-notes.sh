#!/usr/bin/env bash
set -euo pipefail

# --- CONFIGURATION ---
RELEASE_NOTES="/tmp/release-notes.txt"
BUILD_FILE="dependencyManagement/build.gradle.kts"

# --- VERSION ARGUMENT OR AUTO-DETECT ---
VERSION="${1:-}"
if [[ -z "$VERSION" ]]; then
  LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "v0.0.0")
  # Strip the leading "v" and bump patch version
  RAW_VERSION="${LAST_TAG#v}"
  VERSION=$(echo "$RAW_VERSION" | awk -F. -v OFS=. '{$NF++; print}')
fi

RELEASE_DATE=$(date +"%B %-d, %Y")
VERSION_WITH_V="v${VERSION}"
ARN_VERSION="${VERSION//./_}"

# --- READ OTEL VERSIONS FROM dependencyManagement/build.gradle.kts ---
OTEL_AGENT_VERSION=$(grep -E 'val[[:space:]]+otelAgentVersion[[:space:]]*=' "$BUILD_FILE" | sed -E 's/.*"([^"]+)".*/\1/')
OTEL_API_SDK_VERSION=$(grep -E 'val[[:space:]]+otelSdkVersion[[:space:]]*=' "$BUILD_FILE" | sed -E 's/.*"([^"]+)".*/\1/')

# --- HEADER ---
cat > "$RELEASE_NOTES" <<EOF
### ${RELEASE_DATE}
### Java agent: ${VERSION_WITH_V}
### OTel agent: v${OTEL_AGENT_VERSION}

## AWS Lambda layer ARN
\`\`\`
arn:aws:lambda:<region>:851060098468:layer:solarwinds-apm-java-${ARN_VERSION}:1
\`\`\`

## Upstream OpenTelemetry versions

* OpenTelemetry API/SDK \`${OTEL_API_SDK_VERSION}\`
* OpenTelemetry instrumentation \`${OTEL_AGENT_VERSION}\`
EOF

# --- FUNCTION TO FILTER COMMITS BY PREFIX ---
get_commits_by_prefix() {
  local prefix="$1"
  local last_tag
  last_tag=$(git describe --tags --abbrev=0 2>/dev/null || echo "v0.0.0")
  git log --reverse --perl-regexp --pretty=format:"%s" --author='^(?!dependabot\[bot\] )' "${last_tag}..HEAD" \
    | grep -E "^${prefix}" \
    | sed -E "s/^${prefix}[[:space:]]*//" \
    | sed -E "s/^NH-[0-9]+[[:space:]]*//" \
    | sed -E "s/^:[[:space:]]*//" || true
}

append_section_if_not_empty() {
  local title="$1"
  local content="$2"
  if [[ -n "$content" ]]; then
    cat >> "$RELEASE_NOTES" <<EOF

## ${title}

$(echo "$content" | sed 's/^/- /')
EOF
  fi
}

# --- SECTIONS ---
COMMITS_NEW=$(get_commits_by_prefix "New -")
COMMITS_BREAKING=$(get_commits_by_prefix "Breaking -")
COMMITS_FIXES=$(get_commits_by_prefix "Fix -")
COMMITS_INTERNAL=$(get_commits_by_prefix "Internal -")

append_section_if_not_empty "New features and improvements" "$COMMITS_NEW"
append_section_if_not_empty "Breaking Changes" "$COMMITS_BREAKING"
append_section_if_not_empty "Fixes" "$COMMITS_FIXES"
append_section_if_not_empty "Internal changes" "$COMMITS_INTERNAL"
