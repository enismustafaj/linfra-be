package ai.linfra.resource_app.ranker.model

/**
 * BURSTABLE: CPU shares, can be starved under sustained load (like AWS t3). DEDICATED: pinned cores (like AWS c/m
 * families). SHARED: oversubscribed vCPUs without credits (like Hetzner cx), for providers added later.
 */
enum class CpuModel { SHARED, BURSTABLE, DEDICATED }
