ALTER TABLE designs
    ADD COLUMN is_new_arrival BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN view_count     INTEGER NOT NULL DEFAULT 0;

CREATE INDEX idx_designs_new_arrival ON designs(is_new_arrival) WHERE is_new_arrival = TRUE;
CREATE INDEX idx_designs_view_count  ON designs(view_count DESC);
