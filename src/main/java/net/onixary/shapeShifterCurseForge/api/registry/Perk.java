package net.onixary.shapeShifterCurseForge.api.registry;

import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Immutable definition of one unlockable Form Attuner perk.
 *
 * <p>A perk deliberately does not contain its own id or prerequisite list. Its id is supplied by
 * {@link SscJavaRegistries#registerPerk(net.minecraft.resources.ResourceLocation, Perk)} and its
 * position and dependencies belong to one {@link PerkTree}. This keeps the definition reusable
 * and makes the tree, rather than an accidental field on a perk, the single source of truth for
 * the UI graph.</p>
 */
public final class Perk {
    private static final Consumer<ServerPlayer> NO_OP = player -> { };
    private static final Predicate<ServerPlayer> ALWAYS = player -> true;

    private final int requiredAttunerLevel;
    private final int experienceLevels;
    private final Consumer<ServerPlayer> onUnlocked;
    private final Predicate<ServerPlayer> unlockCondition;

    private Perk(Builder builder) {
        requiredAttunerLevel = builder.requiredAttunerLevel;
        experienceLevels = builder.experienceLevels;
        onUnlocked = builder.onUnlocked;
        unlockCondition = builder.unlockCondition;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Minimum active Form Attuner level needed to purchase this perk. */
    public int requiredAttunerLevel() {
        return requiredAttunerLevel;
    }

    /** Number of vanilla experience levels consumed on purchase. */
    public int experienceLevels() {
        return experienceLevels;
    }

    /** Invoked once, server-side, after this perk has been persisted for the player. */
    public void onUnlocked(ServerPlayer player) {
        onUnlocked.accept(Objects.requireNonNull(player, "player"));
    }

    /** Additional server-side rule evaluated after tree, Attuner and experience requirements. */
    public boolean canUnlock(ServerPlayer player) {
        return unlockCondition.test(Objects.requireNonNull(player, "player"));
    }

    public static final class Builder {
        private int requiredAttunerLevel;
        private int experienceLevels;
        private Consumer<ServerPlayer> onUnlocked = NO_OP;
        private Predicate<ServerPlayer> unlockCondition = ALWAYS;

        private Builder() {
        }

        public Builder requiredAttunerLevel(int level) {
            if (level < 0) throw new IllegalArgumentException("Perk Attuner level cannot be negative");
            requiredAttunerLevel = level;
            return this;
        }

        public Builder experienceLevels(int levels) {
            if (levels < 0) throw new IllegalArgumentException("Perk experience cost cannot be negative");
            experienceLevels = levels;
            return this;
        }

        /**
         * Adds a server-authoritative one-time unlock effect. Continuous abilities should instead
         * query {@code PerkService.hasUnlocked(...)} from their normal server tick/event path.
         */
        public Builder onUnlocked(Consumer<ServerPlayer> action) {
            onUnlocked = Objects.requireNonNull(action, "action");
            return this;
        }

        /** Adds an extension-defined server-side gate, for requirements beyond the graph itself. */
        public Builder canUnlock(Predicate<ServerPlayer> condition) {
            unlockCondition = Objects.requireNonNull(condition, "condition");
            return this;
        }

        public Perk build() {
            return new Perk(this);
        }
    }
}
