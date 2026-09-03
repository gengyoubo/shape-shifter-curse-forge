package net.onixary.shapeShifterCurseForge.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Client-only presentation options. Forge writes this to config/shape-shifter-curse-client.toml. */
public final class SscClientConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
/*
预计要做的动画形态一共有三种
覆盖型 / Overlay。它不推翻旧 PAL 动画，而是在旧动画基础上额外叠一层 Geo 动画。
重做型 / Replacement。这个就是真正把旧 PAL 动画替换掉，重新做一套新的主体动作。
添加型 / Additional。原版 SSC 原本根本没有对应动画，但你给某个动作、技能或者特殊状态新增动画。
 */
    public static final ForgeConfigSpec.BooleanValue PREFER_NEW_ANIMATIONS = BUILDER
            .comment("Use player_animation/new/<animation>.json when present.",
                    "If the new file or its animation key is missing, SSC falls back to the old animation.")
            .define("animations.prefer_new_animations", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private SscClientConfig() {
    }
}
