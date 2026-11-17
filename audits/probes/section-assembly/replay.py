#!/usr/bin/env python3
"""Reproduce native section ownership and names without executing documentation code."""
import argparse
import json
import subprocess
import sys
from pathlib import Path

sys.dont_write_bytecode = True
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))
from runtime_receipt import capture_runtime, digest, require_stable_runtime, require_unchanged_files


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java', required=True)
    parser.add_argument('--classpath', required=True)
    parser.add_argument('--bundle', required=True)
    parser.add_argument('--documents', required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    repository = Path(__file__).resolve().parents[3]
    probe = Path(__file__).resolve().parent
    selection_path = repository / 'audits/probes/shell-results/selection.json'
    selection = json.loads(selection_path.read_text())
    plan_path = probe / 'plan.json'
    prior_request_path = repository / selection['request']
    prior_request = json.loads(prior_request_path.read_text())
    runtime = capture_runtime(repository, args.java, args.classpath)
    inputs = {p: digest(p) for p in [Path(args.documents), selection_path, plan_path, prior_request_path, Path(__file__)]}
    args.output.mkdir(parents=True, exist_ok=True)

    def run(main_class, arguments, output):
        with output.open('wb') as stream:
            subprocess.run([args.java, '-Xmx2g', '-cp', args.classpath, main_class, *arguments], stdout=stream, timeout=180, check=True)

    report = args.output / 'report.json'
    repeat = args.output / 'repeat.json'
    for output in [report, repeat]:
        run('dev.morphia.audits.scenarios.ScenariosMainKt', [args.bundle, selection['sourceSha256'], args.documents, str(plan_path)], output)
    if report.read_bytes() != repeat.read_bytes():
        raise ValueError('Native section extraction is not byte-identical')
    owned = json.loads(report.read_text())
    if owned['state'] != 'READY' or owned['problems'] or len(owned['scenarios']) != 3:
        raise ValueError('Expected three completely owned reference scenarios')
    if owned['documentInputSha256'] != selection['documentInputSha256'] or any(p['unassigned'] or not p['namingComplete'] for p in owned['pages']):
        raise ValueError('Source/input pins or source ownership changed')
    absolute = [s for s in owned['scenarios'] if s['rule'] == 'absoluteValue']
    cosine = [s for s in owned['scenarios'] if s['rule'] == 'inverseCosine']
    if len(absolute) != 1 or len(cosine) != 2:
        raise ValueError('Native scenario grouping changed')
    abs_case = absolute[0]
    # Exact source digests and physical origins come from the preceding reviewed native experiment.
    if [(b['sha256'], b['origin'], b['role']) for b in abs_case['blocks']] != [(b['sha256'], b['origin'], b['role']) for b in selection['blocks']]:
        raise ValueError('Section assembly changed raw abs blocks, provenance or roles')
    if [b['source'] for b in abs_case['blocks']] != [b['source'] for b in prior_request['blocks']]:
        raise ValueError('Native abs source text changed')
    program = abs_case['program']
    if program['state'] != 'NORMALIZED' or len(program['commands']) != 2 or len(program['expectations'][0]['published']) != 4:
        raise ValueError('Expected the native seed, action and four published abs rows')
    prior_program_path = args.output / 'prior-explicit-program.json'
    run('dev.morphia.audits.shell.ProgramsMainKt', [str(prior_request_path)], prior_program_path)
    if program['semanticSha256'] != json.loads(prior_program_path.read_text())['semanticSha256']:
        raise ValueError('Automatic roles differ from the prior reviewed native program')
    for case in cosine:
        if case['program'] is not None or case['normalizationProblem'] != 'SOURCE_ONLY_SECTION' or len(case['blocks']) != 3 or any(b['role'] is not None for b in case['blocks']):
            raise ValueError('Displayed cosine samples must not become executable seeds')
    request = {'schemaVersion': 1, 'catalogSha256': owned['plan']['catalogSha256'], 'completeScopes': [p['scope'] for p in owned['plan']['pages']], 'scenarios': [s['naming'] for s in owned['scenarios']], 'occupied': [], 'reviewedNames': [], 'retirements': []}
    request_path = args.output / 'naming-request.json'
    request_path.write_text(json.dumps(request, indent=2) + '\n')
    registry = args.output / 'empty-registry.json'
    registry.write_text('{"schemaVersion":1,"entries":[]}\n')
    names_path = args.output / 'names.json'
    run('dev.morphia.audits.naming.NamesMainKt', [str(registry), str(request_path)], names_path)
    names = json.loads(names_path.read_text())
    if names['state'] != 'READY' or len(names['resolutions']) != 3:
        raise ValueError('Expected three complete descriptive name proposals')
    if {n['stem'] for n in names['resolutions']} != {'absExpressionOperator', 'inverseCosineOfValueInDegrees', 'inverseCosineOfValueInRadians'}:
        raise ValueError('Authored descriptive name proposals changed')
    require_stable_runtime(runtime, capture_runtime(repository, args.java, args.classpath))
    require_unchanged_files(inputs)
    receipt = {'schemaVersion': 2, **runtime, 'purpose': 'Pinned section ownership and authored naming; source-only samples are not executable and no core fixtures are migrated', 'sourceSha256': owned['sourceSha256'], 'documentInputSha256': owned['documentInputSha256'], 'documentReportSha256': digest(args.documents), 'planFileSha256': digest(plan_path), 'planSha256': owned['planSha256'], 'priorSelectionSha256': digest(selection_path), 'priorRequestSha256': digest(prior_request_path), 'programSemanticSha256': program['semanticSha256'], 'probeSha256': digest(__file__), 'reportSha256': digest(report), 'reportBytes': report.stat().st_size, 'repeatByteIdentical': True, 'referenceGroups': 3, 'unassignedBlocks': 0, 'normalizedPrograms': 1, 'sourceOnlyPrograms': 2, 'publishedRows': 4, 'proposedNames': 3, 'nameProposalSha256': digest(names_path), 'physicalInventory': 'Empty only for fixture.* destinations; not a core migration inventory'}
    (args.output / 'receipt.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps({k: receipt[k] for k in ['referenceGroups', 'unassignedBlocks', 'normalizedPrograms', 'sourceOnlyPrograms', 'proposedNames', 'reportSha256']}, indent=2))


if __name__ == '__main__':
    main()
