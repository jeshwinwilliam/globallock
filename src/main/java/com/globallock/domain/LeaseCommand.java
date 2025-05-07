package com.globallock.domain;

public record LeaseCommand(String resource, String ownerId, long ttlMillis, Long token) {
}

