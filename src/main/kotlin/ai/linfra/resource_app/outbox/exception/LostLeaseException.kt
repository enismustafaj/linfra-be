package ai.linfra.resource_app.outbox.exception

import ai.linfra.resource_app.outbox.model.Claim

class LostLeaseException(claim: Claim) :
    RuntimeException("Lease on job ${claim.jobId} lost: token ${claim.fencingToken} is no longer current")
