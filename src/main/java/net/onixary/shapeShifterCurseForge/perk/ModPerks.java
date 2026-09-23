package net.onixary.shapeShifterCurseForge.perk;

import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.api.registry.Perk;
import net.onixary.shapeShifterCurseForge.api.registry.PerkTree;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;
import net.onixary.shapeShifterCurseForge.api.registry.SscRegistrar;
import net.onixary.shapeShifterCurseForge.api.registry.SscRegistryObject;

/** Built-in Perk declarations.  Effects belong to their respective ability systems; this class
 * owns only the attunement costs and the Evolution-style dependency graph. */
public final class ModPerks {
    private static final ResourceLocation AXOLOTL_GROUP = id("axolotl_form");
    private static final SscRegistrar REGISTRAR = SscJavaRegistries.registrar(ShapeShifterCurseForge.RESOURCE_NAMESPACE);

    public static final SscRegistryObject<Perk> AXOLOTL_BEGINNER_CURRENT = perk("axolotl_beginner_current", 1, 1, Perk.Kind.NON_POWER);
    public static final SscRegistryObject<Perk> AXOLOTL_WATER_GUN = perk("axolotl_water_gun", 1, 1, Perk.Kind.ACTIVE_POWER);
    public static final SscRegistryObject<Perk> AXOLOTL_SLOW_CURRENT = perk("axolotl_slow_current", 1, 1, Perk.Kind.PASSIVE_POWER);
    public static final SscRegistryObject<Perk> AXOLOTL_WATER_WALK = perk("axolotl_water_walk", 1, 1, Perk.Kind.PASSIVE_POWER);
    public static final SscRegistryObject<Perk> AXOLOTL_INTERMEDIATE_CURRENT = perk("axolotl_intermediate_current", 2, 2, Perk.Kind.GATE);
    public static final SscRegistryObject<Perk> AXOLOTL_HIGH_PRESSURE_GUN = perk("axolotl_high_pressure_gun", 3, 3, Perk.Kind.ACTIVE_POWER);
    public static final SscRegistryObject<Perk> AXOLOTL_WATER_PRISON = perk("axolotl_water_prison", 3, 3, Perk.Kind.ACTIVE_POWER);
    public static final SscRegistryObject<Perk> AXOLOTL_SURFING = perk("axolotl_surfing", 3, 3, Perk.Kind.PASSIVE_POWER);

    public static final SscRegistryObject<PerkTree> AXOLOTL_WATER = REGISTRAR.perkTree("axolotl_water", () ->
            PerkTree.builder(AXOLOTL_GROUP)
                    .branch(AXOLOTL_BEGINNER_CURRENT.id(), AXOLOTL_WATER_GUN.id(), AXOLOTL_SLOW_CURRENT.id(), AXOLOTL_WATER_WALK.id())
                    .edge(AXOLOTL_WATER_GUN.id(), AXOLOTL_INTERMEDIATE_CURRENT.id())
                    .edge(AXOLOTL_SLOW_CURRENT.id(), AXOLOTL_INTERMEDIATE_CURRENT.id())
                    .edge(AXOLOTL_WATER_WALK.id(), AXOLOTL_INTERMEDIATE_CURRENT.id())
                    .branch(AXOLOTL_INTERMEDIATE_CURRENT.id(), AXOLOTL_HIGH_PRESSURE_GUN.id(), AXOLOTL_WATER_PRISON.id(), AXOLOTL_SURFING.id())
                    .build());

    private ModPerks() {
    }

    public static void initialize() {
        REGISTRAR.init();
    }

    private static SscRegistryObject<Perk> perk(String path, int attunerLevel, int experienceLevels, Perk.Kind kind) {
        return REGISTRAR.perk(path, () -> Perk.builder()
                .requiredAttunerLevel(attunerLevel)
                .experienceLevels(experienceLevels)
                .kind(kind)
                .build());
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, path);
    }
}
