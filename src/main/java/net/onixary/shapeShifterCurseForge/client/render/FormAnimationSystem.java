package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormManager;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Forge-side equivalent of the Fabric player animation FSM. */
public final class FormAnimationSystem {
    private static final String ANIMATION_PATH = "player_animation/";
    private static final ResourceLocation RIDING_ANIMATIONS = resource(ANIMATION_PATH + "form_riding_animation.json");
    private static final Map<UUID, FormSnapshot> FORM_SNAPSHOTS = new HashMap<>();

    private FormAnimationSystem() {
    }

    public record Selection(String id, ResourceLocation resource, float speed) {
        public static Selection of(String id) {
            return of(id, 1.0F);
        }

        public static Selection of(String id, float speed) {
            ResourceLocation direct = FormAnimationSystem.resource(ANIMATION_PATH + id + ".json");
            ResourceLocation source = direct;
            if (!hasResource(direct) && id.endsWith("_riding") && hasResource(RIDING_ANIMATIONS)) {
                source = RIDING_ANIMATIONS;
            }
            return new Selection(id, source, speed);
        }
    }

    private enum State {
        SLEEP, RIDE, CLIMB, SWIM, FLYING, FALL_FLYING, FALL, JUMP,
        USE_ITEM, BLOCK, MINING, ATTACK, CRAWL, WALK, SPRINT, IDLE
    }

    public static Selection select(Player player) {
        FormDefinition form = FormManager.current(player);
        String path = form.id().getPath();
        Selection transition = transitionAnimation(player, form);
        if (transition != null) return transition;
        State state = stateOf(player);
        boolean sneak = player.isCrouching();
        for (String candidate : candidates(path, state, sneak, player)) {
            Selection selection = Selection.of(candidate);
            if (hasAnimation(selection)) return selection;
        }
        return null;
    }

    private static Selection transitionAnimation(Player player, FormDefinition form) {
        UUID uuid = player.getUUID();
        double now = player.tickCount + Minecraft.getInstance().getFrameTime();
        FormSnapshot snapshot = FORM_SNAPSHOTS.get(uuid);
        if (snapshot == null) {
            FORM_SNAPSHOTS.put(uuid, new FormSnapshot(form.id().getPath(), form.bodyType(), form.bodyType(), now));
            return null;
        }
        if (!snapshot.formPath.equals(form.id().getPath())) {
            snapshot = new FormSnapshot(form.id().getPath(), form.bodyType(), snapshot.currentBodyType, now);
            FORM_SNAPSHOTS.put(uuid, snapshot);
        }
        if (now - snapshot.changedAt >= 160.0D) return null;
        String animation = "player_on_transform";
        if (snapshot.previousBodyType == FormBodyType.FERAL && form.bodyType() != FormBodyType.FERAL) {
            animation = "player_on_transform_feral_to_normal";
        } else if (snapshot.previousBodyType == FormBodyType.NORMAL && form.bodyType() == FormBodyType.FERAL) {
            animation = "player_on_transform_normal_to_feral";
        }
        Selection selection = Selection.of(animation);
        return hasAnimation(selection) ? selection : null;
    }

    private static State stateOf(Player player) {
        if (player.isSleeping()) return State.SLEEP;
        if (player.isPassenger()) return State.RIDE;
        if (player.onClimbable() && !player.onGround() && !player.getAbilities().flying && !player.isFallFlying()) return State.CLIMB;
        if (player.isInWaterOrBubble()) return State.SWIM;
        if (player.getAbilities().flying) return State.FLYING;
        if (player.isFallFlying()) return State.FALL_FLYING;
        if (!player.onGround()) return player.getDeltaMovement().y < -0.02D ? State.FALL : State.JUMP;
        if (player.isBlocking()) return State.BLOCK;
        if (player.isUsingItem()) return State.USE_ITEM;
        if (player.swinging) return player.attackAnim > 0.5F ? State.MINING : State.ATTACK;
        if (player.isCrouching() && player.isVisuallyCrawling()) return State.CRAWL;
        if (player.getDeltaMovement().horizontalDistanceSqr() > 0.0004D) return player.isSprinting() ? State.SPRINT : State.WALK;
        return State.IDLE;
    }

