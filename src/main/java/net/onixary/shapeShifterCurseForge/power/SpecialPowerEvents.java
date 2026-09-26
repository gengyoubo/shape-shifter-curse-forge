package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

/** Death prevention and projectile world interactions that originally required Fabric mixins. */
@SuppressWarnings("deprecation")
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class SpecialPowerEvents {
    private SpecialPowerEvents() { }

    /** One usable virtual_totem power, ordered by priority like Fabric's comparator. */
    private record TotemCandidate(net.minecraft.resources.ResourceLocation id, JsonObject power,
                                  double priority, int priorityTier) { }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        java.util.List<TotemCandidate> candidates = new java.util.ArrayList<>();
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:virtual_totem".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))
                    || FormActivePowerService.resource(player, id) >= 1.0D) return;
            boolean highPriority = FormPowerRuntime.booleanValue(power, "high_priority", false);
            boolean lowPriority = FormPowerRuntime.booleanValue(power, "low_priority", false);
            if (highPriority && lowPriority) return;
            int tier = highPriority ? 2 : lowPriority ? 0 : 1;
            candidates.add(new TotemCandidate(id, power,
                    FormPowerRuntime.doubleValue(power, "priority", 1000.0D), tier));
        });
        if (candidates.isEmpty()) return;
        // Fabric checks high, normal, then low priority groups, sorting each by priority.
        candidates.sort(java.util.Comparator.comparingInt(TotemCandidate::priorityTier).reversed()
                .thenComparing(java.util.Comparator.comparingDouble(TotemCandidate::priority).reversed()));
        TotemCandidate chosen = candidates.get(0);
        JsonObject power = chosen.power();

        event.setCanceled(true);
        player.setHealth(Math.max(1.0F, FormPowerRuntime.floatValue(power, "totem_health", 1.0F)));
        player.clearFire();
        if (power.has("totem_status_effects") && power.get("totem_status_effects").isJsonArray()) {
            for (JsonElement effect : power.getAsJsonArray("totem_status_effects")) {
                if (!effect.isJsonObject()) continue;
                JsonObject apply = new JsonObject();
                apply.addProperty("type", "apoli:apply_effect");
                apply.add("effect", effect.getAsJsonObject());
                FormPowerRuntime.execute(player, player, apply);
            }
        }
        if (power.has("entity_actions") && power.get("entity_actions").isJsonArray()) {
            for (JsonElement action : power.getAsJsonArray("entity_actions")) {
                if (action.isJsonObject()) FormPowerRuntime.execute(player, player, action.getAsJsonObject());
            }
        }
        FormActivePowerService.triggerCooldown(player, chosen.id());
        // Custom totem_stack drives the client activation animation (falls back to the map's default).
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            net.onixary.shapeShifterCurseForge.network.ModNetwork.sendVirtualTotem(serverPlayer,
                    itemStackFromJson(power.getAsJsonObject("totem_stack")));
        }
        player.playSound("shape-shifter-curse:form_anubis_wolf_3_undying".equals(
                FormPowerRuntime.stringValue(power, "virtual_totem_type", ""))
                ? SoundEvents.WITHER_DEATH : SoundEvents.TOTEM_USE, 1.0F, 1.0F);
    }

    private static net.minecraft.world.item.ItemStack itemStackFromJson(JsonObject data) {
        if (data == null || !data.has("item")) return net.minecraft.world.item.ItemStack.EMPTY;
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(
                FormPowerRuntime.stringValue(data, "item", ""));
        net.minecraft.world.item.Item item = id == null ? null
                : net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
        return item == null ? net.minecraft.world.item.ItemStack.EMPTY
                : new net.minecraft.world.item.ItemStack(item, FormPowerRuntime.intValue(data, "count", 1));
    }

    /** Called at Entity.tick HEAD and at fluid-state update, as in Fabric. */
    public static void transformSnowballFluid(Snowball snowball, boolean discardOnFluid) {
        if (!(snowball.level() instanceof net.minecraft.server.level.ServerLevel level)
                || !(snowball.getOwner() instanceof Player player)
                || !hasSnowballTransform(player)) return;
        BlockPos pos = snowball.blockPosition();
        var fluid = level.getFluidState(pos);
        if (fluid.isEmpty()) return;
        if (fluid.is(FluidTags.WATER)) {
                level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY_POWDER_SNOW, SoundSource.BLOCKS, 0.8F, 1.2F);
                level.playSound(null, pos, SoundEvents.SNOW_PLACE, SoundSource.BLOCKS, 0.6F, 1.5F);
        } else if (fluid.is(FluidTags.LAVA)) {
                boolean source = fluid.isSource();
                level.setBlockAndUpdate(pos, source
                        ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.STONE.defaultBlockState());
                level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS,
                        source ? 1.0F : 0.8F, source ? 0.8F : 1.0F);
                level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.5F);
        }
        if (discardOnFluid) {
            level.broadcastEntityEvent(snowball, (byte) 3);
            snowball.discard();
        }
    }

    private static boolean hasSnowballTransform(Player player) {
        final boolean[] result = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("shape-shifter-curse:snowball_block_transform".equals(FormPowerRegistry.typeOf(power))) result[0] = true;
        });
        return result[0];
    }
}
