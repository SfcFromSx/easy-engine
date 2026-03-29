-- 每次压测结束写入任务快照与结构化评价 JSON，供管理端展示与 JDBC 迭代对比
ALTER TABLE benchmark_run ADD COLUMN IF NOT EXISTS job_snapshot_json TEXT;
ALTER TABLE benchmark_run ADD COLUMN IF NOT EXISTS evaluation_json TEXT;
