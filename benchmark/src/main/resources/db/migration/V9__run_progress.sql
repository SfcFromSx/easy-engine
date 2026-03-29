-- Add current_progress and total_target to benchmark_run for real-time telemetry
ALTER TABLE benchmark_run ADD COLUMN IF NOT EXISTS current_progress INTEGER DEFAULT 0;
ALTER TABLE benchmark_run ADD COLUMN IF NOT EXISTS total_target INTEGER DEFAULT 0;
