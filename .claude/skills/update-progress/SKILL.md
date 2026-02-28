---
name: update-progress
description: >
  Update GitHub issue percentage titles for the critter integration plan (#4179).
  Use after checking off any task in a phase issue, or after merging a phase PR.
  Recomputes phase % and parent % from live checkbox state and updates all titles.
allowed-tools: Bash(gh:*), Bash(awk:*), Bash(grep:*), Bash(sed:*)
---

# Update Critter Integration Progress

## When to use

After completing any task in issues #4184–#4190 — check the box in the issue first,
then run this skill to recompute and update all percentages.

The GitHub workflow (`.github/workflows/update-critter-progress.yml`) handles this
automatically on PR merges and issue edits. Use this skill for manual updates or
to verify the current state.

## Formulas (source of truth: docs/critter-integration-plan.md)

```
phase_percentage   = (completed_tasks_in_phase / total_tasks_in_phase) * 100
parent_percentage  = (total_completed_tasks_across_all_phases / 53) * 100
```

Both are rounded to the nearest integer.

## Phase reference

| Issue | Phase | Tasks |
|-------|-------|-------|
| #4184 | Phase 1: Extract Mapper interface and add config option | 9 |
| #4185 | Phase 2: VarHandle accessor generator | 6 |
| #4186 | Phase 3: Move critter-core into morphia-core | 8 |
| #4187 | Phase 4: CritterMapper implementation | 9 |
| #4188 | Phase 5: Wire CritterMapper into MorphiaDatastore | 5 |
| #4189 | Phase 6: Test infrastructure and CI matrix | 7 |
| #4190 | Phase 7: Cleanup and documentation | 9 |
| **Total** | | **53** |

## Steps

Run the shared script:

```bash
bash .github/scripts/update-critter-progress.sh
```

The script:
1. Fetches each phase issue body and counts `- [x]` entries
2. Updates each phase issue title to `[XX%] Phase N: ...`
3. Closes phase issues that reach 100% and checks their box in #4179
4. Updates `#4179` title to `[XX%] Integrate critter-core into morphia-core`

## Manual commands (if running ad hoc)

```bash
# Count checked tasks in a phase issue
gh issue view 4184 --repo MorphiaOrg/morphia --json body -q '.body' \
  | grep -c '^\- \[x\]'

# Update a phase issue title
gh issue edit 4184 --repo MorphiaOrg/morphia --title "[33%] Phase 1: Extract Mapper interface and add config option"

# Update parent
gh issue edit 4179 --repo MorphiaOrg/morphia --title "[6%] Integrate critter-core into morphia-core"
```
