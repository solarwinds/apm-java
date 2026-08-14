#!/usr/bin/env bash
set -euo pipefail

# Creates a JIRA release ticket.
#
# Summary:     "Java: Release Notes, <version>"
# Description: the provided message text (matches the Slack release notification)
#
# Required environment variables:
#   JIRA_BASE_URL      - Base URL of the JIRA instance (e.g. https://company.atlassian.net)
#   JIRA_USER_EMAIL    - Email used for basic auth
#   JIRA_API_TOKEN     - JIRA API token
#   JIRA_PROJECT_KEY   - JIRA project key (e.g. NH)
#   JIRA_ASSIGNEE_ID   - Account ID of the assignee
#
# Usage:
#   create-jira-ticket.sh <version> <description>
#
# Outputs:
#   The JIRA ticket browse URL on stdout

VERSION="${1:?version is required}"
DESCRIPTION="${2:?description is required}"

: "${JIRA_BASE_URL:?JIRA_BASE_URL is required}"
: "${JIRA_USER_EMAIL:?JIRA_USER_EMAIL is required}"
: "${JIRA_API_TOKEN:?JIRA_API_TOKEN is required}"
: "${JIRA_PROJECT_KEY:?JIRA_PROJECT_KEY is required}"
: "${JIRA_ASSIGNEE_ID:?JIRA_ASSIGNEE_ID is required}"

SUMMARY="Java: Release Notes, v${VERSION}"

DESCRIPTION_ADF=$(jq -n --arg text "$DESCRIPTION" '{
  version: 1,
  type: "doc",
  content: [{
    type: "paragraph",
    content: [{ type: "text", text: $text }]
  }]
}')

REQUEST_BODY=$(jq -n \
  --arg project    "$JIRA_PROJECT_KEY" \
  --arg summary    "$SUMMARY" \
  --arg assignee   "$JIRA_ASSIGNEE_ID" \
  --argjson desc   "$DESCRIPTION_ADF" \
  '{fields: {
    project:     {key: $project},
    summary:     $summary,
    issuetype:   {name: "Story"},
    description: $desc,
    components:  [{name: "Instrumentation"}, {name: "Instrument Java"}],
    labels:      ["Documentation", "Documentation-APM", "Documentation_changelog-APMJava"],
    assignee:    {accountId: $assignee}
  }}')

RESPONSE=$(curl -sS \
  -X POST \
  -H "Content-Type: application/json" \
  -u "${JIRA_USER_EMAIL}:${JIRA_API_TOKEN}" \
  "${JIRA_BASE_URL}/rest/api/3/issue" \
  -d "$REQUEST_BODY")

ISSUE_KEY=$(echo "$RESPONSE" | jq -r '.key // empty')
if [[ -z "$ISSUE_KEY" ]]; then
  echo "❌ Failed to create JIRA ticket:" >&2
  echo "$RESPONSE" | jq '.' >&2
  exit 1
fi

echo "${JIRA_BASE_URL}/browse/${ISSUE_KEY}"
