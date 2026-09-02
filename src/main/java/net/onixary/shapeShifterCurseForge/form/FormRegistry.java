package net.onixary.shapeShifterCurseForge.form;

import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class FormRegistry {
    public static final ResourceLocation ORIGINAL_BEFORE_ENABLE = id("original_before_enable");
    public static final ResourceLocation ORIGINAL_SHIFTER = id("original_shifter");

    private static final Map<ResourceLocation, FormDefinition> FORMS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, FormGroup> GROUPS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private FormRegistry() {
    }

    public static void bootstrap() {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;

        add("original_before_enable", "base_form", -1, 1, FormBodyType.NORMAL, 1.0F, 1.0F, 1.0F,
                "no_instinct", "inhibitor_immune", "no_cursed_moon_effect", "no_cursed_moon_target");
        add("original_shifter", "base_form", 0, 1, FormBodyType.NORMAL, 1.0F, 1.0F, 1.0F,
                "can_have_transform_effect", "transform_effect_can_apply", "no_instinct", "inhibitor_immune", "no_cursed_moon_target");

        addCreatureGroup("bat", new float[]{0.90F, 0.75F, 0.60F, 0.60F}, new float[]{1.0F, 1.0F, 1.0F, 0.70F}, FormBodyType.NORMAL);
        addCreatureGroup("axolotl", new float[]{1.0F, 1.0F, 0.90F, 0.90F}, null, FormBodyType.NORMAL);
        addCreatureGroup("ocelot", new float[]{0.95F, 0.85F, 0.65F, 0.75F}, new float[]{1.0F, 1.0F, 1.0F, 0.60F}, FormBodyType.NORMAL);
        addCreatureGroup("familiar_fox", new float[]{0.80F, 0.65F, 0.55F, 0.55F}, new float[]{1.0F, 1.0F, 1.0F, 0.60F}, FormBodyType.NORMAL);
        addCreatureGroup("snow_fox", new float[]{0.80F, 0.65F, 0.55F, 0.55F}, new float[]{1.0F, 1.0F, 1.0F, 0.60F}, FormBodyType.NORMAL);
        addCreatureGroup("anubis_wolf", new float[]{1.0F, 1.0F, 0.90F, 0.80F}, new float[]{1.0F, 1.0F, 1.0F, 0.60F}, FormBodyType.NORMAL);
        addCreatureGroup("spider", new float[]{1.0F, 0.85F, 0.90F, 0.90F}, null, FormBodyType.NORMAL);

        add("allay_sp", "allay_form", 1, 1, FormBodyType.NORMAL, 0.55F, 0.55F, 1.0F,
                "no_instinct", "no_cursed_moon_effect", "special_form");
        add("feral_cat_sp", "feral_cat_form", 1, 1, FormBodyType.FERAL, 0.55F, 0.55F, 0.60F,
                "no_instinct", "no_cursed_moon_effect", "special_form");

        add("snow_fox_3_sub_marbled_polecat", "snow_fox_form", 4, 1, FormBodyType.FERAL, 0.55F, 0.55F, 0.60F,
                "sub_form");
        add("bat_3_sub_avali", "bat_form", 4, 1, FormBodyType.NORMAL, 0.65F, 0.65F, 1.0F,
                "sub_form");
    }

    private static void addCreatureGroup(String path, float[] widths, float[] eyes, FormBodyType finalBodyType) {
        String groupPath = path + "_form";
        for (int index = 0; index < widths.length; index++) {
            int tier = index + 1;
            Set<String> flags = new LinkedHashSet<>(switch (tier) {
                case 1 -> Set.of("starter_form");
                case 3 -> Set.of("inhibitor_resist", "lock_instinct", "cursed_moon_final_form", "catalyst_resist", "can_transform_to_final_form");
                case 4 -> Set.of("final_form", "inhibitor_immune", "no_instinct", "no_cursed_moon_effect");
                default -> Set.of();
            });
            if (path.equals("bat") && tier >= 1) {
                flags.add("night_vision");
            }
            if (path.equals("bat") && tier >= 3) {
                flags.add("slow_fall");
            }
            if (path.equals("axolotl") && tier >= 1) {
                flags.add("water_breathing");
            }
            if (path.equals("spider") && tier >= 3) {
                flags.add("night_vision");
                flags.add("climb");
            }
            if (path.equals("spider") && tier <= 1) {
                flags.add("catalyst_immune");
            }
            if (path.equals("spider") && (tier == 1 || tier >= 3)) {
                flags.add("poison_immune");
            }
            FormBodyType bodyType = tier == 4 && (path.equals("ocelot") || path.contains("fox") || path.equals("anubis_wolf"))
                    ? finalBodyType == FormBodyType.NORMAL ? FormBodyType.FERAL : finalBodyType
                    : FormBodyType.NORMAL;
            float eyeScale = eyes == null ? 1.0F : eyes[index];
            float fallProtection = switch (path + "_" + tier) {
                case "bat_2" -> 2.5F;
                case "axolotl_2", "axolotl_3" -> 6.0F;
                case "snow_fox_3" -> 1.5F;
                case "snow_fox_4" -> 2.5F;
                case "spider_1", "spider_3", "spider_4" -> 4.0F;
                default -> 0.0F;
            };
            float jumpBoost = switch (path + "_" + tier) {
                case "bat_4" -> 0.2F;
                case "ocelot_4" -> 0.25F;
                default -> 0.0F;
            };
            add(path + "_" + index, groupPath, tier, 1, bodyType, widths[index], widths[index], eyeScale,
                    fallProtection, jumpBoost, flags.toArray(String[]::new));
        }
    }

    private static void add(String path, String groupPath, int tier, int weight, FormBodyType bodyType,
                             float widthScale, float heightScale, float eyeScale, String... flags) {
        add(path, groupPath, tier, weight, bodyType, widthScale, heightScale, eyeScale,
                0.0F, 0.0F, flags);
    }

    private static void add(String path, String groupPath, int tier, int weight, FormBodyType bodyType,
                             float widthScale, float heightScale, float eyeScale,
                             float fallProtectionDistance, float jumpVelocityAddition, String... flags) {
        ResourceLocation groupId = id(groupPath);
        FormDefinition definition = new FormDefinition(id(path), groupId, tier, weight, bodyType,
                widthScale, heightScale, eyeScale, Set.of(flags), fallProtectionDistance, jumpVelocityAddition);
        FORMS.put(definition.id(), definition);
        GROUPS.computeIfAbsent(groupId, FormGroup::new).add(definition);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(ShapeShifterCurseForge.RESOURCE_NAMESPACE, path);
    }

    public static FormDefinition get(ResourceLocation id) {
        bootstrap();
        return FORMS.get(id);
    }

    public static FormDefinition get(String id) {
        return get(ResourceLocation.tryParse(id));
    }

    public static FormGroup getGroup(ResourceLocation id) {
        bootstrap();
        return GROUPS.get(id);
    }

    public static Map<ResourceLocation, FormDefinition> forms() {
        bootstrap();
        return Collections.unmodifiableMap(FORMS);
    }

    public static Map<ResourceLocation, FormGroup> groups() {
        bootstrap();
        return Collections.unmodifiableMap(GROUPS);
    }
}
