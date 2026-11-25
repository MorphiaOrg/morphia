"""Pinned Snooty boundary. Input is a verified, immutable snapshot exported by Kotlin.

No source code or examples are executed. Run with the locked interpreter and -I -B.
The audit hook is defense in depth for the trusted pinned parser, not an OS sandbox
for arbitrary Python programs. The caller also enforces wall-clock/output budgets.
"""

import base64
import contextlib
import hashlib
import importlib.metadata
import json
import os
from pathlib import Path, PurePosixPath
import sys
import tempfile
import re
import tomllib

PARSER_REVISION = "0c26866083a02177a52ade1ccb17cc61a2816be7"
PYTHON_VERSION = "3.12.16"
MAX_INPUT = 64 * 1024 * 1024


def sha(data):
    return hashlib.sha256(data).hexdigest()


def canonical(value):
    return json.dumps(value, sort_keys=True, ensure_ascii=False, separators=(",", ":"))


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("Duplicate JSON key: " + key)
        result[key] = value
    return result


def valid_path(value):
    return (
        isinstance(value, str)
        and value not in ("", ".")
        and not value.startswith("/")
        and "\\" not in value
        and all(part not in ("", ".", "..") for part in value.split("/"))
        and not any(ord(c) < 32 or ord(c) == 127 for c in value)
    )


def check_input(data):
    if set(data) != {
        "schemaVersion",
        "sourceSha256",
        "projectRoot",
        "pages",
        "resources",
        "sourceProblems",
    }:
        raise ValueError("Unexpected parser input fields")
    if type(data["schemaVersion"]) is not int or data["schemaVersion"] != 1:
        raise ValueError("Expected explicit schemaVersion 1")
    if (
        not isinstance(data["sourceSha256"], str)
        or len(data["sourceSha256"]) != 64
        or any(c not in "0123456789abcdef" for c in data["sourceSha256"])
    ):
        raise ValueError("Invalid source content pin")
    if data["projectRoot"] != "." and not valid_path(data["projectRoot"]):
        raise ValueError("Invalid project root")
    if (
        not isinstance(data["pages"], list)
        or not data["pages"]
        or len(set(data["pages"])) != len(data["pages"])
        or not all(valid_path(p) and p.endswith(".txt") for p in data["pages"])
    ):
        raise ValueError("Expected unique explicit RST page ids")
    if not isinstance(data["resources"], list) or not data["resources"]:
        raise ValueError("Empty source snapshot")
    paths = set()
    for r in data["resources"]:
        if (
            set(r) != {"path", "kind", "sha256", "contentsBase64", "linkTarget"}
            or not valid_path(r["path"])
            or r["path"] in paths
        ):
            raise ValueError("Invalid/duplicate source resource")
        paths.add(r["path"])
        raw = base64.b64decode(r["contentsBase64"], validate=True)
        if sha(raw) != r["sha256"] or r["kind"] not in (
            "FILE",
            "EXECUTABLE",
            "SYMLINK",
        ):
            raise ValueError("Source resource hash/kind mismatch")
        if r["kind"] == "SYMLINK":
            if raw.decode("utf-8") != r["linkTarget"]:
                raise ValueError("Source symlink bytes mismatch")
        elif r["linkTarget"] is not None:
            raise ValueError("Unexpected symlink target")
    if any(
        str(parent) in paths
        for p in paths
        for parent in PurePosixPath(p).parents
        if str(parent) != "."
    ):
        raise ValueError("Source path is also a directory ancestor")
    if not isinstance(data["sourceProblems"], list):
        raise ValueError("Expected source diagnostics")
    for problem in data["sourceProblems"]:
        if (
            set(problem) != {"code", "path", "chain", "message"}
            or not all(
                isinstance(problem[k], str) and problem[k]
                for k in ("code", "path", "message")
            )
            or not valid_path(problem["path"])
            or not isinstance(problem["chain"], list)
            or not all(valid_path(part) for part in problem["chain"])
        ):
            raise ValueError("Malformed source diagnostic")
    return data


