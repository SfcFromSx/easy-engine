from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from scripts.agent_loop import Harness


ROOT_FIXTURE_FILES = {
    "README.md": "# root\n",
    "AGENTS.md": "# agents\n",
    "HUMAN.MD": "# human\n",
    "docs/README.md": "# docs\n",
    "docs/architecture/README.md": "# architecture docs\n",
    "docs/operations/README.md": "# operations docs\n",
    "docs/agent/README.md": "# agent docs\n",
    "docs/operations/agent-loop-runbook.md": "# runbook\n",
    "docs/agent/prompts/orchestrator.md": "orchestrator\n",
    "docs/agent/prompts/implementer.md": "implementer\n",
    "docs/agent/prompts/verifier.md": "verifier\n",
    "docs/agent/prompts/doc-gardener.md": "doc-gardener\n",
    "docs/agent/schemas/orchestrator-output.schema.json": "{}\n",
    "docs/agent/schemas/implementer-output.schema.json": "{}\n",
    "docs/agent/schemas/verifier-output.schema.json": "{}\n",
    "docs/agent/schemas/doc-gardener-output.schema.json": "{}\n",
    ".agent/config.json": json.dumps(
        {
            "version": 1,
            "branch_prefix": "codex/autoloop",
            "commit_message_template": "autoloop: {task_id} {title}",
            "max_iterations": 8,
            "max_task_attempts": 3,
            "doc_gardening_interval": 2,
            "architecture_trigger_paths": ["docs/architecture/overview.md"],
            "mirror_docs": [
                {"source": "README.md", "target": "doc-CN/README.md", "title": "README"}
            ],
            "validation_commands": {"docs": []},
            "runners": {
                "codex": {"bin": "codex", "model": "gpt-5", "args": ["exec"]},
                "claude": {"bin": "claude", "model": "sonnet", "args": ["-p", "--output-format", "json"]},
            },
        },
        indent=2,
    )
    + "\n",
    ".agent/lock.json": json.dumps(
        {"locked": False, "owner": None, "started_at": None, "runner": None, "task_id": None},
        indent=2,
    )
    + "\n",
    "tasks.json": json.dumps(
        {
            "version": 1,
            "repo": {"name": "fixture", "root": "/tmp/fixture"},
            "defaults": {"status": "todo", "max_attempts": 3, "doc_gardening_interval": 2},
            "tasks": [
                {
                    "id": "A",
                    "title": "completed dependency",
                    "module": "docs",
                    "type": "documentation",
                    "priority": 0,
                    "status": "done",
                    "depends_on": [],
                    "context_files": [],
                    "acceptance_criteria": [],
                    "validation_commands": [],
                    "attempts": 0,
                    "last_result": None,
                    "updated_at": "2026-03-29T00:00:00Z",
                },
                {
                    "id": "B",
                    "title": "blocked by dependency",
                    "module": "docs",
                    "type": "documentation",
                    "priority": 1,
                    "status": "todo",
                    "depends_on": ["E"],
                    "context_files": [],
                    "acceptance_criteria": [],
                    "validation_commands": [],
                    "attempts": 0,
                    "last_result": None,
                    "updated_at": "2026-03-29T00:00:02Z",
                },
                {
                    "id": "C",
                    "title": "older ready task",
                    "module": "docs",
                    "type": "documentation",
                    "priority": 2,
                    "status": "todo",
                    "depends_on": [],
                    "context_files": [],
                    "acceptance_criteria": [],
                    "validation_commands": [],
                    "attempts": 0,
                    "last_result": None,
                    "updated_at": "2026-03-29T00:00:01Z",
                },
                {
                    "id": "D",
                    "title": "newer same priority task",
                    "module": "docs",
                    "type": "documentation",
                    "priority": 2,
                    "status": "todo",
                    "depends_on": [],
                    "context_files": [],
                    "acceptance_criteria": [],
                    "validation_commands": [],
                    "attempts": 0,
                    "last_result": None,
                    "updated_at": "2026-03-29T00:00:03Z",
                },
                {
                    "id": "E",
                    "title": "unfinished dependency",
                    "module": "docs",
                    "type": "documentation",
                    "priority": 5,
                    "status": "todo",
                    "depends_on": [],
                    "context_files": [],
                    "acceptance_criteria": [],
                    "validation_commands": [],
                    "attempts": 0,
                    "last_result": None,
                    "updated_at": "2026-03-29T00:00:04Z",
                }
            ],
        },
        indent=2,
    )
    + "\n",
    "doc-CN/README.md": "<!-- MIRROR: README.md -->\n\n# CN\n",
}


