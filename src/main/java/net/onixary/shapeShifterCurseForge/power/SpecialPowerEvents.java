package net.onixary.shapeShifterCurseForge.power;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

/** Death prevention and projectile world interactions that originally required Fabric mixins. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class SpecialPowerEvents {
    private SpecialPowerEvents() { }

    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        final boolean[] used = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (used[0] || !"shape-shifter-curse:virtual_totem".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))
                    || FormActivePowerService.resource(player, id) >= 1.0D) return;
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
            FormActivePowerService.triggerCooldown(player, id);
            player.level().broadcastEntityEvent(player, (byte) 35);
            player.playSound("shape-shifter-curse:form_anubis_wolf_3_undying".equals(
                    FormPowerRuntime.stringValue(power, "virtual_totem_type", ""))
                    ? SoundEvents.WITHER_DEATH : SoundEvents.TOTEM_USE, 1.0F, 1.0F);
            used[0] = true;
        });
    }

    @SubscribeEvent
    public static void levelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;
        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof Snowball snowball) || !(snowball.getOwner() instanceof Player player)
                    || !hasSnowballTransform(player)) continue;
            BlockPos pos = snowball.blockPosition();
            if (level.getFluidState(pos).is(FluidTags.WATER)) {
                level.setBlockAndUpdate(pos, Blocks.ICE.defaultBlockState());
                level.playSound(null, pos, SoundEvents.GLASS_PLACE, player.getSoundSource(), 1.0F, 1.0F);
                snowball.discard();
            } else if (level.getFluidState(pos).is(FluidTags.LAVA)) {
                level.setBlockAndUpdate(pos, level.getFluidState(pos).isSource()
                        ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.STONE.defaultBlockState());
                level.playSound(null, pos, SoundEvents.LAVA_EXTINGUISH, player.getSoundSource(), 1.0F, 1.0F);
                snowball.discard();
            }
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
