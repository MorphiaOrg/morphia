#!/usr/bin/env python3
"""Verify pinned source selection and replay printed results; never executes documentation code."""
import argparse
import json
import subprocess
import sys
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from runtime_receipt import capture_runtime, digest, require_stable_runtime, require_unchanged_files


def verify_source_blocks(blocks, declared_blocks, request_blocks):
    if len(blocks) != len(declared_blocks) or len(request_blocks) != len(blocks):
        raise ValueError("Pinned source block counts changed")
    declared_hashes = [b["sha256"] for b in declared_blocks]
    if len(set(declared_hashes)) != len(declared_hashes):
        raise ValueError("Declared source digests must be unique")
    if sorted(b["sha256"] for b in blocks) != sorted(declared_hashes):
        raise ValueError("Declared source selection must cover every source block exactly once")
    for declared, original in zip(declared_blocks, request_blocks):
        matches = [b for b in blocks if b["sha256"] == declared["sha256"]]
        if len(matches) != 1:
            raise ValueError("Source digest is missing or ambiguous; ordinal matching is forbidden")
        found = matches[0]
        if found["source"] != original["source"] or found["origin"] != declared["origin"] or original["id"] != declared["id"] or original["role"] != declared["role"]:
            raise ValueError("Source text, provenance or declared roles changed")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", required=True)
    parser.add_argument("--classpath", required=True)
    parser.add_argument("--bundle", required=True)
    parser.add_argument("--documents", required=True)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()
    repository = Path(__file__).resolve().parents[3]
    probe = Path(__file__).resolve().parent
    selection = json.loads((probe / "selection.json").read_text())
    request_path = repository / selection["request"]
    request = json.loads(request_path.read_text())
    args.output.mkdir(parents=True, exist_ok=True)
    runtime = capture_runtime(repository, args.java, args.classpath)
    input_digests = {p: digest(p) for p in [Path(args.documents), probe / "selection.json", probe / "source-plan.json", request_path, Path(__file__)]}

    def run(main_class, arguments, output, expected_exit=0):
        with output.open("wb") as destination:
            result = subprocess.run([args.java, "-Xmx2g", "-cp", args.classpath, main_class, *arguments], stdout=destination, check=False, timeout=180)
        if result.returncode != expected_exit:
            raise ValueError(f"{main_class} exited {result.returncode}, expected {expected_exit}")

    extraction = args.output / "source-extraction.json"
    run("dev.morphia.audits.scenarios.ScenariosMainKt", [args.bundle, selection["sourceSha256"], args.documents, str(probe / "source-plan.json")], extraction, 3)
    owned = json.loads(extraction.read_text())
    if owned["sourceSha256"] != selection["sourceSha256"] or owned["documentInputSha256"] != selection["documentInputSha256"]:
        raise ValueError("Native source/input pins changed")
    if owned["state"] != selection["expectedExtractionState"] or len(owned["pages"]) != 1:
        raise ValueError("Section assembly behavior changed; review the source selection experiment")
    if any(p["code"] not in {"GROUP_COUNT_DROP", "UNASSIGNED_CODE"} for p in owned["problems"]):
        raise ValueError("Pinned source has unexpected problems beyond unimplemented section ownership")
    blocks = owned["pages"][0]["unassigned"]
    verify_source_blocks(blocks, selection["blocks"], request["blocks"])

    report, repeat = args.output / "program.json", args.output / "repeat.json"
    for output in (report, repeat):
        run("dev.morphia.audits.shell.ProgramsMainKt", [str(request_path)], output)
    if report.read_bytes() != repeat.read_bytes():
        raise ValueError("Program replay is not byte-identical")
    candidate = json.loads(report.read_text())
    if candidate["state"] != "NORMALIZED" or len(candidate["commands"]) != 2 or len(candidate["expectations"]) != 1 or len(candidate["expectations"][0]["published"]) != 4:
        raise ValueError("Native example did not preserve its commands and four published rows")
    if any(any(s.get(key) != value for key, value in original.items()) for s, original in zip(candidate["sources"], request["blocks"])) or len(candidate["sources"]) != len(request["blocks"]):
        raise ValueError("Executed request source/provenance differs from the verified fixture")
    legacy_request = json.loads(request_path.read_text())
    legacy_request["blocks"][-1]["format"] = "SHELL"
    legacy_path = args.output / "ordinary-shell-request.json"
    legacy_path.write_text(json.dumps(legacy_request, indent=2) + "\n")
    legacy_report = args.output / "ordinary-shell-report.json"
    run("dev.morphia.audits.shell.ProgramsMainKt", [str(legacy_path)], legacy_report, 3)
    rejected = json.loads(legacy_report.read_text())
    if rejected["problems"][0]["code"] != "SYNTAX":
        raise ValueError("Ordinary SHELL rejection changed; re-evaluate the framing experiment")
    require_stable_runtime(runtime, capture_runtime(repository, args.java, args.classpath))
    require_unchanged_files(input_digests)
    receipt = {
        "schemaVersion": 2, **runtime,
        "purpose": "Pinned printed-result normalization with explicit roles; not section assembly, migration or execution coverage",
        "sourceSha256": selection["sourceSha256"], "documentInputSha256": selection["documentInputSha256"],
        "documentReportSha256": digest(args.documents),
        "sourcePlanSha256": digest(probe / "source-plan.json"), "selectionSha256": digest(probe / "selection.json"),
        "sourceExtractionState": owned["state"], "sourceUnassignedBlocks": len(blocks),
        "requestSha256": digest(request_path), "reportSha256": digest(report), "reportBytes": report.stat().st_size,
        "repeatByteIdentical": True, "programState": candidate["state"], "commands": len(candidate["commands"]),
        "publishedRows": 4, "ordinaryShellState": rejected["state"], "ordinaryShellProblem": rejected["problems"][0]["code"],
        "probeSha256": digest(__file__),
    }
    (args.output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({k: receipt[k] for k in ("programState", "commands", "publishedRows", "repeatByteIdentical", "sourceExtractionState", "reportSha256")}, indent=2))


if __name__ == "__main__":
    main()
