"""Shared provenance for offline audit probes; source and executed classpath are distinct evidence."""
import hashlib
import json
import os
import subprocess
import sys
from pathlib import Path


def digest(path):
    with Path(path).open("rb") as source:
        return hashlib.file_digest(source, "sha256").hexdigest()


def capture_runtime(repository, java, classpath):
    entries = []
    for index, entry in enumerate(classpath.split(os.pathsep)):
        path = Path(entry or ".").resolve()
        if path.is_file():
            entries.append({"index": index, "path": str(path), "kind": "FILE", "sha256": digest(path)})
        elif path.is_dir():
            paths = sorted(path.rglob("*"))
            if any(p.is_symlink() for p in paths):
                raise ValueError(f"Classpath directory contains unsupported symlinks: {path}")
            files = [{"path": str(p.relative_to(path)), "sha256": digest(p)} for p in paths if p.is_file()]
            identity = hashlib.sha256(json.dumps(files, separators=(",", ":")).encode()).hexdigest()
            entries.append({"index": index, "path": str(path), "kind": "DIRECTORY", "fileCount": len(files), "sha256": identity})
        else:
            raise ValueError(f"Classpath entry is missing or unsupported: {path}")

    def capture(command):
        return subprocess.run(command, cwd=repository, check=True, capture_output=True, text=True, timeout=30).stdout.strip()

    version = subprocess.run([java, "-version"], check=True, capture_output=True, text=True, timeout=30)
    sources = sorted(p for p in (repository / "audits/src/main").rglob("*") if p.suffix in {".kt", ".java"})
    return {
        "gitHead": capture(["git", "rev-parse", "HEAD"]),
        "gitStatus": capture(["git", "status", "--porcelain=v1", "--untracked-files=all"]),
        "javaVersion": (version.stdout + version.stderr).strip(), "pythonVersion": sys.version,
        "runtimeClasspath": entries,
        "normalizerSources": {str(p.relative_to(repository)): digest(p) for p in sources},
        "buildInputs": {name: digest(repository / name) for name in ("pom.xml", "audits/pom.xml")},
        "runtimeReceiptHelperSha256": digest(__file__),
    }


def require_stable_runtime(before, after):
    for key in ("gitHead", "javaVersion", "pythonVersion", "runtimeClasspath", "normalizerSources", "buildInputs", "runtimeReceiptHelperSha256"):
        if before[key] != after[key]:
            raise ValueError(f"Probe runtime inputs changed during execution: {key}")


def require_unchanged_files(expected):
    for path, identity in expected.items():
        if digest(path) != identity:
            raise ValueError(f"Probe input changed during execution: {path}")
