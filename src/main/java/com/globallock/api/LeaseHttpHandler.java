package com.globallock.api;

import com.globallock.config.ServerConfig;
import com.globallock.domain.Lease;
import com.globallock.domain.LeaseCommand;
import com.globallock.domain.LeaseResult;
import com.globallock.service.LeaseService;
import com.globallock.util.Json;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class LeaseHttpHandler implements HttpHandler {
    private final LeaseService leaseService;
    private final Clock clock;
    private final ServerConfig config;

    public LeaseHttpHandler(LeaseService leaseService, Clock clock, ServerConfig config) {
        this.leaseService = leaseService;
        this.clock = clock;
        this.config = config;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if ("/health".equals(path) && "GET".equals(method)) {
                HttpExchangeResponder.sendJson(exchange, 200, Json.stringify(healthResponse()));
                return;
            }

            if ("/metrics".equals(path) && "GET".equals(method)) {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("nodeId", config.nodeId());
                payload.put("metrics", leaseService.metricsRegistry().snapshot(leaseService.listLeases().size()));
                HttpExchangeResponder.sendJson(exchange, 200, Json.stringify(payload));
                return;
            }

            if ("/v1/leases".equals(path) && "GET".equals(method)) {
                List<Map<String, Object>> leases = leaseService.listLeases().stream()
                        .map(this::leaseToMap)
                        .toList();
                HttpExchangeResponder.sendJson(exchange, 200, Json.stringify(Map.of("leases", leases)));
                return;
            }

            if (path.startsWith("/v1/leases/") && "GET".equals(method)) {
                String resource = path.substring("/v1/leases/".length());
                if (resource.isBlank()) {
                    HttpExchangeResponder.sendJson(exchange, 400, error("resource path is required"));
                    return;
                }

                Optional<Lease> lease = leaseService.getLease(resource);
                if (lease.isPresent()) {
                    HttpExchangeResponder.sendJson(exchange, 200, Json.stringify(Map.of("lease", leaseToMap(lease.get()))));
                } else {
                    HttpExchangeResponder.sendJson(exchange, 404, error("lease not found"));
                }
                return;
            }

            if ("/v1/leases/acquire".equals(path) && "POST".equals(method)) {
                LeaseResult result = leaseService.acquire(parseCommand(exchange, false));
                HttpExchangeResponder.sendJson(exchange, result.success() ? 200 : 409, Json.stringify(resultToMap(result)));
                return;
            }

            if ("/v1/leases/renew".equals(path) && "POST".equals(method)) {
                LeaseResult result = leaseService.renew(parseCommand(exchange, true));
                HttpExchangeResponder.sendJson(exchange, result.success() ? 200 : 409, Json.stringify(resultToMap(result)));
                return;
            }

            if ("/v1/leases/release".equals(path) && "POST".equals(method)) {
                LeaseResult result = leaseService.release(parseCommand(exchange, true));
                HttpExchangeResponder.sendJson(exchange, result.success() ? 200 : 409, Json.stringify(resultToMap(result)));
                return;
            }

            HttpExchangeResponder.sendJson(exchange, 404, error("route not found"));
        } catch (IllegalArgumentException exception) {
            HttpExchangeResponder.sendJson(exchange, 400, error(exception.getMessage()));
        } catch (Exception exception) {
            HttpExchangeResponder.sendJson(exchange, 500, error("internal server error: " + exception.getMessage()));
        }
    }

    private LeaseCommand parseCommand(HttpExchange exchange, boolean tokenRequired) throws IOException {
        Map<String, String> payload = Json.parseObject(HttpExchangeResponder.readBody(exchange));
        String resource = payload.get("resource");
        String ownerId = payload.get("ownerId");
        long ttlMillis = payload.containsKey("ttlMillis") ? Long.parseLong(payload.get("ttlMillis")) : config.defaultTtlMillis();
        Long token = payload.containsKey("token") ? Long.parseLong(payload.get("token")) : null;

        if (tokenRequired && token == null) {
            throw new IllegalArgumentException("token is required");
        }

        return new LeaseCommand(resource, ownerId, ttlMillis, token);
    }

    private Map<String, Object> healthResponse() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "globallock");
        response.put("nodeId", config.nodeId());
        response.put("port", config.port());
        response.put("timeEpochMillis", clock.millis());
        response.put("activeLeases", leaseService.listLeases().size());
        return response;
    }

    private Map<String, Object> resultToMap(LeaseResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", result.success());
        payload.put("operation", result.operation());
        payload.put("message", result.message());
        if (result.lease() != null) {
            payload.put("lease", leaseToMap(result.lease()));
        }
        if (result.conflictingLease() != null) {
            payload.put("conflictingLease", leaseToMap(result.conflictingLease()));
        }
        return payload;
    }

    private Map<String, Object> leaseToMap(Lease lease) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("resource", lease.resource());
        payload.put("ownerId", lease.ownerId());
        payload.put("token", lease.token());
        payload.put("ttlMillis", lease.ttlMillis());
        payload.put("acquiredAtEpochMillis", lease.acquiredAtEpochMillis());
        payload.put("expiresAtEpochMillis", lease.expiresAtEpochMillis());
        payload.put("remainingMillis", lease.remainingMillis(clock.millis()));
        return payload;
    }

    private String error(String message) {
        return Json.stringify(Map.of("error", message));
    }
}

