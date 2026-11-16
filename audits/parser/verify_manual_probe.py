"""Acceptance checks for the immutable source probe; never acquires sources or publishes."""

import json
from pathlib import Path
import sys

SOURCE_SHA256 = "7ca545e9212f459b6b8e2cc18e8141d5338d5f158aa158facc68d1944776d6d1"
EXPECTED = {
    "median": ("grp.$median", {("versionadded", "7.0")}, 2),
    "lookup": (
        "pipe.$lookup",
        {("versionchanged", "8.0"), ("versionadded", "5.0")},
        20,
    ),
    "setWindowFields": (
        "pipe.$setWindowFields",
        {("versionadded", "5.0"), ("versionadded", "8.3")},
        20,
    ),
}


def nodes(node):
    if node:
        yield node
        for key in ("children", "arguments"):
            for child in node[key]:
                yield from nodes(child)


def text(node):
    return (
        node["text"]
        if node["text"] is not None
        else "".join(text(child) for child in node["children"])
    )


def verify(report):
    if report["sourceSha256"] != SOURCE_SHA256 or report["state"] != "PARTIAL":
        raise ValueError("Expected the immutable, deliberately incomplete manual probe")
    failures = {"PARSER_FAILED", "MISSING_PARSED_PAGE", "UNRESOLVED_DEPENDENCY"}
    if any(d["code"] in failures for d in report["diagnostics"]):
        raise ValueError("Probe extraction failed; inspect explicit diagnostics")
    pages = {p["id"]: p for p in report["pages"]}
    expected_ids = {
        "reference/operator/aggregation/" + name + ".txt" for name in EXPECTED
    }
    if set(pages) != expected_ids:
        raise ValueError("Probe page scope changed")
    summary = {}
    for name, (identifier, versions, dependency_count) in EXPECTED.items():
        page = pages["reference/operator/aggregation/" + name + ".txt"]
        if not page["raw"] or not page["expanded"] or not page["dependenciesComplete"]:
            raise ValueError("Missing trees/dependencies for " + name)
        raw, expanded = list(nodes(page["raw"])), list(nodes(page["expanded"]))
        if not any(identifier in n["identifiers"] for n in raw):
            raise ValueError("Lost MongoDB definition role for " + name)
        events = {
            (n["name"], "".join(text(a) for a in n["arguments"]))
            for n in raw
            if n["name"] in ("versionadded", "versionchanged")
        }
        if events != versions:
            raise ValueError(
                "Lost/severed root or feature version evidence for " + name
            )
        code = [n for n in expanded if n["kind"] == "code"]
        if not code or any(not n["origin"]["sha256"] for n in code):
            raise ValueError("Missing code/raw provenance for " + name)
        dependencies = {d["origin"]["path"] for d in page["dependencies"]}
        if None in dependencies or len(dependencies) != dependency_count:
            raise ValueError("External dependency closure changed for " + name)
        summary[name] = dict(
            codeBlocks=len(code),
            dependencies=len(dependencies),
            versionEvents=sorted(events),
        )
    return summary


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit(
            "Usage: python -I -B verify_manual_probe.py <document-report.json>"
        )
    print(
        json.dumps(
            verify(json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))),
            sort_keys=True,
            indent=2,
        )
    )
