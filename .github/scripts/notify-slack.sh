#!/usr/bin/env bash
set -euo pipefail

# Sends a release notification to Slack.
#
# Message format:
#   <Date> - APM Java <version>
#   <github release url>
#   Jira: <jira ticket url>   (only when jira-url is provided)
#
# Required environment variables:
#   SLACK_TOKEN    - Slack bot token used for authorization
#   SLACK_CHANNEL  - Channel id (or name) to post the message to
#
# Usage:
#   notify-slack.sh <version> <release-url> [jira-url]

VERSION="${1:?version is required}"
RELEASE_URL="${2:?release url is required}"
JIRA_URL="${3:-}"

: "${SLACK_TOKEN:?SLACK_TOKEN is required}"
: "${SLACK_CHANNEL:?SLACK_CHANNEL is required}"

RELEASE_DATE=$(date +"%B %-d, %Y")

post_to_slack() {
  local post_body="$1"
  local response

  if ! response=$(curl -sfS https://slack.com/api/chat.postMessage \
    -H "Authorization: Bearer $SLACK_TOKEN" \
    -H 'Content-type: application/json; charset=utf-8' \
    -d "$post_body"); then
    echo "❌ Slack API request failed" >&2
    exit 1
  fi

  if [[ "$(echo "$response" | jq -r '.ok')" != "true" ]]; then
    echo "❌ Slack API error: $(echo "$response" | jq -r '.error // "unknown_error"')" >&2
    exit 1
  fi

  echo "$response"
}

# Build the message text: "<Date> - APM Java <version>" then the release url on a new line.
MESSAGE_TEXT="*${RELEASE_DATE} - APM Java ${VERSION}*"$'\n'"${RELEASE_URL}"
if [[ -n "$JIRA_URL" ]]; then
  MESSAGE_TEXT+=$'\n'"*Jira:* ${JIRA_URL}"
fi

# Use jq to safely construct the JSON payload (handles escaping of newlines/quotes).
POST_BODY=$(jq -n \
  --arg channel "$SLACK_CHANNEL" \
  --arg text "$MESSAGE_TEXT" \
  '{channel: $channel, text: $text}')

post_to_slack "$POST_BODY" > /dev/null
echo "✅ Posted release notification to Slack channel"
