package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.phys.AABB;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.config.SscClientConfig;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Forge-side equivalent of the Fabric player animation FSM. */
public final class FormAnimationSystem {
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();
    // TEMP-DEBUG: diagnose why sneak crawling clips don't play. Remove after confirmed.
    private static String lastDebugLine = "";
    private static final String ANIMATION_PATH = "player_animation/";
    private static final String NEW_ANIMATION_PATH = ANIMATION_PATH + "new/";
    /** Ticks of dry time bridged before leaving the swim state (surface-bob flicker). */
    private static final int WATER_EXIT_GRACE_TICKS = 4;
    private static final ResourceLocation RIDING_ANIMATIONS = resource(ANIMATION_PATH + "form_riding_animation.json");
    private static final AnimationProfile SHARED_ANIMATIONS = AnimationProfile.builder()
            .animation("bat_2_riding", "form_riding_animation", "bat_2_riding", 1.0F, 2)
            .animation("bat_3_riding", "form_riding_animation", "bat_3_riding", 1.0F, 2)
            .animation("ocelot_2_riding", "form_riding_animation", "ocelot_2_riding", 1.0F, 2)
            .animation("ocelot_3_riding", "form_riding_animation", "ocelot_3_riding", 1.0F, 2)
            .animation("familiar_fox_2_riding", "form_riding_animation", "familiar_fox_2_riding", 1.0F, 2)
            .animation("familiar_fox_3_riding", "form_riding_animation", "familiar_fox_3_riding", 1.0F, 2)
            .animation("snow_fox_2_riding", "form_riding_animation", "snow_fox_2_riding", 1.0F, 2)
            .animation("snow_fox_3_riding", "form_riding_animation", "snow_fox_3_riding", 1.0F, 2)
            .animation("feral_cat_sp_riding", "form_riding_animation", "feral_cat_sp_riding", 1.0F, 2)
            .build();
    private static final Map<String, AnimationProfile> ANIMATION_PROFILES = Map.of(
            "bat_3", AnimationProfile.builder()
                    // bat_3_run.json deliberately contains the bat_3_walk animation.
                    .animation("bat_3_sprint", "bat_3_run", "bat_3_walk", 2.4F, 2)
                    .fallback(SHARED_ANIMATIONS)
                    .build(),
            "ocelot_3", AnimationProfile.builder()
                    .animation("ocelot_3_sneak_rush", "form_feral_common_run", "form_feral_common_run", 3.3F, 2)
                    .fallback(SHARED_ANIMATIONS)
                    .build(),
            "axolotl_3", AnimationProfile.builder()
                    // RushJumpAnimController belongs to Axolotl 3, rather than the
                    // cross-form animation registry. Its longer controller blend is
                    // therefore defined with the form that owns the behavior.
                    .animation("axolotl_3_rush_jump", "axolotl_3_rush_jump", "axolotl_3_rush_jump", 1.0F, 10)
                    .fallback(SHARED_ANIMATIONS)
                    .build()
    );
    private static final Map<UUID, TransitionSnapshot> TRANSITIONS = new HashMap<>();
    private static final Map<UUID, MotionSnapshot> MOTION_SNAPSHOTS = new HashMap<>();

    private FormAnimationSystem() {
    }

    public record Selection(String id, String animationId, ResourceLocation resource,
                            ResourceLocation fallbackResource, float speed, int fade) {
        public static Selection of(String id) {
            return of(id, defaultSpeed(id), 2);
        }

        public static Selection of(String id, float speed) {
            return of(id, speed, 2);
        }

        public static Selection of(String id, float speed, int fade) {
            ResourceLocation legacy = FormAnimationSystem.resource(ANIMATION_PATH + id + ".json");
            ResourceLocation source = FormAnimationSystem.preferredResource(id, legacy);
            ResourceLocation fallback = source.equals(legacy) ? null : legacy;
            if (!hasResource(source) && id.endsWith("_riding") && hasResource(RIDING_ANIMATIONS)) {
                source = RIDING_ANIMATIONS;
                fallback = null;
            }
            return new Selection(id, id, source, fallback, speed, fade);
        }

        private static Selection fromProfile(String logicalId, AnimationProfile.Clip clip) {
            ResourceLocation legacy = FormAnimationSystem.resource(ANIMATION_PATH + clip.resourceFile() + ".json");
            ResourceLocation source = FormAnimationSystem.preferredResource(clip.resourceFile(), legacy);
            return new Selection(logicalId, clip.animationId(), source,
                    source.equals(legacy) ? null : legacy, clip.speed(), clip.fade());
        }
    }

