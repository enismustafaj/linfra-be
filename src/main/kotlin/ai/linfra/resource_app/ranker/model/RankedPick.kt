package ai.linfra.resource_app.ranker.model

/** The ranker's decision plus everything needed to explain it. */
data class RankedPick(
    val entry: CatalogEntry,
    val cost: CostBreakdown,
    val diskGb: Int,
    val preferencesMet: List<String>,
    val preferencesMissed: List<String>,
)
