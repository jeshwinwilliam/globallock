package com.globallock.service;

import com.globallock.domain.Lease;
import com.globallock.domain.LeaseCommand;
import com.globallock.domain.LeaseResult;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public final class LeaseService {
    private final ConcurrentMap<String, Lease> leases = new ConcurrentHashMap<>();
    private final AtomicLong fencingSequencer = new AtomicLong(0L);
    private final MetricsRegistry metricsRegistry;
    private final Clock clock;
    private final long defaultTtlMillis;

    public LeaseService(MetricsRegistry metricsRegistry, Clock clock, long defaultTtlMillis) {
        this.metricsRegistry = Objects.requireNonNull(metricsRegistry);
        this.clock = Objects.requireNonNull(clock);
        this.defaultTtlMillis = defaultTtlMillis;
    }

    public LeaseResult acquire(LeaseCommand command) {
        validate(command);
        metricsRegistry.recordAcquireRequest();
        long now = clock.millis();
        long ttlMillis = normalizeTtl(command.ttlMillis());

        LeaseResult[] holder = new LeaseResult[1];
        leases.compute(command.resource(), (resource, current) -> {
            if (current == null || current.isExpired(now)) {
                if (current != null && current.isExpired(now)) {
                    metricsRegistry.recordExpiration();
                }

                Lease newLease = new Lease(
                        resource,
                        command.ownerId(),
                        fencingSequencer.incrementAndGet(),
                        ttlMillis,
                        now,
                        now + ttlMillis
                );
                metricsRegistry.recordAcquireSuccess();
                holder[0] = LeaseResult.success("acquire", "lease acquired", newLease);
                return newLease;
            }

            if (current.ownerId().equals(command.ownerId())) {
                Lease renewedLease = new Lease(
                        resource,
                        command.ownerId(),
                        current.token(),
                        ttlMillis,
                        current.acquiredAtEpochMillis(),
                        now + ttlMillis
                );
                metricsRegistry.recordAcquireSuccess();
                holder[0] = LeaseResult.success("acquire", "lease already owned; ttl extended", renewedLease);
                return renewedLease;
            }

            metricsRegistry.recordConflict();
            holder[0] = LeaseResult.failure("acquire", "resource is currently leased by another owner", current);
            return current;
        });
        return holder[0];
    }

    public LeaseResult renew(LeaseCommand command) {
        validate(command);
        if (command.token() == null) {
            throw new IllegalArgumentException("token is required for renew");
        }

        metricsRegistry.recordRenewRequest();
        long now = clock.millis();
        long ttlMillis = normalizeTtl(command.ttlMillis());
        LeaseResult[] holder = new LeaseResult[1];

        leases.compute(command.resource(), (resource, current) -> {
            if (current == null || current.isExpired(now)) {
                if (current != null && current.isExpired(now)) {
                    metricsRegistry.recordExpiration();
                }
                holder[0] = LeaseResult.failure("renew", "lease is missing or expired", current);
                return null;
            }

            if (!current.ownerId().equals(command.ownerId()) || current.token() != command.token()) {
                metricsRegistry.recordConflict();
                holder[0] = LeaseResult.failure("renew", "owner or token mismatch", current);
                return current;
            }

            Lease renewedLease = new Lease(
                    resource,
                    current.ownerId(),
                    current.token(),
                    ttlMillis,
                    current.acquiredAtEpochMillis(),
                    now + ttlMillis
            );
            metricsRegistry.recordRenewSuccess();
            holder[0] = LeaseResult.success("renew", "lease renewed", renewedLease);
            return renewedLease;
        });

        return holder[0];
    }

    public LeaseResult release(LeaseCommand command) {
        validate(command);
        if (command.token() == null) {
            throw new IllegalArgumentException("token is required for release");
        }

        metricsRegistry.recordReleaseRequest();
        long now = clock.millis();
        LeaseResult[] holder = new LeaseResult[1];

        leases.compute(command.resource(), (resource, current) -> {
            if (current == null || current.isExpired(now)) {
                if (current != null && current.isExpired(now)) {
                    metricsRegistry.recordExpiration();
                }
                holder[0] = LeaseResult.failure("release", "lease is missing or expired", current);
                return null;
            }

            if (!current.ownerId().equals(command.ownerId()) || current.token() != command.token()) {
                metricsRegistry.recordConflict();
                holder[0] = LeaseResult.failure("release", "owner or token mismatch", current);
                return current;
            }

            metricsRegistry.recordReleaseSuccess();
            holder[0] = LeaseResult.success("release", "lease released", current);
            return null;
        });

        return holder[0];
    }

    public Optional<Lease> getLease(String resource) {
        Objects.requireNonNull(resource);
        pruneExpiredLease(resource);
        return Optional.ofNullable(leases.get(resource));
    }

    public List<Lease> listLeases() {
        expireLeases();
        List<Lease> snapshot = new ArrayList<>(leases.values());
        snapshot.sort(Comparator.comparing(Lease::resource));
        return snapshot;
    }

    public int expireLeases() {
        long now = clock.millis();
        int removed = 0;
        for (String resource : leases.keySet()) {
            Lease current = leases.get(resource);
            if (current != null && current.isExpired(now) && leases.remove(resource, current)) {
                metricsRegistry.recordExpiration();
                removed++;
            }
        }
        return removed;
    }

    public MetricsRegistry metricsRegistry() {
        return metricsRegistry;
    }

    private void pruneExpiredLease(String resource) {
        Lease current = leases.get(resource);
        if (current != null && current.isExpired(clock.millis()) && leases.remove(resource, current)) {
            metricsRegistry.recordExpiration();
        }
    }

    private long normalizeTtl(long ttlMillis) {
        return ttlMillis > 0 ? ttlMillis : defaultTtlMillis;
    }

    private void validate(LeaseCommand command) {
        Objects.requireNonNull(command);
        if (command.resource() == null || command.resource().isBlank()) {
            throw new IllegalArgumentException("resource is required");
        }
        if (command.ownerId() == null || command.ownerId().isBlank()) {
            throw new IllegalArgumentException("ownerId is required");
        }
    }
}

