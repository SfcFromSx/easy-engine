#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MYSQL_HOST="${MYSQL_HOST:-localhost}"
MYSQL_PORT="${MYSQL_PORT:-3307}"
MYSQL_DB="${MYSQL_DB:-engine_db}"
MYSQL_USER="${MYSQL_USER:-engine}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-engine123}"
JDBC_URL="jdbc:mysql://${MYSQL_HOST}:${MYSQL_PORT}/${MYSQL_DB}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

usage() {
  echo "Usage: scripts/init-db.sh [manager] [benchmark]" >&2
}

run_module() {
  local module="$1"

  echo "Initializing ${module} schema..."
  mvn -q -f "${ROOT_DIR}/${module}/pom.xml" \
    -DskipTests \
    -Dflyway.url="${JDBC_URL}" \
    -Dflyway.user="${MYSQL_USER}" \
    -Dflyway.password="${MYSQL_PASSWORD}" \
    compile flyway:migrate
}

if [[ $# -eq 0 ]]; then
  modules=(manager benchmark)
else
  modules=("$@")
fi

for module in "${modules[@]}"; do
  case "${module}" in
    manager|benchmark)
      run_module "${module}"
      ;;
    *)
      usage
      exit 1
      ;;
  esac
done

echo "Database initialization complete."
