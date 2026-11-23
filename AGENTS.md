# Morphia Agent Guide

Instructions for every coding agent working in this repo.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

graphify is optional: graphify-out/ is gitignored and only exists where a contributor has installed
[graphify](https://github.com/Graphify-Labs/graphify) and built the graph. If the `graphify` CLI or graphify-out/graph.json
is missing, skip this section and navigate the code with the usual tools.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:
- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.

## Code quality
- After making changes, run `./mvnw spotless:apply` to ensure code formatting is consistent.

## Build Commands
- Build with deps: `./mvnw install -pl :morphia-core -am -DskipTests`
- Skip dokka: `-Ddeploy.skip=true`
- Skip invoker tests: `-Dinvoker.skip=true`
- Typical build: `./mvnw install -pl :morphia-core -DskipTests -Ddeploy.skip=true`
- Run specific test: `./mvnw test -pl :morphia-core -Dtest="ClassName#method" -Ddeploy.skip=true`
- Run with critter mapper: `-Dmorphia.mapper=critter` (e.g., `./mvnw test -pl :morphia-core -Dmorphia.mapper=critter -Ddeploy.skip=true`)
- Don't use `-am` with `-Dtest=` — it applies the test filter to all modules causing failures
- When running tests to verify a fix, be sure to run it once with mapper=reflection and once with mapper=critter to
  ensure both work correctly.
  - run the critter mapper first as it's more likely to have issues and you want to catch those early

## Critter Code Generator
- The `critter-maven` plugin generates models at build time (AOT); runtime generation with the Class-File API
  (`java.lang.classfile` via the `jdk-classfile-backport` library, package `io.smallrye.classfile`) is the fallback
- Build order for critter-maven: `morphia-core` → `critter-maven`
- `morphia.mapper` config: `reflection` (default) or `critter` — selects `ReflectiveMapper` vs `CritterMapper`
- Critter exists to reduce/eliminate reflection as much as possible — including mapping-time discovery, not only
  property get/set — and may become the default mapper. Fix critter/reflection divergence inside the generators
  (shared rules + parity tests); never fold critter back onto reflective discovery. Prefer designs that move work to
  generated code.

## Code Reviews
- When reviewing code, summarize all concerns and recommended changes in a file so they don't get lost.

## Documentation
- When implementing a new feature or fixing a bug, consult the docs in `~/dev/morphia.dev/morphia-docs` (repo
  `MorphiaOrg/morphia-docs`) for any updates the change needs there. Doc changes go there, not in this repo's `docs/`.
  - Until the 3.0 release, doc edits go on the `docs-redesign` branch; 3.0 content lives under `content/morphia/3.0/`
- When filing PRs, if a docs PR is needed, make sure the code PR and the docs PR each reference the other so the docs
  stay in sync

## Operator audits
- `design/operator-audits-design.md` describes how the operator audits (`audits/`, core's operator case runner and
  the discovery workflow) work. A change to how they work updates it in the same PR, alongside the README, the
  component reference and the progress file as they apply.

## Git Operations
- Never commit or push code without an explicit request from the user.
- Once a task's PR has merged, clean up: remove its worktree, delete the local branch, and delete the remote branch if
  it wasn't auto-deleted. Check first that the worktree has no uncommitted or unpushed work worth keeping.

## Pull Requests
- Prefer small PRs: when a task bundles several independent changes, give each its own PR.
- Before pushing, run local multi-agent review rounds in the worktree until clean (see the global review-round
  instructions).
- After opening a PR, request a GitHub Copilot review, evaluate each comment, and fix it or reply with reasoning. Max 3
  review cycles, or stop earlier when Copilot finds nothing new.
- PR bodies describe each problem being solved (what was wrong, with a concrete case), not just a terse restatement of
  the task. Flag significant implementation decisions in the PR description for the user's review.
- For a series of PRs, don't start the next one until the user has reviewed and approved the current one.