class AgentLoopTests(unittest.TestCase):
    def setUp(self) -> None:
        self.temp_dir = tempfile.TemporaryDirectory()
        self.root = Path(self.temp_dir.name)
        for rel_path, content in ROOT_FIXTURE_FILES.items():
            path = self.root / rel_path
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(content, encoding="utf-8")
        self.harness = Harness(self.root)

    def tearDown(self) -> None:
        self.temp_dir.cleanup()

    def test_select_next_task_prefers_ready_oldest(self) -> None:
        tasks = self.harness.load_tasks()
        next_task = self.harness.select_next_task(tasks)
        self.assertIsNotNone(next_task)
        self.assertEqual("C", next_task["id"])

    def test_sync_doc_cn_check_detects_stale_marker(self) -> None:
        failures = self.harness.sync_doc_cn(check=True)
        self.assertEqual(1, failures)
        sync_failures = self.harness.sync_doc_cn(check=False)
        self.assertEqual(0, sync_failures)
        self.assertEqual(0, self.harness.sync_doc_cn(check=True))

    def test_runner_payload_validation_rejects_bad_status(self) -> None:
        with self.assertRaisesRegex(Exception, "implementer status"):
            self.harness.validate_runner_payload(
                "implementer",
                {
                    "task_id": "X",
                    "status": "wrong",
                    "summary": "bad",
                    "files_modified": [],
                    "commands_run": [],
                    "tests_executed": [],
                    "test_results": "none",
                    "error_log": None,
                },
            )

    def test_doctor_fails_without_root_git(self) -> None:
        report = self.harness.doctor()
        self.assertFalse(report.ok)
        self.assertTrue(any("root Git repository is not initialized" in issue for issue in report.issues))

    def test_step_blocks_after_three_rejections(self) -> None:
        tasks = self.harness.load_tasks()
        task_c = next(task for task in tasks["tasks"] if task["id"] == "C")
        task_c["attempts"] = 2
        self.harness.save_tasks(tasks)
        first = self.harness.select_next_task(self.harness.load_tasks())
        self.assertEqual("C", first["id"])

        orchestrator = {
            "task_id": "C",
            "rationale": "work on C",
            "instructions": "do it",
            "context_files": [],
            "acceptance_criteria": [],
            "halt_reason": None,
        }
        implementer = {
            "task_id": "C",
            "status": "implemented",
            "summary": "implemented",
            "files_modified": [],
            "commands_run": [],
            "tests_executed": [],
            "test_results": "ok",
            "error_log": None,
        }
        verifier = {
            "task_id": "C",
            "validation_status": "rejected",
            "summary": "rejected",
            "evidence": ["bad"],
            "severity": "high",
            "next_action": "fix it",
        }

        with mock.patch.object(self.harness, "doctor", return_value=mock.Mock(ok=True, issues=[], warnings=[])), \
            mock.patch.object(self.harness, "ensure_loop_branch", return_value="codex/autoloop/test"), \
            mock.patch.object(self.harness, "commit_verified_task", return_value=None), \
            mock.patch.object(self.harness, "invoke_runner", side_effect=[orchestrator, implementer, verifier]):
            outcome = self.harness.step("codex")

        self.assertEqual("rejected", outcome["status"])
        updated = self.harness.load_tasks()
        task = next(task for task in updated["tasks"] if task["id"] == "C")
        self.assertEqual("blocked", task["status"])
        self.assertEqual(3, task["attempts"])

    def test_step_state_transition_is_runner_agnostic(self) -> None:
        for runner_name in ("codex", "claude"):
            tasks = self.harness.load_tasks()
            task_c = next(task for task in tasks["tasks"] if task["id"] == "C")
            task_c["status"] = "todo"
            task_c["attempts"] = 0
            task_c["last_result"] = None
            self.harness.save_tasks(tasks)
            selected_id = self.harness.select_next_task(self.harness.load_tasks())["id"]
            orchestrator = {
                "task_id": selected_id,
                "rationale": f"work on {selected_id}",
                "instructions": "do it",
                "context_files": [],
                "acceptance_criteria": [],
                "halt_reason": None,
            }
            implementer = {
                "task_id": selected_id,
                "status": "implemented",
                "summary": "implemented",
                "files_modified": ["docs/architecture/overview.md"],
                "commands_run": [],
                "tests_executed": [],
                "test_results": "ok",
                "error_log": None,
            }
            verifier = {
                "task_id": selected_id,
                "validation_status": "approved",
                "summary": "approved",
                "evidence": ["ok"],
                "severity": "low",
                "next_action": "none",
            }
            doc_gardener = {
                "status": "updated",
                "docs_updated": ["docs/architecture/overview.md"],
                "stale_docs": [],
                "unresolved_drift": [],
            }
            with mock.patch.object(self.harness, "doctor", return_value=mock.Mock(ok=True, issues=[], warnings=[])), \
                mock.patch.object(self.harness, "ensure_loop_branch", return_value=f"{runner_name}/branch"), \
                mock.patch.object(self.harness, "commit_verified_task", return_value="commit"), \
                mock.patch.object(
                    self.harness,
                    "invoke_runner",
                    side_effect=[orchestrator, implementer, verifier, doc_gardener],
                ):
                outcome = self.harness.step(runner_name)
            self.assertEqual("done", outcome["status"])
            refreshed = self.harness.load_tasks()
            task = next(task for task in refreshed["tasks"] if task["id"] == selected_id)
            self.assertEqual("done", task["status"])

    def test_invoke_runner_builds_codex_command_with_schema(self) -> None:
        def fake_run_command(cmd, cwd, check=False, capture_output=True):
            output_index = cmd.index("-o") + 1
            Path(cmd[output_index]).write_text(
                json.dumps(
                    {
                        "task_id": "C",
                        "rationale": "why",
                        "instructions": "do it",
                        "context_files": [],
                        "acceptance_criteria": [],
                        "halt_reason": None,
                    }
                ),
                encoding="utf-8",
            )
            self.assertIn("--output-schema", cmd)
            self.assertIn("exec", cmd)
            return mock.Mock(returncode=0, stdout="", stderr="")

        with mock.patch("scripts.agent_loop.run_command", side_effect=fake_run_command):
            payload = self.harness.invoke_runner("codex", "orchestrator", {"task": {"id": "C"}})
        self.assertEqual("C", payload["task_id"])

    def test_invoke_runner_builds_claude_command_with_schema(self) -> None:
        def fake_run_command(cmd, cwd, check=False, capture_output=True):
            self.assertIn("--json-schema", cmd)
            self.assertIn("-p", cmd)
            return mock.Mock(
                returncode=0,
                stdout=json.dumps(
                    {
                        "task_id": "C",
                        "validation_status": "approved",
                        "summary": "ok",
                        "evidence": [],
                        "severity": "low",
                        "next_action": "none",
                    }
                ),
                stderr="",
            )

        with mock.patch("scripts.agent_loop.run_command", side_effect=fake_run_command):
            payload = self.harness.invoke_runner("claude", "verifier", {"task": {"id": "C"}})
        self.assertEqual("approved", payload["validation_status"])


if __name__ == "__main__":
    unittest.main()
