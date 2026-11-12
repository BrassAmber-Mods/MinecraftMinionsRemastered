package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.work.CommandValidation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;
import io.github.jodlodi.minions.MinionsRemastered;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

@ParametersAreNonnullByDefault
public class SitPacket extends AbstractServerboundPacket {
    public static final CustomPacketPayload.Type<SitPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("sit_packet"));
    public static final StreamCodec<FriendlyByteBuf, SitPacket> STREAM_CODEC = StreamCodec.ofMember(SitPacket::encode, SitPacket::new);

    @Override
    public CustomPacketPayload.Type<SitPacket> type() {
        return TYPE;
    }

    private final UUID entityID;

    public SitPacket(UUID id) {
        this.entityID = id;
    }

    public SitPacket(FriendlyByteBuf buf) {
        this.entityID = buf.readUUID();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUUID(this.entityID);
    }

    @Override
    void execute(ServerPlayer player, IMasterCapability capability) {
        if (player.level() instanceof ServerLevel serverLevel && serverLevel.getEntity(this.entityID) instanceof Minion minion
                && CommandValidation.entity(player, minion) && CommandValidation.owned(player, capability, minion)) {
            minion.setSit(!minion.getSit());
        }
    }
}
