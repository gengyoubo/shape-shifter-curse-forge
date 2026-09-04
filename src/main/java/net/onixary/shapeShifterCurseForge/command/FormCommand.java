package net.onixary.shapeShifterCurseForge.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
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
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerEvents;
import net.onixary.shapeShifterCurseForge.cursedmoon.CursedMoonService;

import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FormCommand {
    private FormCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        var form = Commands.literal("form")
                .then(Commands.literal("list").executes(context -> {
                    String forms = FormRegistry.forms().keySet().stream().map(ResourceLocation::toString)
                            .reduce((left, right) -> left + ", " + right).orElse("<none>");
                    context.getSource().sendSuccess(() -> Component.literal(forms), false);
                    return SINGLE_SUCCESS;
                }))
                .then(Commands.literal("get").executes(context -> {
                    FormDefinition current = FormManager.current(context.getSource().getPlayerOrException());
                    context.getSource().sendSuccess(() -> Component.literal(
                            current.id() + " tier=" + current.tier() + " group=" + current.groupId()), false);
                    return SINGLE_SUCCESS;
                }))
                .then(Commands.literal("set").then(Commands.argument("form", ResourceLocationArgument.id())
                        .suggests((context, builder) -> suggestForms(builder))
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
                .then(Commands.literal("next").executes(context -> move(context.getSource().getPlayerOrException(), true)))
                .then(Commands.literal("previous").executes(context -> move(context.getSource().getPlayerOrException(), false)))
                .then(Commands.literal("tier").then(Commands.argument("tier", IntegerArgumentType.integer(-1, 4))
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            int tier = IntegerArgumentType.getInteger(context, "tier");
                            boolean changed = FormManager.moveToTier(player, tier);
                            context.getSource().sendSuccess(() -> Component.literal(
                                    changed ? "Moved to tier " + tier : "No form at tier " + tier), true);
                            return changed ? SINGLE_SUCCESS : 0;
                        })));
        var power = Commands.literal("power")
                .then(Commands.literal("status")
                        .executes(context -> showPowerStatus(context.getSource().getPlayerOrException(), context)))
                .then(Commands.literal("list")
                        .executes(context -> showPowerList(context.getSource().getPlayerOrException(), context)));
        var cursedMoon = Commands.literal("cursed_moon")
                .then(Commands.literal("status").executes(context -> {
                    var level = context.getSource().getServer().overworld();
                    context.getSource().sendSuccess(() -> Component.literal(
                            "Cursed Moon: day=" + CursedMoonService.isCursedMoonDay(level)
                                    + ", night=" + CursedMoonService.isInCursedMoon(level)
                                    + ", phase=" + level.getMoonPhase()), false);
                    return SINGLE_SUCCESS;
                }))
                .then(Commands.literal("force").requires(source -> source.hasPermission(2)).executes(context -> {
                    CursedMoonService.forceTriggerCursedMoon(context.getSource().getServer().overworld());
                    context.getSource().sendSuccess(() -> Component.literal("The next Cursed Moon was scheduled."), true);
                    return SINGLE_SUCCESS;
                }));
        event.getDispatcher().register(Commands.literal("ssc").then(form).then(power).then(cursedMoon));
    }

    private static int move(ServerPlayer player, boolean next) {
        return (next ? FormManager.next(player) : FormManager.previous(player)) ? SINGLE_SUCCESS : 0;
    }

    private static int showPowerStatus(ServerPlayer player, com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context) {
        FormPowerRegistry.DebugInfo info = FormPowerRegistry.debug(player);
        context.getSource().sendSuccess(() -> Component.literal("Power data: " + info.loadedPowers()
                + " definitions, " + info.assignedForms() + " form assignments. Current " + info.currentForm()
                + ": " + info.assignedPowers().size() + " assigned, " + info.resolvedPowers() + " resolved."), false);
        FormPowerEvents.SwimSpeedDebug swim = FormPowerEvents.swimSpeedDebug(player);
        String modifiers = swim.powers().stream().map(power -> power.powerId()
                        + " [condition=" + power.conditionMet() + ", installed=" + power.installed()
                        + ", " + power.operation() + " " + power.amount() + "]")
                .reduce((left, right) -> left + ", " + right).orElse("<none assigned>");
        context.getSource().sendSuccess(() -> Component.literal("SWIM_SPEED: attribute=" + swim.attributePresent()
                + ", base=" + swim.baseValue() + ", effective=" + swim.effectiveValue()
                + ", refreshedTick=" + swim.lastRefreshTick() + "; " + modifiers), false);
        return SINGLE_SUCCESS;
    }

    private static int showPowerList(ServerPlayer player, com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> context) {
        FormPowerRegistry.DebugInfo info = FormPowerRegistry.debug(player);
        String powers = info.assignedPowers().stream().map(ResourceLocation::toString)
                .reduce((left, right) -> left + ", " + right).orElse("<none>");
        context.getSource().sendSuccess(() -> Component.literal(info.currentForm() + " powers: " + powers), false);
        return SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestForms(SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        FormRegistry.forms().keySet().stream()
                .map(ResourceLocation::toString)
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }
}
