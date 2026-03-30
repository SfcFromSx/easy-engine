#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import os
import signal
import time
import shutil
import subprocess
import sys
import tempfile
import uuid
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, List, Optional, Sequence


ROOT = Path(__file__).resolve().parents[1]
TASK_STATUSES = {"todo", "in_progress", "implemented", "verified", "done", "blocked"}
ROLE_TO_SCHEMA = {
    "orchestrator": "orchestrator-output.schema.json",
    "implementer": "implementer-output.schema.json",
    "verifier": "verifier-output.schema.json",
    "doc-gardener": "doc-gardener-output.schema.json",
}
ROLE_TO_PROMPT = {
    "orchestrator": "orchestrator.md",
    "implementer": "implementer.md",
    "verifier": "verifier.md",
    "doc-gardener": "doc-gardener.md",
}


class HarnessError(RuntimeError):
    pass


@dataclass
class DoctorReport:
    ok: bool
    issues: List[str]
    warnings: List[str]


def unlocked_lock_state() -> Dict[str, Any]:
    return {"locked": False, "owner": None, "started_at": None, "runner": None, "task_id": None}


def utc_now() -> str:
    return datetime.now(timezone.utc).replace(microsecond=0).isoformat().replace("+00:00", "Z")


def read_json(path: Path) -> Dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_json(path: Path, payload: Dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile("w", delete=False, dir=path.parent, encoding="utf-8") as handle:
        json.dump(payload, handle, indent=2, ensure_ascii=True)
        handle.write("\n")
        temp_name = handle.name
    os.replace(temp_name, path)


def run_command(
    cmd: Sequence[str],
    cwd: Path,
    *,
    check: bool = False,
    capture_output: bool = True,
    timeout: Optional[int] = None,
) -> subprocess.CompletedProcess[str]:
    completed = subprocess.run(
        list(cmd),
        cwd=str(cwd),
        text=True,
        capture_output=capture_output,
        check=False,
        timeout=timeout,
    )
    if check and completed.returncode != 0:
        raise HarnessError(f"command failed ({completed.returncode}): {' '.join(cmd)}\n{completed.stderr}")
    return completed


def truncate_text(value: Any, limit: int = 4000) -> str:
    if isinstance(value, bytes):
        value = value.decode("utf-8", errors="replace")
    if value is None:
        value = ""
    if not isinstance(value, str):
        value = str(value)
    if len(value) <= limit:
        return value
    return value[:limit] + "\n...[truncated]..."


class Harness:
    def __init__(self, root: Path = ROOT) -> None:
        self.root = root
        self.agent_dir = self.root / ".agent"
        self.docs_dir = self.root / "docs"
        self.prompts_dir = self.docs_dir / "agent" / "prompts"
        self.schemas_dir = self.docs_dir / "agent" / "schemas"
        self.history_dir = self.agent_dir / "history"
        self.runtime_dir = self.agent_dir / "runtime"
        self.runner_logs_dir = self.runtime_dir / "runner-logs"
        self.loop_process_path = self.runtime_dir / "loop-process.json"
        self.lock_path = self.agent_dir / "lock.json"
        self.config_path = self.agent_dir / "config.json"
        self.pause_path = self.agent_dir / "PAUSE"
        self.tasks_path = self.root / "tasks.json"
        self.config = read_json(self.config_path)

    def log_event(self, payload: Dict[str, Any]) -> None:
        self.history_dir.mkdir(parents=True, exist_ok=True)
        history_path = self.history_dir / f"{datetime.now(timezone.utc).strftime('%Y%m%d')}.jsonl"
        event = {"timestamp": utc_now(), **payload}
        with history_path.open("a", encoding="utf-8") as handle:
            handle.write(json.dumps(event, ensure_ascii=True) + "\n")

    def load_tasks(self) -> Dict[str, Any]:
        data = read_json(self.tasks_path)
        self.validate_tasks_file(data)
        return data

    def save_tasks(self, payload: Dict[str, Any]) -> None:
        write_json(self.tasks_path, payload)

    def validate_tasks_file(self, payload: Dict[str, Any]) -> None:
        if payload.get("version") != 1:
            raise HarnessError("tasks.json version must be 1")
        tasks = payload.get("tasks")
        if not isinstance(tasks, list):
            raise HarnessError("tasks.json must contain a tasks array")
        task_ids = set()
        for task in tasks:
            if not isinstance(task, dict):
                raise HarnessError("each task must be an object")
            task_id = task.get("id")
            if not isinstance(task_id, str) or not task_id:
                raise HarnessError("each task requires a non-empty id")
            if task_id in task_ids:
                raise HarnessError(f"duplicate task id: {task_id}")
            task_ids.add(task_id)
            if task.get("status") not in TASK_STATUSES:
                raise HarnessError(f"invalid task status for {task_id}")
            if not isinstance(task.get("priority"), int):
                raise HarnessError(f"task priority must be an integer for {task_id}")
            if not isinstance(task.get("depends_on", []), list):
                raise HarnessError(f"task depends_on must be a list for {task_id}")
            if not isinstance(task.get("context_files", []), list):
                raise HarnessError(f"task context_files must be a list for {task_id}")
            if not isinstance(task.get("acceptance_criteria", []), list):
                raise HarnessError(f"task acceptance_criteria must be a list for {task_id}")
        for task in tasks:
            for dependency in task.get("depends_on", []):
                if dependency not in task_ids:
                    raise HarnessError(f"task {task['id']} depends on missing task {dependency}")

    def validate_config(self) -> None:
        branch_prefix = self.config.get("branch_prefix")
        if not isinstance(branch_prefix, str) or not branch_prefix:
            raise HarnessError("config branch_prefix is required")
        if not isinstance(self.config.get("validation_commands"), dict):
            raise HarnessError("config validation_commands must be an object")
        if not isinstance(self.config.get("runners"), dict):
            raise HarnessError("config runners must be an object")
        lock_poll_seconds = self.config.get("lock_poll_seconds", 5)
        if not isinstance(lock_poll_seconds, int) or lock_poll_seconds <= 0:
            raise HarnessError("config lock_poll_seconds must be a positive integer")

    def schema_path(self, role: str) -> Path:
        return self.schemas_dir / ROLE_TO_SCHEMA[role]

    def prompt_path(self, role: str) -> Path:
        return self.prompts_dir / ROLE_TO_PROMPT[role]

    def check_root_git(self) -> List[str]:
        issues: List[str] = []
        git_bin = shutil.which("git")
        if not git_bin:
            issues.append("git is not installed")
            return issues
        completed = run_command([git_bin, "rev-parse", "--show-toplevel"], self.root)
        if completed.returncode != 0:
            issues.append("root Git repository is not initialized")
            return issues
        top_level = completed.stdout.strip()
        if Path(top_level) != self.root:
            issues.append(f"root Git top-level mismatch: expected {self.root}, got {top_level}")
        return issues

    def find_nested_git_dirs(self) -> List[Path]:
        nested: List[Path] = []
        for candidate in self.root.rglob(".git"):
            if candidate.parent == self.root:
                continue
            try:
                candidate.relative_to(self.runtime_dir)
                continue
            except ValueError:
                pass
            nested.append(candidate)
        return sorted(nested)

    def runner_bin(self, runner: str) -> str:
        runner_cfg = self.config["runners"].get(runner)
        if not runner_cfg:
            raise HarnessError(f"unknown runner: {runner}")
        binary = runner_cfg.get("bin")
        if not isinstance(binary, str):
            raise HarnessError(f"runner {runner} bin is invalid")
        return binary

    def runner_timeout_seconds(self, runner: str, role: Optional[str] = None) -> int:
        runner_cfg = self.config["runners"].get(runner, {})
        if role:
            role_timeouts = runner_cfg.get("role_timeout_seconds", {})
            if isinstance(role_timeouts, dict) and role in role_timeouts:
                role_value = role_timeouts[role]
                if not isinstance(role_value, int) or role_value <= 0:
                    raise HarnessError(f"runner role timeout must be a positive integer for {runner}:{role}")
                return role_value
        value = runner_cfg.get("timeout_seconds", self.config.get("runner_timeout_seconds", 180))
        if not isinstance(value, int) or value <= 0:
            raise HarnessError(f"runner timeout must be a positive integer for {runner}")
        return value

    def runner_retry_attempts(self, runner: str) -> int:
        runner_cfg = self.config["runners"].get(runner, {})
        value = runner_cfg.get("retry_attempts", 1)
        if not isinstance(value, int) or value <= 0:
            raise HarnessError(f"runner retry_attempts must be a positive integer for {runner}")
        return value

    def runner_retry_backoff_seconds(self, runner: str) -> int:
        runner_cfg = self.config["runners"].get(runner, {})
        value = runner_cfg.get("retry_backoff_seconds", 5)
        if not isinstance(value, int) or value < 0:
            raise HarnessError(f"runner retry_backoff_seconds must be a non-negative integer for {runner}")
        return value

    def should_retry_on_timeout(self, runner: str) -> bool:
        runner_cfg = self.config["runners"].get(runner, {})
        return bool(runner_cfg.get("retry_on_timeout", True))

    def retryable_stderr_patterns(self, runner: str) -> List[str]:
        runner_cfg = self.config["runners"].get(runner, {})
        patterns = runner_cfg.get("retryable_stderr_patterns", [])
        if not isinstance(patterns, list):
            raise HarnessError(f"runner retryable_stderr_patterns must be a list for {runner}")
        return [str(pattern).lower() for pattern in patterns]

    def write_runner_log(self, runner: str, role: str, payload: Dict[str, Any]) -> Path:
        self.runner_logs_dir.mkdir(parents=True, exist_ok=True)
        filename = f"{datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ')}-{runner}-{role}.json"
        path = self.runner_logs_dir / filename
        write_json(path, payload)
        return path

    def is_retryable_runner_error(self, runner: str, stderr: str) -> bool:
        haystack = stderr.lower()
        return any(pattern in haystack for pattern in self.retryable_stderr_patterns(runner))

    def doctor(self, *, live_runner_check: bool = False) -> DoctorReport:
        issues: List[str] = []
        warnings: List[str] = []
        required_paths = [
            self.root / "README.md",
            self.root / "AGENTS.md",
            self.root / "HUMAN.MD",
            self.docs_dir / "README.md",
            self.tasks_path,
            self.config_path,
            self.root / "scripts" / "agent_loop.py",
        ]
        for path in required_paths:
            if not path.exists():
                issues.append(f"missing required file: {path.relative_to(self.root)}")
        for path in self.prompts_dir.glob("*.md"):
            if not path.exists():
                issues.append(f"missing prompt file: {path.relative_to(self.root)}")
        for role in ROLE_TO_SCHEMA.values():
            path = self.schemas_dir / role
            if not path.exists():
                issues.append(f"missing schema file: {path.relative_to(self.root)}")
        try:
            self.validate_config()
            self.load_tasks()
        except Exception as exc:
            issues.append(str(exc))
        issues.extend(self.check_root_git())
        nested_repos = self.find_nested_git_dirs()
        if nested_repos:
            issues.append(
                "nested Git repositories detected: "
                + ", ".join(str(path.relative_to(self.root)) for path in nested_repos)
            )
        try:
            stray_processes = self.stray_harness_processes()
        except HarnessError as exc:
            issues.append(str(exc))
        else:
            if stray_processes:
                issues.append(
                    "agent_loop.py process is running while .agent/lock.json is unlocked: "
                    + ", ".join(f"{item['pid']}:{item['command']}" for item in stray_processes)
                )
        for runner in ("codex", "claude"):
            binary = self.runner_bin(runner)
            if shutil.which(binary) is None:
                issues.append(f"{runner} runner binary not found: {binary}")
        if not live_runner_check:
            warnings.append("runner auth was not live-verified; use --live-runner-check for a real invocation")
        ok = not issues
        return DoctorReport(ok=ok, issues=issues, warnings=warnings)

    def read_lock_state(self) -> Dict[str, Any]:
        if not self.lock_path.exists():
            return unlocked_lock_state()
        state = read_json(self.lock_path)
        default = unlocked_lock_state()
        if not isinstance(state, dict):
            raise HarnessError(".agent/lock.json must be an object")
        for key, value in default.items():
            state.setdefault(key, value)
        return state

    def write_lock_state(self, payload: Dict[str, Any]) -> None:
        write_json(self.lock_path, payload)

    def read_loop_process_state(self) -> Optional[Dict[str, Any]]:
        if not self.loop_process_path.exists():
            return None
        payload = read_json(self.loop_process_path)
        if not isinstance(payload, dict):
            raise HarnessError(".agent/runtime/loop-process.json must be an object")
        return payload

    def write_loop_process_state(self, payload: Dict[str, Any]) -> None:
        write_json(self.loop_process_path, payload)

    def clear_loop_process_state(self) -> None:
        self.loop_process_path.unlink(missing_ok=True)

    def acquire_lock(self, runner: str, task_id: Optional[str]) -> None:
        state = self.read_lock_state()
        if state.get("locked"):
            raise HarnessError("loop lock is already active")
        payload = {
            "locked": True,
            "owner": str(uuid.uuid4()),
            "started_at": utc_now(),
            "runner": runner,
            "task_id": task_id,
        }
        self.write_lock_state(payload)

    def release_lock(self) -> None:
        self.write_lock_state(unlocked_lock_state())

    def is_paused(self) -> bool:
        return self.pause_path.exists()

    def lock_poll_seconds(self) -> int:
        value = self.config.get("lock_poll_seconds", 5)
        if not isinstance(value, int) or value <= 0:
            raise HarnessError("lock_poll_seconds must be a positive integer")
        return value

    def pid_is_alive(self, pid: int) -> bool:
        try:
            os.kill(pid, 0)
        except ProcessLookupError:
            return False
        except PermissionError:
            return True
        return True

    def other_harness_processes(self) -> List[Dict[str, Any]]:
        completed = run_command(["ps", "-eo", "pid=,args="], self.root)
        if completed.returncode != 0:
            raise HarnessError("failed to inspect running harness processes")
        current_pid = os.getpid()
        processes: List[Dict[str, Any]] = []
        for line in completed.stdout.splitlines():
            line = line.strip()
            if not line or "scripts/agent_loop.py" not in line:
                continue
            pid_text, _, command = line.partition(" ")
            try:
                pid = int(pid_text)
            except ValueError:
                continue
            if pid == current_pid:
                continue
            processes.append({"pid": pid, "command": command.strip()})
        return processes

    def stray_harness_processes(self, lock_state: Optional[Dict[str, Any]] = None) -> List[Dict[str, Any]]:
        state = lock_state or self.read_lock_state()
        if state.get("locked"):
            return []
        return self.other_harness_processes()

    def stale_lock_without_process(self) -> bool:
        state = self.read_lock_state()
        if not state.get("locked"):
            return False
        return not self.other_harness_processes()

    def clear_stale_lock_if_safe(self) -> bool:
        if self.stale_lock_without_process():
            self.release_lock()
            self.log_event({"role": "lock-recovery", "status": "released_stale_lock"})
            return True
        return False

    def managed_loop_status(self) -> Dict[str, Any]:
        process_state = self.read_loop_process_state()
        lock_state = self.read_lock_state()
        active = False
        if process_state and isinstance(process_state.get("pid"), int):
            active = self.pid_is_alive(int(process_state["pid"]))
        if process_state and not active:
            process_state = {**process_state, "active": False}
        return {
            "active": active,
            "process": process_state,
            "lock": lock_state,
            "stray_processes": self.stray_harness_processes(lock_state),
        }

    def start_managed_run(
        self,
        runner: str,
        max_iterations: Optional[int],
        *,
        live_runner_check: bool = False,
    ) -> Dict[str, Any]:
        process_state = self.read_loop_process_state()
        if process_state and isinstance(process_state.get("pid"), int) and self.pid_is_alive(int(process_state["pid"])):
            raise HarnessError(f"managed loop is already running with pid {process_state['pid']}")
        if process_state and isinstance(process_state.get("pid"), int) and not self.pid_is_alive(int(process_state["pid"])):
            self.clear_loop_process_state()
        self.clear_stale_lock_if_safe()
        doctor_report = self.doctor(live_runner_check=live_runner_check)
        if not doctor_report.ok:
            raise HarnessError("cannot start managed loop because doctor failed")
        self.runtime_dir.mkdir(parents=True, exist_ok=True)
        log_path = self.runtime_dir / f"loop-{runner}.log"
        cmd = [sys.executable, str(self.root / "scripts" / "agent_loop.py"), "run", "--runner", runner]
        if max_iterations is not None:
            cmd.extend(["--max-iterations", str(max_iterations)])
        if live_runner_check:
            cmd.append("--live-runner-check")
        log_handle = log_path.open("a", encoding="utf-8")
        process = subprocess.Popen(  # noqa: S603
            cmd,
            cwd=str(self.root),
            stdin=subprocess.DEVNULL,
            stdout=log_handle,
            stderr=subprocess.STDOUT,
            start_new_session=True,
            text=True,
        )
        log_handle.close()
        payload = {
            "pid": process.pid,
            "runner": runner,
            "started_at": utc_now(),
            "cwd": str(self.root),
            "log_path": str(log_path),
            "max_iterations": max_iterations,
            "live_runner_check": live_runner_check,
        }
        self.write_loop_process_state(payload)
        self.log_event({"role": "loop-start", "runner": runner, "pid": process.pid, "log_path": str(log_path)})
        return payload

    def stop_managed_run(self) -> Dict[str, Any]:
        process_state = self.read_loop_process_state()
        if not process_state or not isinstance(process_state.get("pid"), int):
            released = self.clear_stale_lock_if_safe()
            return {"stopped": False, "released_stale_lock": released}
        pid = int(process_state["pid"])
        was_alive = self.pid_is_alive(pid)
        if was_alive:
            os.kill(pid, signal.SIGTERM)
            for _ in range(10):
                if not self.pid_is_alive(pid):
                    break
                time.sleep(1)
            if self.pid_is_alive(pid):
                os.kill(pid, signal.SIGKILL)
        self.clear_loop_process_state()
        released = self.clear_stale_lock_if_safe()
        self.log_event({"role": "loop-stop", "pid": pid, "was_alive": was_alive, "released_stale_lock": released})
        return {"stopped": was_alive, "released_stale_lock": released, "pid": pid}

    def select_next_task(self, tasks_payload: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        tasks = tasks_payload["tasks"]
        done_tasks = {task["id"] for task in tasks if task["status"] == "done"}
        eligible = []
        for task in tasks:
            if task["status"] != "todo":
                continue
            if all(dep in done_tasks for dep in task.get("depends_on", [])):
                eligible.append(task)
        eligible.sort(key=lambda item: (item["priority"], item.get("updated_at", "")))
        return eligible[0] if eligible else None

    def update_task(self, tasks_payload: Dict[str, Any], task_id: str, **changes: Any) -> Dict[str, Any]:
        for task in tasks_payload["tasks"]:
            if task["id"] == task_id:
                task.update(changes)
                task["updated_at"] = utc_now()
                return task
        raise HarnessError(f"task not found: {task_id}")

    def build_prompt(self, role: str, payload: Dict[str, Any]) -> str:
        role_prompt = self.prompt_path(role).read_text(encoding="utf-8")
        return (
            f"{role_prompt}\n\n"
            "Return JSON only. Follow the provided schema exactly.\n\n"
            "Input payload:\n"
            f"{json.dumps(payload, indent=2, ensure_ascii=True)}\n"
        )

    def parse_runner_json(self, raw: str) -> Dict[str, Any]:
        raw = raw.strip()
        if not raw:
            raise HarnessError("runner returned empty output")
        try:
            payload = json.loads(raw)
        except json.JSONDecodeError:
            start = raw.find("{")
            end = raw.rfind("}")
            if start == -1 or end == -1 or end < start:
                raise HarnessError("runner did not return JSON")
            payload = json.loads(raw[start : end + 1])
        if isinstance(payload, dict) and "result" in payload and isinstance(payload["result"], str):
            return self.parse_runner_json(payload["result"])
        if isinstance(payload, dict) and "content" in payload and isinstance(payload["content"], str):
            return self.parse_runner_json(payload["content"])
        if not isinstance(payload, dict):
            raise HarnessError("runner JSON payload must be an object")
        return payload

    def validate_runner_payload(self, role: str, payload: Dict[str, Any]) -> None:
        def require(key: str, expected_type: type) -> None:
            value = payload.get(key)
            if not isinstance(value, expected_type):
                raise HarnessError(f"{role} output field '{key}' must be {expected_type.__name__}")

        if role == "orchestrator":
            require("task_id", str)
            require("rationale", str)
            require("instructions", str)
            if not isinstance(payload.get("context_files"), list):
                raise HarnessError("orchestrator output field 'context_files' must be a list")
            if not isinstance(payload.get("acceptance_criteria"), list):
                raise HarnessError("orchestrator output field 'acceptance_criteria' must be a list")
            if "halt_reason" not in payload:
                raise HarnessError("orchestrator output must include halt_reason")
            return
        if role == "implementer":
            require("task_id", str)
            require("status", str)
            if payload["status"] not in {"implemented", "failed"}:
                raise HarnessError("implementer status must be implemented or failed")
            require("summary", str)
            for key in ("files_modified", "commands_run", "tests_executed"):
                if not isinstance(payload.get(key), list):
                    raise HarnessError(f"implementer output field '{key}' must be a list")
            require("test_results", str)
            if "error_log" not in payload:
                raise HarnessError("implementer output must include error_log")
            return
        if role == "verifier":
            require("task_id", str)
            require("validation_status", str)
            if payload["validation_status"] not in {"approved", "rejected"}:
                raise HarnessError("verifier validation_status must be approved or rejected")
            require("summary", str)
            if not isinstance(payload.get("evidence"), list):
                raise HarnessError("verifier output field 'evidence' must be a list")
            require("severity", str)
            require("next_action", str)
            return
        if role == "doc-gardener":
            require("status", str)
            if payload["status"] not in {"updated", "no_changes", "blocked"}:
                raise HarnessError("doc-gardener status must be updated, no_changes, or blocked")
            for key in ("docs_updated", "stale_docs", "unresolved_drift"):
                if not isinstance(payload.get(key), list):
                    raise HarnessError(f"doc-gardener output field '{key}' must be a list")
            return
        raise HarnessError(f"unknown role for validation: {role}")

    def invoke_runner(
        self,
        runner: str,
        role: str,
        payload: Dict[str, Any],
        *,
        timeout_override: Optional[int] = None,
    ) -> Dict[str, Any]:
        runner_cfg = self.config["runners"][runner]
        prompt = self.build_prompt(role, payload)
        schema_path = self.schema_path(role)
        binary = self.runner_bin(runner)
        model = runner_cfg.get("model")
        args = list(runner_cfg.get("args", []))
        config_overrides = runner_cfg.get("config_overrides", [])
        timeout_seconds = timeout_override or self.runner_timeout_seconds(runner, role)
        retry_attempts = self.runner_retry_attempts(runner)
        retry_backoff_seconds = self.runner_retry_backoff_seconds(runner)
        retry_on_timeout = self.should_retry_on_timeout(runner)
        started_at = utc_now()
        last_error_message = f"{runner} {role} failed"
        for attempt in range(1, retry_attempts + 1):
            if runner == "codex":
                with tempfile.NamedTemporaryFile("w", delete=False, encoding="utf-8") as handle:
                    output_path = Path(handle.name)
                cmd = [binary] + args + ["--cd", str(self.root), "--output-schema", str(schema_path), "-o", str(output_path)]
                for override in config_overrides:
                    cmd.extend(["-c", str(override)])
                if model:
                    cmd.extend(["--model", str(model)])
                cmd.append(prompt)
                try:
                    started = time.monotonic()
                    completed = run_command(cmd, self.root, timeout=timeout_seconds)
                    elapsed = round(time.monotonic() - started, 3)
                except subprocess.TimeoutExpired as exc:
                    raw_outfile = output_path.read_text(encoding="utf-8") if output_path.exists() else ""
                    log_path = self.write_runner_log(
                        runner,
                        role,
                        {
                            "runner": runner,
                            "role": role,
                            "started_at": started_at,
                            "attempt": attempt,
                            "retry_attempts": retry_attempts,
                            "timeout_seconds": timeout_seconds,
                            "timed_out": True,
                            "command": cmd,
                            "stdout": truncate_text(exc.stdout or ""),
                            "stderr": truncate_text(exc.stderr or ""),
                            "outfile": truncate_text(raw_outfile),
                        },
                    )
                    output_path.unlink(missing_ok=True)
                    last_error_message = (
                        f"{runner} {role} timed out after {timeout_seconds}s on attempt {attempt}/{retry_attempts}; "
                        f"see {log_path.relative_to(self.root)}"
                    )
                    if attempt < retry_attempts and retry_on_timeout:
                        time.sleep(retry_backoff_seconds * attempt)
                        continue
                    raise HarnessError(last_error_message) from exc
                if completed.returncode != 0:
                    stderr_text = truncate_text(completed.stderr or "")
                    log_path = self.write_runner_log(
                        runner,
                        role,
                        {
                            "runner": runner,
                            "role": role,
                            "started_at": started_at,
                            "attempt": attempt,
                            "retry_attempts": retry_attempts,
                            "elapsed_seconds": elapsed,
                            "returncode": completed.returncode,
                            "command": cmd,
                            "stdout": truncate_text(completed.stdout or ""),
                            "stderr": stderr_text,
                            "outfile": truncate_text(output_path.read_text(encoding="utf-8") if output_path.exists() else ""),
                        },
                    )
                    output_path.unlink(missing_ok=True)
                    last_error_message = (
                        f"{runner} {role} failed with code {completed.returncode} on attempt {attempt}/{retry_attempts}; "
                        f"see {log_path.relative_to(self.root)}"
                    )
                    if attempt < retry_attempts and self.is_retryable_runner_error(runner, stderr_text):
                        time.sleep(retry_backoff_seconds * attempt)
                        continue
                    raise HarnessError(last_error_message)
                raw = output_path.read_text(encoding="utf-8")
                self.write_runner_log(
                    runner,
                    role,
                    {
                        "runner": runner,
                        "role": role,
                        "started_at": started_at,
                        "attempt": attempt,
                        "retry_attempts": retry_attempts,
                        "elapsed_seconds": elapsed,
                        "returncode": completed.returncode,
                        "command": cmd,
                        "stdout": truncate_text(completed.stdout or ""),
                        "stderr": truncate_text(completed.stderr or ""),
                        "outfile": truncate_text(raw),
                    },
                )
                output_path.unlink(missing_ok=True)
                break
            elif runner == "claude":
                schema_text = schema_path.read_text(encoding="utf-8")
                cmd = [binary] + args + ["--json-schema", schema_text]
                if model:
                    cmd.extend(["--model", str(model)])
                cmd.append(prompt)
                try:
                    started = time.monotonic()
                    completed = run_command(cmd, self.root, timeout=timeout_seconds)
                    elapsed = round(time.monotonic() - started, 3)
                except subprocess.TimeoutExpired as exc:
                    log_path = self.write_runner_log(
                        runner,
                        role,
                        {
                            "runner": runner,
                            "role": role,
                            "started_at": started_at,
                            "attempt": attempt,
                            "retry_attempts": retry_attempts,
                            "timeout_seconds": timeout_seconds,
                            "timed_out": True,
                            "command": cmd,
                            "stdout": truncate_text(exc.stdout or ""),
                            "stderr": truncate_text(exc.stderr or ""),
                        },
                    )
                    last_error_message = (
                        f"{runner} {role} timed out after {timeout_seconds}s on attempt {attempt}/{retry_attempts}; "
                        f"see {log_path.relative_to(self.root)}"
                    )
                    if attempt < retry_attempts and retry_on_timeout:
                        time.sleep(retry_backoff_seconds * attempt)
                        continue
                    raise HarnessError(last_error_message) from exc
                if completed.returncode != 0:
                    stderr_text = truncate_text(completed.stderr or "")
                    log_path = self.write_runner_log(
                        runner,
                        role,
                        {
                            "runner": runner,
                            "role": role,
                            "started_at": started_at,
                            "attempt": attempt,
                            "retry_attempts": retry_attempts,
                            "elapsed_seconds": elapsed,
                            "returncode": completed.returncode,
                            "command": cmd,
                            "stdout": truncate_text(completed.stdout or ""),
                            "stderr": stderr_text,
                        },
                    )
                    last_error_message = (
                        f"{runner} {role} failed with code {completed.returncode} on attempt {attempt}/{retry_attempts}; "
                        f"see {log_path.relative_to(self.root)}"
                    )
                    if attempt < retry_attempts and self.is_retryable_runner_error(runner, stderr_text):
                        time.sleep(retry_backoff_seconds * attempt)
                        continue
                    raise HarnessError(last_error_message)
                raw = completed.stdout
                self.write_runner_log(
                    runner,
                    role,
                    {
                        "runner": runner,
                        "role": role,
                        "started_at": started_at,
                        "attempt": attempt,
                        "retry_attempts": retry_attempts,
                        "elapsed_seconds": elapsed,
                        "returncode": completed.returncode,
                        "command": cmd,
                        "stdout": truncate_text(completed.stdout or ""),
                        "stderr": truncate_text(completed.stderr or ""),
                    },
                )
                break
            else:
                raise HarnessError(f"unsupported runner: {runner}")
        else:
            raise HarnessError(last_error_message)
        parsed = self.parse_runner_json(raw)
        self.validate_runner_payload(role, parsed)
        return parsed

    def handle_stage_failure(
        self,
        tasks_payload: Dict[str, Any],
        task_id: str,
        stage: str,
        error: Exception,
    ) -> Dict[str, Any]:
        self.update_task(
            tasks_payload,
            task_id,
            status="todo",
            last_result={"stage": stage, "error": str(error)},
        )
        self.save_tasks(tasks_payload)
        return {"status": "halted", "reason": "runner_failed", "task_id": task_id, "stage": stage, "error": str(error)}

    def current_done_count(self, tasks_payload: Dict[str, Any]) -> int:
        return sum(1 for task in tasks_payload["tasks"] if task["status"] in {"done", "verified"})

    def todo_count(self, tasks_payload: Dict[str, Any]) -> int:
        return sum(1 for task in tasks_payload["tasks"] if task["status"] == "todo")

    def todo_warning_threshold(self) -> int:
        value = self.config.get("todo_warning_threshold", 3)
        if not isinstance(value, int) or value < 0:
            raise HarnessError("todo_warning_threshold must be a non-negative integer")
        return value

    def build_todo_warning(self, tasks_payload: Dict[str, Any]) -> Optional[Dict[str, Any]]:
        todo_tasks = [task for task in tasks_payload["tasks"] if task["status"] == "todo"]
        threshold = self.todo_warning_threshold()
        if len(todo_tasks) > threshold:
            return None
        todo_tasks.sort(key=lambda item: (item["priority"], item.get("updated_at", "")))
        return {
            "remaining_todo": len(todo_tasks),
            "threshold": threshold,
            "next_candidates": [task["id"] for task in todo_tasks[:5]],
        }

    def should_run_doc_gardener(self, tasks_payload: Dict[str, Any], changed_files: List[str]) -> bool:
        interval = int(self.config.get("doc_gardening_interval", 2))
        done_count = self.current_done_count(tasks_payload)
        interval_trigger = done_count > 0 and done_count % interval == 0
        architecture_paths = set(self.config.get("architecture_trigger_paths", []))
        path_trigger = any(path in architecture_paths for path in changed_files)
        return interval_trigger or path_trigger

    def ensure_loop_branch(self) -> Optional[str]:
        completed = run_command(["git", "rev-parse", "--abbrev-ref", "HEAD"], self.root)
        if completed.returncode != 0:
            raise HarnessError("failed to determine current branch")
        current_branch = completed.stdout.strip()
        prefix = str(self.config["branch_prefix"])
        if current_branch.startswith(prefix):
            return current_branch
        branch_name = f"{prefix}/{datetime.now(timezone.utc).strftime('%Y%m%d-%H%M%S')}"
        run_command(["git", "checkout", "-b", branch_name], self.root, check=True)
        return branch_name

    def commit_verified_task(self, task: Dict[str, Any]) -> Optional[str]:
        status = run_command(["git", "status", "--porcelain"], self.root, check=True)
        if not status.stdout.strip():
            return None
        run_command(["git", "add", "-A"], self.root, check=True)
        template = str(self.config["commit_message_template"])
        message = template.format(task_id=task["id"], title=task["title"])
        run_command(["git", "commit", "-m", message], self.root, check=True)
        return message

    def current_branch(self) -> str:
        completed = run_command(["git", "rev-parse", "--abbrev-ref", "HEAD"], self.root, check=True)
        branch = completed.stdout.strip()
        if not branch:
            raise HarnessError("failed to determine current branch name")
        return branch

    def auto_push_enabled(self) -> bool:
        return bool(self.config.get("auto_push_after_commit", False))

    def push_remote(self) -> str:
        remote = self.config.get("push_remote", "origin")
        if not isinstance(remote, str) or not remote:
            raise HarnessError("push_remote must be a non-empty string")
        return remote

    def push_current_branch(self) -> Optional[str]:
        branch = self.current_branch()
        remote = self.push_remote()
        run_command(["git", "push", remote, branch], self.root, check=True)
        return branch

    def post_task_refresh_commands(self) -> Dict[str, List[str]]:
        commands = self.config.get("post_task_refresh_commands", {})
        if not isinstance(commands, dict):
            raise HarnessError("post_task_refresh_commands must be an object")
        normalized: Dict[str, List[str]] = {}
        for key, value in commands.items():
            if isinstance(value, list):
                normalized[str(key)] = [str(item) for item in value]
        return normalized

    def infer_refresh_modules(self, changed_files: List[str]) -> List[str]:
        modules = []
        for prefix, module in (
            ("manager/", "manager"),
            ("benchmark/", "benchmark"),
            ("query/", "query"),
        ):
            if any(path.startswith(prefix) for path in changed_files):
                modules.append(module)
        return modules

    def run_post_task_refresh(self, changed_files: List[str]) -> List[Dict[str, Any]]:
        commands_by_module = self.post_task_refresh_commands()
        modules = self.infer_refresh_modules(changed_files)
        results: List[Dict[str, Any]] = []
        for module in modules:
            for command in commands_by_module.get(module, []):
                started = time.monotonic()
                completed = run_command(["/bin/zsh", "-lc", command], self.root)
                elapsed = round(time.monotonic() - started, 3)
                result = {
                    "module": module,
                    "command": command,
                    "returncode": completed.returncode,
                    "elapsed_seconds": elapsed,
                    "stdout": truncate_text(completed.stdout or ""),
                    "stderr": truncate_text(completed.stderr or ""),
                }
                results.append(result)
                if completed.returncode != 0:
                    raise HarnessError(
                        f"post-task refresh failed for {module}: {command} (code {completed.returncode})"
                    )
        return results

    def service_health_checks(self) -> Dict[str, str]:
        checks = self.config.get("service_health_checks", {})
        if not isinstance(checks, dict):
            raise HarnessError("service_health_checks must be an object")
        return {str(key): str(value) for key, value in checks.items()}

    def service_start_commands(self) -> Dict[str, str]:
        commands = self.config.get("service_start_commands", {})
        if not isinstance(commands, dict):
            raise HarnessError("service_start_commands must be an object")
        return {str(key): str(value) for key, value in commands.items()}

    def run_shell_command(self, command: str, *, timeout: Optional[int] = None) -> subprocess.CompletedProcess[str]:
        return run_command(["/bin/zsh", "-lc", command], self.root, timeout=timeout)

    def ensure_local_services(self, changed_files: List[str]) -> List[Dict[str, Any]]:
        checks = self.service_health_checks()
        starters = self.service_start_commands()
        targets = []
        if any(path.startswith("manager/frontend/") for path in changed_files):
            targets.append("manager_frontend")
        if any(path.startswith("benchmark/frontend/") for path in changed_files):
            targets.append("benchmark_frontend")
        if any(path.startswith("manager/") for path in changed_files):
            targets.append("manager_backend")
        if any(path.startswith("benchmark/") for path in changed_files):
            targets.append("benchmark_backend")
        if any(path.startswith("query/") for path in changed_files):
            targets.append("query_backend")

        seen = set()
        ordered_targets = []
        for item in targets:
            if item not in seen:
                seen.add(item)
                ordered_targets.append(item)

        results: List[Dict[str, Any]] = []
        for target in ordered_targets:
            check_command = checks.get(target)
            if not check_command:
                continue
            result = self.run_shell_command(check_command, timeout=10)
            if result.returncode == 0:
                results.append({"service": target, "status": "healthy"})
                continue
            start_command = starters.get(target)
            if start_command:
                self.run_shell_command(start_command, timeout=15)
                time.sleep(3)
                retry = self.run_shell_command(check_command, timeout=10)
                if retry.returncode == 0:
                    results.append({"service": target, "status": "started"})
                    continue
            raise HarnessError(f"service check failed for {target}")
        return results

    def task_context_payload(self, task: Dict[str, Any], extra: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        payload: Dict[str, Any] = {
            "task": task,
            "agents_md": str(self.root / "AGENTS.md"),
            "docs_index": str(self.docs_dir / "README.md"),
            "runbook": str(self.docs_dir / "operations" / "agent-loop-runbook.md"),
        }
        if extra:
            payload.update(extra)
        return payload

    def step(self, runner: str, *, live_runner_check: bool = False) -> Dict[str, Any]:
        doctor_report = self.doctor(live_runner_check=live_runner_check)
        if not doctor_report.ok:
            return {"status": "halted", "reason": "doctor_failed", "issues": doctor_report.issues}
        if self.is_paused():
            return {"status": "halted", "reason": "paused"}
        tasks_payload = self.load_tasks()
        task = self.select_next_task(tasks_payload)
        if not task:
            return {"status": "halted", "reason": "no_tasks"}
        try:
            self.acquire_lock(runner, task["id"])
        except HarnessError as exc:
            return {"status": "halted", "reason": "lock_active", "task_id": task["id"], "error": str(exc)}
        try:
            self.ensure_loop_branch()
            try:
                orchestrator_output = self.invoke_runner(runner, "orchestrator", self.task_context_payload(task))
            except HarnessError as exc:
                return self.handle_stage_failure(tasks_payload, task["id"], "orchestrator", exc)
            self.log_event({"role": "orchestrator", "task_id": task["id"], "payload": orchestrator_output})

            implementer_input = self.task_context_payload(
                task,
                {"orchestrator_output": orchestrator_output},
            )
            try:
                implementer_output = self.invoke_runner(runner, "implementer", implementer_input)
            except HarnessError as exc:
                return self.handle_stage_failure(tasks_payload, task["id"], "implementer", exc)
            self.log_event({"role": "implementer", "task_id": task["id"], "payload": implementer_output})
            task = self.update_task(
                tasks_payload,
                task["id"],
                status="implemented" if implementer_output["status"] == "implemented" else "todo",
                last_result=implementer_output,
            )
            self.save_tasks(tasks_payload)
            if implementer_output["status"] != "implemented":
                return {"status": "halted", "reason": "implementer_failed", "task_id": task["id"]}

            verifier_input = self.task_context_payload(
                task,
                {
                    "orchestrator_output": orchestrator_output,
                    "implementer_output": implementer_output,
                },
            )
            try:
                verifier_output = self.invoke_runner(runner, "verifier", verifier_input)
            except HarnessError as exc:
                return self.handle_stage_failure(tasks_payload, task["id"], "verifier", exc)
            self.log_event({"role": "verifier", "task_id": task["id"], "payload": verifier_output})
            if verifier_output["validation_status"] != "approved":
                attempts = int(task.get("attempts", 0)) + 1
                next_status = "blocked" if attempts >= int(self.config.get("max_task_attempts", 3)) else "todo"
                self.update_task(
                    tasks_payload,
                    task["id"],
                    status=next_status,
                    attempts=attempts,
                    last_result=verifier_output,
                )
                self.save_tasks(tasks_payload)
                return {
                    "status": "rejected",
                    "task_id": task["id"],
                    "attempts": attempts,
                    "next_status": next_status,
                }

            task = self.update_task(
                tasks_payload,
                task["id"],
                status="verified",
                last_result=verifier_output,
            )
            self.save_tasks(tasks_payload)
            changed_files = list(implementer_output.get("files_modified", []))
            if self.should_run_doc_gardener(tasks_payload, changed_files):
                doc_gardener_input = self.task_context_payload(
                    task,
                    {
                        "changed_files": changed_files,
                        "orchestrator_output": orchestrator_output,
                        "implementer_output": implementer_output,
                        "verifier_output": verifier_output,
                    },
                )
                try:
                    doc_output = self.invoke_runner(runner, "doc-gardener", doc_gardener_input)
                except HarnessError as exc:
                    return self.handle_stage_failure(tasks_payload, task["id"], "doc-gardener", exc)
                self.log_event({"role": "doc-gardener", "task_id": task["id"], "payload": doc_output})
            self.update_task(
                tasks_payload,
                task["id"],
                status="done",
                attempts=int(task.get("attempts", 0)),
                last_result=verifier_output,
            )
            self.save_tasks(tasks_payload)
            commit_message = self.commit_verified_task(task)
            pushed_branch = None
            if commit_message and self.auto_push_enabled():
                pushed_branch = self.push_current_branch()
                self.log_event({"role": "git-push", "task_id": task["id"], "branch": pushed_branch})
            refresh_results = []
            refresh_error = None
            service_results = []
            service_error = None
            try:
                refresh_results = self.run_post_task_refresh(changed_files)
                if refresh_results:
                    self.log_event({"role": "post-task-refresh", "task_id": task["id"], "results": refresh_results})
            except HarnessError as exc:
                refresh_error = str(exc)
                self.log_event({"role": "post-task-refresh", "task_id": task["id"], "error": refresh_error})
            try:
                service_results = self.ensure_local_services(changed_files)
                if service_results:
                    self.log_event({"role": "service-refresh", "task_id": task["id"], "results": service_results})
            except HarnessError as exc:
                service_error = str(exc)
                self.log_event({"role": "service-refresh", "task_id": task["id"], "error": service_error})
            todo_warning = self.build_todo_warning(tasks_payload)
            if todo_warning:
                self.log_event({"role": "todo-warning", "task_id": task["id"], "warning": todo_warning})
            return {
                "status": "done",
                "task_id": task["id"],
                "commit": commit_message,
                "pushed_branch": pushed_branch,
                "refresh_results": refresh_results,
                "refresh_error": refresh_error,
                "service_results": service_results,
                "service_error": service_error,
                "todo_warning": todo_warning,
            }
        finally:
            self.release_lock()

    def run(self, runner: str, max_iterations: Optional[int], *, live_runner_check: bool = False) -> int:
        iterations = 0
        waiting_for_lock = False
        while True:
            if max_iterations is not None and iterations >= max_iterations:
                print(json.dumps({"status": "halted", "reason": "max_iterations", "iterations": iterations}, ensure_ascii=True))
                return 0
            if self.is_paused():
                print(json.dumps({"status": "halted", "reason": "paused"}, ensure_ascii=True))
                return 0
            lock_state = self.read_lock_state()
            if lock_state.get("locked"):
                if not waiting_for_lock:
                    print(
                        json.dumps(
                            {
                                "status": "waiting",
                                "reason": "lock_active",
                                "task_id": lock_state.get("task_id"),
                                "runner": lock_state.get("runner"),
                            },
                            ensure_ascii=True,
                        )
                    )
                waiting_for_lock = True
                time.sleep(self.lock_poll_seconds())
                continue
            waiting_for_lock = False
            stray_processes = self.stray_harness_processes(lock_state)
            if stray_processes:
                print(
                    json.dumps(
                        {"status": "halted", "reason": "stale_process", "processes": stray_processes},
                        ensure_ascii=True,
                    )
                )
                return 1
            outcome = self.step(runner, live_runner_check=live_runner_check)
            print(json.dumps(outcome, ensure_ascii=True))
            if outcome["status"] == "done":
                iterations += 1
                continue
            if outcome["status"] == "rejected":
                iterations += 1
                continue
            if outcome["status"] == "halted" and outcome.get("reason") in {"no_tasks", "paused"}:
                return 0
            if outcome["status"] == "halted" and outcome.get("reason") == "lock_active":
                time.sleep(self.lock_poll_seconds())
                continue
            return 1

    def sync_doc_cn(self, *, check: bool = False) -> int:
        failures = 0
        for entry in self.config.get("mirror_docs", []):
            source = self.root / entry["source"]
            target = self.root / entry["target"]
            if not source.exists():
                print(f"missing source mirror doc: {source}", file=sys.stderr)
                failures += 1
                continue
            if not target.exists():
                if check:
                    print(f"missing target mirror doc: {target}", file=sys.stderr)
                    failures += 1
                    continue
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_text("", encoding="utf-8")
            source_hash = hashlib.sha256(source.read_bytes()).hexdigest()[:12]
            marker = f"<!-- MIRROR: {entry['source']} | SOURCE_SHA256: {source_hash} | SYNCED_AT: {utc_now()} -->"
            current_text = target.read_text(encoding="utf-8")
            lines = current_text.splitlines()
            if lines and lines[0].startswith("<!-- MIRROR:"):
                current_marker = lines[0]
                body = "\n".join(lines[1:]).lstrip("\n")
            else:
                current_marker = ""
                body = current_text.lstrip("\n")
            expected_prefix = f"<!-- MIRROR: {entry['source']} | SOURCE_SHA256: {source_hash}"
            if check:
                if not current_marker.startswith(expected_prefix):
                    print(f"mirror out of sync: {target.relative_to(self.root)}", file=sys.stderr)
                    failures += 1
                continue
            new_text = marker + "\n\n" + body.rstrip() + ("\n" if body.strip() else "")
            target.write_text(new_text, encoding="utf-8")
        return failures

    def smoke_runner(self, runner: str, timeout_seconds: Optional[int] = None) -> int:
        payload = {
            "task": {
                "id": "SMOKE",
                "title": "Runner smoke check",
                "module": "docs",
                "type": "documentation",
                "priority": 0,
                "status": "todo",
                "depends_on": [],
                "context_files": [],
                "acceptance_criteria": [],
                "validation_commands": [],
                "attempts": 0,
                "last_result": None,
                "updated_at": utc_now(),
            },
            "agents_md": str(self.root / "AGENTS.md"),
            "docs_index": str(self.docs_dir / "README.md"),
            "runbook": str(self.docs_dir / "operations" / "agent-loop-runbook.md"),
        }
        result = self.invoke_runner(runner, "orchestrator", payload, timeout_override=timeout_seconds)
        print(json.dumps(result, indent=2, ensure_ascii=True))
        return 0


def print_doctor(report: DoctorReport) -> int:
    payload = {"ok": report.ok, "issues": report.issues, "warnings": report.warnings}
    print(json.dumps(payload, indent=2, ensure_ascii=True))
    return 0 if report.ok else 1


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Easy Engine autonomous agent harness")
    subparsers = parser.add_subparsers(dest="command", required=True)

    doctor = subparsers.add_parser("doctor", help="verify repo readiness")
    doctor.add_argument("--live-runner-check", action="store_true", help="invoke the configured runners")

    step = subparsers.add_parser("step", help="run one orchestrator -> implementer -> verifier cycle")
    step.add_argument("--runner", choices=("codex", "claude"), required=True)
    step.add_argument("--live-runner-check", action="store_true", help="invoke the configured runners during doctor")

    run = subparsers.add_parser("run", help="repeat step until halt conditions or max iterations")
    run.add_argument("--runner", choices=("codex", "claude"), required=True)
    run.add_argument("--max-iterations", type=int, default=None)
    run.add_argument("--live-runner-check", action="store_true", help="invoke the configured runners during doctor")

    start = subparsers.add_parser("start", help="launch a detached managed run loop")
    start.add_argument("--runner", choices=("codex", "claude"), required=True)
    start.add_argument("--max-iterations", type=int, default=None)
    start.add_argument("--live-runner-check", action="store_true", help="invoke the configured runners during doctor")

    subparsers.add_parser("status", help="show detached loop process and lock status")
    subparsers.add_parser("stop", help="stop the detached managed run loop")

    sync_doc_cn = subparsers.add_parser("sync-doc-cn", help="refresh Chinese mirror metadata")
    sync_doc_cn.add_argument("--check", action="store_true", help="fail if mirror metadata is stale")

    smoke_runner = subparsers.add_parser("smoke-runner", help="run a minimal orchestrator smoke test")
    smoke_runner.add_argument("--runner", choices=("codex", "claude"), required=True)
    smoke_runner.add_argument("--timeout-seconds", type=int, default=None)
    return parser


def main(argv: Optional[Sequence[str]] = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    harness = Harness()
    if args.command == "doctor":
        return print_doctor(harness.doctor(live_runner_check=args.live_runner_check))
    if args.command == "step":
        outcome = harness.step(args.runner, live_runner_check=args.live_runner_check)
        print(json.dumps(outcome, indent=2, ensure_ascii=True))
        return 0 if outcome["status"] == "done" else 1
    if args.command == "run":
        return harness.run(args.runner, args.max_iterations, live_runner_check=args.live_runner_check)
    if args.command == "start":
        payload = harness.start_managed_run(
            args.runner,
            args.max_iterations,
            live_runner_check=args.live_runner_check,
        )
        print(json.dumps(payload, indent=2, ensure_ascii=True))
        return 0
    if args.command == "status":
        print(json.dumps(harness.managed_loop_status(), indent=2, ensure_ascii=True))
        return 0
    if args.command == "stop":
        print(json.dumps(harness.stop_managed_run(), indent=2, ensure_ascii=True))
        return 0
    if args.command == "sync-doc-cn":
        return harness.sync_doc_cn(check=args.check)
    if args.command == "smoke-runner":
        return harness.smoke_runner(args.runner, timeout_seconds=args.timeout_seconds)
    return 1


if __name__ == "__main__":
    sys.exit(main())
