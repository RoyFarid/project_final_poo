package com.whatsapp.network.strategy;

public class FixedDelayStrategy implements RetryStrategy {
    private final long delay;
    private final int maxAttempts;

    public FixedDelayStrategy(long delay, int maxAttempts) {
        this.delay = delay;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public long getDelay(int attempt) {
        return delay;
    }

    @Override
    public boolean shouldRetry(int attempt, Exception error) {
        return attempt <= maxAttempts;
    }
}

