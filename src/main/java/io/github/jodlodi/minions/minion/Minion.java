package io.github.jodlodi.minions.minion;

import io.github.jodlodi.minions.MinUtil;
import io.github.jodlodi.minions.MinionsConfig;
import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.goals.*;
import io.github.jodlodi.minions.network.BlinkPacket;
import io.github.jodlodi.minions.network.PoofPacket;
import io.github.jodlodi.minions.registry.CommonRegistry;
import io.github.jodlodi.minions.registry.WorkerSounds;
import io.github.jodlodi.minions.work.WorkerPlayer;
import io.github.jodlodi.minions.work.WorkerRecallLedger;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class Minion extends PathfinderMob implements OwnableEntity {
    protected static final EntityDataAccessor<Optional<UUID>> DATA_OWNER_UUID = SynchedEntityData.defineId(Minion.class, EntityDataSerializers.OPTIONAL_UUID);
    protected static final EntityDataAccessor<Integer> DATA_BLINKING = SynchedEntityData.defineId(Minion.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(Minion.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Boolean> DATA_SITTING = SynchedEntityData.defineId(Minion.class, EntityDataSerializers.BOOLEAN);
    protected static final Vec3i CUSTOM_ITEM_PICKUP_REACH = new Vec3i(2, 1, 2);
    public static final int BLINK_COOLDOWN = 20;
    public static final int DEFAULT_RED = 9185572;
    public static final float DEFAULT_WIDTH = 0.6F;
    public static final float DEFAULT_HEIGHT = 0.95F;

    private boolean restoringCargo;
    private boolean deliveryRequested;
    private boolean hauling;
    private WorkerPlayer workerPlayer;
    private int boostTicks;
    private int offlineTicks;
    private long nextBoost;
    private long pickupBlockedUntil;
    private final ItemStackHandler cargo = new ItemStackHandler(27) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!Minion.this.restoringCargo && !Minion.this.level().isClientSide) {
                Minion.this.getMastersCapability().ifPresent(Minion.this::saveWorkerState);
            }
        }
    };

    public Minion(Level level) {
        super(CommonRegistry.MINION.get(), level);
        this.setCanPickUpLoot(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_OWNER_UUID, Optional.empty());
        builder.define(DATA_BLINKING, 0);
        builder.define(DATA_COLOR, DEFAULT_RED);
        builder.define(DATA_SITTING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new CustomFloatGoal(this));
        this.goalSelector.addGoal(1, new HaulItemsGoal(this));
        this.goalSelector.addGoal(2, new EnactOrderGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new FollowMasterGoal(this, 1.2D, 9.0F, 2.0F, false));
        this.goalSelector.addGoal(4, new LookAtMasterGoal(this));
        this.goalSelector.addGoal(5, new MinionRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        this.updateSwingTime();
        super.aiStep();
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new MinionNavigation(this, level);
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            if (this.boostTicks > 0) this.boostTicks--;
            int blink = this.entityData.get(DATA_BLINKING);
            if (blink > -BLINK_COOLDOWN) {
                this.entityData.set(DATA_BLINKING, blink - 1);
                this.getLookControl().tick();
                if (blink == 1) {
                    PacketDistributor.sendToPlayersTrackingEntityAndSelf(this, new BlinkPacket(this.getId(), this.position()));
                }
            }

            LivingEntity owner = this.getOwner();
            if (owner != null) {
                this.offlineTicks = 0;
                CommonRegistry.getMaster(owner).ifPresent(master -> {
                    this.accept(master);
                    if (!this.isRemoved() && this.tickCount % 20 == 0) this.saveWorkerState(master);
                });
            } else if (this.getOwnerUUID() != null && this.level() instanceof ServerLevel server) {
                int seconds = MinionsConfig.OFFLINE_RECALL_SECONDS.get();
                if (seconds > 0 && ++this.offlineTicks >= seconds * 20) {
                    WorkerRecallLedger.get(server.getServer()).record(this);
                    this.ejectPassengers(); this.discard(); return;
                }
            }
            if (this.tickCount % 20 == 0 && this.getAttribute(Attributes.FOLLOW_RANGE) != null) {
                double range = MinionsConfig.FOLLOW_RANGE.get();
                this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(range);
                this.getNavigation().setMaxVisitedNodesMultiplier((float)Math.max(1, range / 16));
            }
        }

        super.tick();
    }

    @Override
    public void remove(RemovalReason removalReason) {
        if (!this.level().isClientSide && removalReason == RemovalReason.KILLED) {
            if (this.getMastersCapability().isEmpty() && this.level() instanceof ServerLevel server && this.getOwnerUUID() != null)
                WorkerRecallLedger.get(server.getServer()).record(this);
            this.getMastersCapability().ifPresent(master -> {
                if (master.isMinion(this.getUUID())) {
                    this.saveWorkerState(master);
                    master.removeMinion(this.getUUID());
                }
            });
        }
        if (!this.level().isClientSide) PacketDistributor.sendToPlayersTrackingEntityAndSelf(this, new PoofPacket(this.getId()));
        super.remove(removalReason);
    }

    public int getMinionID(IMasterCapability masterCapability) {
        return masterCapability.getMinions().indexOf(this.getUUID());
    }

    public boolean isOwnerHoldingStaff() {
        LivingEntity living = this.getOwner();
        if (living != null) {
            return living.getMainHandItem().is(CommonRegistry.MASTERS_STAFF.get()) || living.getOffhandItem().is(CommonRegistry.MASTERS_STAFF.get());
        }
        return false;
    }

    @Override
    public void setCustomName(@Nullable Component component) {
        super.setCustomName(component);
        if (!this.level().isClientSide) this.getMastersCapability().ifPresent(this::saveWorkerState);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack item = player.getItemInHand(hand);
        if (item.getItem() instanceof DyeItem dye && player.getUUID().equals(this.getOwnerUUID())) {
            if (!this.level().isClientSide) {
                this.setColor(MinUtil.dye(dye, this.getColor()));
                this.getMastersCapability().ifPresent(this::saveWorkerState);
                if (!player.getAbilities().instabuild) item.shrink(1);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public InteractionResult checkAndHandleImportantInteractions(Player player, InteractionHand hand) {
        if (!player.getUUID().equals(this.getOwnerUUID())) return InteractionResult.PASS;
        return super.checkAndHandleImportantInteractions(player, hand);
    }

    @Override
    protected void pickUpItem(ItemEntity item) {
        if (this.level().getGameTime() < this.pickupBlockedUntil || this.level().getGameTime() < item.getPersistentData().getLong("MinionPickupAfter")
                || this.getMastersCapability().isEmpty() || item.hasPickUpDelay()
                || item.getTarget() != null && !item.getTarget().equals(this.getOwnerUUID())) return;
        ItemStack original = item.getItem();
        ItemStack remainder = this.insertCargo(original.copy());
        int pickedUp = original.getCount() - remainder.getCount();
        if (pickedUp > 0) {
            this.onItemPickup(item);
            this.take(item, pickedUp);
            if (remainder.isEmpty()) item.discard();
            else item.setItem(remainder);
        }
    }

    @Override
    public boolean canHoldItem(ItemStack stack) { return stack.getItem().canFitInsideContainerItems(); }

    public ItemStack insertCargo(ItemStack stack) {
        if (!this.canHoldItem(stack)) return stack;
        ItemStack remainder = ItemHandlerHelper.insertItemStacked(this.cargo, stack, false);
        if (!remainder.isEmpty() || this.isCargoFull()) this.requestDelivery();
        return remainder;
    }

    public boolean hasCargo() {
        for (int i = 0; i < this.cargo.getSlots(); i++) if (!this.cargo.getStackInSlot(i).isEmpty()) return true;
        return false;
    }

    private boolean isCargoFull() {
        for (int i = 0; i < this.cargo.getSlots(); i++) if (this.cargo.getStackInSlot(i).isEmpty()) return false;
        return true;
    }

    public boolean needsDelivery() { return this.hasCargo() && (this.deliveryRequested || this.isCargoFull()); }
    public void requestDelivery() {
        if (this.hasCargo()) { this.deliveryRequested = true; this.getMastersCapability().ifPresent(this::saveWorkerState); }
    }
    public boolean isHauling() { return this.hauling; }
    public void setHauling(boolean hauling) { this.hauling = hauling; }

    public void dropCargo() {
        if (!(this.level() instanceof ServerLevel level)) return;
        this.pickupBlockedUntil = level.getGameTime() + 60;
        for (int i = 0; i < this.cargo.getSlots(); i++) {
            ItemStack stack = this.cargo.getStackInSlot(i); if (stack.isEmpty()) continue;
            ItemEntity drop = new ItemEntity(level, this.getX(), this.getY() + 0.5, this.getZ(), stack.copy());
            drop.setTarget(this.getOwnerUUID()); drop.setNoPickUpDelay();
            drop.getPersistentData().putLong("MinionPickupAfter", level.getGameTime() + 60);
            if (level.addFreshEntity(drop)) this.cargo.setStackInSlot(i, ItemStack.EMPTY);
        }
        if (!this.hasCargo()) this.deliveryRequested = false;
        this.getMastersCapability().ifPresent(this::saveWorkerState);
        WorkerSounds.play(this, "foryou");
    }

    public boolean boostOrRelease(ServerPlayer player) {
        if (!player.getUUID().equals(this.getOwnerUUID())) return false;
        if (this.isVehicle()) { this.ejectPassengers(); return true; }
        if (this.level().getGameTime() < this.nextBoost) return false;
        this.nextBoost = this.level().getGameTime() + 20; this.boostTicks = 600;
        WorkerSounds.play(this, "minionsqueak");
        player.crit(this); this.getMastersCapability().ifPresent(this::saveWorkerState); return true;
    }

    public CompoundTag recallState() { return this.workerState(); }

    public int unloadCargo(IItemHandler destination) {
        int moved = 0;
        for (int i = 0; i < this.cargo.getSlots(); i++) {
            ItemStack stack = this.cargo.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            ItemStack remaining = ItemHandlerHelper.insertItemStacked(destination, stack.copy(), false);
            moved += stack.getCount() - remaining.getCount();
            this.cargo.setStackInSlot(i, remaining);
        }
        if (!this.hasCargo()) this.deliveryRequested = false;
        this.getMastersCapability().ifPresent(this::saveWorkerState);
        return moved;
    }

    public WorkerPlayer workerPlayer() {
        ServerPlayer owner = (ServerPlayer)this.getOwner();
        if (this.workerPlayer == null || this.workerPlayer.level() != this.level() || !this.workerPlayer.getUUID().equals(owner.getUUID())) {
            this.workerPlayer = new WorkerPlayer((ServerLevel)this.level(), owner, this);
        }
        return this.workerPlayer;
    }

    public void saveWorkerState(IMasterCapability master) {
        if (this.restoringCargo || !master.isMinion(this.getUUID())) return;
        CompoundTag tag = this.workerState();
        master.setInventory(this.getMinionID(master), tag);
    }

    private CompoundTag workerState() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Color", this.getColor());
        tag.putBoolean("Sit", this.getSit());
        if (this.hasCustomName()) tag.putString("CustomName", Component.Serializer.toJson(this.getCustomName(), this.registryAccess()));
        if (this.hasCargo()) tag.put("Cargo", this.cargo.serializeNBT(this.registryAccess()));
        tag.putBoolean("DeliveryRequested", this.deliveryRequested);
        tag.putString("WorkerDimension", this.level().dimension().location().toString());
        tag.putLong("WorkerPosition", this.blockPosition().asLong());
        tag.putInt("WorkBoostTicks", this.boostTicks);
        tag.putInt("OfflineTicks", this.offlineTicks);
        return tag;
    }

    public void restoreWorkerState(CompoundTag tag) {
        this.restoringCargo = true;
        try {
            this.setColor(MinUtil.getColor(tag));
            this.setSit(tag.getBoolean("Sit"));
            for (int i = 0; i < this.cargo.getSlots(); i++) this.cargo.setStackInSlot(i, ItemStack.EMPTY);
            if (tag.contains("Cargo")) {
                CompoundTag items = tag.getCompound("Cargo").copy();
                items.putInt("Size", 27);
                this.cargo.deserializeNBT(this.registryAccess(), items);
            }
            if (tag.contains("CustomName")) {
                try { this.setCustomName(Component.Serializer.fromJson(tag.getString("CustomName"), this.registryAccess())); }
                catch (RuntimeException exception) { MinionsRemastered.LOGGER.warn("Invalid saved minion name", exception); }
            }
            this.deliveryRequested = tag.getBoolean("DeliveryRequested");
            this.boostTicks = Math.max(0, Math.min(600, tag.getInt("WorkBoostTicks")));
            this.offlineTicks = Math.max(0, tag.getInt("OfflineTicks"));
        } finally { this.restoringCargo = false; }
    }

    @Override
    public boolean isPushedByFluid(FluidType type) {
        return !this.isEnactingOrder();
    }

    @Override
    protected boolean isAffectedByFluids() {
        return !this.isEnactingOrder();
    }

    public boolean getBlinking() {
        return this.entityData.get(DATA_BLINKING) > 0;
    }

    public boolean canBlink() {
        return this.entityData.get(DATA_BLINKING) <= -BLINK_COOLDOWN;
    }

    public void setColor(int color) {
        this.entityData.set(DATA_COLOR, color);
    }

    public int getColor() {
        return this.entityData.get(DATA_COLOR);
    }

    public void setSit(boolean sit) {
        this.entityData.set(DATA_SITTING, sit);
        if (!this.level().isClientSide && this.cargo != null) this.getMastersCapability().ifPresent(this::saveWorkerState);
    }

    public boolean getSit() {
        return this.entityData.get(DATA_SITTING);
    }

    public boolean sittingOrRiding() {
        return this.isPassenger() || this.getSit();
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource damageSource, float v) {
        if (!this.level().isClientSide && damageSource.getEntity() instanceof ServerPlayer player) return this.boostOrRelease(player);
        return false;
    }

    @Override
    protected Vec3i getPickupReach() {
        return CUSTOM_ITEM_PICKUP_REACH;
    }

    public boolean isEnactingOrder() {
        return this.goalSelector.getAvailableGoals().stream().anyMatch(goal -> goal.isRunning() && goal.getGoal() instanceof EnactOrderGoal);
    }

    public void blink(BlockPos pos) {
        this.blink(Vec3.atBottomCenterOf(pos));
    }

    public void blink(Vec3 vec3) {
        this.blink(vec3.x, vec3.y, vec3.z, this.getXRot(), this.getYRot());
    }

    public void blink(double x, double y, double z, float f, float g) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(this, new BlinkPacket(this.getId(), this.position()));
        this.moveTo(x, y, z, f, g);
        this.entityData.set(DATA_BLINKING, 15);
    }

    @Override
    public ItemStack getMainHandItem() {
        return this.getBlinking() ? ItemStack.EMPTY : super.getMainHandItem();
    }

    @Override
    public boolean isInvisible() {
        return this.getBlinking() || super.isInvisible();
    }

    @SuppressWarnings("deprecation")
    public float getMineSpeed() {
        float speed = this.boostTicks > 0 ? 7.5F : 3.75F;

        if (MobEffectUtil.hasDigSpeed(this)) {
            speed *= 1.0F + (float)(MobEffectUtil.getDigSpeedAmplification(this) + 1) * 0.2F;
        }

        if (this.hasEffect(MobEffects.DIG_SLOWDOWN)) {
            float f1 = switch (Objects.requireNonNull(this.getEffect(MobEffects.DIG_SLOWDOWN)).getAmplifier()) {
                case 0 -> 0.3F;
                case 1 -> 0.09F;
                case 2 -> 0.0027F;
                default -> 8.1E-4F;
            };

            speed *= f1;
        }

        if (this.isEyeInFluid(FluidTags.WATER)) {
            speed *= (float)this.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
        }

        if (!this.onGround()) {
            speed /= 5.0F;
        }

        return speed;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        return new Vec3(0.0D, dimensions.height() * 0.75D * (this.getSit() ? 0.9D : 1.2D), 0.0D);
    }

    @Override
    public boolean canRiderInteract() {
        return true;
    }

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(DATA_OWNER_UUID).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(DATA_OWNER_UUID, Optional.ofNullable(uuid));
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        try {
            UUID uuid = this.getOwnerUUID();
            return uuid == null ? null : this.level() instanceof ServerLevel server ? server.getServer().getPlayerList().getPlayer(uuid) : this.level().getPlayerByUUID(uuid);
        } catch (IllegalArgumentException illegalargumentexception) {
            return null;
        }
    }

    public Optional<IMasterCapability> getMastersCapability() {
        LivingEntity owner = this.getOwner();
        if (owner == null) return Optional.empty();
        else return CommonRegistry.getMaster(owner);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.getOwnerUUID() != null) tag.putUUID("Owner", this.getOwnerUUID());
        tag.merge(this.workerState());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        this.restoringCargo = true;
        try {
            super.readAdditionalSaveData(tag);
            UUID owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : this.getServer() != null
                    ? OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), tag.getString("Owner")) : null;
            this.setOwnerUUID(owner);
            this.restoreWorkerState(tag);
        } finally { this.restoringCargo = false; }
    }

    private void accept(@NotNull IMasterCapability iMasterCapability) {
        if (!iMasterCapability.isMinion(this.getUUID())) {
            this.discard();
        }
    }

    @Override
    public void travel(Vec3 input) {
        super.travel(this.sittingOrRiding() ? Vec3.ZERO : input);
    }

    @Override
    protected void tickRidden(Player player, Vec3 input) {
        super.tickRidden(player, input);
        this.setYRot(player.getYRot());
        this.yRotO = this.getYRot();
        this.setXRot(player.getXRot() * 0.5F);
        this.setRot(this.getYRot(), this.getXRot());
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player player, Vec3 input) {
        return new Vec3(0.0D, 0.0D, 1.0D);
    }

    @Override
    protected float getRiddenSpeed(Player player) {
        return this.getSteeringSpeed();
    }

    @Override
    protected float getFlyingSpeed() {
        return this.getControllingPassenger() != null ? this.getSpeed() * 0.1F : super.getFlyingSpeed();
    }

    @Override
    public Vec3 collide(Vec3 vec34) {
        AABB aabb = MinUtil.getRidingAABB(this);
        List<VoxelShape> list = this.level().getEntityCollisions(this, aabb.expandTowards(vec34));
        Vec3 vec3 = vec34.lengthSqr() == 0.0D ? vec34 : collideBoundingBox(this, vec34, aabb, this.level(), list);
        boolean flag = vec34.x != vec3.x;
        boolean flag1 = vec34.y != vec3.y;
        boolean flag2 = vec34.z != vec3.z;
        boolean flag3 = this.onGround() || flag1 && vec34.y < 0.0D;
        float stepHeight = this.maxUpStep();
        if (stepHeight > 0.0F && flag3 && (flag || flag2)) {
            Vec3 vec31 = collideBoundingBox(this, new Vec3(vec34.x, stepHeight, vec34.z), aabb, this.level(), list);
            Vec3 vec32 = collideBoundingBox(this, new Vec3(0.0D, stepHeight, 0.0D), aabb.expandTowards(vec34.x, 0.0D, vec34.z), this.level(), list);
            if (vec32.y < (double)stepHeight) {
                Vec3 vec33 = collideBoundingBox(this, new Vec3(vec34.x, 0.0D, vec34.z), aabb.move(vec32), this.level(), list).add(vec32);
                if (vec33.horizontalDistanceSqr() > vec31.horizontalDistanceSqr()) {
                    vec31 = vec33;
                }
            }

            if (vec31.horizontalDistanceSqr() > vec3.horizontalDistanceSqr()) {
                return vec31.add(collideBoundingBox(this, new Vec3(0.0D, -vec31.y + vec34.y, 0.0D), aabb.move(vec31), this.level(), list));
            }
        }

        return vec3;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        if (entity instanceof Minion minion && this.getOwner() != null && minion.getOwner() == this.getOwner()) return false;
        return super.canCollideWith(entity);
    }

    @Nullable
    public LivingEntity getControllingPassenger() {
        Entity entity = this.getFirstPassenger();
        return entity instanceof LivingEntity living && this.canBeControlledBy(living) ? living : null;
    }

    protected boolean canBeControlledBy(Entity entity) {
        if (entity instanceof Player player) {
            return player.getUUID().equals(this.getOwnerUUID()) && (player.getMainHandItem().is(CommonRegistry.MASTERS_STAFF.get()) || player.getOffhandItem().is(CommonRegistry.MASTERS_STAFF.get()));
        } else {
            return false;
        }
    }

    public float getSteeringSpeed() {
        return (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.225F;
    }
}
