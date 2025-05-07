package com.globallock.domain;

public record LeaseResult(
        boolean success,
        String operation,
        String message,
        Lease lease,
        Lease conflictingLease
) {
    public static LeaseResult success(String operation, String message, Lease lease) {
        return new LeaseResult(true, operation, message, lease, null);
    }

    public static LeaseResult failure(String operation, String message, Lease conflictingLease) {
        return new LeaseResult(false, operation, message, null, conflictingLease);
    }
}

