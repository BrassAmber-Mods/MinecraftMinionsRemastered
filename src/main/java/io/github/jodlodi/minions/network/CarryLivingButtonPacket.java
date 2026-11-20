package io.github.jodlodi.minions.network;

import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.capabilities.IMasterCapability;
import io.github.jodlodi.minions.minion.Minion;
import io.github.jodlodi.minions.orders.CarryLivingOrder;
import io.github.jodlodi.minions.work.CommandValidation;
import io.github.jodlodi.minions.work.GameplayRules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.ParametersAreNonnullByDefault;

import java.util.UUID;

@ParametersAreNonnullByDefault
public class CarryLivingButtonPacket extends AbstractServerboundPacket {
    public static final CustomPacketPayload.Type<CarryLivingButtonPacket> TYPE = new CustomPacketPayload.Type<>(MinionsRemastered.locate("carry_living_button_packet"));
    public static final StreamCodec<FriendlyByteBuf, CarryLivingButtonPacket> STREAM_CODEC = StreamCodec.ofMember(CarryLivingButtonPacket::encode, CarryLivingButtonPacket::new);

    @Override
    public CustomPacketPayload.Type<CarryLivingButtonPacket> type() {
        return TYPE;
    }

	private final UUID id;

	public CarryLivingButtonPacket(UUID id) {
		this.id = id;
	}

	public CarryLivingButtonPacket(FriendlyByteBuf buf) {
		this.id = buf.readUUID();
	}

	@Override
	public void encode(FriendlyByteBuf buf) {
		buf.writeUUID(this.id);
	}

	@Override
	void execute(ServerPlayer player, IMasterCapability capability) {
		Entity entity = player.serverLevel().getEntity(this.id);
		if (!(entity instanceof LivingEntity) || !CommandValidation.entity(player, entity) || !CommandValidation.friendly(player, entity)) return;
		if (entity.getVehicle() instanceof Minion minion && CommandValidation.owned(player, capability, minion)) entity.stopRiding();
		else if (!entity.isPassenger()) GameplayRules.start(player, capability, new CarryLivingOrder(this.id, entity.blockPosition()));
	}
}
