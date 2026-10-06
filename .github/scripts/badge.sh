#!/usr/bin/env bash
set -euo pipefail

releases_url="https://api.github.com/repos/Darkstar085/Sonora/releases?per_page=100"

auth_header=()
if [[ -n "${GITHUB_TOKEN:-}" ]]; then
  auth_header=(-H "Authorization: Bearer $GITHUB_TOKEN")
fi

curl --fail --silent --show-error \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "${auth_header[@]}" \
  "$releases_url" \
  -o /tmp/sonora-releases.json

total=$(
  jq '[.[].assets[]? | select(.name | endswith(".apk")) | .download_count] | add // 0' \
    /tmp/sonora-releases.json
)

mkdir -p .github/badges

jq -n --arg total "$total" '{
  schemaVersion: 1,
  label: "APK Downloads",
  message: $total,
  color: "00ACC1"
}' > .github/badges/downloads.json
