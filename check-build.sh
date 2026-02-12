#!/bin/bash
# Check GitHub Actions build status
# Uses gh CLI for authentication

# Get token from gh CLI
if ! TOKEN=$(gh auth token 2>/dev/null); then
  echo "Error: GitHub CLI not authenticated. Run 'gh auth login' first."
  exit 1
fi

wget -qO- --header="Authorization: token $TOKEN" \
  "https://api.github.com/repos/huuugs/readwise-android/actions/runs?per_page=5" 2>/dev/null | \
python3 -c "
import json, sys
data = json.load(sys.stdin)
print('=== GitHub Actions Build Status ===')
print()
for run in data['workflow_runs'][:5]:
    status_emoji = '✅' if run['conclusion'] == 'success' else '❌' if run['conclusion'] == 'failure' else '⏳'
    print(f\"{status_emoji} Run #{run['run_number']}: {run['display_title'][:40]}\")
    print(f\"   Status: {run['status']} | {run['conclusion']}\")
    print(f\"   URL: {run['html_url']}\")
    print()
"
