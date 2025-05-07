package com.globallock.config;

public record ServerConfig(int port, long reaperIntervalMillis, long defaultTtlMillis, String nodeId) {
    public static ServerConfig fromArgs(String[] args) {
        int port = 8081;
        long reaperIntervalMillis = 1000L;
        long defaultTtlMillis = 15_000L;
        String nodeId = "node-local-1";

        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                port = Integer.parseInt(arg.substring("--port=".length()));
            } else if (arg.startsWith("--reaper-interval-ms=")) {
                reaperIntervalMillis = Long.parseLong(arg.substring("--reaper-interval-ms=".length()));
            } else if (arg.startsWith("--default-ttl-ms=")) {
                defaultTtlMillis = Long.parseLong(arg.substring("--default-ttl-ms=".length()));
            } else if (arg.startsWith("--node-id=")) {
                nodeId = arg.substring("--node-id=".length());
            }
        }

        return new ServerConfig(port, reaperIntervalMillis, defaultTtlMillis, nodeId);
    }
}

