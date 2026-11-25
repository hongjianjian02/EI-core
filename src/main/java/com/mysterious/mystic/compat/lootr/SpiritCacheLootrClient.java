package com.mysterious.mystic.compat.lootr;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class SpiritCacheLootrClient {
    private SpiritCacheLootrClient() { }

    public static boolean hasUnclaimedLoot(SpiritCacheLootrProvider provider) {
        Player player = Minecraft.getInstance().player;
        return player != null && provider.hasClientContents(player);
    }
}
