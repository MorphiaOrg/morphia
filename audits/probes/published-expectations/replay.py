#!/usr/bin/env python3
"""Offline reproducibility probe; invokes compiled project code, never documentation programs."""
import argparse
import collections
import json
import subprocess
import sys
from pathlib import Path


sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from runtime_receipt import capture_runtime, digest, require_stable_runtime, require_unchanged_files


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--java", required=True)
    parser.add_argument("--classpath", required=True, help="Compiled audits classes and dependencies")
    parser.add_argument("--bundle", required=True)
    parser.add_argument("--source-sha256", required=True)
    parser.add_argument("--documents", required=True)
    parser.add_argument("--output", required=True, type=Path, help="Disposable artifact directory")
    parser.add_argument("--prior-names", type=Path, help="Optional prior probe output to verify name stability")
    args = parser.parse_args()
    repository = Path(__file__).resolve().parents[3]
    plan = repository / "audits/probes/scenarios/plan.json"
    args.output.mkdir(parents=True, exist_ok=True)
    runtime = capture_runtime(repository, args.java, args.classpath)
    inputs = [Path(args.documents), plan, Path(__file__)] + ([args.prior_names] if args.prior_names else [])
    input_digests = {p: digest(p) for p in inputs}

    def run(main_class, arguments, output):
        with output.open("wb") as destination:
            subprocess.run([args.java, "-Xmx2g", "-cp", args.classpath, main_class, *arguments],
                           stdout=destination, check=True, timeout=180)

    report = args.output / "report.json"
    repeat = args.output / "repeat.json"
    arguments = [args.bundle, args.source_sha256, args.documents, str(plan)]
    for output in (report, repeat):
        run("dev.morphia.audits.scenarios.ScenariosMainKt", arguments, output)
    if report.read_bytes() != repeat.read_bytes():
        raise ValueError("Native extraction output is not byte-identical")
    candidate = json.loads(report.read_text())
    if candidate["state"] != "READY":
        raise ValueError("Probe did not produce complete source grouping")
    scenarios = candidate["scenarios"]
    states = collections.Counter(s["program"]["state"] for s in scenarios if s["program"])
    if states != {"NORMALIZED": 16} or len(scenarios) != 18:
        raise ValueError(f"Unexpected pinned probe counts: {states}, {len(scenarios)} groups")
    request = {
        "schemaVersion": 1, "catalogSha256": candidate["plan"]["catalogSha256"],
        "completeScopes": [p["scope"] for p in candidate["plan"]["pages"]],
        "scenarios": [s["naming"] for s in scenarios if s["naming"]],
        "occupied": [], "reviewedNames": [], "retirements": [],
    }
    naming_request = args.output / "naming-request.json"
    naming_request.write_text(json.dumps(request, indent=2) + "\n")
    registry = args.output / "empty-registry.json"
    registry.write_text('{"schemaVersion":1,"entries":[]}\n')
    names = args.output / "names.json"
    run("dev.morphia.audits.naming.NamesMainKt", [str(registry), str(naming_request)], names)
    proposal = json.loads(names.read_text())
    if proposal["state"] != "READY" or len(proposal["resolutions"]) != 16:
        raise ValueError("Expected sixteen complete descriptive-name proposals")
    if args.prior_names and names.read_bytes() != args.prior_names.read_bytes():
        raise ValueError("Descriptive-name proposal changed")
    require_stable_runtime(runtime, capture_runtime(repository, args.java, args.classpath))
    require_unchanged_files(input_digests)
    receipt = {
        "schemaVersion": 2,
        **runtime,
        "purpose": "Exploratory published-constraint normalization; not an accepted baseline or execution attestation",
        "sourceSha256": candidate["sourceSha256"],
        "documentInputSha256": candidate["documentInputSha256"],
        "documentReportSha256": digest(args.documents),
        "catalogSha256": candidate["plan"]["catalogSha256"],
        "planSha256": candidate["planSha256"], "planFileSha256": digest(plan),
        "reportSha256": digest(report), "reportBytes": report.stat().st_size,
        "repeatByteIdentical": True,
        "referenceGroups": sum(s["variant"] == "REFERENCE" for s in scenarios),
        "foreignGroups": sum(s["variant"] == "FOREIGN" for s in scenarios),
        "programStates": dict(states), "proposedNames": len(proposal["resolutions"]),
        "nameProposalSha256": digest(names),
        "priorNameProposalSha256": digest(args.prior_names) if args.prior_names else None,
        "physicalInventory": "Empty only for synthetic fixture.* destinations; not a core migration inventory",
        "probeSha256": digest(__file__),
    }
    (args.output / "receipt.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps({key: receipt[key] for key in (
        "referenceGroups", "foreignGroups", "programStates", "proposedNames", "reportSha256", "reportBytes")}, indent=2))


if __name__ == "__main__":
    main()
