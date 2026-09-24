package net.onixary.shapeShifterCurseForge.mixin;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.onixary.shapeShifterCurseForge.registry.ModEntities;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

/** Adds the same structure-specific spawn pools used by Fabric for desert temples and mineshafts. */
@Mixin(Structure.class)
public abstract class TransformativeStructureSpawnsMixin {
    private static final TagKey<Structure> SSC_MINESHAFTS = TagKey.create(
            Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("minecraft", "mineshaft"));

    @Inject(method = "getModifiedStructureSettings", at = @At("RETURN"), cancellable = true, remap = false)
    private void ssc$addTransformativeStructureSpawns(CallbackInfoReturnable<Structure.StructureSettings> cir) {
        Structure structure = (Structure) (Object) this;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        Registry<Structure> structures = server.registryAccess().registryOrThrow(Registries.STRUCTURE);
        ResourceLocation id = structures.getKey(structure);
        var structureKey = structures.getResourceKey(structure).orElse(null);
        if (structureKey == null) return;
        MobCategory category;
        EntityType<?> entityType;
        int weight;
        int min;
        int max;

        if (ResourceLocation.fromNamespaceAndPath("minecraft", "desert_pyramid").equals(id)) {
            category = MobCategory.CREATURE;
            entityType = ModEntities.TRANSFORMATIVE_WOLF.get();
            weight = 20;
            min = 3;
            max = 5;
        } else if (structures.getHolder(structureKey).map(holder -> holder.is(SSC_MINESHAFTS)).orElse(false)) {
            category = MobCategory.MONSTER;
            entityType = ModEntities.TRANSFORMATIVE_SPIDER.get();
            weight = 5;
            min = 1;
            max = 2;
        } else {
            return;
        }

        Structure.StructureSettings current = cir.getReturnValue();
        Map<MobCategory, StructureSpawnOverride> overrides = new HashMap<>(current.spawnOverrides());
        overrides.put(category, new StructureSpawnOverride(StructureSpawnOverride.BoundingBoxType.PIECE,
                WeightedRandomList.create(new MobSpawnSettings.SpawnerData(entityType, weight, min, max))));
        cir.setReturnValue(new Structure.StructureSettings(current.biomes(), ImmutableMap.copyOf(overrides),
                current.step(), current.terrainAdaptation()));
    }
}
