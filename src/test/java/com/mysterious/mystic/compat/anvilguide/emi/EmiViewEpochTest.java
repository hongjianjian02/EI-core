package com.mysterious.mystic.compat.anvilguide.emi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The token used by retained recipe widgets; this does not open an EMI screen. */
class EmiViewEpochTest {
    @Test
    void retainedPagesStayExpiredAfterAReplacementSnapshotIsReady() {
        long openPage = EmiViewEpoch.current();
        assertTrue(EmiViewEpoch.isCurrent(openPage));

        EmiViewEpoch.invalidate();
        long replacementPage = EmiViewEpoch.current();
        assertFalse(EmiViewEpoch.isCurrent(openPage), "the old open page must stop using its world data");
        assertTrue(EmiViewEpoch.isCurrent(replacementPage));
        assertFalse(EmiViewEpoch.isCurrent(openPage), "reopening history must not revive an old page");
    }

    @Test
    void successiveWorldAndReloadInvalidationsDoNotReuseAnyRetainedPageToken() {
        long firstWorldPage = EmiViewEpoch.current();
        EmiViewEpoch.invalidate();
        long reloadPage = EmiViewEpoch.current();
        EmiViewEpoch.invalidate();
        long nextWorldPage = EmiViewEpoch.current();

        assertFalse(EmiViewEpoch.isCurrent(firstWorldPage));
        assertFalse(EmiViewEpoch.isCurrent(reloadPage));
        assertTrue(EmiViewEpoch.isCurrent(nextWorldPage));
    }
}
