package com.globallock.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public final class MetricsRegistry {
    private final AtomicLong acquireRequests = new AtomicLong();
    private final AtomicLong acquireSuccess = new AtomicLong();
    private final AtomicLong renewRequests = new AtomicLong();
    private final AtomicLong renewSuccess = new AtomicLong();
    private final AtomicLong releaseRequests = new AtomicLong();
    private final AtomicLong releaseSuccess = new AtomicLong();
    private final AtomicLong conflicts = new AtomicLong();
    private final AtomicLong expirations = new AtomicLong();

    public void recordAcquireRequest() {
        acquireRequests.incrementAndGet();
    }

    public void recordAcquireSuccess() {
        acquireSuccess.incrementAndGet();
    }

    public void recordRenewRequest() {
        renewRequests.incrementAndGet();
    }

    public void recordRenewSuccess() {
        renewSuccess.incrementAndGet();
    }

    public void recordReleaseRequest() {
        releaseRequests.incrementAndGet();
    }

    public void recordReleaseSuccess() {
        releaseSuccess.incrementAndGet();
    }

    public void recordConflict() {
        conflicts.incrementAndGet();
    }

    public void recordExpiration() {
        expirations.incrementAndGet();
    }

    public Map<String, Long> snapshot(int activeLeaseCount) {
        Map<String, Long> metrics = new LinkedHashMap<>();
        metrics.put("acquireRequests", acquireRequests.get());
        metrics.put("acquireSuccess", acquireSuccess.get());
        metrics.put("renewRequests", renewRequests.get());
        metrics.put("renewSuccess", renewSuccess.get());
        metrics.put("releaseRequests", releaseRequests.get());
        metrics.put("releaseSuccess", releaseSuccess.get());
        metrics.put("conflicts", conflicts.get());
        metrics.put("expirations", expirations.get());
        metrics.put("activeLeases", (long) activeLeaseCount);
        return metrics;
    }
}

