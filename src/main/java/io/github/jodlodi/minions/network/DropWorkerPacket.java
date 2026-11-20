package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.work.CommandValidation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public final class DropWorkerPacket extends AbstractServerboundPacket {
    public static final Type<DropWorkerPacket> TYPE = new Type<>(MinionsRemastered.locate("drop_worker"));
    public static final StreamCodec<FriendlyByteBuf, DropWorkerPacket> STREAM_CODEC = StreamCodec.ofMember(DropWorkerPacket::encode, DropWorkerPacket::new);
    private final UUID id;
    public DropWorkerPacket(UUID id) { this.id = id; }
    public DropWorkerPacket(FriendlyByteBuf buf) { this(buf.readUUID()); }
    @Override public void encode(FriendlyByteBuf buf) { buf.writeUUID(this.id); }
    @Override public Type<DropWorkerPacket> type() { return TYPE; }
    @Override void execute(ServerPlayer player, IMasterCapability master) {
        if (player.serverLevel().getEntity(this.id) instanceof Minion worker && CommandValidation.entity(player, worker) && CommandValidation.owned(player, master, worker)) {
            if (worker.isVehicle()) worker.ejectPassengers(); else worker.dropCargo();
        }
    }
}
