package com.esd.lab2.security;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class TokenDenylistService {

    private final ConcurrentMap<String, Instant> deniedTokens =
            new ConcurrentHashMap<>();

    public void deny(String tokenId, Instant expiration) {
        if (tokenId == null || tokenId.isBlank() || expiration == null) {
            throw new IllegalArgumentException(
                    "Token ID and expiration are required");
        }

        if (expiration.isAfter(Instant.now())) {
            deniedTokens.put(tokenId, expiration);
        }
    }

    public boolean isDenied(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }

        Instant expiration = deniedTokens.get(tokenId);

        if (expiration == null) {
            return false;
        }

        if (!expiration.isAfter(Instant.now())) {
            deniedTokens.remove(tokenId, expiration);
            return false;
        }

        return true;
    }
}
