#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import shutil
import sys
from pathlib import Path

try:
    import tomllib
except ModuleNotFoundError:  # pragma: no cover
    tomllib = None


def toml_literal(value):
    if isinstance(value, bool):
        return "true" if value else "false"
    if isinstance(value, int):
        return str(value)
    if value is None:
        return '""'
    escaped = str(value).replace("\\", "\\\\").replace('"', '\\"')
    return f'"{escaped}"'


def build_minimal_config(source: dict) -> str:
    lines: list[str] = []
    root_keys = [
        "model_provider",
        "model",
        "model_reasoning_effort",
        "model_context_window",
        "model_auto_compact_token_limit",
        "service_tier",
    ]
    for key in root_keys:
        if key in source:
            lines.append(f"{key} = {toml_literal(source[key])}")
    provider_name = source.get("model_provider")
    providers = source.get("model_providers", {})
    if provider_name and isinstance(providers, dict) and provider_name in providers:
        provider = providers[provider_name]
        lines.append("")
        lines.append(f'[model_providers.{provider_name}]')
        for key, value in provider.items():
            if key == "requires_openai_auth":
                value = False
            lines.append(f"{key} = {toml_literal(value)}")
    return "\n".join(lines).strip() + "\n"


def ensure_codex_home(repo_root: Path) -> Path:
    original_home = Path.home()
    source_codex = original_home / ".codex"
    isolated_home = repo_root / ".agent" / "runtime" / "codex-home"
    target_codex = isolated_home / ".codex"
    target_codex.mkdir(parents=True, exist_ok=True)

    auth_source = source_codex / "auth.json"
    auth_target = target_codex / "auth.json"
    if auth_source.exists():
        shutil.copy2(auth_source, auth_target)

    config_source = source_codex / "config.toml"
    config_target = target_codex / "config.toml"
    if config_source.exists() and tomllib is not None:
        parsed = tomllib.loads(config_source.read_text(encoding="utf-8"))
        config_target.write_text(build_minimal_config(parsed), encoding="utf-8")
    elif not config_target.exists():
        config_target.write_text("", encoding="utf-8")

    return isolated_home


def main() -> int:
    repo_root = Path(__file__).resolve().parents[1]
    isolated_home = ensure_codex_home(repo_root)

    env = os.environ.copy()
    env["HOME"] = str(isolated_home)
    env["CODEX_HOME"] = str(isolated_home / ".codex")

    cmd = [
        "codex",
        *sys.argv[1:],
        "-c",
        "mcp_servers={}",
        "-c",
        "plugins={}",
        "-c",
        "features.multi_agent=false",
    ]
    os.execvpe(cmd[0], cmd, env)


if __name__ == "__main__":
    raise SystemExit(main())
