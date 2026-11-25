"""Offline regression tests for source selection and immutable probe inputs."""
import copy
import hashlib
import importlib.util
import sys
import tempfile
import unittest
from pathlib import Path

sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location("shell_results_replay", Path(__file__).with_name("replay.py"))
replay = importlib.util.module_from_spec(spec)
spec.loader.exec_module(replay)
from runtime_receipt import digest, require_stable_runtime, require_unchanged_files


class ProbeContractsTest(unittest.TestCase):
    def setUp(self):
        self.blocks = [{"source": value, "sha256": hashlib.sha256(value.encode()).hexdigest(), "origin": {"path": "owned-source", "reportedLine": i + 1}} for i, value in enumerate(("seed", "action", "expected"))]
        self.declared = [{"id": str(i), "role": role, "sha256": block["sha256"], "origin": block["origin"]} for i, (role, block) in enumerate(zip(("SETUP", "ACTION", "EXPECTED"), self.blocks))]
        self.request = [{"id": d["id"], "role": d["role"], "source": b["source"]} for d, b in zip(self.declared, self.blocks)]

    def test_source_lookup_survives_reordering_without_ordinals(self):
        replay.verify_source_blocks(self.blocks[::-1], self.declared, self.request)

    def test_duplicate_declarations_cannot_reuse_one_source_and_hide_another(self):
        declared = copy.deepcopy(self.declared)
        declared[1]["sha256"] = declared[0]["sha256"]
        declared[1]["origin"] = declared[0]["origin"]
        request = copy.deepcopy(self.request)
        request[1]["source"] = request[0]["source"]
        with self.assertRaises(ValueError):
            replay.verify_source_blocks(self.blocks, declared, request)

    def test_extra_missing_and_ambiguous_sources_fail(self):
        for blocks in (self.blocks[:-1], self.blocks + [self.blocks[0]], [self.blocks[0], self.blocks[0], self.blocks[2]]):
            with self.subTest(blocks=blocks), self.assertRaises(ValueError):
                replay.verify_source_blocks(blocks, self.declared, self.request)

    def test_source_text_origin_roles_and_ids_are_all_owned(self):
        for target, field, value in (("request", "source", "changed"), ("request", "role", "ACTION"), ("request", "id", "other"), ("declared", "origin", {"path": "different"})):
            declared, request = copy.deepcopy(self.declared), copy.deepcopy(self.request)
            (request if target == "request" else declared)[2][field] = value
            with self.subTest(field=field), self.assertRaises(ValueError):
                replay.verify_source_blocks(self.blocks, declared, request)

    def test_runtime_identity_changes_fail_but_output_status_changes_do_not(self):
        before = {key: "unchanged" for key in ("gitHead", "javaVersion", "pythonVersion", "runtimeClasspath", "normalizerSources", "buildInputs", "runtimeReceiptHelperSha256")}
        before["gitStatus"] = "before outputs"
        after = before | {"gitStatus": "after outputs"}
        require_stable_runtime(before, after)
        for key in before.keys() - {"gitStatus"}:
            with self.subTest(key=key), self.assertRaises(ValueError):
                require_stable_runtime(before, after | {key: "changed"})

    def test_mutated_input_cannot_receive_a_receipt(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "input.json"
            path.write_text("original")
            expected = {path: digest(path)}
            require_unchanged_files(expected)
            path.write_text("changed")
            with self.assertRaises(ValueError):
                require_unchanged_files(expected)


if __name__ == "__main__":
    unittest.main()
