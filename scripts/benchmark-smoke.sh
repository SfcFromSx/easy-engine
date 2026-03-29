#!/usr/bin/env bash
# 压测冒烟（可选，面向 CI/无人值守）：日常请以 Benchmark 前端「控制台」发起压测并在「运行记录」查看结构化评价。
# 依赖 Kylin 17070、Redis 6380、Postgres 5433、benchmark 8091。
set -euo pipefail

JOB_NAME="${BENCHMARK_SMOKE_JOB:-benchmark-smoke-global}"
BENCH_URL="${BENCHMARK_URL:-http://127.0.0.1:8091}"
MAX_WAIT_SEC="${BENCHMARK_SMOKE_WAIT_SEC:-300}"
POLL_SEC="${BENCHMARK_SMOKE_POLL_SEC:-2}"

need_cmd() { command -v "$1" >/dev/null 2>&1 || { echo "missing command: $1" >&2; exit 1; }; }
need_cmd curl
need_cmd jq

echo "== 依赖探测（失败则退出） =="
curl -sf -m 5 "http://127.0.0.1:17070/kylin/api/user/authentication" -u 'ADMIN:KYLIN' >/dev/null \
  || { echo "Kylin 不可用: http://127.0.0.1:17070" >&2; exit 1; }
# benchmark-smoke-global 已绑定仅 Kylin 测试集；若需校验 Presto 再设 BENCHMARK_SMOKE_REQUIRE_PRESTO=1
if [[ "${BENCHMARK_SMOKE_REQUIRE_PRESTO:-}" == "1" ]]; then
  curl -sf -m 3 "http://127.0.0.1:18081/v1/info" >/dev/null \
    || { echo "Presto 不可用: http://127.0.0.1:18081" >&2; exit 1; }
fi
if command -v redis-cli >/dev/null 2>&1; then
  redis-cli -h 127.0.0.1 -p 6380 ping 2>/dev/null | grep -q PONG || REDIS_FAIL=1
else
  docker exec engine-redis redis-cli ping 2>/dev/null | grep -q PONG || REDIS_FAIL=1
fi
[[ -z "${REDIS_FAIL:-}" ]] || { echo "Redis 不可用: 127.0.0.1:6380（或 docker 容器 engine-redis）" >&2; exit 1; }
if command -v pg_isready >/dev/null 2>&1; then
  pg_isready -h 127.0.0.1 -p 5433 -U engine -d engine_db >/dev/null 2>&1 || PG_FAIL=1
else
  docker exec engine-db pg_isready -U engine -d engine_db >/dev/null 2>&1 || PG_FAIL=1
fi
[[ -z "${PG_FAIL:-}" ]] || { echo "Postgres 不可用: 127.0.0.1:5433 engine_db" >&2; exit 1; }
curl -sf -m 3 "${BENCH_URL}/api/v1/jobs" >/dev/null \
  || { echo "Benchmark API 不可用: ${BENCH_URL}" >&2; exit 1; }

echo "== 解析任务: ${JOB_NAME} =="
JOB_ID="$(curl -sf "${BENCH_URL}/api/v1/jobs" | jq -r ".[] | select(.name==\"${JOB_NAME}\") | .id" | head -1)"
if [[ -z "${JOB_ID}" || "${JOB_ID}" == "null" ]]; then
  echo "未找到名为 \"${JOB_NAME}\" 的任务（请先执行 Flyway V5 或在前端创建）" >&2
  exit 1
fi
echo "jobId=${JOB_ID}"

echo "== 启动压测 =="
RUN_JSON="$(curl -sf -X POST "${BENCH_URL}/api/v1/runs/start" \
  -H 'Content-Type: application/json' \
  -d "{\"jobId\": ${JOB_ID}}")"
RUN_ID="$(echo "${RUN_JSON}" | jq -r '.id')"
if [[ -z "${RUN_ID}" || "${RUN_ID}" == "null" ]]; then
  echo "启动失败: ${RUN_JSON}" >&2
  exit 1
fi
echo "runId=${RUN_ID}"

echo "== 轮询运行状态（最多 ${MAX_WAIT_SEC}s） =="
deadline=$((SECONDS + MAX_WAIT_SEC))
status=""
while (( SECONDS < deadline )); do
  RUN="$(curl -sf "${BENCH_URL}/api/v1/runs/${RUN_ID}")"
  status="$(echo "${RUN}" | jq -r '.status')"
  if [[ "${status}" == "COMPLETED" || "${status}" == "FAILED" ]]; then
    echo "${RUN}" | jq .
    err="$(echo "${RUN}" | jq -r '.errorCount // 0')"
    succ="$(echo "${RUN}" | jq -r '.successCount // 0')"
    total="$(echo "${RUN}" | jq -r '.totalQueries // 0')"
    sample="$(echo "${RUN}" | jq -r '.errorSample // empty')"
    if [[ "${err}" -ne 0 ]]; then
      echo "FAIL: errorCount=${err} successCount=${succ} totalQueries=${total}" >&2
      [[ -n "${sample}" ]] && echo "error_sample: ${sample}" >&2
      exit 1
    fi
    echo "OK: errorCount=0 successCount=${succ} totalQueries=${total}"
    exit 0
  fi
  sleep "${POLL_SEC}"
done

echo "TIMEOUT waiting for run ${RUN_ID}, lastStatus=${status}" >&2
exit 1
