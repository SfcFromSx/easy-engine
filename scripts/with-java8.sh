#!/usr/bin/env bash
set -euo pipefail

usage() {
  echo "Usage: scripts/with-java8.sh <command> [args...]" >&2
}

detect_java_home() {
  if [[ -n "${JAVA8_HOME:-}" ]]; then
    echo "${JAVA8_HOME}"
    return 0
  fi

  if [[ -n "${JAVA_HOME:-}" ]]; then
    echo "${JAVA_HOME}"
    return 0
  fi

  if [[ -x /usr/libexec/java_home ]]; then
    local macos_java_home
    macos_java_home="$(/usr/libexec/java_home -v 1.8 2>/dev/null || true)"
    if [[ -n "${macos_java_home}" ]]; then
      echo "${macos_java_home}"
      return 0
    fi
  fi

  local javac_bin
  javac_bin="$(command -v javac || true)"
  if [[ -n "${javac_bin}" ]]; then
    cd "$(dirname "${javac_bin}")/.." && pwd -P
    return 0
  fi

  return 1
}

ensure_java8_home() {
  local candidate="$1"

  if [[ ! -x "${candidate}/bin/java" || ! -x "${candidate}/bin/javac" ]]; then
    echo "Java 8 JDK is required, but ${candidate} does not provide both bin/java and bin/javac." >&2
    echo "Set JAVA8_HOME (preferred) or JAVA_HOME to a Java 8 JDK before running Maven-backed Easy Engine commands." >&2
    exit 1
  fi

  local version_line
  version_line="$("${candidate}/bin/java" -version 2>&1 | head -1)"
  case "${version_line}" in
    *\"1.8.*\"*)
      ;;
    *)
      echo "Java 8 JDK is required, but ${candidate} reports: ${version_line}" >&2
      echo "Set JAVA8_HOME (preferred) or JAVA_HOME to a Java 8 JDK before running Maven-backed Easy Engine commands." >&2
      exit 1
      ;;
  esac
}

if [[ $# -eq 0 ]]; then
  usage
  exit 1
fi

JAVA8_RESOLVED_HOME="$(detect_java_home || true)"
if [[ -z "${JAVA8_RESOLVED_HOME}" ]]; then
  echo "Java 8 JDK is required, but no candidate home was detected." >&2
  echo "Set JAVA8_HOME (preferred) or JAVA_HOME to a Java 8 JDK before running Maven-backed Easy Engine commands." >&2
  exit 1
fi

ensure_java8_home "${JAVA8_RESOLVED_HOME}"

export JAVA_HOME="${JAVA8_RESOLVED_HOME}"
export PATH="${JAVA_HOME}/bin:${PATH}"

exec "$@"
