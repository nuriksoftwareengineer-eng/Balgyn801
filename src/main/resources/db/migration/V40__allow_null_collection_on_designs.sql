-- Archived designs used in orders cannot be physically deleted,
-- but they must not block collection / category deletion forever.
-- Allow collection_id to be NULL so orphaned archived designs can persist
-- without being tied to a deleted collection.
ALTER TABLE designs ALTER COLUMN collection_id DROP NOT NULL;
