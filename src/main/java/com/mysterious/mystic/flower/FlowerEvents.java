package com.mysterious.mystic.flower;

import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.ObjectCompletedEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.block.CropGrowEvent;

public final class FlowerEvents {
    private static final Map<Level, Set<BlockPos>> LOADED = new WeakHashMap<>();

    private FlowerEvents() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(FlowerEvents::cropPost);
        ObjectCompletedEvent.QUEST.register(event -> {
            Set<GeneratingFlowerEntity> flowers = new HashSet<>();
            for (var player : event.getOnlineMembers()) {
                if (player.isSpectator()) {
                    continue;
                }
                ServerLevel level = player.serverLevel();
                for (BlockPos pos : Set.copyOf(LOADED.getOrDefault(level, Set.of()))) {
                    if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof GeneratingFlowerEntity flower
                            && flower.kind() == FlowerKind.QUEST
                            && flower.getEffectivePos().distToCenterSqr(player.position()) <= 256) {
                        flowers.add(flower);
                    }
                }
            }
            flowers.forEach(flower -> flower.questCompleted(event.getData().getTeamId(), event.getQuest().getId()));
            return EventResult.pass();
        });
    }

    public static void track(GeneratingFlowerEntity flower) {
        if (flower.kind() == FlowerKind.AGRICULTURAL || flower.kind() == FlowerKind.QUEST) {
            LOADED.computeIfAbsent(flower.getLevel(), unused -> new HashSet<>()).add(flower.getBlockPos().immutable());
        }
    }

    public static void untrack(GeneratingFlowerEntity flower) {
        Set<BlockPos> positions = LOADED.get(flower.getLevel());
        if (positions != null) {
            positions.remove(flower.getBlockPos());
        }
    }

    private static void cropPost(CropGrowEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            cropGrown(level, event.getPos(), event.getOriginalState(), level.getBlockState(event.getPos()));
        }
    }

    public static void cropGrown(ServerLevel level, BlockPos pos, BlockState original, BlockState current) {
        if (!(current.getBlock() instanceof CropBlock) || !original.is(current.getBlock())) {
            return;
        }
        for (var property : current.getProperties()) {
            if (property instanceof IntegerProperty age && property.getName().equals("age")
                    && current.getValue(age) > original.getValue(age)) {
                int maximum = age.getPossibleValues().stream().mapToInt(Integer::intValue).max().orElse(0);
                for (BlockPos flowerPos : Set.copyOf(LOADED.getOrDefault(level, Set.of()))) {
                    if (level.hasChunkAt(flowerPos)
                            && level.getBlockEntity(flowerPos) instanceof GeneratingFlowerEntity flower) {
                        flower.cropGrown(pos, current.getValue(age) == maximum);
                    }
                }
                return;
            }
        }
    }
}
