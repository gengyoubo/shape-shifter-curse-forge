package net.onixary.shapeShifterCurseForge.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only presentation options. Forge writes this to config/shape-shifter-curse-client.toml. */
public final class SscClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue PREFER_NEW_ANIMATIONS = BUILDER
            .comment("Use player_animation/new/<animation>.json when present.",
                    "If the new file or its animation key is missing, SSC falls back to the old animation.")
            .define("animations.prefer_new_animations", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SscClientConfig() {
    }
}
