#!/usr/bin/env bash
# Updates GitHub issue percentage titles for the critter integration plan.
# Source of truth for formulas: docs/critter-integration-plan.md
#
# Usage: GH_TOKEN=<token> ./update-critter-progress.sh
#
# Phase percentages: (completed_tasks_in_phase / total_tasks_in_phase) * 100
# Parent percentage: (total_completed_tasks_across_all_phases / 53) * 100

set -euo pipefail

REPO="MorphiaOrg/morphia"
PARENT_ISSUE=4179
TOTAL_TASKS=53

declare -a PHASE_ISSUES=(4184 4185 4186 4187 4188 4189 4190)
declare -a PHASE_NAMES=(
    "Phase 1: Extract Mapper interface and add config option"
    "Phase 2: VarHandle accessor generator"
    "Phase 3: Move critter-core into morphia-core"
    "Phase 4: CritterMapper implementation"
    "Phase 5: Wire CritterMapper into MorphiaDatastore"
    "Phase 6: Test infrastructure and CI matrix"
    "Phase 7: Cleanup and documentation"
)
declare -a PHASE_TASKS=(9 6 8 9 5 7 9)

total_done=0
declare -a completed_phases=()

echo "Computing critter integration progress..."

for i in "${!PHASE_ISSUES[@]}"; do
    issue="${PHASE_ISSUES[$i]}"
    name="${PHASE_NAMES[$i]}"
    tasks="${PHASE_TASKS[$i]}"

    body=$(gh issue view "$issue" --repo "$REPO" --json body -q '.body')
    done_count=$(printf '%s' "$body" | grep -c '^\- \[x\]' || true)
    total_done=$((total_done + done_count))

    pct=$(awk "BEGIN { printf \"%.0f\", ($done_count * 100) / $tasks }")
    new_title="[$pct%] $name"
    current_title=$(gh issue view "$issue" --repo "$REPO" --json title -q '.title')

    if [ "$current_title" != "$new_title" ]; then
        gh issue edit "$issue" --repo "$REPO" --title "$new_title"
        echo "  #$issue: $current_title → $new_title"
    else
        echo "  #$issue: unchanged ($current_title)"
    fi

    if [ "$done_count" -eq "$tasks" ]; then
        completed_phases+=("$issue")
        state=$(gh issue view "$issue" --repo "$REPO" --json state -q '.state')
        if [ "$state" = "OPEN" ]; then
            gh issue close "$issue" --repo "$REPO"
            echo "  #$issue: closed (all $tasks tasks complete)"
        fi
    fi
done

# Update parent title
parent_pct=$(awk "BEGIN { printf \"%.0f\", ($total_done * 100) / $TOTAL_TASKS }")
new_parent_title="[$parent_pct%] Integrate critter-core into morphia-core"
current_parent_title=$(gh issue view "$PARENT_ISSUE" --repo "$REPO" --json title -q '.title')

if [ "$current_parent_title" != "$new_parent_title" ]; then
    gh issue edit "$PARENT_ISSUE" --repo "$REPO" --title "$new_parent_title"
    echo "  #$PARENT_ISSUE: $current_parent_title → $new_parent_title"
else
    echo "  #$PARENT_ISSUE: unchanged ($current_parent_title)"
fi

# Check completed-phase boxes in parent body
if [ ${#completed_phases[@]} -gt 0 ]; then
    parent_body=$(gh issue view "$PARENT_ISSUE" --repo "$REPO" --json body -q '.body')
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

echo "Done: $total_done/$TOTAL_TASKS tasks complete ($parent_pct%)"
