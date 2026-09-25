package net.onixary.shapeShifterCurseForge.other.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.other.cursedmoon.CursedMoonService;
import net.onixary.shapeShifterCurseForge.power.FormActivePowerService;
import net.onixary.shapeShifterCurseForge.power.FormPowerEvents;
import net.onixary.shapeShifterCurseForge.power.InstinctService;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** Fabric's public form command names and argument order on Forge. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FabricFormCommand {
    private FabricFormCommand() { }

    private enum Kind { NORMAL, DYNAMIC, SUB, ANY }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("shape_shifter_curse")
                .then(formCommand("set_form", Kind.NORMAL, false))
                .then(formCommand("transform_to_form", Kind.NORMAL, true))
                .then(formCommand("set_dynamic_form", Kind.DYNAMIC, false))
                .then(formCommand("transform_to_dynamic_form", Kind.DYNAMIC, true))
                .then(formCommand("set_sub_form", Kind.SUB, false))
                .then(formCommand("transform_to_sub_form", Kind.SUB, true))
                .then(Commands.literal("jump_to_next_cursed_moon").requires(source -> source.hasPermission(2))
                        .executes(context -> {
                            CursedMoonService.forceTriggerCursedMoon(context.getSource().getLevel());
                            context.getSource().sendSuccess(() -> Component.literal("Set cursed moon to next night!"), true);
                            return 1;
                        }))
                .then(Commands.literal("world_time").requires(source -> source.hasPermission(2))
                        .then(Commands.literal("set")
                                .then(Commands.argument("time", IntegerArgumentType.integer())
                                        .executes(context -> changeTime(context, false))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("time", IntegerArgumentType.integer())
                                        .executes(context -> changeTime(context, true)))))
                .then(Commands.literal("keep_original_skin")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    SscApi.currentSkin(player).ifPresent(skin ->
                                            skin.setKeepOriginalSkin(BoolArgumentType.getBool(context, "value")));
                                    ModNetwork.sendSkinSync(player);
                                    return 1;
                                })))
                .then(Commands.literal("set_form_color")
                        .executes(FabricFormCommand::showFormColor)
                        .then(Commands.argument("enable", BoolArgumentType.bool())
                                .executes(FabricFormCommand::setFormColorEnabled)
                                .then(Commands.argument("primaryColorRGBA", StringArgumentType.word())
                                        .then(Commands.argument("accentColor1RGBA", StringArgumentType.word())
                                                .then(Commands.argument("accentColor2RGBA", StringArgumentType.word())
                                                        .then(Commands.argument("eyeColorA", StringArgumentType.word())
                                                                .then(Commands.argument("eyeColorB", StringArgumentType.word())
                                                                        .then(Commands.argument("primaryGreyReverse", BoolArgumentType.bool())
                                                                                .then(Commands.argument("accent1GreyReverse", BoolArgumentType.bool())
                                                                                        .then(Commands.argument("accent2GreyReverse", BoolArgumentType.bool())
                                                                                                .executes(FabricFormCommand::setFormColor)))))))))))
                .then(Commands.literal("debug")
                        .then(formCommand("set_form", Kind.ANY, false))
                        .then(Commands.literal("clear_player_form_data").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                            SscApi.currentForm(target).ifPresent(data ->
                                                    data.copyFrom(new net.onixary.shapeShifterCurseForge.capability.PlayerFormData()));
                                            FormActivePowerService.onFormChanged(target);
                                            FormPowerEvents.onFormChanged(target);
                                            target.refreshDimensions();
                                            ModNetwork.sendFormSync(target);
                                            SscApi.currentForm(target).ifPresent(data ->
                                                    ModNetwork.sendItemStores(target, data.getItemStores()));
                                            FormActivePowerService.synchronizeMana(target);
                                            InstinctService.synchronizeHud(target);
                                            context.getSource().sendSuccess(() -> Component.literal("Form Data Cleared!"), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("clear_player_skin_data").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                            SscApi.currentSkin(target).ifPresent(skin ->
                                                    skin.copyFrom(new net.onixary.shapeShifterCurseForge.capability.PlayerSkinData()));
                                            ModNetwork.sendSkinSync(target);
                                            context.getSource().sendSuccess(() -> Component.literal("Skin Data Cleared!"), false);
                                            return 1;
                                        })))
                        .then(Commands.literal("clear_player_mana_data").requires(source -> source.hasPermission(2))
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(context -> {
                                            ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                            SscApi.currentForm(target).ifPresent(data -> {
                                                if (data instanceof net.onixary.shapeShifterCurseForge.capability.PlayerFormData formData) {
                                                    formData.clearManaPools();
                                                }
                                            });
                                            FormActivePowerService.synchronizeMana(target);
                                            context.getSource().sendSuccess(() -> Component.literal("Mana Data Cleared!"), false);
                                            return 1;
                                        }))))
                .then(Commands.literal("debug_attrs").requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer target = EntityArgument.getPlayer(context, "target");
                                    return FormCommand.showPowerStatus(target, context);
                                })));
        event.getDispatcher().register(root);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> formCommand(String name, Kind kind, boolean animate) {
        return Commands.literal(name).requires(source -> source.hasPermission(2))
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("form", ResourceLocationArgument.id())
                                .suggests((context, builder) -> suggestForms(builder, kind))
                                .executes(context -> changeForm(context, kind, animate))));
    }

    private static int changeForm(CommandContext<CommandSourceStack> context, Kind kind, boolean animate)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "target");
        ResourceLocation id = ResourceLocationArgument.getId(context, "form");
        FormDefinition form = FormRegistry.get(id);
        if (form == null || !allows(kind, id)) {
            context.getSource().sendFailure(Component.literal("Invalid Form Id: " + id));
            return 0;
        }
        FormManager.setForm(player, id, animate);
        context.getSource().sendSuccess(() -> Component.literal("Form set to " + id), true);
        return 1;
    }

    private static boolean allows(Kind kind, ResourceLocation id) {
        return switch (kind) {
            case NORMAL -> !FormRegistry.isDynamicForm(id) && FormRegistry.masterFormOf(id) == null;
            case DYNAMIC -> FormRegistry.isDynamicForm(id) && FormRegistry.masterFormOf(id) == null;
            case SUB -> FormRegistry.masterFormOf(id) != null;
            case ANY -> true;
        };
    }

    private static CompletableFuture<Suggestions> suggestForms(SuggestionsBuilder builder, Kind kind) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        FormRegistry.forms().keySet().stream()
                .filter(id -> allows(kind, id))
                .map(ResourceLocation::toString)
                .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    private static int changeTime(CommandContext<CommandSourceStack> context, boolean add) {
        ServerLevel level = context.getSource().getLevel();
        long amount = IntegerArgumentType.getInteger(context, "time");
        level.setDayTime(add ? level.getDayTime() + amount : amount);
        context.getSource().sendSuccess(() -> Component.literal("World time set to " + level.getDayTime()), false);
        return 1;
    }

    private static int showFormColor(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        var skin = SscApi.currentSkin(player).orElse(null);
        if (skin == null) return 0;
        FormTextureUtils.ColorSetting color = skin.getFormColor();
        context.getSource().sendSuccess(() -> Component.literal("Form color setting: enable="
                + skin.isEnableFormColor() + " primary=" + argbString(color.primaryColor())
                + " accent1=" + argbString(color.accentColor1())
                + " accent2=" + argbString(color.accentColor2())
                + " eyeA=" + argbString(color.eyeColorA())
                + " eyeB=" + argbString(color.eyeColorB())), false);
        return 1;
    }

    private static int setFormColorEnabled(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        SscApi.currentSkin(player).ifPresent(skin ->
                skin.setEnableFormColor(BoolArgumentType.getBool(context, "enable")));
        ModNetwork.sendSkinSync(player);
        return 1;
    }

    private static int setFormColor(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Integer primary = parseRgba(StringArgumentType.getString(context, "primaryColorRGBA"));
        Integer accent1 = parseRgba(StringArgumentType.getString(context, "accentColor1RGBA"));
        Integer accent2 = parseRgba(StringArgumentType.getString(context, "accentColor2RGBA"));
        Integer eyeA = parseRgba(StringArgumentType.getString(context, "eyeColorA"));
        Integer eyeB = parseRgba(StringArgumentType.getString(context, "eyeColorB"));
        if (primary == null || accent1 == null || accent2 == null || eyeA == null || eyeB == null) {
            context.getSource().sendFailure(Component.literal("Invalid color format!"));
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayerOrException();
        SscApi.currentSkin(player).ifPresent(skin -> {
            skin.setFormColor(new FormTextureUtils.ColorSetting(
                    FormTextureUtils.rgba2Abgr(primary), FormTextureUtils.rgba2Abgr(accent1),
                    FormTextureUtils.rgba2Abgr(accent2), FormTextureUtils.rgba2Abgr(eyeA),
                    FormTextureUtils.rgba2Abgr(eyeB),
                    BoolArgumentType.getBool(context, "primaryGreyReverse"),
                    BoolArgumentType.getBool(context, "accent1GreyReverse"),
                    BoolArgumentType.getBool(context, "accent2GreyReverse")));
            skin.setEnableFormColor(BoolArgumentType.getBool(context, "enable"));
        });
        ModNetwork.sendSkinSync(player);
        return 1;
    }

    private static Integer parseRgba(String text) {
        try {
            if (text.length() == 6) return (Integer.parseUnsignedInt(text, 16) << 8) | 0xFF;
            if (text.length() == 8) return Integer.parseUnsignedInt(text, 16);
        } catch (NumberFormatException ignored) { }
        return null;
    }

    private static String argbString(int abgr) {
        return String.format("%08X", FormTextureUtils.abgr2Argb(abgr));
    }
}
