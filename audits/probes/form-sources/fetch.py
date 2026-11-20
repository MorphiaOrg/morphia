#!/usr/bin/env python3
"""Acquire the historical documentation pages behind locked MORPHIA_FORM fixtures.

This is the only networked step of the form migration. It never edits the repository.

  fetch.py candidates <repo-root> <first|last> <out.json>
      For every locked example whose only repair category is MORPHIA_FORM (per
      audits/catalog/curated-repairs.json), find a Morphia origin commit and the last
      mongodb/docs commit that touched the operator page at or before that commit's date.
      "first" uses the commit that created the fixture; "last" uses the latest commit that
      added or modified one of its files (pure renames do not count), which finds fixtures a
      later audit run regenerated from newer pages. Writes a FormMigration.Candidates file.

  fetch.py pages <sources.json> <pages-dir>
      Fetch every page named by a candidates or review file, plus its absolute
      `.. include::` targets (recursively), at the exact recorded commit, into
      <pages-dir>/<commit>/<path>. Every file, fetched or already present, must be exactly the
      git blob GitHub reports for that path and commit. Generated includes that are not
      tracked files are reported and skipped; the migration leaves those directives in place.

Requires an authenticated `gh` CLI.
"""
import hashlib
import json
import os
import re
import subprocess
import sys

REPOSITORY = "mongodb/docs"
DOCS_DIRECTORY = {
    "aggregation/expressions": "aggregation",
    "aggregation/stages": "aggregation",
    "query/filters": "query",
    "query/updates": "update",
}
INCLUDE = re.compile(r"^\s*\.\. include:: (/\S+)\s*$")


def run(*command, cwd=None, text=True):
    return subprocess.run(command, cwd=cwd, check=True, capture_output=True, text=text).stdout


FILES = ("action.json", "data.json", "expected.json", "index.json", "name")


def origin(root, fixture, which):
    folder = f"core/src/test/resources/dev/morphia/test/{fixture}"
    commits = []
    for name in FILES:
        if os.path.exists(os.path.join(root, folder, name)):
            log = run("git", "log", "--follow", "--diff-filter=AM", "--format=%H %cI", "--",
                      f"{folder}/{name}", cwd=root)
            commits += [line.split() for line in log.split("\n") if line]
    commits.sort(key=lambda commit: commit[1])
    return commits[0] if which == "first" else commits[-1]


def candidates(root, which, out):
    report = json.load(open(os.path.join(root, "audits/catalog/curated-repairs.json")))
    result = []
    for example in report["examples"]:
        if example["categories"] != ["MORPHIA_FORM"] or not example["markers"]:
            continue
        fixture = example["fixture"]
        commit, date = origin(root, fixture, which)
        package, operator = fixture.rsplit("/", 2)[0], fixture.split("/")[2]
        page = f"reference/operator/{DOCS_DIRECTORY[package]}/{operator}.txt"
        found = []
        # The manual moved from source/ to content/manual/manual/source/; the last commit to
        # touch the old path may be the one that removed it, so keep only pages that exist.
        for root_path in ("source", "content/manual/manual/source"):
            path = f"{root_path}/{page}"
            history = run("gh", "api", f"repos/{REPOSITORY}/commits?path={path}&until={date}&per_page=1",
                          "--jq", ".[0] | .sha + \" \" + .commit.committer.date").strip()
            if history and exists(history.split()[0], path):
                found.append((history.split()[1], history.split()[0], path))
        if not found:
            print(f"skipping {fixture}: no {REPOSITORY} commit holds {page} at {date}", file=sys.stderr)
            continue
        _, docs, path = max(found)
        result.append({"fixture": fixture, "origin": commit, "source": {"commit": docs, "path": path}})
    with open(out, "w") as handle:
        json.dump({"schemaVersion": 1, "candidates": result}, handle, indent=2)
        handle.write("\n")


def exists(commit, path):
    try:
        run("gh", "api", f"repos/{REPOSITORY}/contents/{path}?ref={commit}", "--jq", ".sha")
        return True
    except subprocess.CalledProcessError:
        return False


def blob(content):
    return hashlib.sha1(b"blob %d\0" % len(content) + content).hexdigest()


def fetch(commit, path, pages, include=False):
    target = os.path.join(pages, commit, path)
    if os.path.exists(target):
        # An existing file is kept only if it is exactly the tracked blob at that commit.
        expected = run("gh", "api", f"repos/{REPOSITORY}/contents/{path}?ref={commit}", "--jq", ".sha").strip()
        with open(target, "rb") as handle:
            if blob(handle.read()) != expected:
                sys.exit(f"{target} is not blob {expected} of {path} at {commit}")
    else:
        try:
            content = run("gh", "api", "-H", "Accept: application/vnd.github.raw",
                          f"repos/{REPOSITORY}/contents/{path}?ref={commit}", text=False)
        except subprocess.CalledProcessError:
            if not include:
                raise
            # Generated includes (for example /includes/extracts/) are not tracked files.
            print(f"not a tracked file at {commit[:10]}: {path}", file=sys.stderr)
            return
        os.makedirs(os.path.dirname(target), exist_ok=True)
        expected = run("gh", "api", f"repos/{REPOSITORY}/contents/{path}?ref={commit}", "--jq", ".sha").strip()
        if blob(content) != expected:
            sys.exit(f"Fetched {path} at {commit} is not blob {expected}")
        # Keep the exact bytes: the migration records each page's git blob id.
        with open(target, "wb") as handle:
            handle.write(content)
    source_root = path[: path.index("source/") + len("source")]
    with open(target, encoding="utf-8") as handle:
        for line in handle.read().split("\n"):
            match = INCLUDE.match(line)
            if match:
                nested = source_root + match.group(1)
                if ".." in nested.split("/"):
                    sys.exit(f"Invalid include {nested}")
                fetch(commit, nested, pages, True)


def pages(sources, directory):
    data = json.load(open(sources))
    entries = data.get("candidates", []) + data.get("declarations", []) + data.get("unresolved", [])
    for entry in entries:
        fetch(entry["source"]["commit"], entry["source"]["path"], directory)


if __name__ == "__main__":
    if len(sys.argv) == 5 and sys.argv[1] == "candidates" and sys.argv[3] in ("first", "last"):
        candidates(sys.argv[2], sys.argv[3], sys.argv[4])
    elif len(sys.argv) == 4 and sys.argv[1] == "pages":
        pages(sys.argv[2], sys.argv[3])
    else:
        sys.exit(__doc__)
