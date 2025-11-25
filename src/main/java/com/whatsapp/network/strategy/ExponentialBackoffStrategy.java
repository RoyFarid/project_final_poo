package com.whatsapp.network.strategy;

public class ExponentialBackoffStrategy implements RetryStrategy {
    private final long initialDelay;
    private final long maxDelay;
    private final int maxAttempts;

    public ExponentialBackoffStrategy(long initialDelay, long maxDelay, int maxAttempts) {
        this.initialDelay = initialDelay;
        this.maxDelay = maxDelay;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public long getDelay(int attempt) {
        long delay = initialDelay * (1L << (attempt - 1));
        return Math.min(delay, maxDelay);
    }

    @Override
    public boolean shouldRetry(int attempt, Exception error) {
        return attempt <= maxAttempts;
    }
}

