package org.starSlayer;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class rateLimiter {
    private final ConcurrentHashMap<InetSocketAddress, Long> requestCounts = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final long timeWindow;

    public rateLimiter(int maxRequests, long timeWindow) {
        this.maxRequests = maxRequests;
        this.timeWindow = timeWindow;
    }

    public boolean isAllowed(InetSocketAddress clientId) {
        long now = System.currentTimeMillis();
        Long lastRequestTime = requestCounts.get(clientId);
        if (lastRequestTime == null) {
            requestCounts.put(clientId, now);
            return true;
        }

        if (now - lastRequestTime > timeWindow) {
            requestCounts.put(clientId, now);
            return true;
        }

        return false;
    }

}
