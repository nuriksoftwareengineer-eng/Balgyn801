-- V26: Catalog hardening
--
-- 1. Pre-flight check: abort if duplicate (design_id, garment_type) exist.
-- 2. Add UNIQUE constraint on (design_id, garment_type).
-- 3. Add sort_order, published_at, archived_at to designs.
-- 4. Add sort_order to design_garments.
-- 5. Back-fill published_at for already-published designs.
-- 6. Add indexes for sort and publication queries.

-- ── 1. Pre-flight: fail fast if duplicates exist ──────────────────────────────
DO $$
DECLARE dup_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO dup_count
    FROM (
        SELECT design_id, garment_type
        FROM design_garments
        GROUP BY design_id, garment_type
        HAVING COUNT(*) > 1
    ) sub;
    IF dup_count > 0 THEN
        RAISE EXCEPTION
            'V26 aborted: % duplicate (design_id, garment_type) combination(s) found in design_garments. '
            'Resolve duplicates before running this migration. '
            'Query: SELECT design_id, garment_type, COUNT(*) FROM design_garments GROUP BY design_id, garment_type HAVING COUNT(*) > 1',
            dup_count;
    END IF;
END $$;

-- ── 2. UNIQUE constraint on (design_id, garment_type) ────────────────────────
ALTER TABLE design_garments
    ADD CONSTRAINT uq_design_garment_type UNIQUE (design_id, garment_type);

-- ── 3. New columns on designs ─────────────────────────────────────────────────
ALTER TABLE designs
    ADD COLUMN IF NOT EXISTS sort_order   INTEGER,
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS archived_at  TIMESTAMP;

-- ── 4. New column on design_garments ─────────────────────────────────────────
ALTER TABLE design_garments
    ADD COLUMN IF NOT EXISTS sort_order INTEGER;

-- ── 5. Back-fill published_at for existing PUBLISHED designs ──────────────────
-- Use created_at as best-effort proxy since the original publish time is unknown.
UPDATE designs
SET published_at = created_at
WHERE status = 'PUBLISHED'
  AND published_at IS NULL;

-- ── 6. Indexes ────────────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_designs_sort
    ON designs (collection_id, sort_order NULLS LAST);

CREATE INDEX IF NOT EXISTS idx_designs_published_at
    ON designs (published_at DESC)
    WHERE status = 'PUBLISHED';
