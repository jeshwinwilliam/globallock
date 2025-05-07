package com.globallock.server;

import com.globallock.config.ServerConfig;
import com.globallock.service.LeaseService;
import com.globallock.service.MetricsRegistry;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class GlobalLockApplication {
    private GlobalLockApplication() {
    }

    public static void main(String[] args) throws Exception {
        ServerConfig config = ServerConfig.fromArgs(args);
        Clock clock = Clock.systemUTC();
        MetricsRegistry metricsRegistry = new MetricsRegistry();
        LeaseService leaseService = new LeaseService(metricsRegistry, clock, config.defaultTtlMillis());

        ScheduledExecutorService reaper = Executors.newSingleThreadScheduledExecutor();
        reaper.scheduleAtFixedRate(leaseService::expireLeases, config.reaperIntervalMillis(),
                config.reaperIntervalMillis(), TimeUnit.MILLISECONDS);

        ApiServer apiServer = new ApiServer(config, leaseService, clock);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            apiServer.close();
            reaper.shutdownNow();
        }));

        apiServer.start();
        System.out.println("GlobalLock started on port " + config.port() + " as " + config.nodeId());
        System.out.println("Health endpoint: http://localhost:" + config.port() + "/health");
    }
}