def runtime_provenance():
    version = ".".join(str(v) for v in sys.version_info[:3])
    if version != PYTHON_VERSION:
        raise ValueError(
            "Expected locked CPython " + PYTHON_VERSION + "; got " + version
        )
    dist = importlib.metadata.distribution("snooty")
    direct = json.loads(dist.read_text("direct_url.json") or "{}")
    if direct.get("vcs_info", {}).get("commit_id") != PARSER_REVISION:
        raise ValueError("Snooty installation differs from the immutable parser pin")
    lock_bytes = Path(__file__).with_name("uv.lock").read_bytes()
    normalize = lambda name: re.sub(r"[-_.]+", "-", name).lower()
    from packaging.markers import Marker

    packages = {
        normalize(p["name"]): p
        for p in tomllib.loads(lock_bytes.decode("utf-8"))["package"]
    }
    required = set()

    def visit(name):
        if name in required:
            return
        required.add(name)
        for dependency in packages[name].get("dependencies", []):
            if "marker" not in dependency or Marker(dependency["marker"]).evaluate():
                visit(normalize(dependency["name"]))

    visit("morphia-document-adapter")
    locked = {
        name: packages[name]["version"]
        for name in required
        if "virtual" not in packages[name]["source"]
    }
    installed = {}
    for distribution in importlib.metadata.distributions():
        name = normalize(distribution.metadata["Name"])
        if name in installed:
            raise ValueError("Duplicate installed distribution: " + name)
        installed[name] = distribution.version
        # Frozen installation verifies archive hashes. Here detect local file drift against
        # installed RECORDs, without claiming a cryptographic attestation of the toolchain.
        if distribution.files is None:
            raise ValueError("Missing installed RECORD: " + name)
        for entry in distribution.files:
            if entry.hash is not None:
                contents = distribution.locate_file(entry).read_bytes()
                digest = (
                    base64.urlsafe_b64encode(
                        hashlib.new(entry.hash.mode, contents).digest()
                    )
                    .decode()
                    .rstrip("=")
                )
                if digest != entry.hash.value:
                    raise ValueError(
                        "Installed RECORD mismatch: " + name + "/" + str(entry)
                    )
    if installed != locked:
        raise ValueError(
            "Installed distributions differ from the frozen dependency versions"
        )
    return {
        "engine": "snooty",
        "revision": PARSER_REVISION,
        "version": dist.version,
        "pythonVersion": version,
        "adapterSha256": sha(Path(__file__).read_bytes()),
        "dependencyLockSha256": sha(lock_bytes),
        "dependencies": installed,
    }


