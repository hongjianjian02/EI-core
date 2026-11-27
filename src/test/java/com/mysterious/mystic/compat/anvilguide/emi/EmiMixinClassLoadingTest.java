package com.mysterious.mystic.compat.anvilguide.emi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Runs through forgejunitdev's real Mixin transformer, rather than a plain JVM loader. */
class EmiMixinClassLoadingTest {
    private static final String MARKER_CLASS =
            "com.mysterious.mystic.compat.anvilguide.emi.EmiFocusedQueryManager";
    private static final String FORBIDDEN_HELPER_CLASS =
            "com.mysterious.mystic.compat.anvilguide.mixin.ForbiddenMixinHelperProbe";
    private static final String ILLEGAL_CLASS_LOAD_ERROR =
            "org.spongepowered.asm.mixin.transformer.throwables.IllegalClassLoadError";

    @Test
    void ordinaryQueryMarkerLoadsOutsideTheReservedMixinPackage() {
        ClassLoader loader = transformerLoader();
        Class<?> marker = assertDoesNotThrow(() -> Class.forName(MARKER_CLASS, true, loader));

        assertSame(loader, marker.getClassLoader(), "the marker must be loaded by the tested mod's transformer");
        assertTrue(marker.isInterface());
        assertEquals("1.1.24+1.21.1+neoforge",
                assertDoesNotThrow(() -> marker.getField("SUPPORTED_EMI_VERSION").get(null)));
    }

    @Test
    void ordinaryHelperInTheConfiguredMixinPackageIsRejected() {
        ClassLoader loader = transformerLoader();
        // This negative control must fail if the server harness never registers our Mixin config.
        // A successful marker load alone would not reproduce the original client loading failure.
        Throwable failure = assertThrows(Throwable.class,
                () -> Class.forName(FORBIDDEN_HELPER_CLASS, true, loader),
                "the real Mixin config must reserve its package even when the client hook is skipped");

        assertEquals(ILLEGAL_CLASS_LOAD_ERROR, failure.getClass().getName(),
                "a missing probe or an unrelated linkage error does not verify Mixin namespace isolation");
    }

    private static ClassLoader transformerLoader() {
        ClassLoader loader = EmiMixinClassLoadingTest.class.getClassLoader();
        assertEquals("TRANSFORMER", loader.getName(), "this regression requires the NeoForge transformer loader");
        return loader;
    }
}
