# Design Status Migration Audit

**Migration:** `V25__design_status_enum.sql`
**Audit date:** 2026-06-20
**Status:** SAFE — no action required

---

## What the migration does

Replaces the boolean `active` column on the `designs` table with a `VARCHAR(20) status` column backed by the `DesignStatus` enum (`DRAFT | READY | PUBLISHED | ARCHIVED`).

```sql
-- 1. Add nullable column (avoids NOT NULL violation on existing rows)
ALTER TABLE designs ADD COLUMN status VARCHAR(20);

-- 2. Backfill from existing boolean
UPDATE designs SET status = CASE
    WHEN active = TRUE THEN 'PUBLISHED'
    ELSE 'DRAFT'
END;

-- 3. Apply NOT NULL + default
ALTER TABLE designs ALTER COLUMN status SET NOT NULL;
ALTER TABLE designs ALTER COLUMN status SET DEFAULT 'DRAFT';

-- 4. Drop old column
ALTER TABLE designs DROP COLUMN active;

-- 5. Indexes for storefront queries
CREATE INDEX idx_designs_status ON designs(status);
CREATE INDEX idx_designs_collection_status ON designs(collection_id, status);
```

---

## Safety checklist

| Check | Result |
|-------|--------|
| Column added nullable before backfill | ✅ Yes — prevents NOT NULL constraint failure on existing rows |
| Backfill covers all rows | ✅ Yes — CASE covers both `TRUE` and all other values (NULL/FALSE → DRAFT) |
| NOT NULL applied after backfill | ✅ Correct order |
| Default set for new rows | ✅ `DEFAULT 'DRAFT'` |
| Old column removed cleanly | ✅ DROP COLUMN after data migration complete |
| Indexes present for common queries | ✅ Two indexes added |
| Enum values in migration match Java enum | ✅ `PUBLISHED`, `DRAFT` — both present in `DesignStatus` |

---

## Java entity alignment

`Design.java` line 46–48:
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private DesignStatus status = DesignStatus.DRAFT;
```

- `@Enumerated(EnumType.STRING)` — Hibernate stores enum name as VARCHAR, matching the SQL column type.
- `nullable = false` — matches `ALTER COLUMN status SET NOT NULL`.
- `length = 20` — matches `VARCHAR(20)`.
- Default `DRAFT` — matches SQL `DEFAULT 'DRAFT'`.
- No `active` field remains in the entity — clean removal.

---

## Hibernate startup safety

Hibernate will:
1. Find `status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'` in the schema.
2. Map it to `DesignStatus` via `EnumType.STRING` — all four enum values (`DRAFT`, `READY`, `PUBLISHED`, `ARCHIVED`) are valid VARCHAR values.
3. Not attempt DDL changes (assuming `ddl-auto = validate` or `none` in production).

Any row with a `status` value not in the Java enum would throw a `JDBCException` at read time — but since the backfill only writes `PUBLISHED` or `DRAFT`, and no other process writes to this column, this cannot occur.

---

## Data migration correctness

| Before (active) | After (status) | Correct? |
|----------------|----------------|----------|
| `true` | `PUBLISHED` | ✅ Visible in catalog |
| `false` | `DRAFT` | ✅ Hidden, awaiting publish |
| `NULL` (if any) | `DRAFT` | ✅ Safe fallback |

`READY` and `ARCHIVED` are new states with no prior equivalent — they start with zero rows, which is correct.

---

## Conclusion

The V25 migration is **safe to run on existing databases**. The nullable-add → backfill → NOT NULL → drop sequence is the standard safe pattern for replacing a boolean flag with an enum column. No data loss, no downtime risk on small-to-medium tables.
