package net.onixary.shapeShifterCurseForge.client.codex;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.form.FormManager;

/** Centralized Codex content: headers, status, per-form texts and tier descriptions. */
public final class CodexData {
    private CodexData() {
    }

    public enum ContentType {
        TITLE,
        EQUIP,
        APPEARANCE,
        PROS,
        CONS,
        INSTINCTS,
        NAME
    }

    public static final Component headerStatus = Component.translatable("codex.header.status");
    public static final Component headerEquip = Component.translatable("codex.header.equip");
    public static final Component headerAppearance = Component.translatable("codex.header.appearance");
    public static final Component headerPros = Component.translatable("codex.header.pros");
    public static final Component headerCons = Component.translatable("codex.header.cons");
    public static final Component headerInstincts = Component.translatable("codex.header.instincts");

    private static final Component statusNormal = Component.translatable("codex.status.normal");
    private static final Component statusInfected = Component.translatable("codex.status.infected");
    private static final Component statusBeforeMoon = Component.translatable("codex.status.before_moon");
    private static final Component statusUnderMoon = Component.translatable("codex.status.under_moon");

    private static final Component descAppearance_normal = Component.translatable("codex.desc.appearance_normal");
    private static final Component descPros_normal = Component.translatable("codex.desc.pros_normal");
    private static final Component descCons_normal = Component.translatable("codex.desc.cons_normal");
    private static final Component descInstincts_normal = Component.translatable("codex.desc.instincts_normal");
    private static final Component descAppearance_0 = Component.translatable("codex.desc.appearance_0");
    private static final Component descPros_0 = Component.translatable("codex.desc.pros_0");
    private static final Component descCons_0 = Component.translatable("codex.desc.cons_0");
    private static final Component descInstincts_0 = Component.translatable("codex.desc.instincts_0");
    private static final Component descAppearance_1 = Component.translatable("codex.desc.appearance_1");
    private static final Component descPros_1 = Component.translatable("codex.desc.pros_1");
    private static final Component descCons_1 = Component.translatable("codex.desc.cons_1");
    private static final Component descInstincts_1 = Component.translatable("codex.desc.instincts_1");
    private static final Component descAppearance_2 = Component.translatable("codex.desc.appearance_2");
    private static final Component descPros_2 = Component.translatable("codex.desc.pros_2");
    private static final Component descCons_2 = Component.translatable("codex.desc.cons_2");
    private static final Component descInstincts_2 = Component.translatable("codex.desc.instincts_2");
    private static final Component descAppearance_3 = Component.translatable("codex.desc.appearance_3");
    private static final Component descPros_3 = Component.translatable("codex.desc.pros_3");
    private static final Component descCons_3 = Component.translatable("codex.desc.cons_3");
    private static final Component descInstincts_3 = Component.translatable("codex.desc.instincts_3");

    public static Component getPlayerStatusText(Player player) {
        // Mirrors Fabric: infected when a transformative effect is present, then the
        // cursed-moon day/night states, otherwise normal.
        // TODO: transformative effect check picks up once that system lands.
        StringBuilder statusTextBuilder = new StringBuilder();
        boolean hasAnyStatus = false;

        if (hasTransformativeEffect(player)) {
            statusTextBuilder.append(statusInfected.getString());
            hasAnyStatus = true;
        }

        if (CursedMoonData.isCursedMoonDay(player.level())) {
            if (CursedMoonData.isNight(player.level())) {
                statusTextBuilder.append(statusUnderMoon.getString());
            } else {
                statusTextBuilder.append(statusBeforeMoon.getString());
            }
            hasAnyStatus = true;
        }

        if (!hasAnyStatus) {
            statusTextBuilder.append(statusNormal.getString());
        }

        return Component.literal(statusTextBuilder.toString());
    }

    private static boolean hasTransformativeEffect(Player player) {
        // TODO: transformative mob-effect system is not ported yet.
        return false;
    }

    public static Component getDescText(ContentType type, Player player) {
        int tier = FormManager.current(player).tier();
        if (type == ContentType.INSTINCTS) {
            return switch (tier) {
                case -1, 0 -> descInstincts_normal;
                case 1 -> descInstincts_0;
                case 2 -> descInstincts_1;
                case 3 -> descInstincts_2;
                case 4 -> descInstincts_3;
                default -> Component.empty();
            };
        }
        return Component.empty();
    }

    public static Component getContentText(ContentType type, Player player) {
        return Component.translatable("codex.form." + FormManager.current(player).id().getNamespace()
                + "." + FormManager.current(player).id().getPath() + "." + type.toString().toLowerCase());
    }
}
