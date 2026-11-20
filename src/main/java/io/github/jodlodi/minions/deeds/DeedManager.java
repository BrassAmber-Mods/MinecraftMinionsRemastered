package io.github.jodlodi.minions.deeds;

import io.github.jodlodi.minions.MinionsConfig;
import io.github.jodlodi.minions.network.DeedsScreenPacket;
import io.github.jodlodi.minions.registry.CommonRegistry;
import io.github.jodlodi.minions.registry.DeedSounds;
import io.github.jodlodi.minions.registry.PacketRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class DeedManager {
    private DeedManager() {}

    public static void openMenu(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;
        DeedProgress progress = player.getData(CommonRegistry.DEED_PROGRESS);
        int cost = MinionsConfig.EVIL_DEED_XP_COST.get();
        List<String> options = new ArrayList<>();
        if (cost >= 0 && !progress.isPerformingDeed()) {
            List<EvilDeed> available = new ArrayList<>(EvilDeed.configured());
            for (int i = 0; i < 3; i++) {
                options.add(available.remove(player.getRandom().nextInt(available.size())).id());
            }
        }
        progress.offer(options, cost);
        PacketRegistry.sendToPlayer(player, new DeedsScreenPacket(progress.offerId(), cost, progress.completedDeeds(),
                progress.remainingTicks(), progress.rewardPending(), options));
    }

    public static void commit(ServerPlayer player, UUID offerId, String deedId) {
        if (!player.isAlive() || player.isSpectator()) return;
        DeedProgress progress = player.getData(CommonRegistry.DEED_PROGRESS);
        int cost = MinionsConfig.EVIL_DEED_XP_COST.get();
        if (deedId.equals("__legacy_cheat") && MinionsConfig.LEGACY_XP_EASTER_EGG.get() && offerId.equals(progress.offerId()) && !progress.isPerformingDeed()) {
            progress.offer(List.of(), cost);
            player.giveExperiencePoints(200);
            return;
        }
        if (cost < 0) {
            player.displayClientMessage(Component.translatable("gui.minions_remastered.deeds.disabled"), false);
            return;
        }
        if (progress.isPerformingDeed()) return;
        EvilDeed deed = EvilDeed.byId(deedId);
        if (deed == null || !progress.wasOffered(offerId, deedId)) return;
        if (cost != progress.offeredXpCost()) {
            openMenu(player);
            return;
        }
        if (player.experienceLevel < cost) {
            player.displayClientMessage(Component.translatable("gui.minions_remastered.deeds.insufficient_xp", cost), false);
            return;
        }

        player.giveExperienceLevels(-cost);
        progress.begin(deed);
        // The original blindness lasts 1.5 times the selected audio duration.
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, deed.durationSeconds() * 30, 0));
        playSound(player, DeedSounds.forDeed(deed), SoundSource.PLAYERS);
        player.displayClientMessage(Component.translatable("gui.minions_remastered.deeds.started", deed.displayName()), true);
    }

    public static void tick(ServerPlayer player) {
        if (!player.isAlive() || !player.hasData(CommonRegistry.DEED_PROGRESS)) return;
        DeedProgress progress = player.getData(CommonRegistry.DEED_PROGRESS);
        if (!progress.advance()) return;
        boolean reward = progress.rewardPending();
        progress.finish();
        if (reward) {
            ItemStack staff = new ItemStack(CommonRegistry.MASTERS_STAFF.get());
            if (!player.getInventory().add(staff)) {
                var dropped = player.drop(staff, false);
                if (dropped != null) {
                    dropped.setTarget(player.getUUID());
                    dropped.setNoPickUpDelay();
                }
            }
            playSound(player, DeedSounds.REWARDED.get(), SoundSource.HOSTILE);
            player.displayClientMessage(Component.translatable("gui.minions_remastered.deeds.rewarded"), false);
        } else {
            playSound(player, DeedSounds.ACCEPTED.get(), SoundSource.PLAYERS);
            player.displayClientMessage(Component.translatable("gui.minions_remastered.deeds.accepted", progress.completedDeeds(), DeedProgress.DEEDS_PER_STAFF), false);
        }
    }

    private static void playSound(ServerPlayer player, SoundEvent sound, SoundSource source) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), sound, source, 1.0F, 1.0F);
    }
}