    private enum State {
        SLEEP, RIDE, CLIMB, SWIM, FLYING, FALL_FLYING, FALL, JUMP,
        USE_ITEM, BLOCK, MINING, ATTACK, CRAWL, WALK, SPRINT, IDLE
    }

    public static Selection select(Player player) {
        FormDefinition form = FormManager.current(player);
        String path = form.id().getPath();
        Selection transition = transitionAnimation(player);
        if (transition != null) return transition;
        State state = stateOf(player);
        // Yarn PlayerEntity#isSneaking is Mojmap Player#isShiftKeyDown. isCrouching
        // is the pose flag instead and becomes false/late during several crawl states.
        boolean sneak = player.isShiftKeyDown();
        List<String> offered = candidates(path, state, sneak, player);
        for (String candidate : offered) {
            Selection selection = selectionFor(path, state, candidate);
            if (hasAnimation(selection)) {
                debugSelection(player, path, state, sneak, offered, selection);
                return selection;
            }
        }
        debugSelection(player, path, state, sneak, offered, null);
        return null;
    }

    /**
     * TEMP-DEBUG: logs the FSM decision for axolotl_3 so a missing crawling clip can be
     * traced to its exact stage (state, sneak flag, candidates, per-candidate resource
     * resolution). Only fires on decision change to avoid log spam. Remove after confirmed.
     */
    private static void debugSelection(Player player, String path, State state, boolean sneak,
                                       List<String> offered, Selection selected) {
        if (!"axolotl_3".equals(path)) {
            return;
        }
        StringBuilder detail = new StringBuilder();
        for (String candidate : offered) {
            Selection probe = selectionFor(path, state, candidate);
            detail.append(candidate).append('=').append(hasAnimation(probe)).append(' ');
        }
        String line = "state=" + state + " sneak=" + sneak + " offered=" + offered
                + " resolved=[" + detail.toString().trim() + "] selected="
                + (selected == null ? "null" : selected.animationId());
        if (!line.equals(lastDebugLine)) {
            lastDebugLine = line;
            LOGGER.info("[SSC-ANIM-DEBUG] {}", line);
        }
    }

    /** Resolves Fabric's registered power-animation ids to a concrete SSC player clip. */
    public static Selection powerSelection(Player player, ResourceLocation powerAnimationId) {
        if (powerAnimationId == null) return null;
        String path = powerAnimationId.getPath();
        String animation = switch (path) {
            case "attach_side" -> FormManager.current(player).id().getPath().equals("bat_3_sub_avali")
                    ? "avali_attach_side" : "bat_3_attach_side";
            case "attach_bottom" -> "bat_3_attach_bottom";
            default -> path;
        };
        Selection selection = Selection.of(animation);
        return hasAnimation(selection) ? selection : null;
    }

    /**
     * Seeds the current form without a transition. Used for the first server sync after
     * joining a world, where the old client-side fallback form was never a real transform.
     */
    public static void prime(Player player) {
        TRANSITIONS.remove(player.getUUID());
    }

    /** Starts TransformingController only after a server-confirmed form change. */
    public static void startTransition(Player player, String previousFormId) {
        ResourceLocation previousId = ResourceLocation.tryParse(previousFormId);
        FormDefinition previous = previousId == null ? null : FormRegistry.get(previousId);
        FormDefinition current = FormManager.current(player);
        TRANSITIONS.put(player.getUUID(), new TransitionSnapshot(
                previous == null ? current.bodyType() : previous.bodyType(), current.bodyType(),
                player.tickCount + Minecraft.getInstance().getFrameTime()));
    }

    /** Clears world-specific animation snapshots before the next client world is initialized. */
    public static void clearClientState() {
        TRANSITIONS.clear();
        MOTION_SNAPSHOTS.clear();
    }

