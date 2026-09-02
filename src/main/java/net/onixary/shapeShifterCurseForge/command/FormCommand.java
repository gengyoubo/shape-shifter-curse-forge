package net.onixary.shapeShifterCurseForge.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FormCommand {
    private FormCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ssc")
                .then(Commands.literal("form")
                        .then(Commands.literal("list")
                                .executes(context -> {
                                    String forms = FormRegistry.forms().keySet().stream()
                                            .map(ResourceLocation::toString)
                                            .reduce((left, right) -> left + ", " + right)
                                            .orElse("<none>");
                                    context.getSource().sendSuccess(() -> Component.literal(forms), false);
                                    return SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("get")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    FormDefinition current = FormManager.current(player);
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            current.id() + " tier=" + current.tier() + " group=" + current.groupId()), false);
                                    return SINGLE_SUCCESS;
                                }))
                        .then(Commands.literal("set")
                                .then(Commands.argument("form", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                FormRegistry.forms().keySet(), builder))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            ResourceLocation formId = ResourceLocationArgument.getId(context, "form");
                                            if (!FormManager.setForm(player, formId)) {
                                                if (FormRegistry.get(formId) == null) {
                                                    context.getSource().sendFailure(Component.literal("Unknown form: " + formId));
                                                } else {
                                                    context.getSource().sendSuccess(() -> Component.literal("Form unchanged: " + formId), false);
                                                }
                                                return 0;
                                            }
                                            context.getSource().sendSuccess(() -> Component.literal("Form set to " + formId), true);
                                            return SINGLE_SUCCESS;
                                        })))
                        .then(Commands.literal("next")
                                .executes(context -> move(context.getSource().getPlayerOrException(), true)))
                        .then(Commands.literal("previous")
                                .executes(context -> move(context.getSource().getPlayerOrException(), false)))
                        .then(Commands.literal("tier")
                                .then(Commands.argument("tier", IntegerArgumentType.integer(-1, 4))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int tier = IntegerArgumentType.getInteger(context, "tier");
                                            boolean changed = FormManager.moveToTier(player, tier);
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    changed ? "Moved to tier " + tier : "No form at tier " + tier), true);
                                            return changed ? SINGLE_SUCCESS : 0;
                                         })))));
    }

    private static int move(ServerPlayer player, boolean next) {
        return (next ? FormManager.next(player) : FormManager.previous(player)) ? SINGLE_SUCCESS : 0;
    }
}
