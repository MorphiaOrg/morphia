"""Real pinned parser regressions; every test runs in a fresh isolated subprocess."""

import base64
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ADAPTER = Path(__file__).resolve().parents[1] / "adapter.py"


def nodes(value):
    if value:
        yield value
        for key in ("arguments", "children"):
            for child in value[key]:
                yield from nodes(child)


class AdapterTest(unittest.TestCase):
    def run_parser(self, files, pages=None, **changes):
        resources = []
        for path, text in sorted(files.items()):
            link = text[1] if isinstance(text, tuple) else None
            text = link if link is not None else text
            raw = text.encode("utf-8") if isinstance(text, str) else text
            resources.append(
                dict(
                    path=path,
                    kind="SYMLINK" if link is not None else "FILE",
                    sha256=hashlib.sha256(raw).hexdigest(),
                    contentsBase64=base64.b64encode(raw).decode(),
                    linkTarget=link,
                )
            )
        data = dict(
            schemaVersion=1,
            sourceSha256="1" * 64,
            projectRoot=".",
            pages=pages or ["index.txt"],
            resources=resources,
            sourceProblems=[],
        )
        data.update(changes)
        with tempfile.TemporaryDirectory() as temp:
            request = Path(temp) / "input.json"
            request.write_text(json.dumps(data), encoding="utf-8")
            result = subprocess.run(
                [sys.executable, "-I", "-B", str(ADAPTER), str(request)],
                capture_output=True,
                timeout=30,
            )
        self.assertIn(result.returncode, (0, 3), result.stderr.decode())
        return result, json.loads(result.stdout)

    def fixture(self, page, **files):
        return {
            "snooty.toml": 'name = "morphia-offline-fixture"\ntitle = "Offline fixture"\n',
            "source/index.txt": page,
            **files,
        }

    def test_includes_io_ranges_raw_text_and_provenance(self):
        files = self.fixture(
            """Operator examples
=================

.. include:: /includes/scenario.rst
""",
            **{
                "source/includes/scenario.rst": """Named scenario
--------------

.. versionadded:: 7.0

.. io-code-block::

   .. input:: /examples/query.js
      :language: javascript
      :start-after: start-example
      :end-before: end-example

   .. output:: /examples/output.json
      :language: json
""",
                "source/examples/query.js": '// start-example\ndb.a.aggregate([{ $match: { s: "a  b", d: ISODate("2020-01-01") } }]);\n// end-example\n',
                "source/examples/output.json": '{ "s": "a  b" }\n',
            },
        )
        result, report = self.run_parser(files)
        tree = list(nodes(report["pages"][0]["expanded"]))
        code = [n for n in tree if n["kind"] == "code"]
        self.assertEqual("READY", report["state"], report["diagnostics"])
        self.assertEqual(2, len(report["pages"][0]["dependencies"]))
        self.assertEqual(
            ["source/index.txt", "source/includes/scenario.rst"],
            report["pages"][0]["dependencies"][0]["origin"]["includeChain"],
        )
        self.assertEqual(2, len(code))
        self.assertIn('s: "a  b"', code[0]["text"])
        self.assertNotIn("start-example", code[0]["text"])
        self.assertEqual("source/includes/scenario.rst", code[0]["origin"]["path"])
        self.assertEqual(
            ["source/index.txt", "source/includes/scenario.rst"],
            code[0]["origin"]["includeChain"],
        )
        self.assertTrue(any(n["name"] == "versionadded" for n in tree))
        self.assertFalse(
            any(n["kind"] == "code" for n in nodes(report["pages"][0]["raw"]))
        )
        # Dependencies in included pages must be retained too (regression requirement).
        self.assertTrue(any(n["name"] == "input" and n["arguments"] for n in tree))
        again, _ = self.run_parser(files)
        self.assertEqual(result.stdout, again.stdout)

    def test_missing_resource_is_partial(self):
        _, report = self.run_parser(
            self.fixture(
                "Title\n=====\n\n.. literalinclude:: /missing.js\n   :language: javascript\n"
            )
        )
        self.assertEqual("PARTIAL", report["state"])
        self.assertTrue(report["diagnostics"])
        self.assertFalse(
            any(n["kind"] == "code" for n in nodes(report["pages"][0]["expanded"]))
        )

    def test_remote_resources_cannot_fetch(self):
        files = self.fixture("Title\n=====\n\n.. sharedinclude:: missing.rst\n")
        files["snooty.toml"] += (
            'sharedinclude_root = "https://example.invalid/moving/"\nintersphinx = ["https://example.invalid/objects.inv"]\n'
        )
        _, report = self.run_parser(files)
        self.assertEqual("PARTIAL", report["state"])
        self.assertIn("https://example.invalid/objects.inv", report["unresolvedUrls"])

    def test_external_reads_blocked(self):
        _, report = self.run_parser(
            self.fixture("Title\n=====\n\n.. literalinclude:: ../../../../etc/passwd\n")
        )
        self.assertEqual("PARTIAL", report["state"])
        self.assertFalse(
            any(
                "root:x:" in (n["text"] or "")
                for n in nodes(report["pages"][0]["expanded"])
            )
        )

    def test_unknown_directive_diagnostic_survives(self):
        _, report = self.run_parser(
            self.fixture("Title\n=====\n\n.. future-operator-shape:: $future\n")
        )
        self.assertEqual("PARTIAL", report["state"])
        self.assertTrue(
            any("future-operator-shape" in d["message"] for d in report["diagnostics"])
        )

    def test_duplicate_range_markers_not_guessed(self):
        _, report = self.run_parser(
            self.fixture(
                "Title\n=====\n\n.. literalinclude:: /code.js\n   :start-after: marker\n",
                **{"source/code.js": "// marker\na();\n// marker\nb();\n"},
            )
        )
        self.assertEqual("PARTIAL", report["state"])
        self.assertTrue(report["diagnostics"])

    def test_tabs_substitution_and_generated_yaml(self):
        files = self.fixture(
            "Title\n=====\n\n.. tabs::\n\n   .. tab:: Shell\n      :tabid: shell\n\n      Example |thing|\n\n      .. include:: /includes/extracts/fact.rst\n\n   .. tab:: Java\n      :tabid: java-sync\n\n      Other language\n",
            **{
                "source/includes/extracts-test.yaml": "ref: fact\ncontent: |\n  Generated explanation.\n...\n"
            },
        )
        files["snooty.toml"] += '\n[substitutions]\nthing = "value"\n'
        _, report = self.run_parser(files)
        tree = list(nodes(report["pages"][0]["expanded"]))
        self.assertTrue(any(n["text"] == "Generated explanation." for n in tree))
        self.assertTrue(any(n["text"] == "value" for n in tree))
        generated = [n for n in tree if n["text"] == "Generated explanation."][0]
        self.assertEqual("GENERATED_OR_UNKNOWN", generated["origin"]["precision"])
        self.assertIsNone(generated["origin"]["reportedLine"])
        self.assertEqual(2, len([n for n in tree if n["name"] == "tab"]))

    def test_figure_uses_static_asset_digest(self):
        pixel = base64.b64decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jB1sAAAAASUVORK5CYII="
        )
        _, report = self.run_parser(
            self.fixture(
                "Title\n=====\n\n.. figure:: /pixel.png\n   :alt: Pixel\n",
                **{"source/pixel.png": pixel},
            )
        )
        self.assertEqual("READY", report["state"], report["diagnostics"])
        self.assertEqual(64, len(report["pages"][0]["dependencies"][0]["parserDigest"]))

    def test_symlink_chains_and_unhealthy_links(self):
        files = self.fixture(
            "Title\n=====\n\n.. literalinclude:: /a/query.js\n   :language: javascript\n",
            **{
                "source/a": ("SYMLINK", "z"),
                "source/z": ("SYMLINK", "examples"),
                "source/examples/query.js": "db.a.find({});\n",
            },
        )
        _, report = self.run_parser(files)
        self.assertEqual("READY", report["state"], report["diagnostics"])
        self.assertEqual(
            "source/examples/query.js",
            report["pages"][0]["dependencies"][0]["origin"]["path"],
        )
        _, report = self.run_parser(
            files,
            sourceProblems=[
                dict(
                    code="UNDECLARED_DEPENDENCY",
                    path="source/a",
                    chain=["source/a"],
                    message="Dependency root is not declared",
                )
            ],
        )
        self.assertEqual("PARTIAL", report["state"])
        self.assertFalse(
            any(n["kind"] == "code" for n in nodes(report["pages"][0]["expanded"]))
        )

    def test_nonroot_project_and_one_missing_page(self):
        files = {
            "manual/" + p: value for p, value in self.fixture("Title\n=====\n").items()
        }
        _, report = self.run_parser(
            files, pages=["missing.txt", "index.txt"], projectRoot="manual"
        )
        pages = {p["id"]: p for p in report["pages"]}
        self.assertIsNotNone(pages["index.txt"]["expanded"])
        self.assertTrue(
            any(d["path"] == "manual/source/missing.txt" for d in report["diagnostics"])
        )
        self.assertEqual(
            "manual/source/index.txt", pages["index.txt"]["raw"]["origin"]["path"]
        )

    def test_bad_nested_link_preserves_healthy_sibling_include(self):
        files = {
            "manual/" + p: v
            for p, v in self.fixture(
                "Title\n=====\n\n.. include:: /includes/good.rst\n\n.. literalinclude:: /includes/bad.rst\n"
            ).items()
        }
        files.update(
            {
                "manual/source/includes": ("SYMLINK", "../../shared/includes"),
                "shared/includes/good.rst": "Preserved explanation.\n",
                "shared/includes/bad.rst": ("SYMLINK", "../../../outside.rst"),
            }
        )
        _, report = self.run_parser(
            files,
            projectRoot="manual",
            sourceProblems=[
                dict(
                    code="UNDECLARED_DEPENDENCY",
                    path="shared/includes/bad.rst",
                    chain=["manual/source/includes", "shared/includes/bad.rst"],
                    message="Bad descendant link",
                )
            ],
        )
        self.assertEqual("PARTIAL", report["state"])
        self.assertTrue(
            any(
                n["text"] == "Preserved explanation."
                for n in nodes(report["pages"][0]["expanded"])
            )
        )

        dependencies = report["pages"][0]["dependencies"]
        self.assertTrue(dependencies)
        self.assertTrue(
            all(d["origin"]["path"] != "shared/includes/bad.rst" for d in dependencies)
        )
        self.assertFalse(report["pages"][0]["dependenciesComplete"])

    def test_diagnostic_fileids_do_not_alias_bundle_root_names(self):
        files = self.fixture(
            "Title\n=====\n\n.. future-unknown:: value\n",
            **{"index.txt": "An unrelated root file\n"},
        )
        _, report = self.run_parser(files)
        self.assertTrue(report["diagnostics"])
        self.assertTrue(
            all(d["path"] == "source/index.txt" for d in report["diagnostics"])
        )

    def test_guard_directly_blocks_file_network_and_process(self):
        # Exercise audit-hook events directly; a parser path rejection alone proves less.
        script = """
import importlib.util, pathlib, socket, subprocess, tempfile, os
spec = importlib.util.spec_from_file_location('adapter', ADAPTER_PATH)
adapter = importlib.util.module_from_spec(spec)
spec.loader.exec_module(adapter)
with tempfile.TemporaryDirectory() as temporary:
    root = pathlib.Path(temporary)
    source, cache = root/'source', root/'cache'
    source.mkdir(); cache.mkdir()
    outside = root/'secret.txt'; outside.write_text('private')
    guard = adapter.install_guard(source, cache)
    try:
        for action in (outside.read_bytes, socket.socket, lambda: subprocess.run(['true']), lambda: os.posix_spawn('/usr/bin/true', ['true'], {})):
            try: action()
            except PermissionError as failure: print(str(failure).split(':')[0])
            else: raise AssertionError('guard did not block operation')
    finally:
        guard[0] = False
""".replace("ADAPTER_PATH", repr(str(ADAPTER)))
        result = subprocess.run(
            [sys.executable, "-I", "-B", "-c", script], capture_output=True, timeout=15
        )
        self.assertEqual(0, result.returncode, result.stderr.decode())
        self.assertEqual(
            [
                "PARSER_READ_BLOCKED",
                "PARSER_OPERATION_BLOCKED",
                "PARSER_OPERATION_BLOCKED",
                "PARSER_OPERATION_BLOCKED",
            ],
            result.stdout.decode().splitlines(),
        )


if __name__ == "__main__":
    unittest.main()