    private static Selection transitionAnimation(Player player) {
        UUID uuid = player.getUUID();
        double now = player.tickCount + Minecraft.getInstance().getFrameTime();
        TransitionSnapshot snapshot = TRANSITIONS.get(uuid);
        if (snapshot == null) {
            return null;
        }
        String animation = "player_on_transform";
        if (snapshot.previousBodyType == FormBodyType.FERAL && snapshot.currentBodyType != FormBodyType.FERAL) {
            animation = "player_on_transform_feral_to_normal";
        } else if (snapshot.previousBodyType == FormBodyType.NORMAL && snapshot.currentBodyType == FormBodyType.FERAL) {
            animation = "player_on_transform_normal_to_feral";
        }
        Selection selection = Selection.of(animation);
        if (!hasAnimation(selection)) {
            TRANSITIONS.remove(uuid);
            return null;
        }
        if (now - snapshot.changedAt >= Math.max(1.0F, BedrockAnimationPlayer.animationLength(selection)) * 20.0D) {
            TRANSITIONS.remove(uuid);
            return null;
        }
        return selection;
    }

    private static State stateOf(Player player) {
        MotionSnapshot motion = motionOf(player);
        boolean onGround = groundedForAnimation(player);
        if (player.isSleeping()) return State.SLEEP;
        if (player.isPassenger()) return State.RIDE;
        if (isClimbingForAnimation(player, onGround)) return State.CLIMB;
        // Fabric's v3 FSM treats any water contact as the universal swim state.  The
        // separate isSwimmingAnimation check below then chooses swim versus float.
        // Uses the graced contact from motionOf so surface bobbing cannot flip SWIM
        // against ground states frame to frame.
        if (motion.touchingWater) return State.SWIM;
        if (!onGround) {
            if (player.getAbilities().flying) return State.FLYING;
            if (player.isFallFlying()) return State.FALL_FLYING;
            if (motion.verticalDelta < 0.0D
                    && (FormManager.current(player).hasFlag("slow_fall") || player.fallDistance > 0.6F)) {
                return State.FALL;
            }
            return State.JUMP;
        }
        if (player.isUsingItem() || player.swinging) {
            if (player.isUsingItem()) return player.isBlocking() ? State.BLOCK : State.USE_ITEM;
            return motion.swingTicks >= 10 ? State.MINING : State.ATTACK;
        }
        if (player.isVisuallyCrawling()) return State.CRAWL;
        if (motion.moving) return player.isSprinting() ? State.SPRINT : State.WALK;
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
                case RIDE -> add(result, rideAnimation(player, "bat_2_riding", "bat_1_sneak_idle"));
                default -> { }
            }
        } else if (path.equals("bat_3")) {
            switch (state) {
                case IDLE -> add(result, sneak ? "bat_1_sneak_idle" : "bat_3_idle");
                case WALK -> add(result, sneak ? "bat_3_sneak_walk" : "bat_3_walk");
                // The Fabric controller intentionally speeds up the walk cycle for sprinting.
                case SPRINT -> add(result, sneak ? "bat_3_sneak_walk" : "bat_3_sprint");
                case JUMP -> add(result, "bat_3_jump");
                case FALL, FLYING, FALL_FLYING, CRAWL -> add(result, "bat_2_slow_falling");
                case MINING -> add(result, "bat_3_digging");
                case ATTACK -> add(result, "bat_3_attack");
                case CLIMB -> add(result, player.getDeltaMovement().y > 0.0D ? "bat_3_climb" : "bat_3_attach_side");
                case RIDE -> add(result, rideAnimation(player, "bat_3_riding", "bat_1_sneak_idle"));
                case SLEEP -> add(result, "bat_3_sleep");
                default -> { }
            }
        } else if (path.equals("axolotl_1")) {
            // Form_Axolotl1 inherits the normal form for every state except water.
            if (state == State.SWIM) add(result, "axolotl_2_swimming_idle");
        } else if (path.equals("axolotl_2")) {
            if (state == State.SWIM) add(result, isSwimmingAnimation(player) ? "axolotl_2_swimming" : "axolotl_2_swimming_idle");
            else if (sneak) {
                switch (state) {
                    case IDLE -> add(result, "axolotl_2_crawling_idle_new", "axolotl_2_crawling_idle");
                    // Form_Axolotl2 only overrides WALK. Sprint, physical crawling and
                    // falling deliberately inherit the normal-form controller.
                    case WALK -> add(result, "axolotl_2_crawling_new");
                    case JUMP -> add(result, "axolotl_2_crawling_jump");
                    case ATTACK -> add(result, "axolotl_2_crawling_attack_once");
                    case MINING -> add(result, "axolotl_2_crawling_tool_swing");
                    default -> { }
                }
            }
        } else if (path.equals("axolotl_3")) {
            switch (state) {
                case SWIM -> add(result, isSwimmingAnimation(player) ? "axolotl_2_swimming" : "axolotl_2_swimming_idle");
                case IDLE -> add(result, sneak ? "axolotl_3_crawling_idle" : "axolotl_3_idle");
                case WALK -> add(result, sneak ? "axolotl_3_crawling" : "axolotl_3_walk");
                case SPRINT -> add(result, sneak ? "axolotl_3_crawling" : "axolotl_3_run");
                case JUMP -> add(result, sneak ? "axolotl_2_crawling_jump"
                        : isRisingRushJump(player) ? "axolotl_3_rush_jump" : "axolotl_3_jump");
                case FALL -> add(result, sneak ? "axolotl_3_crawling_idle" : "axolotl_3_jump");
                // The Fabric WithSneak controllers intentionally have no normal
                // attack/mining animation for Axolotl 3.
                case ATTACK -> add(result, sneak ? "axolotl_2_crawling_attack_once" : null);
                case MINING -> add(result, sneak ? "axolotl_2_crawling_tool_swing" : null);
                case FLYING -> add(result, "axolotl_3_creative_flight");
                case SLEEP -> add(result, "axolotl_3_sleep");
                case CRAWL -> add(result, "axolotl_3_idle");
                default -> { }
            }
        } else if (path.equals("ocelot_2")) {
            switch (state) {
                case IDLE -> add(result, sneak ? "ocelot_2_sneak_idle" : null);
                case WALK, SPRINT -> add(result, canSneakRush(player, sneak) ? "ocelot_2_sneak_rush_2" : null);
                case JUMP, FALL -> add(result, canSneakRush(player, sneak) ? "ocelot_2_rush_jump" : null);
                case RIDE -> add(result, rideAnimation(player, "ocelot_2_riding", "ocelot_2_sneak_idle"));
                default -> { }
            }
        } else if (path.equals("familiar_fox_2") || path.equals("snow_fox_2")) {
            if (state == State.IDLE && sneak) add(result, "ocelot_2_sneak_idle");
            if (state == State.RIDE) add(result, rideAnimation(player, path + "_riding", "ocelot_2_sneak_idle"));
        } else if (path.equals("spider_1")) {
            if (state == State.IDLE) add(result, "spider_1_idle");
            if (state == State.WALK || state == State.SPRINT) add(result, "spider_1_move");
        } else if (path.equals("spider_2")) {
            if (state == State.IDLE && sneak) add(result, "spider_2_sneak_idle");
        } else if (path.equals("spider_3")) {
            switch (state) {
                case IDLE -> add(result, sneak ? "spider_3_sneak_idle" : "spider_3_idle");
                case WALK -> add(result, sneak ? "spider_3_sneak_walk" : "spider_3_walk");
                case SPRINT -> add(result, sneak ? "spider_3_sneak_walk" : "spider_3_run");
                case JUMP -> add(result, "spider_3_jump");
                case FALL -> add(result, "spider_3_fall");
                // Fabric only defines a float animation for Spider 3.  It is intentionally
                // used for both surface floating and the missing active-swim variant.
                case SWIM -> add(result, "spider_3_swim_idle");
                case CLIMB -> add(result, player.getDeltaMovement().y > 0.0D
                        ? "spider_3_climb" : "spider_3_climb_idle");
                case RIDE -> add(result, "spider_3_ride");
                case SLEEP -> add(result, "spider_3_sleep");
                case FLYING -> add(result, "spider_3_creative_flight");
                case BLOCK -> add(result, "spider_3_shielding");
                default -> { }
            }
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
            addFeral(result, state, sneak, player, "feral_cat_sp_riding", "form_feral_common_sneak_idle");
        } else if (path.equals("snow_fox_3")) {
            addFeral(result, state, sneak, player, "form_feral_common_sneak_idle", "snow_fox_3_riding");
            if (state == State.FALL) replaceLast(result, "form_snow_fox_3_fall");
        } else if (path.equals("ocelot_3")) {
            addFeral(result, state, sneak, player, "ocelot_3_riding", "form_feral_common_sneak_idle", true);
        } else if (path.equals("familiar_fox_3")) {
            addFeral(result, state, sneak, player, "familiar_fox_3_riding", "form_feral_common_sneak_idle");
        } else if (path.equals("anubis_wolf_3")) {
            addFeral(result, state, sneak, player, "form_feral_common_sneak_idle", "snow_fox_3_riding");
        }
        return result;
    }

    private static void addFeral(List<String> result, State state, boolean sneak, Player player,
                                 String ride, String vehicleRide) {
        addFeral(result, state, sneak, player, ride, vehicleRide, false);
    }

    private static void addFeral(List<String> result, State state, boolean sneak, Player player,
                                 String ride, String vehicleRide, boolean sneakRush) {
        switch (state) {
            case SLEEP -> add(result, "form_feral_common_sleep");
            case CLIMB -> add(result, player.getDeltaMovement().y > 0.0D ? "form_feral_common_climb" : "form_feral_common_climb_idle");
            case FALL -> add(result, "form_feral_common_fall");
            case JUMP -> add(result, "form_feral_common_jump");
            case RIDE -> add(result, rideAnimation(player, ride, vehicleRide));
            case SWIM -> add(result, isSwimmingAnimation(player) ? "form_feral_common_swim" : "form_feral_common_float");
            case USE_ITEM, BLOCK -> add(result, sneak ? "form_feral_common_sneak_idle" : "form_feral_common_idle");
            case WALK -> add(result, canSneakRush(player, sneakRush && sneak)
                    ? sneakRush ? "ocelot_3_sneak_rush" : "form_feral_common_run"
                    : sneak ? "form_feral_common_sneak_walk" : "form_feral_common_walk");
            case SPRINT -> add(result, canSneakRush(player, sneakRush && sneak)
                    ? sneakRush ? "ocelot_3_sneak_rush" : "form_feral_common_run"
                    : sneak ? "form_feral_common_sneak_walk" : "form_feral_common_run");
            case IDLE -> add(result, sneak ? "form_feral_common_sneak_idle" : "form_feral_common_idle");
            case MINING -> add(result, "form_feral_common_dig");
            case ATTACK -> add(result, "form_feral_common_attack");
            case FLYING, FALL_FLYING -> add(result, "form_feral_common_elytra_fly");
            case CRAWL -> add(result, sneak ? "form_feral_common_sneak_idle" : "form_feral_common_idle");
            default -> { }
        }
    }

    private static void addAvali(List<String> result, State state, boolean sneak, Player player) {
        switch (state) {
            case SLEEP -> add(result, "avali_sleep");
            case CLIMB -> add(result, player.getDeltaMovement().y > 0.0D ? "avali_climb" : "avali_attach_side");
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
            case CRAWL -> add(result, "avali_slow_falling");
            default -> { }
        }
    }

    private static void addWeasel(List<String> result, State state, boolean sneak, Player player) {
        switch (state) {
            case SLEEP -> add(result, "weasel_sleep");
            case CLIMB -> add(result, player.getDeltaMovement().y > 0.0D ? "weasel_climb" : "weasel_climb_idle");
            case FALL -> add(result, "weasel_fall");
            case JUMP -> add(result, "weasel_jump");
            case SWIM -> add(result, isSwimmingAnimation(player) ? "weasel_swim" : "weasel_float");
            case WALK -> add(result, sneak ? "weasel_sneak_walk" : "weasel_walk");
            case SPRINT -> add(result, sneak ? "weasel_sneak_walk" : "weasel_run");
            case IDLE -> add(result, !sneak && motionOf(player).idleTicks >= 100 ? "weasel_idle_stay" : sneak ? "weasel_sneak_idle" : "weasel_idle");
            case MINING -> add(result, "weasel_dig");
            case ATTACK -> add(result, "weasel_attack");
            case FALL_FLYING, FLYING -> add(result, "weasel_elytra_fly");
            case CRAWL -> add(result, sneak ? "weasel_sneak_idle" : "weasel_idle");
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
        return BedrockAnimationPlayer.hasAnimation(selection.resource(), selection.animationId())
                || selection.fallbackResource() != null
                && BedrockAnimationPlayer.hasAnimation(selection.fallbackResource(), selection.animationId());
    }

    private static Selection selectionFor(String formPath, State state, String animation) {
        AnimationProfile formProfile = ANIMATION_PROFILES.get(formPath);
        AnimationProfile.Clip clip = formProfile == null ? SHARED_ANIMATIONS.get(animation) : formProfile.get(animation);
        Selection selection;
        if (clip != null) {
            selection = Selection.fromProfile(animation, clip);
        } else {
            selection = Selection.of(animation);
        }
        return selection;
    }

    private static boolean canSneakRush(Player player, boolean sneaking) {
        return sneaking && player.getFoodData().getFoodLevel() >= 6;
    }

    /** Fabric's checkOnGroundSuper: flight is never grounded; otherwise include the tiny
     * collision volume immediately below the player to avoid frame-level ground flicker. */
    private static boolean groundedForAnimation(Player player) {
        if (player.onGround()) return true;
        if (player.getAbilities().flying) return false;
        AABB box = player.getBoundingBox().move(0.0D, -0.01D, 0.0D);
        AABB below = new AABB(box.minX, box.minY, box.minZ, box.maxX, player.getY(), box.maxZ);
        return !player.level().noCollision(player, below);
    }

    /** Fabric's climb FSM additionally rejects the situation where the lower body is
     * already supported by a collision shape, preventing a ground pose from flickering
     * into a climb pose at ladder bottoms. */
    private static boolean isClimbingForAnimation(Player player, boolean onGround) {
        if (!player.onClimbable() || onGround || player.getAbilities().flying || player.isFallFlying()) return false;
        AABB box = player.getBoundingBox().move(0.0D, -0.6D, 0.0D);
        AABB probe = new AABB(box.minX, box.minY, box.minZ, box.maxX, player.getY(), box.maxZ);
        return player.level().noCollision(player, probe);
    }

    private static String rideAnimation(Player player, String normal, String boatOrMinecart) {
        return player.getVehicle() instanceof Boat || player.getVehicle() instanceof Minecart
                ? boatOrMinecart : normal;
    }

    /**
     * Equivalent of Fabric's PlayerEntity#isTouchingWater for the animation FSM.
     * isInWaterOrBubble covers the body and the eye check keeps the state active while
     * the player's eyes are submerged at the surface, where the body check can flicker.
     */
    private static boolean isTouchingWater(Player player) {
        return player.isInWaterOrBubble() || player.isEyeInFluid(FluidTags.WATER);
    }

    /**
     * The original controllers use the swimming animation while the player is in the
     * swimming pose.  SwimAnimController in Fabric uses only this synchronized entity
     * state; the always-sprint-swimming power is responsible for setting it when needed.
     */
    private static boolean isSwimmingAnimation(Player player) {
        return player.isSwimming();
    }

    /** Exact RushJumpAnimController threshold: either horizontal component exceeds 0.15. */
    private static boolean isRushJump(Player player) {
        net.minecraft.world.phys.Vec3 velocity = player.getDeltaMovement();
        return Math.abs(velocity.x) > 0.15D || Math.abs(velocity.z) > 0.15D;
    }

    /**
     * The rush-jump pose exits as soon as vertical motion turns downward, rather than
     * lingering through the falling phase. Position-based delta is used instead of the
     * velocity field so the apex frame is evaluated exactly like the FALL state above.
     */
    private static boolean isRisingRushJump(Player player) {
        return isRushJump(player) && motionOf(player).verticalDelta >= 0.0D;
    }

    private static boolean hasResource(ResourceLocation location) {
        return Minecraft.getInstance().getResourceManager().getResource(location).isPresent();
    }

    private static ResourceLocation resource(String path) {
        return ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE, path);
    }

    private static ResourceLocation preferredResource(String animationFile, ResourceLocation legacy) {
        ResourceLocation modern = resource(NEW_ANIMATION_PATH + animationFile + ".json");
        return SscClientConfig.PREFER_NEW_ANIMATIONS.get() && hasResource(modern) ? modern : legacy;
    }

    private static float defaultSpeed(String id) {
        return switch (id) {
            case "form_feral_common_walk" -> 1.2F;
            case "form_feral_common_run" -> 2.3F;
            case "bat_3_walk" -> 1.7F;
            case "bat_3_digging", "bat_3_attack", "bat_3_jump" -> 1.5F;
            case "bat_3_climb" -> 1.25F;
            case "ocelot_2_sneak_rush_2" -> 3.3F;
            case "spider_3_walk" -> 1.2F;
            case "spider_3_run" -> 1.8F;
            case "avali_walk", "avali_run" -> 3.0F;
            case "avali_digging", "avali_attack" -> 1.8F;
            case "avali_jump" -> 1.5F;
            case "weasel_walk" -> 2.6F;
            case "weasel_run" -> 3.5F;
            default -> 1.0F;
        };
    }

    private static MotionSnapshot motionOf(Player player) {
        MotionSnapshot snapshot = MOTION_SNAPSHOTS.computeIfAbsent(player.getUUID(),
                ignored -> new MotionSnapshot(player.position()));
        if (snapshot.lastTick != player.tickCount) {
            // SSC Fabric v3 derives IsWalking from the player's actual movement between
            // ticks. Client delta movement can be zero while the local player is walking,
            // which previously made the Forge renderer fall through to vanilla idle arms.
            net.minecraft.world.phys.Vec3 position = player.position();
            boolean moving = !snapshot.lastPosition.equals(position);
            snapshot.verticalDelta = position.y - snapshot.lastPosition.y;
            if (player.swinging) {
                snapshot.swingTicks = snapshot.swinging ? snapshot.swingTicks + 1 : 1;
            } else {
                snapshot.swingTicks = 0;
            }
            if (!moving && !player.swinging && !player.isUsingItem() && !player.isPassenger()
                    && !player.isSleeping() && groundedForAnimation(player) && !player.isShiftKeyDown()
                    && !player.isVisuallyCrawling() && !isTouchingWater(player)
                    && !isClimbingForAnimation(player, groundedForAnimation(player))) {
                snapshot.idleTicks = snapshot.idle ? snapshot.idleTicks + 1 : 1;
            } else {
                snapshot.idleTicks = 0;
            }
            snapshot.swinging = player.swinging;
            snapshot.idle = snapshot.idleTicks > 0;
            snapshot.moving = moving;
            // Water contact gets a short exit grace: bobbing at the surface flickers
            // the raw body/eye checks frame to frame, and each flip restarts the
            // cross-fade from t=0 on both clips, which reads as a head twitch.
            // Entry stays immediate so dives respond at once.
            if (isTouchingWater(player)) {
                snapshot.lastWetTick = player.tickCount;
                snapshot.touchingWater = true;
            } else {
                snapshot.touchingWater = player.tickCount - snapshot.lastWetTick <= WATER_EXIT_GRACE_TICKS;
            }
            snapshot.lastPosition = position;
            snapshot.lastTick = player.tickCount;
        }
        return snapshot;
    }

    private static final class TransitionSnapshot {
        private final FormBodyType previousBodyType;
        private final FormBodyType currentBodyType;
        private final double changedAt;

        private TransitionSnapshot(FormBodyType previousBodyType, FormBodyType currentBodyType, double changedAt) {
            this.previousBodyType = previousBodyType;
            this.currentBodyType = currentBodyType;
            this.changedAt = changedAt;
        }
    }

    private static final class MotionSnapshot {
        private net.minecraft.world.phys.Vec3 lastPosition;
        private int lastTick = Integer.MIN_VALUE;
        private int swingTicks;
        private int idleTicks;
        private int lastWetTick = -1000;
        private boolean swinging;
        private boolean idle;
        private boolean moving;
        private boolean touchingWater;
        private double verticalDelta;

        private MotionSnapshot(net.minecraft.world.phys.Vec3 lastPosition) {
            this.lastPosition = lastPosition;
        }
    }
}
