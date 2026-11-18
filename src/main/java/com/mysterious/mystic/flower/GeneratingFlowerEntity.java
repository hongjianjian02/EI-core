package com.mysterious.mystic.flower;

import com.mysterious.mystic.MysteriousMod;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import vazkii.botania.api.block_entity.GeneratingFlowerBlockEntity;
import vazkii.botania.api.block_entity.RadiusDescriptor;

public final class GeneratingFlowerEntity extends GeneratingFlowerBlockEntity {
    private static final TagKey<Block> IRON_FOOD = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(MysteriousMod.MODID, "iron_flower_food"));
    private final FlowerKind kind;
    private int feedingTick;
    private int feedingStage;
    private boolean rawBeef;
    private boolean feeding;
    private UUID owner;
    private UUID ownerTeam;
    private final Set<String> rewardedQuests = new HashSet<>();
    private final Map<BlockPos, Long> growthTicks = new HashMap<>();

    public GeneratingFlowerEntity(FlowerKind kind, BlockPos pos, BlockState state) {
        super(ModFlowers.TYPES_BY_KIND.get(kind).get(), pos, state);
        this.kind = kind;
    }

    public FlowerKind kind() {
        return kind;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!level.isClientSide) {
            FlowerEvents.track(this);
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (level != null && !level.isClientSide) {
            FlowerEvents.track(this);
        }
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            FlowerEvents.untrack(this);
        }
        super.setRemoved();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity entity, ItemStack stack) {
        super.setPlacedBy(level, pos, state, entity, stack);
        if (!level.isClientSide && entity instanceof ServerPlayer player && !(player instanceof FakePlayer)) {
            owner = player.getUUID();
            if (kind == FlowerKind.QUEST) {
                resolveTeam(player);
            }
            setChanged();
        }
    }

    private void resolveTeam(ServerPlayer player) {
        ServerQuestFile.getInstance().flatMap(file -> file.getTeamData(player)).ifPresent(data -> {
            ownerTeam = data.getTeamId();
            setChanged();
        });
    }

    @Override
    public void tickFlower() {
        super.tickFlower();
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        switch (kind) {
            case BREAD, BEEF -> tickFood(server);
            case IRON -> {
                if (server.getGameTime() % 20 == 0) {
                    eatIron(server);
                }
            }
            case TREADMILL -> tickTreadmill(server);
            case QUEST -> {
                if (ownerTeam == null && owner != null && server.getGameTime() % 20 == 0
                        && server.getPlayerByUUID(owner) instanceof ServerPlayer player) {
                    resolveTeam(player);
                }
            }
            default -> {
            }
        }
    }

    private void tickFood(ServerLevel server) {
        if (feeding) {
            int interval = kind == FlowerKind.BREAD ? 40 : 20;
            if (feedingTick < interval) {
                feedingTick++;
                setChanged();
            }
            if (feedingTick >= interval) {
                int[] portions = kind == FlowerKind.BREAD ? new int[]{2000, 4000, 5000}
                        : rawBeef ? new int[]{6300, 3500, 4200} : new int[]{9000, 5000, 6000};
                int portion = portions[feedingStage];
                if (getMaxMana() - getMana() >= portion) {
                    addMana(portion);
                    feedingTick = 0;
                    feedingStage++;
                    if (feedingStage == portions.length) {
                        feeding = false;
                        feedingStage = 0;
                    }
                    setChanged();
                }
            }
            return;
        }
        if (server.getGameTime() % 5 != 0 || getMana() >= getMaxMana()) {
            return;
        }
        AABB area = new AABB(getEffectivePos()).inflate(2);
        for (ItemEntity entity : server.getEntitiesOfClass(ItemEntity.class, area,
                entity -> entity.isAlive() && !entity.hasPickUpDelay())) {
            ItemStack stack = entity.getItem();
            boolean accepted = kind == FlowerKind.BREAD ? stack.is(Items.BREAD)
                    : stack.is(Items.BEEF) || stack.is(Items.COOKED_BEEF);
            if (accepted) {
                rawBeef = stack.is(Items.BEEF);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setItem(stack);
                }
                feeding = true;
                feedingTick = 0;
                feedingStage = 0;
                setChanged();
                return;
            }
        }
        if (kind == FlowerKind.BEEF && getMaxMana() - getMana() >= 30000) {
            for (Cow cow : server.getEntitiesOfClass(Cow.class, area,
                    cow -> cow.isAlive() && !cow.isBaby() && !cow.isInvulnerable())) {
                if (cow.hurt(server.damageSources().magic(), 1000) && !cow.isAlive()) {
                    addMana(30000);
                    return;
                }
            }
        }
    }

    private void eatIron(ServerLevel server) {
        for (Direction direction : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH,
                Direction.WEST, Direction.EAST}) {
            BlockPos target = getEffectivePos().relative(direction);
            if (getMaxMana() - getMana() < 1000 || !server.hasChunkAt(target)) {
                continue;
            }
            BlockState state = server.getBlockState(target);
            if (state.is(IRON_FOOD) && !state.hasBlockEntity() && server.removeBlock(target, false)) {
                addMana(1000);
                server.levelEvent(2001, target, Block.getId(state));
                return;
            }
        }
    }

    private void tickTreadmill(ServerLevel server) {
        if (owner == null || !(server.getPlayerByUUID(owner) instanceof ServerPlayer player)
                || player instanceof FakePlayer || player.isSpectator() || !player.isAlive()
                || player.isShiftKeyDown() || player.isPassenger() || player.isFallFlying()
                || !player.onGround() || !new AABB(getEffectivePos()).inflate(1, 1, 1).intersects(player.getBoundingBox())) {
            return;
        }
        if (player.isSprinting()) {
            addMana(500);
            if (server.getGameTime() % 5 == 0) {
                BlockPos center = getEffectivePos();
                player.teleportTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5);
                player.setSprinting(true);
            }
        }
    }

    public void cropGrown(BlockPos pos, boolean mature) {
        BlockPos center = getEffectivePos();
        if (level == null || kind != FlowerKind.AGRICULTURAL || Math.abs(center.getX() - pos.getX()) > 4
                || Math.abs(center.getZ() - pos.getZ()) > 4 || Math.abs(center.getY() - pos.getY()) > 2) {
            return;
        }
        long now = level.getGameTime();
        if (growthTicks.getOrDefault(pos, Long.MIN_VALUE) == now) {
            return;
        }
        growthTicks.entrySet().removeIf(entry -> entry.getValue() < now);
        growthTicks.put(pos.immutable(), now);
        addMana(mature ? 750 : 100);
    }

    public void questCompleted(UUID team, long quest) {
        if (kind == FlowerKind.QUEST && team.equals(ownerTeam)
                && rewardedQuests.add(team + ":" + Long.toUnsignedString(quest))) {
            addMana(20000);
            setChanged();
        }
    }

    @Override
    public void addMana(int amount) {
        int before = getMana();
        super.addMana(amount);
        if (getMana() != before) {
            setChanged();
            setMarkedForSync(true);
        }
    }

    @Override
    public int getMaxMana() {
        return kind.capacity;
    }

    @Override
    public int getColor() {
        return kind.color;
    }

    @Override
    public RadiusDescriptor getRadius() {
        int radius = switch (kind) {
            case AGRICULTURAL -> 4;
            case QUEST -> 16;
            case BREAD, BEEF -> 2;
            default -> 1;
        };
        return RadiusDescriptor.Rectangle.square(getEffectivePos(), radius);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("feeding", feeding);
        tag.putBoolean("raw_beef", rawBeef);
        tag.putInt("feeding_tick", feedingTick);
        tag.putInt("feeding_stage", feedingStage);
        if (owner != null) {
            tag.putUUID("flower_owner", owner);
        }
        if (ownerTeam != null) {
            tag.putUUID("flower_team", ownerTeam);
        }
        ListTag quests = new ListTag();
        rewardedQuests.forEach(id -> quests.add(StringTag.valueOf(id)));
        tag.put("rewarded_quests", quests);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        feeding = tag.getBoolean("feeding");
        rawBeef = tag.getBoolean("raw_beef");
        feedingTick = Math.max(0, Math.min(40, tag.getInt("feeding_tick")));
        feedingStage = Math.max(0, Math.min(2, tag.getInt("feeding_stage")));
        owner = tag.hasUUID("flower_owner") ? tag.getUUID("flower_owner") : null;
        ownerTeam = tag.hasUUID("flower_team") ? tag.getUUID("flower_team") : null;
        rewardedQuests.clear();
        ListTag quests = tag.getList("rewarded_quests", Tag.TAG_STRING);
        for (int index = 0; index < quests.size(); index++) {
            rewardedQuests.add(quests.getString(index));
        }
    }
}
