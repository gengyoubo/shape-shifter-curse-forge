package net.onixary.shapeShifterCurseForge.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraft.server.level.ServerPlayer;
import net.onixary.shapeShifterCurseForge.api.PlayerFormData;
import net.onixary.shapeShifterCurseForge.api.SscApi;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.block.entity.FormAttunerBlockEntity;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;

import java.util.Optional;
import java.util.Set;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "8";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    // TODO[C-S] Packets are registered on both dists during mod construction. Client-bound handlers
    //   must not reference client-only classes (Screens/Minecraft) directly, or the dedicated server
    //   throws "invalid dist DEDICATED_SERVER" while registering them. Route client work through
    //   net.onixary.shapeShifterCurseForge.client.ClientPacketHandlers.
    public static void initialize() {
        CHANNEL.registerMessage(
                0,
                SyncFormPacket.class,
                SyncFormPacket::encode,
                SyncFormPacket::decode,
                SyncFormPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                1,
                ActivePowerKeyPacket.class,
                ActivePowerKeyPacket::encode,
                ActivePowerKeyPacket::decode,
                ActivePowerKeyPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                2,
                PowerAnimationPacket.class,
                PowerAnimationPacket::encode,
                PowerAnimationPacket::decode,
                PowerAnimationPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                3,
                UpdateSkinPacket.class,
                UpdateSkinPacket::encode,
                UpdateSkinPacket::decode,
                UpdateSkinPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                4,
                SyncSkinPacket.class,
                SyncSkinPacket::encode,
                SyncSkinPacket::decode,
                SyncSkinPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                5,
                ValidateStartBookPacket.class,
                ValidateStartBookPacket::encode,
                ValidateStartBookPacket::decode,
                ValidateStartBookPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                6,
                OpenSelectFormPacket.class,
                OpenSelectFormPacket::encode,
                OpenSelectFormPacket::decode,
                OpenSelectFormPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                7,
                SetFormPacket.class,
                SetFormPacket::encode,
                SetFormPacket::decode,
                SetFormPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                8,
                ModifyFcdPacket.class,
                ModifyFcdPacket::encode,
                ModifyFcdPacket::decode,
                ModifyFcdPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                9,
                OpenColorMenuPacket.class,
                OpenColorMenuPacket::encode,
                OpenColorMenuPacket::decode,
                OpenColorMenuPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                10,
                CursedMoonSyncPacket.class,
                CursedMoonSyncPacket::encode,
                CursedMoonSyncPacket::decode,
                CursedMoonSyncPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                11,
                OpenFormAttunerPacket.class,
                OpenFormAttunerPacket::encode,
                OpenFormAttunerPacket::decode,
                OpenFormAttunerPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                12,
                UnlockPerkPacket.class,
                UnlockPerkPacket::encode,
                UnlockPerkPacket::decode,
                UnlockPerkPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                13,
                ManaSyncPacket.class,
                ManaSyncPacket::encode,
                ManaSyncPacket::decode,
                ManaSyncPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                14,
                VirtualTotemPacket.class,
                VirtualTotemPacket::encode,
                VirtualTotemPacket::decode,
                VirtualTotemPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                15,
                InstinctSyncPacket.class,
                InstinctSyncPacket::encode,
                InstinctSyncPacket::decode,
                InstinctSyncPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                16,
                ItemStoresSyncPacket.class,
                ItemStoresSyncPacket::encode,
                ItemStoresSyncPacket::decode,
                ItemStoresSyncPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                17,
                BatAttachStatePacket.class,
                BatAttachStatePacket::encode,
                BatAttachStatePacket::decode,
                BatAttachStatePacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                18,
                BatDetachRequestPacket.class,
                BatDetachRequestPacket::encode,
                BatDetachRequestPacket::decode,
                BatDetachRequestPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                19,
                TransformStatePacket.class,
                TransformStatePacket::encode,
                TransformStatePacket::decode,
                TransformStatePacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                20,
                MovementLockPacket.class,
                MovementLockPacket::encode,
                MovementLockPacket::decode,
                MovementLockPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                21,
                SyncForcedSneakingPacket.class,
                SyncForcedSneakingPacket::encode,
                SyncForcedSneakingPacket::decode,
                SyncForcedSneakingPacket::handle,
                Optional.of(net.minecraftforge.network.NetworkDirection.PLAY_TO_CLIENT)
        );
    }

    public static void sendFormSync(ServerPlayer player) {
        sendFormSync(player, false);
    }

    /** A true marker is sent only by FormManager after a genuine server-side form change. */
    public static void sendFormSync(ServerPlayer player, boolean playTransformAnimation) {
        SscApi.currentForm(player).ifPresent(data -> CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                packetFor(player, data, playTransformAnimation)
        ));
    }

    public static void sendFormSyncTo(ServerPlayer target, ServerPlayer receiver) {
        SscApi.currentForm(target).ifPresent(data -> CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> receiver),
                packetFor(target, data, false)
        ));
    }

    public static void sendForcedSneakingSync(ServerPlayer player, boolean forced) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new SyncForcedSneakingPacket(player.getId(), forced));
    }

    public static void sendForcedSneakingSyncTo(ServerPlayer target, ServerPlayer receiver, boolean forced) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver),
                new SyncForcedSneakingPacket(target.getId(), forced));
    }

    public static void sendPowerAnimation(ServerPlayer player, PowerAnimationPacket packet) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), packet.forEntity(player.getId()));
    }

    public static void sendBatAttachState(ServerPlayer player, net.minecraft.core.BlockPos pos,
                                          net.minecraft.core.Direction side) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new BatAttachStatePacket(player.getUUID(), pos, side));
    }

    public static void sendTransformState(ServerPlayer player, boolean transforming,
                                          net.minecraft.resources.ResourceLocation startForm,
                                          net.minecraft.resources.ResourceLocation endForm) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new TransformStatePacket(player.getId(), transforming,
                        startForm == null ? null : startForm.toString(),
                        endForm == null ? null : endForm.toString()));
    }

    public static void sendMovementLock(ServerPlayer player, int noMoveTicks, int noJumpTicks) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new MovementLockPacket(noMoveTicks, noJumpTicks));
    }

    public static void sendPowerAnimationTo(ServerPlayer target, ServerPlayer receiver, PowerAnimationPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> receiver), packet.forEntity(target.getId()));
    }

    public static void sendSkinSync(ServerPlayer player) {
        SscApi.currentSkin(player).ifPresent(data -> CHANNEL.send(
                PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                SyncSkinPacket.forPlayer(player, data)
        ));
    }

    public static void sendSkinSyncTo(ServerPlayer target, ServerPlayer receiver) {
        SscApi.currentSkin(target).ifPresent(data -> CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> receiver),
                SyncSkinPacket.forPlayer(target, data)
        ));
    }

    public static void sendCursedMoonSync(ServerPlayer player, boolean cursedMoonDay) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new CursedMoonSyncPacket(cursedMoonDay));
    }

    public static void sendManaSync(ServerPlayer player, String manaType, float mana, float maximum) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ManaSyncPacket(manaType, mana, maximum));
    }

    public static void sendInstinctSync(ServerPlayer player, float value, float rate,
                                        boolean visible, boolean locked) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new InstinctSyncPacket(value, rate, visible, locked));
    }

    public static void sendItemStores(ServerPlayer player, net.minecraft.nbt.CompoundTag stores) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ItemStoresSyncPacket(stores.copy()));
    }

    /** Plays the totem activation animation for a virtual totem's (possibly custom) stack. */
    public static void sendVirtualTotem(ServerPlayer player, net.minecraft.world.item.ItemStack stack) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new VirtualTotemPacket(stack));
    }

    /** Opens the form select menu for {@code player}, acting on {@code target}. */
    public static void sendOpenSelectForm(ServerPlayer player, Player target) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenSelectFormPacket(target.getGameProfile().getName(), target.getUUID()));
    }

    public static void sendOpenFormAttuner(ServerPlayer player, int level, int maxLevel, String formGroupId) {
        sendOpenFormAttuner(player, level, maxLevel, formGroupId, "");
    }

    private static void sendOpenFormAttuner(ServerPlayer player, int level, int maxLevel, String formGroupId,
                                             String statusKey) {
        Set<String> unlocked = SscApi.currentForm(player).map(data -> data.getUnlockedPerks().stream()
                .map(ResourceLocation::toString).collect(java.util.stream.Collectors.toUnmodifiableSet())).orElse(Set.of());
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new OpenFormAttunerPacket(level, maxLevel, formGroupId == null ? "" : formGroupId, unlocked,
                        statusKey == null ? "" : statusKey));
    }

    /** Reopens/refreshes the client view after an authoritative Perk purchase. */
    public static void refreshFormAttuner(ServerPlayer player) {
        refreshFormAttuner(player, "");
    }

    public static void refreshFormAttuner(ServerPlayer player, String statusKey) {
        var attuner = FormAttunerBlockEntity.getLastUsed(player);
        if (attuner == null) return;
        sendOpenFormAttuner(player, attuner.getAttunementLevel(),
                FormAttunerBlockEntity.getMaxLevel(),
                SscApi.currentForm(player).map(PlayerFormData::getFormGroupId).orElse(""), statusKey);
    }


    private static SyncFormPacket packetFor(ServerPlayer player,
                                            PlayerFormData data,
                                            boolean playTransformAnimation) {
        ResourceLocation formId = ResourceLocation.tryParse(data.getFormId());
        var assignedPowerIds = formId == null ? java.util.List.<ResourceLocation>of()
                : FormPowerRegistry.idsForForm(formId);
        return new SyncFormPacket(player.getId(), data.getFormId(), data.getPreviousFormId(), data.getFormGroupId(),
                data.getFormTier(), data.isContentEnabled(), assignedPowerIds, playTransformAnimation);
    }
}
