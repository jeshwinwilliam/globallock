package com.globallock.domain;

public record Lease(
        String resource,
        String ownerId,
        long token,
        long ttlMillis,
        long acquiredAtEpochMillis,
        long expiresAtEpochMillis
) {
    public boolean isExpired(long nowEpochMillis) {
        return expiresAtEpochMillis <= nowEpochMillis;
    }

    public long remainingMillis(long nowEpochMillis) {
        return Math.max(0L, expiresAtEpochMillis - nowEpochMillis);
    }
}