def install_guard(source, scratch):
    """Forbid parser network/processes and reads outside source + isolated runtime."""
    roots = (
        source.resolve(),
        Path(sys.prefix).resolve(),
        Path(sys.base_prefix).resolve(),
        Path(__file__).resolve().parent,
    )
    scratch = scratch.resolve()

    def beneath(path, allowed):
        return any(path.is_relative_to(root) for root in allowed)

    control = [True]

    def audit(event, args):
        if not control[0]:
            return
        if event.startswith(
            ("socket.", "subprocess.", "os.exec", "os.spawn", "os.posix_spawn")
        ) or event in ("os.system", "os.fork", "os.forkpty", "ctypes.dlopen"):
            raise PermissionError("PARSER_OPERATION_BLOCKED: " + event)
        if event == "os.chdir":
            raise PermissionError("PARSER_OPERATION_BLOCKED: os.chdir")
        if event in ("os.listdir", "os.scandir"):
            name = args[0]
            if not isinstance(name, int):
                path = Path(
                    os.fsdecode(name) if name is not None else os.getcwd()
                ).resolve()
                if not beneath(path, roots + (scratch,)):
                    raise PermissionError("PARSER_READ_BLOCKED: directory listing")
        if event == "open":
            name, mode, flags = args
            if isinstance(name, int):
                return
            path = Path(os.fsdecode(name)).resolve()
            writing = flags & (
                os.O_WRONLY | os.O_RDWR | os.O_CREAT | os.O_TRUNC | os.O_APPEND
            )
            if writing:
                if not beneath(path, (scratch,)):
                    raise PermissionError("PARSER_WRITE_BLOCKED")
            elif not beneath(path, roots + (scratch,)):
                raise PermissionError("PARSER_READ_BLOCKED: " + str(path))
        if event in (
            "os.remove",
            "os.rmdir",
            "os.mkdir",
            "os.rename",
            "os.link",
            "os.symlink",
            "os.chmod",
            "os.truncate",
            "os.utime",
            "os.chown",
        ):
            descriptor_positions = {
                "os.remove": (1,),
                "os.rmdir": (1,),
                "os.mkdir": (2,),
                "os.rename": (2, 3),
                "os.link": (2, 3),
                "os.symlink": (2,),
                "os.chmod": (2,),
                "os.chown": (3,),
                "os.utime": (3,),
            }
            if any(
                index < len(args) and args[index] not in (None, -1)
                for index in descriptor_positions.get(event, ())
            ):
                raise PermissionError("PARSER_OPERATION_BLOCKED: dir_fd mutation")
            if isinstance(args[0], int):
                raise PermissionError("PARSER_OPERATION_BLOCKED: descriptor mutation")
            # The parser has no reason to mutate its source/runtime. Cache writes only.
            for arg in args[
                : 2 if event in ("os.rename", "os.link", "os.symlink") else 1
            ]:
                if isinstance(arg, (str, bytes, os.PathLike)) and not beneath(
                    Path(os.fsdecode(arg)).resolve(), (scratch,)
                ):
                    raise PermissionError("PARSER_WRITE_BLOCKED: " + event)

    original_open = os.open

    def confined_open(path, flags, mode=0o777, *, dir_fd=None):
        if control[0] and dir_fd is not None:
            raise PermissionError("PARSER_OPERATION_BLOCKED: dir_fd open")
        return original_open(path, flags, mode, dir_fd=dir_fd)

    os.open = confined_open
    sys.addaudithook(audit)
    return control