    private static List<String> candidates(String path, State state, boolean sneak, Player player) {
        List<String> result = new ArrayList<>();
        if (path.equals("bat_1")) {
            addIf(result, state, State.JUMP, "bat_1_jump");
        } else if (path.equals("bat_2")) {
            switch (state) {
                case IDLE -> add(result, sneak ? "bat_1_sneak_idle" : null);
                case JUMP -> add(result, "bat_2_jump");
                case FALL, FLYING, FALL_FLYING -> add(result, "bat_2_slow_falling");
                case MINING -> add(result, "bat_2_digging");
                case ATTACK -> add(result, "bat_2_attack");
                case RIDE -> add(result, "bat_2_riding", "bat_1_sneak_idle");
                default -> { }
            }
        } else if (path.equals("bat_3")) {
            switch (state) {
                case IDLE -> add(result, sneak ? "bat_1_sneak_idle" : "bat_3_idle");
                case WALK -> add(result, sneak ? "bat_3_sneak_walk" : "bat_3_walk");
                case SPRINT -> add(result, sneak ? "bat_3_sneak_walk" : "bat_3_run");
                case JUMP -> add(result, "bat_3_jump");
                case FALL, FLYING, FALL_FLYING, CRAWL -> add(result, "bat_2_slow_falling");
                case MINING -> add(result, "bat_3_digging");
                case ATTACK -> add(result, "bat_3_attack");
                case CLIMB -> add(result, player.getDeltaMovement().y > 0.0D ? "bat_3_climb" : "bat_3_attach_side");
                case RIDE -> add(result, "bat_3_riding", "bat_1_sneak_idle");
                case SLEEP -> add(result, "bat_3_sleep");
                default -> { }
            }
        } else if (path.equals("axolotl_2")) {
            if (state == State.SWIM) add(result, player.isSwimming() ? "axolotl_2_swimming" : "axolotl_2_swimming_idle");
            else if (sneak) {
                switch (state) {
                    case IDLE -> add(result, "axolotl_2_crawling_idle_new", "axolotl_2_crawling_idle");
                    case WALK, SPRINT -> add(result, "axolotl_2_crawling_new");
                    case JUMP, FALL -> add(result, "axolotl_2_crawling_jump");
                    case ATTACK -> add(result, "axolotl_2_crawling_attack_once");
                    case MINING -> add(result, "axolotl_2_crawling_tool_swing");
                    default -> { }
                }
            }
        } else if (path.equals("axolotl_3")) {
            switch (state) {
                case SWIM -> add(result, player.isSwimming() ? "axolotl_2_swimming" : "axolotl_2_swimming_idle");
                case IDLE -> add(result, sneak ? "axolotl_3_crawling_idle" : "axolotl_3_idle");
                case WALK -> add(result, sneak ? "axolotl_3_crawling" : "axolotl_3_walk");
                case SPRINT -> add(result, sneak ? "axolotl_3_crawling" : "axolotl_3_run");
                case JUMP -> add(result, sneak ? "axolotl_2_crawling_jump" : "axolotl_3_jump");
                case FALL -> add(result, sneak ? "axolotl_3_crawling_idle" : "axolotl_3_jump");
                case ATTACK -> add(result, "axolotl_2_crawling_attack_once");
                case MINING -> add(result, "axolotl_2_crawling_tool_swing");
                case FLYING -> add(result, "axolotl_3_creative_flight");
                case SLEEP -> add(result, "axolotl_3_sleep");
                case CRAWL -> add(result, "axolotl_3_idle");
                default -> { }
            }
        } else if (path.equals("ocelot_2")) {
            switch (state) {
                case IDLE -> add(result, sneak ? "ocelot_2_sneak_idle" : null);
                case WALK, SPRINT -> add(result, sneak ? "ocelot_2_sneak_rush_2" : null);
                case JUMP, FALL -> add(result, "ocelot_2_rush_jump");
                case RIDE -> add(result, "ocelot_2_riding", "ocelot_2_sneak_idle");
                default -> { }
            }
        } else if (path.equals("familiar_fox_2") || path.equals("snow_fox_2")) {
            if (state == State.IDLE && sneak) add(result, "ocelot_2_sneak_idle");
            if (state == State.RIDE) add(result, path + "_riding", "ocelot_2_sneak_idle");
        } else if (path.equals("spider_1")) {
            if (state == State.IDLE) add(result, "spider_1_idle");
            if (state == State.WALK || state == State.SPRINT) add(result, "spider_1_move");
        } else if (path.equals("spider_2")) {
            if (state == State.IDLE && sneak) add(result, "spider_2_sneak_idle");
        } else if (path.equals("allay_sp")) {
            switch (state) {
                case IDLE -> add(result, sneak ? "allay_sp_sneaking" : "allay_sp_idle");
                case WALK -> add(result, sneak ? "allay_sp_sneaking_walk" : "allay_sp_moving");
                case SPRINT -> add(result, sneak ? "allay_sp_sneaking_walk" : "allay_sp_run");
                case MINING -> add(result, "allay_sp_digging");
                case ATTACK -> add(result, "allay_sp_attack");
                case JUMP, FALL, FLYING, FALL_FLYING -> add(result, "allay_sp_fly");
                default -> { }
            }
        } else if (path.equals("bat_3_sub_avali")) {
            addAvali(result, state, sneak, player);
        } else if (path.equals("snow_fox_3_sub_marbled_polecat")) {
            addWeasel(result, state, sneak, player);
        } else if (path.equals("feral_cat_sp")) {
            addFeral(result, state, sneak, player, "feral_cat_sp_riding");
        } else if (path.equals("snow_fox_3")) {
            addFeral(result, state, sneak, player, "snow_fox_3_riding");
            if (state == State.FALL) replaceLast(result, "form_snow_fox_3_fall");
        } else if (path.equals("ocelot_3")) {
            addFeral(result, state, sneak, player, "ocelot_3_riding");
        } else if (path.equals("familiar_fox_3")) {
            addFeral(result, state, sneak, player, "familiar_fox_3_riding");
        } else if (path.equals("anubis_wolf_3")) {
            addFeral(result, state, sneak, player, "snow_fox_3_riding");
        }
        return result;
    }

