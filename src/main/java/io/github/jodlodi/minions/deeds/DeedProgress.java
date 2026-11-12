package io.github.jodlodi.minions.deeds;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Persistent paid progress; menu offers are deliberately limited to the current connection. */
public final class DeedProgress implements INBTSerializable<CompoundTag> {
    public static final int DEEDS_PER_STAFF = 4;
    private int completedDeeds;
    private String pendingDeed = "";
    private int remainingTicks;
    private boolean rewardPending;
    private UUID offerId = UUID.randomUUID();
    private List<String> offeredDeeds = List.of();
    private int offeredXpCost;

    public int completedDeeds() {
        return this.completedDeeds;
    }

    public int remainingTicks() {
        return this.remainingTicks;
    }

    public boolean rewardPending() {
        return this.rewardPending;
    }

    public boolean isPerformingDeed() {
        return this.remainingTicks > 0;
    }

    public UUID offerId() {
        return this.offerId;
    }

    public void offer(List<String> deeds, int xpCost) {
        this.offerId = UUID.randomUUID();
        this.offeredDeeds = List.copyOf(deeds);
        this.offeredXpCost = xpCost;
    }

    public int offeredXpCost() {
        return this.offeredXpCost;
    }

    public boolean wasOffered(UUID offerId, String deedId) {
        return this.offerId.equals(offerId) && this.offeredDeeds.contains(deedId);
    }

    public void begin(EvilDeed deed) {
        this.offeredDeeds = List.of();
        this.pendingDeed = deed.id();
        this.remainingTicks = deed.durationSeconds() * 20;
        this.completedDeeds++;
        this.rewardPending = this.completedDeeds >= DEEDS_PER_STAFF;
        if (this.rewardPending) this.completedDeeds = 0;
    }

    /** Returns true once when the paid deed is ready for its completion feedback/reward. */
    public boolean advance() {
        return this.remainingTicks > 0 && --this.remainingTicks == 0;
    }

    public void finish() {
        this.pendingDeed = "";
        this.rewardPending = false;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("CompletedDeeds", this.completedDeeds);
        if (this.isPerformingDeed()) {
            tag.putString("PendingDeed", this.pendingDeed);
            tag.putInt("RemainingTicks", this.remainingTicks);
            tag.putBoolean("RewardPending", this.rewardPending);
        }
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.completedDeeds = Math.max(0, Math.min(DEEDS_PER_STAFF - 1, tag.getInt("CompletedDeeds")));
        EvilDeed pending = EvilDeed.byId(tag.getString("PendingDeed"));
        this.pendingDeed = pending != null ? pending.id() : "";
        this.remainingTicks = pending != null ? Math.max(0, Math.min(pending.durationSeconds() * 20, tag.getInt("RemainingTicks"))) : 0;
        this.rewardPending = this.remainingTicks > 0 && tag.getBoolean("RewardPending");
        this.offeredDeeds = List.of();
        this.offerId = UUID.randomUUID();
    }
}
