#!/usr/bin/env bash
# Updates GitHub issue percentage titles for the critter integration plan.
# Source of truth for formulas: docs/critter-integration-plan.md
#
# Usage: GH_TOKEN=<token> ./update-critter-progress.sh
#
# Derives subissues dynamically from the checkboxes in the parent issue body.
# Phase percentages: (completed_tasks_in_phase / total_tasks_in_phase) * 100
# Parent percentage: (total_completed_across_all_phases / total_across_all_phases) * 100

set -euo pipefail

REPO="MorphiaOrg/morphia"
PARENT_ISSUE=4179

total_done=0
total_tasks=0
declare -a completed_phases=()

echo "Computing critter integration progress..."

# Fetch parent body and extract subissue numbers from its checkboxes
parent_body=$(gh issue view "$PARENT_ISSUE" --repo "$REPO" --json body -q '.body')
subissues=$(printf '%s' "$parent_body" | grep '^\- \[' | grep -o '#[0-9]*' | tr -d '#')

for issue in $subissues; do
    body=$(gh issue view "$issue" --repo "$REPO" --json body -q '.body')
    done_count=$(printf '%s' "$body" | grep -ci '^\- \[x\]' || true)
    tasks=$(printf '%s' "$body" | grep -ci '^\- \[' || true)

    total_done=$((total_done + done_count))
    total_tasks=$((total_tasks + tasks))

    pct=$(awk "BEGIN { printf \"%.0f\", ($done_count * 100) / ($tasks > 0 ? $tasks : 1) }")
    current_title=$(gh issue view "$issue" --repo "$REPO" --json title -q '.title')
    bare_title=$(printf '%s' "$current_title" | sed 's/^\[[0-9]*%\] //')
    new_title="[$pct%] $bare_title"

    if [ "$current_title" != "$new_title" ]; then
        gh issue edit "$issue" --repo "$REPO" --title "$new_title"
        echo "  #$issue: $current_title → $new_title"
    else
        echo "  #$issue: unchanged ($current_title)"
    fi

    if [ "$done_count" -eq "$tasks" ] && [ "$tasks" -gt 0 ]; then
        completed_phases+=("$issue")
        state=$(gh issue view "$issue" --repo "$REPO" --json state -q '.state')
        if [ "$state" = "OPEN" ]; then
            gh issue close "$issue" --repo "$REPO"
            echo "  #$issue: closed (all $tasks tasks complete)"
        fi
    fi
done

# Update parent title
parent_pct=$(awk "BEGIN { printf \"%.0f\", ($total_done * 100) / ($total_tasks > 0 ? $total_tasks : 1) }")
current_parent_title=$(gh issue view "$PARENT_ISSUE" --repo "$REPO" --json title -q '.title')
bare_parent_title=$(printf '%s' "$current_parent_title" | sed 's/^\[[0-9]*%\] //')
new_parent_title="[$parent_pct%] $bare_parent_title"

if [ "$current_parent_title" != "$new_parent_title" ]; then
    gh issue edit "$PARENT_ISSUE" --repo "$REPO" --title "$new_parent_title"
    echo "  #$PARENT_ISSUE: $current_parent_title → $new_parent_title"
else
    echo "  #$PARENT_ISSUE: unchanged ($current_parent_title)"
fi

# Check completed-phase boxes in parent body
if [ ${#completed_phases[@]} -gt 0 ]; then
    updated_body="$parent_body"
    changed=false
    for issue in "${completed_phases[@]}"; do
        if printf '%s' "$updated_body" | grep -q "^- \[ \] #$issue"; then
            updated_body=$(printf '%s' "$updated_body" | sed "s/^- \[ \] #$issue/- [x] #$issue/")
            changed=true
            echo "  #$PARENT_ISSUE: checked box for #$issue"
        fi
    done
    if [ "$changed" = "true" ]; then
        gh issue edit "$PARENT_ISSUE" --repo "$REPO" --body "$updated_body"
    fi
fi

echo "Done: $total_done/$total_tasks tasks complete ($parent_pct%)"