    private static void addFeral(List<String> result, State state, boolean sneak, Player player, String ride) {
        switch (state) {
            case SLEEP -> add(result, "form_feral_common_sleep");
            case CLIMB -> add(result, player.getDeltaMovement().y > 0.0D ? "form_feral_common_climb" : "form_feral_common_climb_idle");
            case FALL -> add(result, "form_feral_common_fall");
            case JUMP -> add(result, "form_feral_common_jump");
            case RIDE -> add(result, ride, "form_feral_common_sneak_idle");
            case SWIM -> add(result, player.isSwimming() ? "form_feral_common_swim" : "form_feral_common_float");
            case USE_ITEM, BLOCK -> add(result, sneak ? "form_feral_common_sneak_idle" : "form_feral_common_idle");
            case WALK -> add(result, sneak ? "form_feral_common_sneak_walk" : "form_feral_common_walk");
            case SPRINT -> add(result, sneak ? "form_feral_common_sneak_walk" : "form_feral_common_run");
            case IDLE -> add(result, sneak ? "form_feral_common_sneak_idle" : "form_feral_common_idle");
            case MINING -> add(result, "form_feral_common_dig");
            case ATTACK -> add(result, "form_feral_common_attack");
            case FLYING, FALL_FLYING -> add(result, "form_feral_common_elytra_fly");
            default -> { }
        }
    }

    private static void addAvali(List<String> result, State state, boolean sneak, Player player) {
        switch (state) {
            case SLEEP -> add(result, "avali_sleep");
            case CLIMB -> add(result, "avali_climb");
            case FALL -> add(result, "avali_slow_falling");
            case JUMP -> add(result, "avali_jump");
            case RIDE -> add(result, "avali_ride");
            case SWIM -> add(result, "avali_water_float");
            case WALK -> add(result, sneak ? "avali_sneak_walk" : "avali_walk");
            case SPRINT -> add(result, sneak ? "avali_sneak_walk" : "avali_run");
            case IDLE -> add(result, sneak ? "avali_sneak_idle" : "avali_idle");
            case MINING -> add(result, "avali_digging");
            case ATTACK -> add(result, "avali_attack");
            case FLYING -> add(result, "avali_slow_falling");
            case FALL_FLYING -> add(result, "avali_elytra_fly");
            case BLOCK -> add(result, "avali_shielding");
            default -> { }
        }
    }

    private static void addWeasel(List<String> result, State state, boolean sneak, Player player) {
        switch (state) {
            case SLEEP -> add(result, "weasel_sleep");
            case CLIMB -> add(result, player.getDeltaMovement().y > 0.0D ? "weasel_climb" : "weasel_climb_idle");
            case FALL -> add(result, "weasel_fall");
            case JUMP -> add(result, "weasel_jump");
            case SWIM -> add(result, player.isSwimming() ? "weasel_swim" : "weasel_float");
            case WALK -> add(result, sneak ? "weasel_sneak_walk" : "weasel_walk");
            case SPRINT -> add(result, sneak ? "weasel_sneak_walk" : "weasel_run");
            case IDLE -> add(result, sneak ? "weasel_sneak_idle" : "weasel_idle");
            case MINING -> add(result, "weasel_dig");
            case ATTACK -> add(result, "weasel_attack");
            case FALL_FLYING, FLYING -> add(result, "weasel_elytra_fly");
            default -> { }
        }
    }

    private static void add(List<String> result, String... ids) {
        for (String id : ids) if (id != null) result.add(id);
    }

    private static void addIf(List<String> result, State actual, State expected, String id) {
        if (actual == expected) add(result, id);
    }

    private static void replaceLast(List<String> result, String id) {
        if (!result.isEmpty()) result.set(result.size() - 1, id);
        else result.add(id);
    }

    private static boolean hasAnimation(Selection selection) {
        return hasResource(selection.resource());
    }

    private static boolean hasResource(ResourceLocation location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }

    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, path);
    }

    private static final class FormSnapshot {
        private final String formPath;
        private final FormBodyType currentBodyType;
        private final FormBodyType previousBodyType;
        private final double changedAt;

        private FormSnapshot(String formPath, FormBodyType currentBodyType, FormBodyType previousBodyType, double changedAt) {
            this.formPath = formPath;
            this.currentBodyType = currentBodyType;
            this.previousBodyType = previousBodyType;
            this.changedAt = changedAt;
        }
    }
}
