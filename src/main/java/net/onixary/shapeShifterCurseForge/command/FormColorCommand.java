package net.onixary.shapeShifterCurseForge.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.capability.ModCapabilities;
import net.onixary.shapeShifterCurseForge.client.render.FormTextureUtils;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.network.ModifyFcdPacket;
import net.onixary.shapeShifterCurseForge.network.OpenColorMenuPacket;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

/** {@code /ssc form_color ...}: color slots, clipboard sharing and the color menu. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FormColorCommand {
    private static final ResourceLocation EMPTY_FORM = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "empty");

    private FormColorCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        var scopeArg = Commands.argument("scope", StringArgumentType.word())
                .suggests((context, builder) -> suggestStrings(builder, "form", "global", "form_default"));
        var formColor = Commands.literal("form_color")
                .then(Commands.literal("menu").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    ModNetwork.CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new OpenColorMenuPacket());
                    return SINGLE_SUCCESS;
                }))
                .then(Commands.literal("save")
                        .then(Commands.argument("scope", StringArgumentType.word())
                                .suggests((context, builder) -> suggestStrings(builder, "form", "global", "form_default"))
                                .then(Commands.argument("slot", StringArgumentType.word())
                                        .then(Commands.argument("form", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> suggestForms(builder))
                                                .executes(context -> modify(context, "save", true)))
                                        .executes(context -> modify(context, "save", false)))))
                .then(Commands.literal("load")
                        .then(Commands.argument("scope", StringArgumentType.word())
                                .suggests((context, builder) -> suggestStrings(builder, "form", "global", "form_default"))
                                .then(Commands.argument("slot", StringArgumentType.word())
                                        .then(Commands.argument("form", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> suggestForms(builder))
                                                .executes(context -> modify(context, "load", true)))
                                        .executes(context -> modify(context, "load", false)))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("scope", StringArgumentType.word())
                                .suggests((context, builder) -> suggestStrings(builder, "form", "global", "form_default"))
                                .then(Commands.argument("slot", StringArgumentType.word())
                                        .then(Commands.argument("form", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> suggestForms(builder))
                                                .executes(context -> modify(context, "delete", true)))
                                        .executes(context -> modify(context, "delete", false)))))
                .then(Commands.literal("config")
                        .then(Commands.argument("value", BoolArgumentType.bool())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ModNetwork.CHANNEL.send(
                                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                                            new ModifyFcdPacket("config", EMPTY_FORM.toString(),
                                                    String.valueOf(BoolArgumentType.getBool(context, "value")),
                                                    "", "", ""));
                                    return SINGLE_SUCCESS;
                                })))
                .then(Commands.literal("list")
                        .then(Commands.argument("scope", StringArgumentType.word())
                                .suggests((context, builder) -> suggestStrings(builder, "form", "global", "form_default"))
                                .then(Commands.argument("form", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> suggestForms(builder))
                                        .executes(context -> modify(context, "list", true)))
                                .executes(context -> modify(context, "list", false))))
                .then(Commands.literal("to_chat")
                        .then(Commands.argument("target", StringArgumentType.word())
                                .suggests((context, builder) -> suggestStrings(builder, "local", "server"))
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests((context, builder) -> suggestStrings(builder, "raw", "command"))
                                        .then(Commands.argument("format", StringArgumentType.word())
                                                .suggests((context, builder) -> suggestStrings(builder, "base64", "hex"))
                                                .executes(FormColorCommand::toChat)))))
                .then(Commands.literal("set_color_from_string")
                        .then(Commands.argument("data", StringArgumentType.greedyString())
                                .executes(FormColorCommand::setColorFromString)));
        event.getDispatcher().register(Commands.literal("ssc").then(formColor));
    }

    private static int modify(CommandContext<CommandSourceStack> context, String command, boolean withForm)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String scope = StringArgumentType.getString(context, "scope");
        String slot = StringArgumentType.getString(context, "slot");
        ResourceLocation form = withForm ? ResourceLocationArgument.getId(context, "form")
                : FormManager.current(player).id();
        ModNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new ModifyFcdPacket(command, form.toString(), scope, slot, "", ""));
        return SINGLE_SUCCESS;
    }

    private static int toChat(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String target = StringArgumentType.getString(context, "target");
        String mode = StringArgumentType.getString(context, "mode");
        String format = StringArgumentType.getString(context, "format");
        FormTextureUtils.ColorSetting color = player.getCapability(ModCapabilities.PLAYER_SKIN)
                .map(data -> net.onixary.shapeShifterCurseForge.client.color.FormColorData.abgr2Argb(data.getFormColor()))
                .orElse(null);
        if (color == null) {
            context.getSource().sendFailure(Component.literal("No skin color set"));
            return 0;
        }
        String encoded = net.onixary.shapeShifterCurseForge.client.color.FormColorData.colorSettingToString(
                color, "base64".equals(format));
        if (encoded == null) {
            context.getSource().sendFailure(Component.literal("Encode failed"));
            return 0;
        }
        Component message = "command".equals(mode)
                ? net.onixary.shapeShifterCurseForge.client.color.FormColorData.toCopyableText(encoded, encoded)
                : Component.literal(encoded);
        if ("server".equals(target)) {
            PlayerList players = player.server.getPlayerList();
            players.broadcastSystemMessage(message, false);
        } else {
            player.sendSystemMessage(message);
        }
        return SINGLE_SUCCESS;
    }

    private static int setColorFromString(CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String data = StringArgumentType.getString(context, "data");
        FormTextureUtils.ColorSetting parsed =
                net.onixary.shapeShifterCurseForge.client.color.FormColorData.colorSettingFromString(data);
        if (parsed == null) {
            context.getSource().sendFailure(Component.literal("Unrecognized color string"));
            return 0;
        }
        FormTextureUtils.ColorSetting abgr =
                net.onixary.shapeShifterCurseForge.client.color.FormColorData.argb2Abgr(parsed);
        player.getCapability(ModCapabilities.PLAYER_SKIN).ifPresent(skin -> {
            skin.setFormColor(abgr);
            ModNetwork.sendSkinSync(player);
        });
        context.getSource().sendSuccess(() -> Component.literal("Skin color updated"), false);
        return SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestStrings(SuggestionsBuilder builder, String... options) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(option);
            }
        }
        return builder.buildFuture();
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
