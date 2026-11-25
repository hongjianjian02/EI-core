package com.mysterious.mystic.compat.lootr;

import org.jetbrains.annotations.Nullable;

public interface SpiritCacheLootrAccess {
    @Nullable
    SpiritCacheLootrProvider eicore$getLootrProvider();

    void eicore$markPlayerPlaced();
}
