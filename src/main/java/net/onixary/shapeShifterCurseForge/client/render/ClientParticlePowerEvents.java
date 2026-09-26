package net.onixary.shapeShifterCurseForge.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;

/** Apoli creates particle powers on each client during the owning entity's tick. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class ClientParticlePowerEvents {
    private ClientParticlePowerEvents() { }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player) || !player.level().isClientSide) return;
        Minecraft minecraft = Minecraft.getInstance();
        Player viewer = minecraft.player;
        if (viewer == null) return;
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:particle".equals(FormPowerRegistry.typeOf(power))) return;
            if (!FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            if (player.isInvisibleTo(viewer)
                    && !FormPowerRuntime.booleanValue(power, "visible_while_invisible", false)) return;
            if (player == viewer && minecraft.options.getCameraType().isFirstPerson()
                    && !FormPowerRuntime.booleanValue(power, "visible_in_first_person", false)) return;
            int frequency = Math.max(1, FormPowerRuntime.intValue(power, "frequency", 1));
            int count = FormPowerRuntime.intValue(power, "count", 1);
            double speed = FormPowerRuntime.doubleValue(power, "speed", 0.0D);
            if (player.tickCount % frequency != 0 || count <= 0 || speed < 0.0D) return;
            JsonElement particleData = power.get("particle");
            if (particleData == null) return;
            String particleName = particleData.isJsonObject()
                    ? FormPowerRuntime.stringValue(particleData.getAsJsonObject(), "type", "")
                    : particleData.getAsString();
            ResourceLocation particleId = ResourceLocation.tryParse(particleName);
            if (particleId == null || !(BuiltInRegistries.PARTICLE_TYPE.get(particleId) instanceof ParticleOptions particle)) return;
            JsonObject spread = power.getAsJsonObject("spread");
            double spreadX = FormPowerRuntime.doubleValue(spread, "x", 0.25D);
            double spreadY = FormPowerRuntime.doubleValue(spread, "y", 0.5D);
            double spreadZ = FormPowerRuntime.doubleValue(spread, "z", 0.25D);
            double offsetY = FormPowerRuntime.doubleValue(power, "offset_y", 1.0D);
            for (int i = 0; i < count; i++) {
                var random = player.getRandom();
                player.level().addParticle(particle,
                        player.getX() + random.nextGaussian() * spreadX,
                        player.getY() + offsetY + random.nextGaussian() * spreadY,
                        player.getZ() + random.nextGaussian() * spreadZ,
                        (2.0D * random.nextDouble() - 1.0D) * speed,
                        (2.0D * random.nextDouble() - 1.0D) * speed,
                        (2.0D * random.nextDouble() - 1.0D) * speed);
            }
        });
    }
}
