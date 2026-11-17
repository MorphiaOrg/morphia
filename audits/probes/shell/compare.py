#!/usr/bin/env python3
"""Explicit, offline syntax benchmark. Requires already acquired, pinned parser artifacts."""
import argparse
import base64
import collections
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile

RHINO_SHA = "12dbb224cea124a5d32968722e8eb161c967bf15f3c9a795d1b05592b1147975"
ACORN_SHA = "b95392d79457d0357555c5ed3e56302d074b92dff119bcf3b44179500b10e9df"


def digest(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--documents", required=True, type=Path)
    parser.add_argument("--source-sha256", required=True)
    parser.add_argument("--rhino-jar", required=True, type=Path)
    parser.add_argument("--acorn-module", required=True, type=Path)
    parser.add_argument("--java-home", required=True, type=Path)
    parser.add_argument("--node", required=True, type=Path)
    parser.add_argument("--repository", type=Path, default=Path(__file__).resolve().parents[3])
    args = parser.parse_args()
    helper = Path(__file__).resolve().parent
    repository = args.repository.resolve()
    rhino = args.rhino_jar.resolve()
    acorn = args.acorn_module.resolve()
    if digest(rhino) != RHINO_SHA or digest(acorn) != ACORN_SHA:
        raise ValueError("Parser artifact digest differs from the benchmark pin")
    if args.documents.stat().st_size > 512 * 1024 * 1024:
        raise ValueError("Document report exceeds benchmark size limit")
    document_hash = digest(args.documents)
    document = json.loads(args.documents.read_text(encoding="utf-8"))
    if document["schemaVersion"] != 1 or document["sourceSha256"] != args.source_sha256:
        raise ValueError("Unexpected document schema or source identity")
    # The coordinator must have validated this owned artifact before the benchmark. This
    # script measures parser syntax only; it does not authenticate bundles or accept a baseline.
    rows = []
    for page in document["pages"]:
        todo = [page.get("expanded")]
        while todo:
            node = todo.pop()
            if node is None:
                continue
            todo.extend(reversed(node.get("children", [])))
            todo.extend(reversed(node.get("arguments", [])))
            if node["kind"] == "code":
                rows.append({"id": f"doc-{len(rows)}", "corpus": "documentation",
                             "page": page["id"], "origin": node["origin"],
                             "language": json.loads(node["attributes"].get("lang", "null")),
                             "text": node.get("text") or ""})
    del document
    corpus_file = repository / "audits/catalog/legacy-corpus.json"
    manifest = json.loads(corpus_file.read_text(encoding="utf-8"))
    resources = (repository / "core/src/test/resources/dev/morphia/test").resolve()
    for case in manifest["examples"]:
        for item in case["files"]:
            if not item["path"].endswith(".json"):
                continue
            path = (repository / item["path"]).resolve()
            if not path.is_relative_to(resources):
                raise ValueError("Legacy fixture escapes resource root")
            source = path.read_bytes()
            if hashlib.sha256(source).hexdigest() != item["sha256"]:
                raise ValueError(f"Legacy fixture changed: {item['path']}")
            rows.append({"id": f"legacy-{len(rows)}", "corpus": "legacy", "page": item["path"],
                         "language": "curated-json", "text": source.decode("utf-8"),
                         "locked": bool(case["markers"])})
    probes = {
        "strings": 'db.items.find({url:"https://host/a//b", text:"a:  b"})',
        "decimal": 'db.items.aggregate([{$project:{n:NumberDecimal("0.10")}}])',
        "eof": 'db.items.find({x:1})',
        "vars": 'const pipeline=[{$match:{a:1}}]; db.items.aggregate(pipeline);',
        "unsafe": 'Java.type("java.lang.Runtime").getRuntime().exec("touch /tmp/morphia-shell-must-not-execute");',
        "modern": 'const x = items?.map(x=>({...x}));',
        "truncated": 'db.items.find({x:',
        "duplicate": '({a:1,a:2})',
    }
    for name, text in probes.items():
        rows.append({"id": "probe-" + name, "corpus": "probe", "page": name,
                     "language": "javascript", "text": text})
    if len(rows) > 10000 or sum(len(r["text"]) for r in rows) > 16 * 1024 * 1024:
        raise ValueError("Benchmark corpus exceeds supported bounds")
    input_bytes = "".join(r["id"] + "\t" + base64.b64encode(r["text"].encode()).decode() + "\n"
                          for r in rows).encode()
    corpus_bytes = "".join(json.dumps(r, ensure_ascii=False) + "\n" for r in rows).encode()
    with tempfile.TemporaryDirectory(prefix="morphia-shell-spike-") as temporary:
        scratch = Path(temporary)
        subprocess.run([str(args.java_home / "bin/javac"), "-proc:none", "-cp", str(rhino), "-d", str(scratch),
                        str(helper / "MorphiaShellSpike.java")], check=True, timeout=30)
        commands = {
            "rhino": [str(args.java_home / "bin/java"), "-Xmx256m", "-cp",
                      str(scratch) + ":" + str(rhino), "MorphiaShellSpike"],
            "acorn": [str(args.node), "--max-old-space-size=256", str(helper / "acorn-spike.cjs"), str(acorn)],
        }
        results = {}
        for name, command in commands.items():
            run = subprocess.run(command, input=input_bytes, capture_output=True, check=True, timeout=60)
            if len(run.stdout) > 64 * 1024 * 1024:
                raise ValueError("Parser result exceeds benchmark limit")
            result = {}
            for line in run.stdout.decode("utf-8").splitlines():
                identity, mode, ok, error = line.split("\t")
                if mode in result.setdefault(identity, {}) or ok not in ("true", "false"):
                    raise ValueError("Duplicate or malformed parser result")
                result[identity][mode] = ok == "true"
            if set(result) != {r["id"] for r in rows} or any(
                    set(modes) != {"script", "expression"} for modes in result.values()):
                raise ValueError("Incomplete parser results")
            results[name] = result
    counts = {}
    for name, result in results.items():
        count = collections.Counter()
        for row in rows:
            key = (row["corpus"], str(row["language"]))
            count[key + ("total",)] += 1
            for mode in ("script", "expression"):
                count[key + (mode,)] += result[row["id"]][mode]
            count[key + ("either",)] += any(result[row["id"]].values())
        counts[name] = {" / ".join(key): value for key, value in sorted(count.items())}
    differences = []
    for row in rows:
        rhino_ok = any(results["rhino"][row["id"]].values())
        acorn_ok = any(results["acorn"][row["id"]].values())
        if rhino_ok != acorn_ok:
            differences.append({"id": row["id"], "page": row["page"], "language": row["language"],
                                "textSha256": hashlib.sha256(row["text"].encode()).hexdigest(),
                                "rhino": rhino_ok, "acorn": acorn_ok})
    node_version = subprocess.run([str(args.node), "--version"], capture_output=True, check=True,
                                  timeout=10).stdout.decode().strip()
    java_version = subprocess.run([str(args.java_home / "bin/java"), "-version"], capture_output=True,
                                  check=True, timeout=10).stderr.decode().strip()
    print(json.dumps({"schemaVersion": 1, "documentSha256": document_hash,
                      "sourceSha256": args.source_sha256, "legacyCorpusSha256": digest(corpus_file),
                      "rhinoSha256": RHINO_SHA, "acornSha256": ACORN_SHA,
                      "rhinoVersion": "1.9.1", "acornVersion": "8.19.0", "nodeVersion": node_version, "javaVersion": java_version,
                      "languageModes": {"rhino": "VERSION_ECMASCRIPT", "acorn": 2026},
                      "helperSha256": {name: digest(helper / name) for name in
                                       ["compare.py", "MorphiaShellSpike.java", "acorn-spike.cjs"]},
                      "inputSha256": hashlib.sha256(input_bytes).hexdigest(),
                      "corpusSha256": hashlib.sha256(corpus_bytes).hexdigest(),
                      "counts": counts, "differentAcceptance": differences}, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
