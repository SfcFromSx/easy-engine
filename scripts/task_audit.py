#!/usr/bin/env python3
"""Audit the task ledger against governance rules."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
TASKS_PATH = ROOT / "tasks.md"
TASKS_DONE_PATH = ROOT / "tasks-done.md"
CONFIG_PATH = ROOT / ".agent" / "config.json"
LEGACY_INBOX_ID = "INBOX-20260331-001"
LEGACY_MISSING_COMMIT_IDS = {
    "ARCH-011",
    "ARCH-012",
    "ARCH-013",
    "ARCH-014",
    "BENCH-ACTIVE-001",
    "BENCH-DATA-001",
    "BENCH-RUNS-API-001",
    "BENCH-REVIEW-001",
    "BENCH-TEST-001",
    "BENCH-UX-005",
    "MGR-BUG-001",
    "MGR-DASH-002",
    "MGR-REVIEW-001",
    "MGR-TEST-001",
    "MGR-UX-003",
    "QUERY-REVIEW-001",
}


@dataclass
class TaskBlock:
    task_id: str
    status: str
    attempts: int
    block: str


def load_max_attempts() -> int:
    config = json.loads(CONFIG_PATH.read_text())
    return int(config.get("max_task_attempts", 3))


def load_tasks() -> str:
    return TASKS_PATH.read_text()


def load_done_archive() -> str:
    return TASKS_DONE_PATH.read_text()


def split_active_section(text: str) -> str:
    todo_match = re.search(r"^## Todo$", text, re.MULTILINE)
    if not todo_match:
        raise ValueError("tasks.md is missing ## Todo heading")
    todo_start = todo_match.start()
    return text[todo_start:]


def parse_todo_blocks(todo_section: str) -> list[TaskBlock]:
    matches = list(re.finditer(r"^### ([A-Z0-9-]+)\n", todo_section, re.MULTILINE))
    blocks: list[TaskBlock] = []
    for index, match in enumerate(matches):
        start = match.start()
        end = matches[index + 1].start() if index + 1 < len(matches) else len(todo_section)
        block = todo_section[start:end].strip()
        task_id = match.group(1)
        status_match = re.search(r"- \*\*Status:\*\* (\w+)", block)
        attempts_match = re.search(r"- \*\*Attempts:\*\* (\d+)", block)
        status = status_match.group(1) if status_match else "missing"
        attempts = int(attempts_match.group(1)) if attempts_match else 0
        blocks.append(TaskBlock(task_id=task_id, status=status, attempts=attempts, block=block))
    return blocks


def parse_done_ids(done_section: str) -> list[str]:
    done_ids: list[str] = []
    for line in done_section.splitlines():
        if not line.startswith("| "):
            continue
        if line.startswith("| ID ") or line.startswith("|----"):
            continue
        parts = [part.strip() for part in line.strip("|").split("|")]
        if parts and parts[0]:
            done_ids.append(parts[0])
    return done_ids


def latest_progress_entry(block: str) -> str:
    entries = list(re.finditer(r"^\*\*\d{4}-\d{2}-\d{2} — [^\n]+\*\*$", block, re.MULTILINE))
    if not entries:
        return ""
    last = entries[-1]
    next_start = len(block)
    return block[last.start():next_start]


def git_subjects() -> str:
    return subprocess.check_output(
        ["git", "log", "--all", "--format=%s"],
        cwd=ROOT,
        text=True,
    )


def audit() -> tuple[list[str], list[str]]:
    text = load_tasks()
    done_text = load_done_archive()
    todo_section = split_active_section(text)
    todo_blocks = parse_todo_blocks(todo_section)
    done_ids = parse_done_ids(done_text)
    max_attempts = load_max_attempts()
    issues: list[str] = []
    notes: list[str] = []

    if re.search(r"^## Done$", text, re.MULTILINE):
        issues.append("tasks.md: active ledger still contains a ## Done section; completed tasks must live in tasks-done.md")

    todo_ids = [block.task_id for block in todo_blocks]
    for block in todo_blocks:
        if block.status == "done":
            issues.append(f"{block.task_id}: task block is still under ## Todo with status done")
        if block.status == "blocked" and block.attempts >= max_attempts:
            latest = latest_progress_entry(block.block)
            if "Next action:" not in latest:
                issues.append(f"{block.task_id}: blocked task at max attempts is missing Next action in latest progress entry")
            if "Escalation:" not in latest:
                issues.append(f"{block.task_id}: blocked task at max attempts is missing Escalation in latest progress entry")

    duplicates = sorted(set(todo_ids) & set(done_ids))
    for task_id in duplicates:
        issues.append(f"{task_id}: task id appears in both active tasks.md and tasks-done.md")

    subjects = git_subjects()
    missing_commit_ids = sorted(task_id for task_id in done_ids if task_id not in subjects)
    legacy_missing = sorted(task_id for task_id in missing_commit_ids if task_id in LEGACY_MISSING_COMMIT_IDS)
    unexpected_missing = sorted(task_id for task_id in missing_commit_ids if task_id not in LEGACY_MISSING_COMMIT_IDS)

    for task_id in unexpected_missing:
        issues.append(f"{task_id}: tasks-done.md entry has no git commit subject containing the task id")

    if legacy_missing:
        notes.append(
            "Grandfathered legacy Done rows without task-id commit subjects: "
            + ", ".join(legacy_missing)
            + f" ({LEGACY_INBOX_ID})"
        )

    stale_allowlist = sorted(task_id for task_id in LEGACY_MISSING_COMMIT_IDS if task_id not in done_ids)
    if stale_allowlist:
        notes.append(
            "Legacy allowlist ids no longer present in tasks-done.md: " + ", ".join(stale_allowlist)
        )

    return issues, notes


def main() -> int:
    parser = argparse.ArgumentParser(description="Audit tasks.md governance rules")
    parser.add_argument("--check", action="store_true", help="exit non-zero on audit failures")
    args = parser.parse_args()

    issues, notes = audit()
    for note in notes:
        print(f"NOTE: {note}")

    if issues:
        print("FAIL: task audit found governance issues:")
        for issue in issues:
            print(f"- {issue}")
        return 1 if args.check else 0

    print("PASS: task ledger audit clean")
    return 0


if __name__ == "__main__":
    sys.exit(main())
