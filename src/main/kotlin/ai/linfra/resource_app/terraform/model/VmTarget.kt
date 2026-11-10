package ai.linfra.resource_app.terraform.model

import ai.linfra.resource_app.ranker.model.CatalogEntry
import java.util.UUID

/**
 * Everything a module's variables may be derived from. No free-form strings: ids are UUIDs, the entry comes from
 * the catalog (not from the intent), and the disk size is a range-checked number.
 */
data class VmTarget(
    val intentId: UUID,
    val tenantId: UUID,
    val entry: CatalogEntry,
    val diskGb: Int,
)
