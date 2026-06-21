-- V25: Replace boolean active on designs with a DesignStatus enum column.
--
-- Migration plan:
--   1. Add status column (nullable first, so existing rows are not rejected)
--   2. Backfill: active=true → PUBLISHED, active=false → DRAFT
--   3. Set NOT NULL constraint
--   4. Drop the old active column
--   5. Add index for common storefront query (status = PUBLISHED)

ALTER TABLE designs ADD COLUMN status VARCHAR(20);

UPDATE designs SET status = CASE
    WHEN active = TRUE THEN 'PUBLISHED'
    ELSE 'DRAFT'
END;

ALTER TABLE designs ALTER COLUMN status SET NOT NULL;
ALTER TABLE designs ALTER COLUMN status SET DEFAULT 'DRAFT';

ALTER TABLE designs DROP COLUMN active;

CREATE INDEX idx_designs_status ON designs(status);
CREATE INDEX idx_designs_collection_status ON designs(collection_id, status);
