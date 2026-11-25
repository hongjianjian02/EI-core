package com.mysterious.mystic.mixin;

import com.breakinblocks.neovitae.common.blockentity.SpiritCacheBlockEntity;
import com.mysterious.mystic.compat.lootr.SpiritCacheLootrAccess;
import com.mysterious.mystic.compat.lootr.SpiritCacheLootrClient;
import com.mysterious.mystic.compat.lootr.SpiritCacheLootrProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import noobanidus.mods.lootr.common.api.data.inventory.ILootrInventory;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpiritCacheBlockEntity.class, remap = false)
public abstract class SpiritCacheLootrBlockEntityMixin implements RandomizableContainer, SpiritCacheLootrAccess {
    @Unique private static final String EICORE_LOOTR_TAG = "EICoreSpiritLootr";
    @Unique private SpiritCacheLootrProvider eicore$provider;
    @Unique private boolean eicore$playerPlaced;

    @Unique private SpiritCacheBlockEntity eicore$cache() { return (SpiritCacheBlockEntity) (Object) this; }

    @Override
    @Nullable
    public SpiritCacheLootrProvider eicore$getLootrProvider() {
        SpiritCacheBlockEntity cache = eicore$cache();
        if (eicore$playerPlaced || cache.getLootTable() == null
                || BuiltInLootTables.EMPTY.equals(cache.getLootTable()) || cache.getLevel() == null) {
            return null;
        }
        if (eicore$provider == null || !eicore$provider.belongsToCache()) {
            if (cache.getLevel().isClientSide()) {
                return null;
            }
            eicore$provider = new SpiritCacheLootrProvider(cache);
            cache.setChangedNoSync();
        }
        return eicore$provider;
    }

    @Override
    public void eicore$markPlayerPlaced() {
        eicore$playerPlaced = true;
        eicore$provider = null;
        eicore$cache().setChanged();
    }

    /** The target inherits this default method; defining it here also protects external unpack calls. */
    @Override
    public void unpackLootTable(@Nullable Player player) {
        if (eicore$getLootrProvider() == null) {
            RandomizableContainer.super.unpackLootTable(player);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return eicore$getLootrProvider() == null;
    }

    @Inject(method = "getItem", at = @At("HEAD"), cancellable = true)
    private void eicore$hideSharedInventory(int slot, CallbackInfoReturnable<ItemStack> callback) {
        if (eicore$getLootrProvider() != null) callback.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(method = "removeItem", at = @At("HEAD"), cancellable = true)
    private void eicore$blockSharedExtraction(int slot, int count, CallbackInfoReturnable<ItemStack> callback) {
        if (eicore$getLootrProvider() != null) callback.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(method = "removeItemNoUpdate", at = @At("HEAD"), cancellable = true)
    private void eicore$blockSharedExtractionWithoutUpdate(int slot, CallbackInfoReturnable<ItemStack> callback) {
        if (eicore$getLootrProvider() != null) callback.setReturnValue(ItemStack.EMPTY);
    }

    @Inject(method = "setItem", at = @At("HEAD"), cancellable = true)
    private void eicore$blockSharedInsertion(int slot, ItemStack stack, CallbackInfo callback) {
        if (eicore$getLootrProvider() != null) callback.cancel();
    }

    @Inject(method = "createMenu", at = @At("HEAD"), cancellable = true)
    private void eicore$usePersonalMenu(int id, Inventory inventory, Player player, CallbackInfoReturnable<AbstractContainerMenu> callback) {
        SpiritCacheLootrProvider provider = eicore$getLootrProvider();
        if (provider != null) {
            ILootrInventory personal = player instanceof ServerPlayer serverPlayer && !player.isSpectator()
                    ? provider.getPersonalInventory(serverPlayer) : null;
            callback.setReturnValue(personal == null ? null : personal.createMenu(id, inventory, player));
        }
    }

    @Inject(method = "stopOpen", at = @At("TAIL"))
    private void eicore$syncPersonalContents(Player player, CallbackInfo callback) {
        SpiritCacheLootrProvider provider = eicore$getLootrProvider();
        if (provider != null && !eicore$cache().getLevel().isClientSide()) {
            provider.performUpdate();
        }
    }

    @Inject(method = "hasContents", at = @At("HEAD"), cancellable = true)
    private void eicore$showOriginalParticles(CallbackInfoReturnable<Boolean> callback) {
        if (eicore$cache().getLevel() != null && eicore$cache().getLevel().isClientSide()) {
            SpiritCacheLootrProvider provider = eicore$getLootrProvider();
            if (provider != null) callback.setReturnValue(SpiritCacheLootrClient.hasUnclaimedLoot(provider));
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    private void eicore$saveLootrIdentity(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo callback) {
        if (eicore$playerPlaced) tag.putBoolean("EICoreSpiritPlayerPlaced", true);
        SpiritCacheLootrProvider provider = eicore$getLootrProvider();
        if (provider != null) tag.put(EICORE_LOOTR_TAG, provider.save());
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    private void eicore$loadLootrIdentity(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo callback) {
        eicore$playerPlaced = tag.getBoolean("EICoreSpiritPlayerPlaced");
        eicore$provider = null;
        CompoundTag data = tag.getCompound(EICORE_LOOTR_TAG);
        if (!eicore$playerPlaced && eicore$cache().getLootTable() != null && data.hasUUID("Id") && data.contains("Dimension", 8)) {
            eicore$provider = SpiritCacheLootrProvider.load(eicore$cache(), data);
        }
    }
}
