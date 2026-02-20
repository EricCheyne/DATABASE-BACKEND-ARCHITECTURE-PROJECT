-- V1: Initial schema with high-throughput optimizations

-- 1. Create partitioning-ready table
-- Partitioning strategy: Range on created_at for efficient cleanup and local index maintenance
CREATE TABLE tasks (
    id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    retry_count INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (id, created_at)
) PARTITION BY RANGE (created_at);

-- 2. Create initial partitions for current and near future
CREATE TABLE tasks_y2026_m02 PARTITION OF tasks
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');

CREATE TABLE tasks_y2026_m03 PARTITION OF tasks
    FOR VALUES FROM ('2026-03-01') TO ('2026-04-01');

-- Default partition for safety (though ideally should be pre-created)
CREATE TABLE tasks_default PARTITION OF tasks DEFAULT;

-- 3. JSONB Indexing
-- GIN index for full payload searchability with high performance
CREATE INDEX idx_tasks_payload_gin ON tasks USING GIN (payload);

-- 4. Partial Indexing
-- High-throughput optimization: Only index tasks that are currently active or need retry.
-- This keeps the index small and fast for the most critical queries.
CREATE INDEX idx_tasks_active_status ON tasks (status) 
WHERE status IN ('PENDING', 'PROCESSING', 'RETRYING');

-- 5. Query Optimization: Composite Index
-- For dashboard/listing queries sorted by creation time
CREATE INDEX idx_tasks_status_created_at ON tasks (status, created_at);

-- 6. Trigger for updated_at maintenance
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_tasks_updated_at
    BEFORE UPDATE ON tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
