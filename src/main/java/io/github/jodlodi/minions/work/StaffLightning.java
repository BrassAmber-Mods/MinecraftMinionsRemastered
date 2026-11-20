package io.github.jodlodi.minions.work;

import io.github.jodlodi.minions.MinionsConfig;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.network.LightningVisualPacket;
import io.github.jodlodi.minions.registry.WorkerSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;
import java.util.WeakHashMap;

public final class StaffLightning {
    private static final Map<ServerPlayer, Long> LAST_CAST = new WeakHashMap<>();
    private StaffLightning() {}
    public static void cast(ServerPlayer player) {
        long now = player.level().getGameTime(); Long last = LAST_CAST.get(player);
        if (last != null && now - last < 2 && now >= last) return;
        LAST_CAST.put(player, now);
        Vec3 start = player.getEyePosition(), aim = start.add(player.getLookAngle().scale(10));
        var block = player.level().clip(new ClipContext(start, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = block.getLocation();
        var hit = ProjectileUtil.getEntityHitResult(player.level(), player, start, end,
                player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1), entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator());
        if (hit != null) end = hit.getLocation();
        if (hit != null && hit.getEntity() instanceof Minion worker && player.getUUID().equals(worker.getOwnerUUID())) {
            worker.boostOrRelease(player); return;
        }
        if (!GameplayRules.pay(player, MinionsConfig.FOOD_COST_LIGHTNING.get())) return;
        if (hit != null && hit.getEntity() instanceof LivingEntity victim
                && (!(victim instanceof Player other) || player.canHarmPlayer(other))
                && !NeoForge.EVENT_BUS.post(new AttackEntityEvent(player, victim)).isCanceled()) {
            if (victim.hurt(player.damageSources().playerAttack(player), 1.0F) && !(victim instanceof Player)) victim.igniteForSeconds(2);
        }
        Vec3 hand = start.add(player.getLookAngle().cross(new Vec3(0, 1, 0)).scale(0.2)).add(0, -0.2, 0);
        PacketDistributor.sendToPlayersNear(player.serverLevel(), null, player.getX(), player.getY(), player.getZ(), 32, new LightningVisualPacket(hand, end, player.getRandom().nextLong()));
        if (now % 4 == 0) WorkerSounds.play(player, "bolt");
    }
}
