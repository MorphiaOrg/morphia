#!/usr/bin/env python3
"""Acquire the historical documentation pages behind locked MORPHIA_FORM fixtures.

This is the only networked step of the form migration. It never edits the repository.

  fetch.py candidates <repo-root> <first|last> <out.json> [category...]
      For every locked example whose repair categories are all among the given ones (default
      MORPHIA_FORM; per audits/catalog/curated-repairs.json) and include at least one, find a Morphia origin commit and the last
      mongodb/docs commit that touched the operator page at or before that commit's date.
      "first" uses the commit that created the fixture; "last" uses the latest commit that
      added or modified one of its files (pure renames do not count), which finds fixtures a
      later audit run regenerated from newer pages. When the last commit at that date removed the
      page (the manual moved), its parent is used; when no commit holds the page by then, the
      commit that first added it is. Writes a FormMigration.Candidates file.

  fetch.py references <repo-root> <first|last> <out.json>
      The same search for every never-locked example (disposition UNCORRECTED, no marker), whose
      fixture was written from its page and never edited to match Morphia: the candidates for
      its pinned reference.

  fetch.py current <out.json> [commit]
      Every operator page of the manual at one mongodb/docs commit (default: the head of the
      default branch), for the generator's inventory of documented examples. Writes a sources
      file that `pages` fetches.

  fetch.py local <docs-checkout> <out.json> <pages-dir>
      The same sources file for a local mongodb/docs checkout (sparse is enough: the operator
      directories, source/includes and content/code-examples/tested), at its checked-out commit. Links <pages-dir>/<commit> to
      the checkout instead of fetching, so it makes no API calls; git already checked every blob.

  fetch.py pages <sources.json> <pages-dir>
      Fetch every page named by a candidates or review file, plus its absolute
      `.. include::` targets (recursively) and the tested snippets and include files its
      `.. input::`, `.. output::` and `.. literalinclude::` directives name, at the exact recorded commit, into
      <pages-dir>/<commit>/<path>. Every file, fetched or already present, must be exactly the
      git blob GitHub reports for that path and commit; ids already checked are cached in
      <pages-dir>/.verified.json and reused while a file's bytes still hash to them. A generated /includes/extracts/ file
      fetches the commit's extracts*.yaml sources instead, which the migration assembles; other
      untracked includes are reported and skipped, and the migration leaves them in place.

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
INCLUDE = re.compile(r"^\s*\.\. include::\s+(/\S+)\s*$")
# Code directives whose target the generator splices in: tested snippets and include files.
SNIPPET = re.compile(r"^\s*\.\. (?:input|output|literalinclude)::\s+(/(?:code-examples|includes)/\S+)\s*$")


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


def candidates(root, which, out, categories=("MORPHIA_FORM",)):
    def locked(example):
        return example["markers"] and example["categories"] and set(example["categories"]) <= set(categories)
    search(root, which, out, locked)


def references(root, which, out):
    # Never-locked examples: the fixture was written from the page and never edited to match Morphia.
    search(root, which, out, lambda example: example["disposition"] == "UNCORRECTED" and not example["markers"])


def search(root, which, out, selected):
    report = json.load(open(os.path.join(root, "audits/catalog/curated-repairs.json")))
    result = []
    for example in report["examples"]:
        if not selected(example):
            continue
        fixture = example["fixture"]
        commit, date = origin(root, fixture, which)
        package, operator = fixture.rsplit("/", 2)[0], fixture.split("/")[2]
        page = f"reference/operator/{DOCS_DIRECTORY[package]}/{operator}.txt"
        found = []
        # The manual moved from source/ to content/manual/manual/source/; the last commit to
        # touch the old path may be the one that removed it, in which case its parent holds the
        # page as it last was.
        for root_path in ("source", "content/manual/manual/source"):
            path = f"{root_path}/{page}"
            history = run("gh", "api", f"repos/{REPOSITORY}/commits?path={path}&until={date}&per_page=1",
                          "--jq", ".[0] | .sha + \" \" + .commit.committer.date").strip()
            if not history:
                continue
            sha, when = history.split()
            if not exists(sha, path):
                sha = run("gh", "api", f"repos/{REPOSITORY}/commits/{sha}", "--jq", ".parents[0].sha").strip()
            if exists(sha, path):
                found.append((when, sha, path))
        if not found:
            # The fixture predates every page (for example, written against unreleased docs): use
            # the commit that first added the page, and let the comparison record any gap.
            path = f"source/{page}"
            first = run("gh", "api", "--paginate", f"repos/{REPOSITORY}/commits?path={path}",
                        "--jq", ".[].sha").split()
            if not first or not exists(first[-1], path):
                print(f"skipping {fixture}: no {REPOSITORY} commit holds {page}", file=sys.stderr)
                continue
            print(f"{fixture}: no page at {date}; using the first one, {first[-1][:10]}", file=sys.stderr)
            found.append(("", first[-1], path, "AFTER_ORIGIN"))
        _, docs, path, relation = max(item + ("AT_ORIGIN",) if len(item) == 3 else item for item in found)
        result.append({"fixture": fixture, "origin": commit,
                       "source": {"commit": docs, "path": path, "relation": relation}})
    with open(out, "w") as handle:
        json.dump({"schemaVersion": 1, "candidates": result}, handle, indent=2)
        handle.write("\n")


CURRENT_DIRECTORIES = ("aggregation", "query", "update")


def current(out, commit=None):
    if commit is None:
        branch = run("gh", "api", f"repos/{REPOSITORY}", "--jq", ".default_branch").strip()
        commit = run("gh", "api", f"repos/{REPOSITORY}/commits/{branch}", "--jq", ".sha").strip()
    if not re.fullmatch(r"[0-9a-f]{40}", commit):
        sys.exit(f"Not a full commit SHA: {commit}")
    result = []
    for directory in CURRENT_DIRECTORIES:
        folder = f"content/manual/manual/source/reference/operator/{directory}"
        names = run("gh", "api", f"repos/{REPOSITORY}/contents/{folder}?ref={commit}",
                    "--jq", '.[] | select(.type == "file") | .name').split()
        result += [{"source": {"commit": commit, "path": f"{folder}/{name}"}}
                   for name in sorted(names) if name.endswith(".txt")]
    with open(out, "w") as handle:
        json.dump({"schemaVersion": 1, "commit": commit, "pages": result}, handle, indent=2)
        handle.write("\n")


def local(checkout, out, pages):
    commit = run("git", "rev-parse", "HEAD", cwd=checkout).strip()
    result = []
    for directory in CURRENT_DIRECTORIES:
        folder = f"content/manual/manual/source/reference/operator/{directory}"
        names = sorted(name for name in os.listdir(os.path.join(checkout, folder))
                       if name.endswith(".txt") and os.path.isfile(os.path.join(checkout, folder, name)))
        result += [{"source": {"commit": commit, "path": f"{folder}/{name}"}} for name in names]
    os.makedirs(pages, exist_ok=True)
    link = os.path.join(pages, commit)
    if not os.path.lexists(link):
        os.symlink(os.path.abspath(checkout), link)
    elif os.path.realpath(link) != os.path.realpath(checkout):
        sys.exit(f"{link} already points elsewhere")
    with open(out, "w") as handle:
        json.dump({"schemaVersion": 1, "commit": commit, "pages": result}, handle, indent=2)
        handle.write("\n")


def missing(error):
    """Whether a failed `gh api` call was GitHub reporting the path absent, not any other failure."""
    return "HTTP 404" in (error.stderr if isinstance(error.stderr, str) else error.stderr.decode())


def exists(commit, path):
    try:
        run("gh", "api", f"repos/{REPOSITORY}/contents/{path}?ref={commit}", "--jq", ".sha")
        return True
    except subprocess.CalledProcessError as error:
        if not missing(error):
            raise
        return False


def blob(content):
    return hashlib.sha1(b"blob %d\0" % len(content) + content).hexdigest()


VERIFIED = {}


def verified(pages):
    """Blob ids already checked against GitHub, cached beside the pages (never committed)."""
    if pages not in VERIFIED:
        cache = os.path.join(pages, ".verified.json")
        VERIFIED[pages] = json.load(open(cache)) if os.path.exists(cache) else {}
    return VERIFIED[pages]


def remember(pages, key, value):
    verified(pages)[key] = value
    with open(os.path.join(pages, ".verified.json"), "w") as handle:
        json.dump(verified(pages), handle, indent=0, sort_keys=True)


def fetch(commit, path, pages, include=False):
    target = os.path.join(pages, commit, path)
    key = f"{commit}/{path}"
    if os.path.exists(target):
        # An existing file is kept only if it is exactly the tracked blob at that commit. A blob
        # id checked earlier is reused while the bytes still hash to it.
        with open(target, "rb") as handle:
            actual = blob(handle.read())
        if verified(pages).get(key) != actual:
            expected = run("gh", "api", f"repos/{REPOSITORY}/contents/{path}?ref={commit}", "--jq", ".sha").strip()
            if actual != expected:
                sys.exit(f"{target} is not blob {expected} of {path} at {commit}")
            remember(pages, key, actual)
    elif key in verified(pages) and verified(pages)[key] is None:
        # Known not to be a tracked file at this commit.
        if "/includes/extracts/" in path:
            extracts(commit, path[: path.index("/includes/extracts/")], pages)
        return
    else:
        try:
            content = run("gh", "api", "-H", "Accept: application/vnd.github.raw",
                          f"repos/{REPOSITORY}/contents/{path}?ref={commit}", text=False)
        except subprocess.CalledProcessError as error:
            # Only a confirmed absence is cached: at a pinned commit it never changes, while a rate
            # limit or network failure must not turn a tracked include into an untracked one.
            if not include or not missing(error):
                raise
            remember(pages, key, None)
            # Generated includes are not tracked files. Giza builds /includes/extracts/ from the
            # extracts*.yaml sources, so fetch those; the migration assembles the extract.
            if "/includes/extracts/" in path:
                extracts(commit, path[: path.index("/includes/extracts/")], pages)
            else:
                print(f"not a tracked file at {commit[:10]}: {path}", file=sys.stderr)
            return
        os.makedirs(os.path.dirname(target), exist_ok=True)
        expected = run("gh", "api", f"repos/{REPOSITORY}/contents/{path}?ref={commit}", "--jq", ".sha").strip()
        if blob(content) != expected:
            sys.exit(f"Fetched {path} at {commit} is not blob {expected}")
        # Keep the exact bytes: the migration records each page's git blob id.
        with open(target, "wb") as handle:
            handle.write(content)
        remember(pages, key, expected)
    if "source/" not in path:
        return  # a tested snippet, outside the manual's source tree
    source_root = path[: path.index("source/") + len("source")]
    with open(target, encoding="utf-8") as handle:
        for line in handle.read().split("\n"):
            match = INCLUDE.match(line)
            snippet = SNIPPET.match(line)
            if match:
                nested = source_root + match.group(1)
            elif snippet and snippet.group(1).startswith("/code-examples/"):
                # source/code-examples/tested is a repository symlink to ../../../../code-examples/tested.
                nested = os.path.normpath(os.path.join(
                    source_root, "code-examples", "../../../../code-examples",
                    snippet.group(1)[len("/code-examples/"):]))
            elif snippet:
                nested = source_root + snippet.group(1)
            else:
                continue
            if ".." in nested.split("/"):
                sys.exit(f"Invalid include {nested}")
            fetch(commit, nested, pages, True)


FETCHED_EXTRACTS = set()


def extracts(commit, source_root, pages):
    """Fetches every extracts*.yaml file under <source_root>/includes at the commit, once."""
    if (commit, source_root) in FETCHED_EXTRACTS:
        return
    FETCHED_EXTRACTS.add((commit, source_root))
    # GraphQL lists the includes tree, then returns the YAML texts in batches of aliased objects.
    owner, name = REPOSITORY.split("/")
    query = """
      query($owner: String!, $name: String!, $expression: String!) {
        repository(owner: $owner, name: $name) {
          object(expression: $expression) { ... on Tree { entries { name type oid } } }
        }
      }"""
    result = json.loads(run("gh", "api", "graphql", "-f", f"query={query}", "-F", f"owner={owner}",
                            "-F", f"name={name}", "-F", f"expression={commit}:{source_root}/includes"))
    tree = result["data"]["repository"]["object"]
    if tree is None:
        sys.exit(f"no {source_root}/includes at {commit}")
    wanted = [entry for entry in tree["entries"] if entry["type"] == "blob"
              and entry["name"].startswith("extracts") and entry["name"].endswith(".yaml")]
    for start in range(0, len(wanted), 40):
        batch = wanted[start:start + 40]
        fields = " ".join(
            f'f{i}: object(expression: {json.dumps(commit + ":" + source_root + "/includes/" + e["name"])}) '
            "{ ... on Blob { text isTruncated isBinary } }" for i, e in enumerate(batch))
        texts = json.loads(run("gh", "api", "graphql", "-f",
                               f'query=query {{ repository(owner: "{owner}", name: "{name}") {{ {fields} }} }}'))
        texts = texts["data"]["repository"]
        for i, entry in enumerate(batch):
            path = f"{source_root}/includes/{entry['name']}"
            found = texts[f"f{i}"]
            content = None
            if found and not found["isTruncated"] and not found["isBinary"]:
                content = found["text"].encode("utf-8")
            if content is None or blob(content) != entry["oid"]:
                # Text that does not round-trip exactly is fetched as raw bytes.
                fetch(commit, path, pages, True)
                continue
            target = os.path.join(pages, commit, path)
            os.makedirs(os.path.dirname(target), exist_ok=True)
            with open(target, "wb") as handle:
                handle.write(content)
            remember(pages, f"{commit}/{path}", entry["oid"])
            for line in content.decode("utf-8").split("\n"):
                match = INCLUDE.match(line)
                if match and "/includes/extracts/" not in match.group(1):
                    fetch(commit, source_root + match.group(1), pages, True)

def prefill(pages):
    """Checks the blob ids of files already on disk against GitHub in batched GraphQL queries."""
    owner, name = REPOSITORY.split("/")
    pending = []
    for commit in sorted(os.listdir(pages)):
        base = os.path.join(pages, commit)
        if not os.path.isdir(base):
            continue
        for folder, _, files in os.walk(base):
            for file in files:
                path = os.path.relpath(os.path.join(folder, file), base)
                with open(os.path.join(base, path), "rb") as handle:
                    actual = blob(handle.read())
                if verified(pages).get(f"{commit}/{path}") != actual:
                    pending.append((commit, path, actual))
    for start in range(0, len(pending), 100):
        batch = pending[start:start + 100]
        fields = " ".join(f'f{i}: object(expression: {json.dumps(c + ":" + p)}) {{ oid }}'
                          for i, (c, p, _) in enumerate(batch))
        query = f'query {{ repository(owner: "{owner}", name: "{name}") {{ {fields} }} }}'
        result = json.loads(run("gh", "api", "graphql", "-f", f"query={query}"))["data"]["repository"]
        for i, (commit, path, actual) in enumerate(batch):
            found = result[f"f{i}"]
            if found is None or found["oid"] != actual:
                sys.exit(f"{commit}/{path} is not the tracked blob at that commit")
            verified(pages)[f"{commit}/{path}"] = actual
        remember(pages, f"{batch[0][0]}/{batch[0][1]}", batch[0][2])


def pages(sources, directory):
    prefill(directory)
    data = json.load(open(sources))
    entries = (data.get("candidates", []) + data.get("declarations", []) + data.get("unresolved", [])
               + data.get("pages", []))
    for entry in entries:
        fetch(entry["source"]["commit"], entry["source"]["path"], directory)


if __name__ == "__main__":
    if len(sys.argv) >= 5 and sys.argv[1] == "candidates" and sys.argv[3] in ("first", "last"):
        candidates(sys.argv[2], sys.argv[3], sys.argv[4], sys.argv[5:] or ("MORPHIA_FORM",))
    elif len(sys.argv) == 5 and sys.argv[1] == "references" and sys.argv[3] in ("first", "last"):
        references(sys.argv[2], sys.argv[3], sys.argv[4])
    elif len(sys.argv) in (3, 4) and sys.argv[1] == "current":
        current(*sys.argv[2:])
    elif len(sys.argv) == 5 and sys.argv[1] == "local":
        local(*sys.argv[2:])
    elif len(sys.argv) == 4 and sys.argv[1] == "pages":
        pages(sys.argv[2], sys.argv[3])
    else:
        sys.exit(__doc__)
