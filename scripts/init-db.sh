#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAVA8_WRAPPER="${ROOT_DIR}/scripts/with-java8.sh"

usage() {
  echo "Usage: scripts/init-db.sh <dev|test|pro> [manager] [benchmark]" >&2
}

main_class_for() {
  case "$1" in
    manager)
      echo "com.smartbi.engine.ManagerDbInitApplication"
      ;;
    benchmark)
      echo "com.smartbi.benchmark.BenchmarkDbInitApplication"
      ;;
    *)
      return 1
      ;;
  esac
}

run_module() {
  local profile="$1"
  local module="$2"
  local main_class
  local mvn_goal="org.codehaus.mojo:exec-maven-plugin:3.1.0:java"

  main_class="$(main_class_for "${module}")"
  echo "Initializing ${module} schema with profile ${profile}..."
  bash "${JAVA8_WRAPPER}" mvn -q -f "${ROOT_DIR}/${module}/pom.xml" \
    -DskipTests \
    -Dspring.profiles.active="${profile}" \
    -Dexec.mainClass="${main_class}" \
    -Dexec.classpathScope="runtime" \
    "${mvn_goal}"
}

if [[ $# -lt 1 ]]; then
  usage
  exit 1
fi

profile="$1"
shift

case "${profile}" in
  dev|test|pro)
    ;;
  *)
    usage
    exit 1
    ;;
esac

if [[ $# -eq 0 ]]; then
  modules=(manager benchmark)
else
  modules=("$@")
fi

for module in "${modules[@]}"; do
  case "${module}" in
    manager|benchmark)
      run_module "${profile}" "${module}"
      ;;
    *)
      usage
      exit 1
      ;;
  esac
done

echo "Database initialization complete."
