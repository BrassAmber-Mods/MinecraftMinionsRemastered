package io.github.jodlodi.minions.work;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.jodlodi.minions.MinionsRemastered;
import io.github.jodlodi.minions.registry.CommonRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = MinionsRemastered.MODID)
public final class MinionCommands {
    private MinionCommands() {}
    @SubscribeEvent public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("minions").requires(source -> source.getEntity() instanceof ServerPlayer)
                .executes(context -> { context.getSource().sendSuccess(() -> Component.translatable("message.minions_remastered.commands"), false); return 1; });
        String[] names = {"move", "follow", "vein", "stripmine", "drop", "unsummon", "dig", "deliver"};
        for (int i = 0; i < names.length; i++) {
            final int action = i;
            if (i != StaffActions.DIG) root.then(Commands.literal(names[i]).executes(context -> run(context, action, 3, 3)));
        }
        root.then(Commands.literal("dig").then(Commands.argument("width", IntegerArgumentType.integer(3, 71))
                .suggests((context, builder) -> {
                    for (int width = 3; width <= 71; width += 2) if (Integer.toString(width).startsWith(builder.getRemaining())) builder.suggest(width);
                    return builder.buildFuture();
                })
                .then(Commands.argument("height", IntegerArgumentType.integer(3, 25)).executes(context -> run(context, StaffActions.DIG,
                        IntegerArgumentType.getInteger(context, "width"), IntegerArgumentType.getInteger(context, "height"))))));
        root.then(Commands.literal("recall").executes(context -> run(context, StaffActions.RECALL, 3, 3)));
        for (String action : new String[]{"pause", "resume", "stop"}) root.then(Commands.literal(action).executes(context -> {
            var player = context.getSource().getPlayerOrException();
            if (!GameplayRules.hasStaff(player) || !CommandValidation.canCommand(player)) return 0;
            var master = CommonRegistry.getMaster(player).orElseThrow();
            if (action.equals("stop")) master.setOrder(null); else master.setPaused(action.equals("pause")); return 1;
        }));
        root.then(Commands.literal("status").executes(context -> {
            var player = context.getSource().getPlayerOrException(); var master = CommonRegistry.getMaster(player).orElseThrow();
            context.getSource().sendSuccess(() -> Component.translatable("message.minions_remastered.status", master.minionCount(),
                    Component.translatable("order.minions_remastered." + (master.getOrder() == null ? "following" : master.getOrder().getID().getPath())),
                    Component.translatable(master.isPaused() ? "message.minions_remastered.status.paused" : "message.minions_remastered.status.active")), false); return 1;
        }));
        event.getDispatcher().register(root);
    }
    private static int run(CommandContext<CommandSourceStack> context, int action, int width, int height) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        if (action == StaffActions.DIG && !GameplayRules.isHoleWidth(width, 71)) {
            context.getSource().sendFailure(Component.translatable("message.minions_remastered.dig_dimensions")); return 0;
        }
        if (!GameplayRules.hasStaff(player)) { context.getSource().sendFailure(Component.translatable("message.minions_remastered.staff_required")); return 0; }
        int result = StaffActions.execute(player, CommonRegistry.getMaster(player).orElseThrow(), action, StaffActions.lookedAt(player), width, height);
        if (result == 0) context.getSource().sendFailure(Component.translatable("message.minions_remastered.order_failed"));
        return result;
    }
}
