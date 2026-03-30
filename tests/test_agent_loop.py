from __future__ import annotations

import io
import json
import subprocess
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
            "push_remote": "origin",
            "auto_push_after_commit": True,
            "max_iterations": 8,
            "lock_poll_seconds": 1,
            "max_task_attempts": 3,
            "doc_gardening_interval": 2,
            "runner_timeout_seconds": 30,
            "architecture_trigger_paths": ["docs/architecture/overview.md"],
            "mirror_docs": [
                {"source": "README.md", "target": "doc-CN/README.md", "title": "README"}
            ],
            "validation_commands": {"docs": []},
            "runners": {
                "codex": {
                    "bin": "codex",
                    "model": "gpt-5.4",
                    "timeout_seconds": 30,
                    "role_timeout_seconds": {"verifier": 45},
                    "retry_attempts": 2,
                    "retry_backoff_seconds": 0,
                    "retry_on_timeout": True,
                    "best_effort": True,
                    "retryable_stderr_patterns": ["stream disconnected", "timeout"],
                    "config_overrides": ["model_reasoning_effort=\"high\""],
                    "args": ["exec"],
                },
                "claude": {
                    "bin": "claude",
                    "model": "sonnet",
                    "timeout_seconds": 30,
                    "retry_attempts": 1,
                    "retry_backoff_seconds": 0,
                    "retry_on_timeout": False,
                    "best_effort": False,
                    "args": ["-p", "--output-format", "json"]
                },
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

    def test_doctor_flags_stray_process_when_lock_is_unlocked(self) -> None:
        with mock.patch.object(self.harness, "check_root_git", return_value=[]), \
            mock.patch.object(self.harness, "find_nested_git_dirs", return_value=[]), \
            mock.patch.object(
                self.harness,
                "stray_harness_processes",
                return_value=[{"pid": 4242, "command": "python scripts/agent_loop.py run --runner codex"}],
            ), \
            mock.patch("scripts.agent_loop.shutil.which", return_value="/usr/bin/mock"):
            report = self.harness.doctor()
        self.assertFalse(report.ok)
        self.assertTrue(any("agent_loop.py process is running while .agent/lock.json is unlocked" in issue for issue in report.issues))

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

    def test_step_keeps_selected_task_todo_until_implementer_result(self) -> None:
        tasks = self.harness.load_tasks()
        selected = self.harness.select_next_task(tasks)
        self.assertEqual("C", selected["id"])

        responses = [
            {
                "task_id": "C",
                "rationale": "work on C",
                "instructions": "do it",
                "context_files": [],
                "acceptance_criteria": [],
                "halt_reason": None,
            },
            {
                "task_id": "C",
                "status": "failed",
                "summary": "blocked",
                "files_modified": [],
                "commands_run": [],
                "tests_executed": [],
                "test_results": "none",
                "error_log": "blocked",
            },
        ]

        def fake_invoke_runner(runner_name, role, payload, timeout_override=None):
            if role == "orchestrator":
                fresh = self.harness.load_tasks()
                task = next(item for item in fresh["tasks"] if item["id"] == "C")
                self.assertEqual("todo", task["status"])
            return responses.pop(0)

        with mock.patch.object(self.harness, "doctor", return_value=mock.Mock(ok=True, issues=[], warnings=[])), \
            mock.patch.object(self.harness, "ensure_loop_branch", return_value="codex/autoloop/test"), \
            mock.patch.object(self.harness, "invoke_runner", side_effect=fake_invoke_runner):
            outcome = self.harness.step("codex")

        self.assertEqual("halted", outcome["status"])
        refreshed = self.harness.load_tasks()
        task = next(item for item in refreshed["tasks"] if item["id"] == "C")
        self.assertEqual("todo", task["status"])

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
                mock.patch.object(self.harness, "push_current_branch", return_value=f"{runner_name}/branch"), \
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

    def test_step_marks_done_before_commit_and_push(self) -> None:
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
            "files_modified": ["README.md"],
            "commands_run": [],
            "tests_executed": [],
            "test_results": "ok",
            "error_log": None,
        }
        verifier = {
            "task_id": "C",
            "validation_status": "approved",
            "summary": "approved",
            "evidence": ["ok"],
            "severity": "low",
            "next_action": "none",
        }
        seen = {}

        def fake_commit(task):
            current = self.harness.load_tasks()
            task_c = next(item for item in current["tasks"] if item["id"] == "C")
            seen["status_at_commit"] = task_c["status"]
            return "commit"

        with mock.patch.object(self.harness, "doctor", return_value=mock.Mock(ok=True, issues=[], warnings=[])), \
            mock.patch.object(self.harness, "ensure_loop_branch", return_value="codex/autoloop/test"), \
            mock.patch.object(self.harness, "should_run_doc_gardener", return_value=False), \
            mock.patch.object(self.harness, "commit_verified_task", side_effect=fake_commit), \
            mock.patch.object(self.harness, "push_current_branch", return_value="codex/autoloop/test"), \
            mock.patch.object(self.harness, "invoke_runner", side_effect=[orchestrator, implementer, verifier]):
            outcome = self.harness.step("codex")

        self.assertEqual("done", outcome["status"])
        self.assertEqual("done", seen["status_at_commit"])
        self.assertEqual("codex/autoloop/test", outcome["pushed_branch"])

    def test_invoke_runner_builds_codex_command_with_schema(self) -> None:
        def fake_run_command(cmd, cwd, check=False, capture_output=True, timeout=None):
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
        def fake_run_command(cmd, cwd, check=False, capture_output=True, timeout=None):
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

    def test_role_specific_timeout_overrides_runner_default(self) -> None:
        self.assertEqual(45, self.harness.runner_timeout_seconds("codex", "verifier"))
        self.assertEqual(30, self.harness.runner_timeout_seconds("codex", "implementer"))

    def test_handle_stage_failure_resets_task_to_todo(self) -> None:
        tasks = self.harness.load_tasks()
        self.harness.update_task(tasks, "C", status="in_progress", last_result=None)
        result = self.harness.handle_stage_failure(tasks, "C", "orchestrator", RuntimeError("boom"))
        self.assertEqual("halted", result["status"])
        updated = self.harness.load_tasks()
        task = next(task for task in updated["tasks"] if task["id"] == "C")
        self.assertEqual("todo", task["status"])
        self.assertEqual("orchestrator", task["last_result"]["stage"])

    def test_codex_timeout_retries_then_succeeds(self) -> None:
        state = {"calls": 0}

        def fake_run_command(cmd, cwd, check=False, capture_output=True, timeout=None):
            state["calls"] += 1
            output_index = cmd.index("-o") + 1
            output_path = Path(cmd[output_index])
            if state["calls"] == 1:
                raise subprocess.TimeoutExpired(cmd=cmd, timeout=timeout, output="", stderr=b"stream disconnected")
            output_path.write_text(
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
            return mock.Mock(returncode=0, stdout="", stderr="")

        with mock.patch("scripts.agent_loop.run_command", side_effect=fake_run_command):
            payload = self.harness.invoke_runner("codex", "orchestrator", {"task": {"id": "C"}})
        self.assertEqual("C", payload["task_id"])
        self.assertEqual(2, state["calls"])

    def test_run_drains_queue_until_no_tasks(self) -> None:
        step_results = iter(
            [
                {"status": "done", "task_id": "C"},
                {"status": "done", "task_id": "D"},
                {"status": "halted", "reason": "no_tasks"},
            ]
        )
        unlocked = {"locked": False, "owner": None, "started_at": None, "runner": None, "task_id": None}
        with mock.patch.object(self.harness, "read_lock_state", side_effect=[unlocked, unlocked, unlocked]), \
            mock.patch.object(self.harness, "stray_harness_processes", return_value=[]), \
            mock.patch.object(self.harness, "step", side_effect=lambda *args, **kwargs: next(step_results)) as step_mock, \
            mock.patch("sys.stdout", new_callable=io.StringIO):
            exit_code = self.harness.run("codex", None)
        self.assertEqual(0, exit_code)
        self.assertEqual(3, step_mock.call_count)

    def test_run_continues_after_rejected_until_no_tasks(self) -> None:
        step_results = iter(
            [
                {"status": "rejected", "task_id": "C", "attempts": 1, "next_status": "todo"},
                {"status": "done", "task_id": "D"},
                {"status": "halted", "reason": "no_tasks"},
            ]
        )
        unlocked = {"locked": False, "owner": None, "started_at": None, "runner": None, "task_id": None}
        with mock.patch.object(self.harness, "read_lock_state", side_effect=[unlocked, unlocked, unlocked]), \
            mock.patch.object(self.harness, "stray_harness_processes", return_value=[]), \
            mock.patch.object(self.harness, "step", side_effect=lambda *args, **kwargs: next(step_results)) as step_mock, \
            mock.patch("sys.stdout", new_callable=io.StringIO):
            exit_code = self.harness.run("codex", None)
        self.assertEqual(0, exit_code)
        self.assertEqual(3, step_mock.call_count)

    def test_run_respects_max_iterations(self) -> None:
        unlocked = {"locked": False, "owner": None, "started_at": None, "runner": None, "task_id": None}
        with mock.patch.object(self.harness, "read_lock_state", side_effect=[unlocked, unlocked]), \
            mock.patch.object(self.harness, "stray_harness_processes", return_value=[]), \
            mock.patch.object(self.harness, "step", return_value={"status": "done", "task_id": "C"}) as step_mock, \
            mock.patch("sys.stdout", new_callable=io.StringIO):
            exit_code = self.harness.run("codex", 2)
        self.assertEqual(0, exit_code)
        self.assertEqual(2, step_mock.call_count)

    def test_run_waits_for_active_lock_then_resumes(self) -> None:
        locked = {
            "locked": True,
            "owner": "owner-1",
            "started_at": "2026-03-30T00:00:00Z",
            "runner": "codex",
            "task_id": "C",
        }
        unlocked = {"locked": False, "owner": None, "started_at": None, "runner": None, "task_id": None}
        step_results = iter(
            [
                {"status": "done", "task_id": "D"},
                {"status": "halted", "reason": "no_tasks"},
            ]
        )
        with mock.patch.object(self.harness, "read_lock_state", side_effect=[locked, unlocked, unlocked]), \
            mock.patch.object(self.harness, "stray_harness_processes", return_value=[]), \
            mock.patch.object(self.harness, "step", side_effect=lambda *args, **kwargs: next(step_results)) as step_mock, \
            mock.patch("scripts.agent_loop.time.sleep") as sleep_mock, \
            mock.patch("sys.stdout", new_callable=io.StringIO) as stdout:
            exit_code = self.harness.run("codex", None)
        self.assertEqual(0, exit_code)
        self.assertEqual(2, step_mock.call_count)
        sleep_mock.assert_called_once_with(1)
        self.assertIn('"status": "waiting"', stdout.getvalue())

    def test_run_halts_on_stray_process_when_lock_is_unlocked(self) -> None:
        unlocked = {"locked": False, "owner": None, "started_at": None, "runner": None, "task_id": None}
        with mock.patch.object(self.harness, "read_lock_state", return_value=unlocked), \
            mock.patch.object(
                self.harness,
                "stray_harness_processes",
                return_value=[{"pid": 4242, "command": "python scripts/agent_loop.py run --runner codex"}],
            ), \
            mock.patch.object(self.harness, "step") as step_mock, \
            mock.patch("sys.stdout", new_callable=io.StringIO) as stdout:
            exit_code = self.harness.run("codex", None)
        self.assertEqual(1, exit_code)
        step_mock.assert_not_called()
        self.assertIn('"reason": "stale_process"', stdout.getvalue())


if __name__ == "__main__":
    unittest.main()
