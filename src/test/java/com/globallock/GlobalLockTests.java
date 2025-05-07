package com.globallock;

import com.globallock.domain.LeaseCommand;
import com.globallock.domain.LeaseResult;
import com.globallock.service.LeaseService;
import com.globallock.service.MetricsRegistry;

import java.time.Instant;
import java.time.ZoneOffset;

public final class GlobalLockTests {
    private GlobalLockTests() {
    }

    public static void main(String[] args) {
        shouldAcquireLeaseWithFencingToken();
        shouldRejectConflictingOwner();
        shouldRenewLeaseWithMatchingToken();
        shouldExpireAndAllowNewOwner();
        shouldReleaseLease();
        System.out.println("All GlobalLock tests passed.");
    }

    private static void shouldAcquireLeaseWithFencingToken() {
        LeaseService service = serviceAtEpoch();
        LeaseResult result = service.acquire(new LeaseCommand("orders", "worker-a", 5_000L, null));

        assert result.success();
        assert result.lease() != null;
        assert result.lease().token() == 1L;
        assert "worker-a".equals(result.lease().ownerId());
    }

    private static void shouldRejectConflictingOwner() {
        LeaseService service = serviceAtEpoch();
        service.acquire(new LeaseCommand("orders", "worker-a", 5_000L, null));
        LeaseResult result = service.acquire(new LeaseCommand("orders", "worker-b", 5_000L, null));

        assert !result.success();
        assert result.conflictingLease() != null;
        assert "worker-a".equals(result.conflictingLease().ownerId());
    }

    private static void shouldRenewLeaseWithMatchingToken() {
        LeaseService service = serviceAtEpoch();
        LeaseResult acquire = service.acquire(new LeaseCommand("orders", "worker-a", 5_000L, null));
        LeaseResult renew = service.renew(new LeaseCommand("orders", "worker-a", 7_500L, acquire.lease().token()));

        assert renew.success();
        assert renew.lease() != null;
        assert renew.lease().token() == acquire.lease().token();
        assert renew.lease().ttlMillis() == 7_500L;
    }

    private static void shouldExpireAndAllowNewOwner() {
        MutableClock clock = new MutableClock(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
        LeaseService service = new LeaseService(new MetricsRegistry(), clock, 5_000L);

        LeaseResult first = service.acquire(new LeaseCommand("orders", "worker-a", 2_000L, null));
        clock.advanceMillis(2_500L);
        LeaseResult second = service.acquire(new LeaseCommand("orders", "worker-b", 2_000L, null));

        assert first.success();
        assert second.success();
        assert second.lease() != null;
        assert "worker-b".equals(second.lease().ownerId());
        assert second.lease().token() == 2L;
    }

    private static void shouldReleaseLease() {
        LeaseService service = serviceAtEpoch();
        LeaseResult acquire = service.acquire(new LeaseCommand("orders", "worker-a", 5_000L, null));
        LeaseResult release = service.release(new LeaseCommand("orders", "worker-a", 5_000L, acquire.lease().token()));

        assert release.success();
        assert service.getLease("orders").isEmpty();
    }

    private static LeaseService serviceAtEpoch() {
        MutableClock clock = new MutableClock(Instant.ofEpochMilli(0L), ZoneOffset.UTC);
        return new LeaseService(new MetricsRegistry(), clock, 5_000L);
    }
}