def parse(data, raw_input):
    provenance = runtime_provenance()
    # Import before installing the read guard: runtime imports may consult platform metadata.
    import requests
    import mimetypes

    # Use the Python runtime MIME table, never machine-specific /etc/mime.types.
    mimetypes.knownfiles = []
    mimetypes.init()
    from snooty import parser, util
    from snooty.n import FileId
    from multiprocessing.pool import ThreadPool

    # Upstream always creates a process pool, even with one worker. Keep parsing
    # in one trusted worker thread so no child process inherits private inputs.
    parser.multiprocessing.Pool = lambda workers: ThreadPool(1)
    resources = {r["path"]: r for r in data["resources"]}
    with tempfile.TemporaryDirectory(prefix="morphia-doc-parser-") as temporary:
        root = Path(temporary).resolve()
        source = root / "source"
        source.mkdir()
        scratch = root / "cache"
        scratch.mkdir()
        problems = []
        for r in data["resources"]:
            if r["kind"] != "SYMLINK":
                p = source / r["path"]
                p.parent.mkdir(parents=True, exist_ok=True)
                p.write_bytes(base64.b64decode(r["contentsBase64"], validate=True))
        # Unhealthy links are never traversed, even if their bytes happen to be present.
        blocked = {problem["path"] for problem in data["sourceProblems"]}
        pending = [
            r
            for r in data["resources"]
            if r["kind"] == "SYMLINK" and r["path"] not in blocked
        ]
        while pending:
            next_pending = []
            for resource in pending:
                path = source / resource["path"]
                path.parent.mkdir(parents=True, exist_ok=True)
                target = (path.parent / resource["linkTarget"]).resolve()
                if target.is_relative_to(source) and target.exists():
                    path.symlink_to(resource["linkTarget"])
                else:
                    next_pending.append(resource)
            if len(next_pending) == len(pending):
                for resource in next_pending:
                    problems.append(
                        {
                            "code": "UNRESOLVED_SYMLINK",
                            "path": resource["path"],
                            "line": None,
                            "severity": "ERROR",
                            "message": "Link target unavailable in pinned snapshot",
                        }
                    )
                break
            pending = next_pending
        upstream_get_files = util.get_files

        def bundle_files(root, extensions, must_be_relative_to=None, diagnostics=None):
            # Verified repository symlinks may reach shared RST outside this property.
            # Preserve upstream traversal/cycle/nested-project handling inside our captured jail.
            return upstream_get_files(root, extensions, source, diagnostics)

        util.get_files = bundle_files
        project_root = source / data["projectRoot"]
        if not (project_root / "snooty.toml").is_file():
            raise ValueError("Selected project has no pinned snooty.toml")
        os.environ["XDG_CACHE_HOME"] = str(scratch)
        # Do not allow inherited HTTP/proxy/cache credentials to affect interpretation.
        for name in list(os.environ):
            if name.upper().endswith("_PROXY") or name.startswith(
                ("AWS_", "SNOOTY_", "GIT_")
            ):
                del os.environ[name]
        unresolved_urls = set()

        def offline_get(self, url, *args, **kwargs):
            unresolved_urls.add(str(url))
            raise requests.exceptions.ConnectionError(
                "UNRESOLVED_PINNED_RESOURCE: " + str(url)
            )

        util.HTTPCache.get = offline_get

        def clean(message):
            # Private temp paths are incidental and must not change report bytes.
            return (
                str(message)
                .replace(str(source) + "/", "")
                .replace(str(root), "<parser-runtime>")
            )

        project_prefix = "" if data["projectRoot"] == "." else data["projectRoot"] + "/"
        source_prefix = project_prefix + "source/"

        def diagnostic_path(path):
            # Snooty callback ids are source-relative, except its project config id.
            path = str(path)
            if path == "snooty.toml":
                return project_prefix + path
            candidate = (
                Path(path)
                if Path(path).is_absolute()
                else source / source_prefix / path
            )
            candidate = candidate.resolve()
            return (
                candidate.relative_to(source).as_posix()
                if candidate.is_relative_to(source)
                else "<unresolved-resource>"
            )

        raw_pages, resolved_pages, all_dependencies = {}, {}, {}

        class Backend(parser.ProjectBackend):
            def on_diagnostics(self, path, diagnostics):
                for item in diagnostics:
                    record = item.serialize()
                    problems.append(
                        {
                            "code": type(item).__name__,
                            "path": diagnostic_path(path),
                            "line": record.get("start", 0) + 1,
                            "severity": record["severity"],
                            "message": clean(record["message"]),
                        }
                    )

            def set_diagnostics(self, path, diagnostics):
                self.on_diagnostics(path, diagnostics)

            def on_update(self, prefix, build_identifiers, page_id, page):
                for identity in {str(page_id), str(page.ast.fileid)}:
                    dependencies = page.dependencies.dependencies
                    if identity not in all_dependencies:
                        all_dependencies[identity] = dependencies
                    elif all_dependencies[identity] is None or dependencies is None:
                        all_dependencies[identity] = None
                    else:
                        all_dependencies[identity] = {
                            **all_dependencies[identity],
                            **dependencies,
                        }
                if str(page_id) in data["pages"]:
                    resolved_pages[str(page_id)] = page.ast.serialize()

        backend = Backend()
        guard = install_guard(source, scratch)
        try:
            try:
                project = parser.Project(
                    project_root, backend, {}, branch="pinned-audit"
                )
                if (
                    project.config.source_path.resolve()
                    != (project_root / "source").resolve()
                ):
                    raise ValueError(
                        "UNSUPPORTED_SOURCE_ROOT: expected the explicit project/source directory"
                    )
                # Isolate raw-page failures and always restore the callback.
                for page_id in sorted(data["pages"]):
                    captured = []
                    original = backend.on_update

                    def capture(prefix, build_identifiers, current_id, page):
                        captured.append(
                            (page.ast.serialize(), page.dependencies.dependencies)
                        )

                    backend.on_update = capture
                    try:
                        project.update(FileId(page_id))
                        if len(captured) != 1:
                            raise ValueError("Expected one raw page callback")
                        raw_pages[page_id] = captured[0]
                    except Exception as failure:
                        problems.append(
                            {
                                "code": "PARSER_PAGE_FAILED",
                                "path": source_prefix + page_id,
                                "line": None,
                                "severity": "ERROR",
                                "message": clean(str(failure)),
                            }
                        )
                    finally:
                        backend.on_update = original
                project.build(max_workers=1, postprocess=True)
            except Exception as failure:
                problems.append(
                    {
                        "code": "PARSER_FAILED",
                        "path": project_prefix + "snooty.toml",
                        "line": None,
                        "severity": "ERROR",
                        "message": clean(str(failure)),
                    }
                )

            def origin(fileid, line, chain):
                candidate = source / source_prefix / fileid
                actual = candidate.resolve()
                canonical_path = (
                    actual.relative_to(source).as_posix()
                    if actual.is_relative_to(source)
                    else None
                )
                resource = resources.get(canonical_path)
                if resource and (resource["kind"] == "SYMLINK" or not actual.is_file()):
                    resource = None
                # Giza/generated nodes do not have trustworthy RST line positions.
                precision = (
                    "PARSER_REPORTED"
                    if resource and fileid.endswith((".txt", ".rst"))
                    else "GENERATED_OR_UNKNOWN"
                )
                return {
                    "path": canonical_path if resource else None,
                    "sha256": resource["sha256"] if resource else None,
                    "reportedLine": line + 1
                    if type(line) is int and precision == "PARSER_REPORTED"
                    else None,
                    "precision": precision,
                    "includeChain": [source_prefix + part for part in chain],
                }

            def node(value, fileid, chain):
                if value["type"] == "root":
                    fileid = value["fileid"]
                    chain = (
                        chain + [fileid] if not chain or chain[-1] != fileid else chain
                    )
                fields = {
                    k: canonical(v)
                    for k, v in value.items()
                    if k
                    not in (
                        "type",
                        "position",
                        "children",
                        "argument",
                        "value",
                        "name",
                        "domain",
                        "ids",
                        "options",
                    )
                }
                return {
                    "kind": value["type"],
                    "origin": origin(
                        fileid,
                        value.get("position", {}).get("start", {}).get("line"),
                        chain,
                    ),
                    "text": value.get("value"),
                    "name": value.get("name"),
                    "domain": value.get("domain"),
                    "identifiers": value.get("ids", []),
                    "options": {
                        k: canonical(v) for k, v in value.get("options", {}).items()
                    },
                    "attributes": fields,
                    "arguments": [
                        node(v, fileid, chain) for v in value.get("argument", [])
                    ],
                    "children": [
                        node(v, fileid, chain) for v in value.get("children", [])
                    ],
                }

            pages = []
            for page_id in sorted(data["pages"]):
                try:
                    raw, deps = raw_pages.get(page_id, (None, None))
                    if raw is None or page_id not in resolved_pages:
                        problems.append(
                            {
                                "code": "MISSING_PARSED_PAGE",
                                "path": source_prefix + page_id,
                                "line": None,
                                "severity": "ERROR",
                                "message": "Requested page did not produce both raw and expanded trees",
                            }
                        )
                    dependencies = []

                    def roots_in(value, chain=()):
                        if value.get("type") == "root":
                            chain = chain + (value["fileid"],)
                            yield value["fileid"], chain
                        for child in value.get("children", []):
                            yield from roots_in(child, chain)

                    dependent_pages = set(roots_in(resolved_pages.get(page_id, {}))) | {
                        (page_id, (page_id,))
                    }
                    complete = deps is not None
                    for dependent_page, chain in sorted(dependent_pages):
                        other = all_dependencies.get(
                            dependent_page, deps if dependent_page == page_id else None
                        )
                        complete = complete and other is not None
                        if other is None:
                            problems.append(
                                {
                                    "code": "INCOMPLETE_DEPENDENCY_GRAPH",
                                    "path": diagnostic_path(dependent_page),
                                    "line": None,
                                    "severity": "ERROR",
                                    "message": "Include has no complete parser dependency record",
                                }
                            )
                        for path, digest in sorted(
                            (other or {}).items(), key=lambda item: str(item[0])
                        ):
                            o = origin(str(path), None, list(chain))
                            dependencies.append(
                                {
                                    "requestedPath": str(path),
                                    "origin": o,
                                    "parserDigest": digest,
                                }
                            )
                            expected = resources.get(o["path"])
                            if (
                                not expected
                                or not digest
                                or len(digest) not in (64, 128)
                                or hashlib.blake2b(
                                    base64.b64decode(expected["contentsBase64"]),
                                    digest_size=len(digest) // 2,
                                ).hexdigest()
                                != digest
                            ):
                                complete = False
                                problems.append(
                                    {
                                        "code": "UNRESOLVED_DEPENDENCY",
                                        "path": diagnostic_path(path),
                                        "line": None,
                                        "severity": "ERROR",
                                        "message": "Parser dependency has no matching captured raw source",
                                    }
                                )
                    pages.append(
                        {
                            "id": page_id,
                            "raw": node(raw, page_id, []) if raw else None,
                            "expanded": node(resolved_pages[page_id], page_id, [])
                            if page_id in resolved_pages
                            else None,
                            "dependencies": dependencies,
                            "dependenciesComplete": complete,
                        }
                    )
                except Exception as failure:
                    problems.append(
                        {
                            "code": "PARSER_ADAPTER_FAILED",
                            "path": source_prefix + page_id,
                            "line": None,
                            "severity": "ERROR",
                            "message": clean(str(failure)),
                        }
                    )
                    pages.append(
                        {
                            "id": page_id,
                            "raw": None,
                            "expanded": None,
                            "dependencies": [],
                            "dependenciesComplete": False,
                        }
                    )
            for p in data["sourceProblems"]:
                problems.append(
                    {
                        "code": p["code"],
                        "path": p["path"],
                        "line": None,
                        "severity": "ERROR",
                        "message": p["message"],
                    }
                )
            problems = sorted(
                {canonical(p): p for p in problems}.values(), key=canonical
            )
            result = {
                "schemaVersion": 1,
                "sourceSha256": data["sourceSha256"],
                "inputSha256": sha(raw_input),
                "parser": provenance,
                "pages": pages,
                "diagnostics": problems,
                "unresolvedUrls": sorted(unresolved_urls),
                "state": "PARTIAL"
                if problems
                or unresolved_urls
                or any(not p["dependenciesComplete"] for p in pages)
                else "READY",
            }
            return result
        finally:
            guard[0] = False


def main():
    if len(sys.argv) != 2:
        raise ValueError("Usage: python -I -B adapter.py <exported-input.json>")
    with Path(sys.argv[1]).open("rb") as stream:
        raw = stream.read(MAX_INPUT + 1)
    if len(raw) > MAX_INPUT:
        raise ValueError("Parser input exceeds 64 MiB")
    data = check_input(json.loads(raw.decode("utf-8"), object_pairs_hook=unique_object))
    with contextlib.redirect_stdout(sys.stderr):
        result = parse(data, raw)
    sys.stdout.write(canonical(result) + "\n")
    return 0 if result["state"] == "READY" else 3


if __name__ == "__main__":
    try:
        sys.exit(main())
    except Exception as failure:
        print("PARSER_INVALID: " + str(failure), file=sys.stderr)
        sys.exit(2)
