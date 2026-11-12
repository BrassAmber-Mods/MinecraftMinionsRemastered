package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.work.CommandValidation;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import io.github.jodlodi.minions.MinionsRemastered;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class SummonPacket extends AbstractServerboundPacket {
    public static final CustomPacketPayload.Type<SummonPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("summon_packet"));
    public static final StreamCodec<FriendlyByteBuf, SummonPacket> STREAM_CODEC = StreamCodec.ofMember(SummonPacket::encode, SummonPacket::new);

    @Override
    public CustomPacketPayload.Type<SummonPacket> type() {
        return TYPE;
    }

	private final BlockPos pos;
	private final int specificID;

	public SummonPacket(BlockPos pos) {
		this(pos, -1);
	}

	public SummonPacket(BlockPos pos, int specificID) {
		this.pos = pos;
		this.specificID = specificID;
	}

	public SummonPacket(FriendlyByteBuf buf) {
		this.pos = buf.readBlockPos();
		this.specificID = buf.readInt();
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeBlockPos(this.pos);
		buf.writeInt(this.specificID);
	}

    @Override
    void execute(ServerPlayer player, IMasterCapability master) {
        if (!CommandValidation.block(player, this.pos) || master.minionCount() >= 4 || this.specificID < -1 || this.specificID >= 4) return;
        int slot = this.specificID >= 0 ? this.specificID : master.getMinions().indexOf(null);
        if (slot < 0 || master.getMinions().get(slot) != null) return;
        BlockPos base = player.level().getBlockState(this.pos).canBeReplaced() ? this.pos : this.pos.above();
        Minion minion = new Minion(player.level());
        minion.setOwnerUUID(player.getUUID());
        BlockPos destination = null;
        for (BlockPos candidate : java.util.List.of(base, base.north(), base.south(), base.east(), base.west())) {
            if (!player.level().isInWorldBounds(candidate) || !player.level().hasChunkAt(candidate) || !player.level().getWorldBorder().isWithinBounds(candidate)
                    || !player.level().getBlockState(candidate).getFluidState().isEmpty() || !player.level().mayInteract(player, candidate)) continue;
            Vec3 center = Vec3.atBottomCenterOf(candidate);
            minion.moveTo(center.x, center.y, center.z, player.getYRot(), 0);
            if (player.level().noCollision(minion, minion.getBoundingBox())) { destination = candidate; break; }
        }
        if (destination == null) return;
        master.addSpecificMinion(minion.getUUID(), slot);
        master.finalizeMinion(minion);
        if (!player.level().addFreshEntity(minion)) { master.removeMinion(minion.getUUID()); return; }
        minion.blink(destination);
    }
}
