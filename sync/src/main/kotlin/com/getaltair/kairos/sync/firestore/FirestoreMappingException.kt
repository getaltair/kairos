package com.getaltair.kairos.sync.firestore

class FirestoreMappingException(entityType: String, field: String, cause: Throwable? = null) :
    RuntimeException(
        "Failed to map Firestore document to $entityType: invalid or missing field '$field'",
        cause
    )
