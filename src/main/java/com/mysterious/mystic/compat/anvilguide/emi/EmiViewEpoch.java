package com.mysterious.mystic.compat.anvilguide.emi;

import java.util.concurrent.atomic.AtomicLong;

/** EMI history can retain recipe objects after their world snapshot has been discarded. */
final class EmiViewEpoch {
    private static final AtomicLong CURRENT = new AtomicLong();

    private EmiViewEpoch() {
    }

    static long current() {
        return CURRENT.get();
    }

    static boolean isCurrent(long capturedEpoch) {
        return capturedEpoch == current();
    }

    static void invalidate() {
        CURRENT.incrementAndGet();
    }
}
