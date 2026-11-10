package ai.linfra.resource_app.ranker.model

/** NETWORK: separate volume, billed per GB, any size. LOCAL_NVME: disk that comes with the instance type, fixed size. */
enum class StorageKind { LOCAL_NVME, NETWORK }
