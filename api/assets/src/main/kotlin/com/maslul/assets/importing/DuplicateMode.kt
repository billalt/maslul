package com.maslul.assets.importing

enum class DuplicateMode {
    // Existing row untouched; reported as DUPLICATE_SKIPPED with the matched ServicePoint id.
    SKIP,

    // Overwrite non-null incoming fields on the matched ServicePoint; a blank cell never
    // nulls out an existing value; tenant_id, id, and created_at are never touched.
    UPDATE,

    // Abort this row only (see ServicePointImportRowProcessor - each row is its own transaction).
    FAIL,
}
