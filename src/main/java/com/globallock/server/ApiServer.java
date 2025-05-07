package com.globallock.server;

import com.globallock.api.LeaseHttpHandler;
import com.globallock.config.ServerConfig;
import com.globallock.service.LeaseService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ApiServer implements AutoCloseable {
    private final HttpServer httpServer;
    private final ExecutorService executorService;

    public ApiServer(ServerConfig config, LeaseService leaseService, Clock clock) throws IOException {
        this.httpServer = HttpServer.create(new InetSocketAddress(config.port()), 0);
        this.executorService = Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors()));
        this.httpServer.setExecutor(executorService);
        this.httpServer.createContext("/", new LeaseHttpHandler(leaseService, clock, config));
    }

    public void start() {
        httpServer.start();
    }

    @Override
    public void close() {
        httpServer.stop(0);
        executorService.shutdownNow();
    }
}

