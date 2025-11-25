package com.whatsapp.network.strategy;

public interface RetryStrategy {
    long getDelay(int attempt);
    boolean shouldRetry(int attempt, Exception error);
}

