package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.other.advancement.SscAdvancementTriggers;


/** Accumulates web-bullet binding time and fires the full-entanglement advancement. */
@SuppressWarnings("deprecation")
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class WebEntanglementService {
    private static final int FULL_THRESHOLD = 20 * 5 * 5;
    private static final TagKey<EntityType<?>> SPIDER_FLUID_COCOON_BLACKLIST = TagKey.create(
            Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE,
                    "spider_fluid_cocoon_blacklist"));

    private WebEntanglementService() {
    }

    public static void apply(Entity owner, LivingEntity target, int duration) {
        if (target.level().isClientSide || duration <= 0) {
            return;
        }
        var entangled = net.onixary.shapeShifterCurseForge.registry.ModEffects.ENTANGLED.get();
        var full = net.onixary.shapeShifterCurseForge.registry.ModEffects.ENTANGLED_FULL.get();
        if (target.hasEffect(full)) return;
        MobEffectInstance existing = target.getEffect(entangled);
        int newDuration = existing == null ? duration : existing.getDuration() + duration;
        int amplifier = existing == null ? duration / (20 * 5)
                : Math.min(existing.getAmplifier() + 1, 4);
        if (existing != null) target.removeEffect(entangled);
        target.addEffect(new MobEffectInstance(entangled, newDuration, amplifier));
        if (newDuration >= FULL_THRESHOLD) {
            target.removeEffect(entangled);
            target.addEffect(new MobEffectInstance(full,
                    target instanceof net.minecraft.world.entity.player.Player ? 20 * 5 : 20 * 15, 0));
            if (owner instanceof ServerPlayer player) {
                ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
                SscAdvancementTriggers.ON_WEB_ENTITY.triggerEntity(player, entityId);
            }
        }
    }

    /** Whether the entity is currently fully entangled (cocooned). */
    public static boolean isFull(LivingEntity target) {
        return target.hasEffect(net.onixary.shapeShifterCurseForge.registry.ModEffects.ENTANGLED_FULL.get());
    }

    /**
     * SSC's can_loot_spider_fluid_cocoon marker: a creature that dies while cocooned may
     * drop Nutrient Sacs when killed by a spider-form player.
     */
    @SubscribeEvent
    public static void onDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide) {
            return;
        }
        if (!target.hasEffect(net.onixary.shapeShifterCurseForge.registry.ModEffects.ENTANGLED_FULL.get())) {
            return;
        }

        var position = target.blockPosition();
        if (target.level().getBlockState(position).isAir()) {
            target.level().setBlock(position, Blocks.COBWEB.defaultBlockState(), 3);
        }

        if (!(target instanceof Mob mob)
                || !(event.getSource().getEntity() instanceof ServerPlayer player)
                || mob.getType().is(SPIDER_FLUID_COCOON_BLACKLIST)) {
            return;
        }
        if (!FormPowerRegistry.has(player, ResourceLocation.fromNamespaceAndPath(
                ShapeShifterCurseForge.RESOURCE_NAMESPACE, "can_loot_spider_fluid_cocoon"))) {
            return;
        }
        if (player.getRandom().nextInt(100) >= 40) {
            return;
        }
        int maxCount = net.minecraft.util.Mth.ceil(mob.getMaxHealth() / 4.0F);
        // Keep Fabric's nextInt(bound) behavior (exclusive upper bound), including its
        // one-item minimum for entities whose max health is below four.
        int count = Math.max(player.getRandom().nextInt(maxCount), 1);
        target.spawnAtLocation(new net.minecraft.world.item.ItemStack(
                net.onixary.shapeShifterCurseForge.registry.ModItems.SPIDER_FLUID_COCOON.get(), count));
    }

}
