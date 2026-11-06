package com.mysterious.mystic;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class VoidHerrscher extends Monster {
    private static final int MAX_SHIELDS = 5;
    private static final double BASE_HEALTH = 400.0D;
    private static final double ARENA_RANGE = 15.0D;
    private static final EntityDataAccessor<Integer> SHIELDS =
            SynchedEntityData.defineId(VoidHerrscher.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PHASE_TWO =
            SynchedEntityData.defineId(VoidHerrscher.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> PHASE_THREE =
            SynchedEntityData.defineId(VoidHerrscher.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<BlockPos> SOURCE =
            SynchedEntityData.defineId(VoidHerrscher.class, EntityDataSerializers.BLOCK_POS);

    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.translatable("entity.eicore.void_herrscher"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.PROGRESS
    );
    private int playerCount = 1;
    private int attackCooldown;
    private int arenaTicks;

    public VoidHerrscher(EntityType<? extends VoidHerrscher> type, Level level) {
        super(type, level);
        this.xpReward = 1725;
        this.bossBar.setDarkenScreen(true);
        this.setNoGravity(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, BASE_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.4D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SHIELDS, MAX_SHIELDS);
        builder.define(PHASE_TWO, false);
        builder.define(PHASE_THREE, false);
        builder.define(SOURCE, BlockPos.ZERO);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        if (this.getShields() > 0 && amount > 0.0F) {
            this.setShields(this.getShields() - 1);
            this.invulnerableTime = 12;
            this.playSound(SoundEvents.SHIELD_BLOCK, 1.0F, 0.8F + this.random.nextFloat() * 0.2F);
            return true;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }

        this.updateBossPlayers();
        if (this.tickCount % 20 == 0) {
            this.updatePhases();
        }
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }
        if (this.attackCooldown <= 0) {
            this.useVoidPulse();
            this.attackCooldown = this.isPhaseThree() ? 50 : this.isPhaseTwo() ? 70 : 90;
        }
        this.keepPlayersNearSource();
        this.bossBar.setProgress(Math.max(0.0F, this.getHealth() / this.getMaxHealth()));
    }

    private void updateBossPlayers() {
        List<ServerPlayer> nearby = this.level().getEntitiesOfClass(ServerPlayer.class, this.getArenaBox());
        this.playerCount = Math.max(1, nearby.size());
        if (this.playerCount > 1 && this.getMaxHealth() == BASE_HEALTH) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(BASE_HEALTH * this.playerCount);
            this.setHealth(this.getMaxHealth());
        }
        for (ServerPlayer player : nearby) {
            this.bossBar.addPlayer(player);
        }
        this.bossBar.getPlayers().removeIf(player -> !nearby.contains(player));
    }

    private void updatePhases() {
        if (!this.isPhaseTwo() && this.getHealth() <= this.getMaxHealth() * 0.80F) {
            this.entityData.set(PHASE_TWO, true);
            this.setShields(1);
            this.heal(this.getMaxHealth() * 0.08F);
            this.playSound(SoundEvents.WITHER_SPAWN, 1.2F, 1.2F);
        }
        if (!this.isPhaseThree() && this.getHealth() <= this.getMaxHealth() * 0.25F) {
            this.entityData.set(PHASE_THREE, true);
            this.setShields(3);
            this.heal(this.getMaxHealth() * 0.25F);
            this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 1.0F, 0.8F);
        }
    }

    private void useVoidPulse() {
        AABB box = this.getArenaBox();
        List<ServerPlayer> players = this.level().getEntitiesOfClass(ServerPlayer.class, box);
        double damage = this.isPhaseThree() ? 8.0D : this.isPhaseTwo() ? 6.0D : 4.0D;
        for (ServerPlayer player : players) {
            if (player.distanceToSqr(this) <= 64.0D) {
                player.hurt(this.damageSources().mobAttack(this), (float) damage);
                Vec3 push = player.position().subtract(this.position()).normalize().scale(0.18D);
                player.push(push.x, 0.12D, push.z);
            }
        }
        this.level().broadcastEntityEvent(this, (byte) 4);
    }

    private void keepPlayersNearSource() {
        if (this.tickCount % 10 != 0) {
            return;
        }
        BlockPos source = this.getSourceData();
        for (ServerPlayer player : this.level().getEntitiesOfClass(ServerPlayer.class, this.getArenaBox())) {
            double dx = player.getX() - (source.getX() + 0.5D);
            double dz = player.getZ() - (source.getZ() + 0.5D);
            if (dx * dx + dz * dz > ARENA_RANGE * ARENA_RANGE) {
                player.teleportTo(source.getX() + 0.5D, source.getY() + 1.0D, source.getZ() + 0.5D);
            }
        }
    }

    private AABB getArenaBox() {
        BlockPos source = this.getSourceData();
        return new AABB(
                source.getX() - ARENA_RANGE, source.getY() - 8.0D, source.getZ() - ARENA_RANGE,
                source.getX() + ARENA_RANGE, source.getY() + 24.0D, source.getZ() + ARENA_RANGE
        );
    }

    public int getShields() {
        return this.entityData.get(SHIELDS);
    }

    public void setShields(int shields) {
        this.entityData.set(SHIELDS, Math.max(0, Math.min(MAX_SHIELDS, shields)));
    }

    public boolean isPhaseTwo() {
        return this.entityData.get(PHASE_TWO);
    }

    public boolean isPhaseThree() {
        return this.entityData.get(PHASE_THREE);
    }

    public void setSource(BlockPos pos) {
        this.entityData.set(SOURCE, pos);
    }

    private BlockPos getSourceData() {
        return this.entityData.get(SOURCE);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Shields", this.getShields());
        tag.putBoolean("PhaseTwo", this.isPhaseTwo());
        tag.putBoolean("PhaseThree", this.isPhaseThree());
        BlockPos source = this.getSourceData();
        tag.putInt("SourceX", source.getX());
        tag.putInt("SourceY", source.getY());
        tag.putInt("SourceZ", source.getZ());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setShields(tag.getInt("Shields"));
        this.entityData.set(PHASE_TWO, tag.getBoolean("PhaseTwo"));
        this.entityData.set(PHASE_THREE, tag.getBoolean("PhaseThree"));
        this.entityData.set(SOURCE, new BlockPos(
                tag.getInt("SourceX"), tag.getInt("SourceY"), tag.getInt("SourceZ")));
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossBar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossBar.removePlayer(player);
    }

}
