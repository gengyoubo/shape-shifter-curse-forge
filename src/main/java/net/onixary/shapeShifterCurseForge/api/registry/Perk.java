package net.onixary.shapeShifterCurseForge.api.registry;

import net.minecraft.server.level.ServerPlayer;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;

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
    private final Kind kind;
    private final Consumer<ServerPlayer> onUnlocked;
    private final Predicate<ServerPlayer> unlockCondition;
    private final int experiencePoints;
    private final int itemCost;
    private final ResourceLocation costItem;
    private final Set<ResourceLocation> powersAdded;
    private final Set<ResourceLocation> powersRemoved;
    private final int iconX, iconY;
    private final boolean implemented;

    private Perk(Builder builder) {
        requiredAttunerLevel = builder.requiredAttunerLevel;
        experienceLevels = builder.experienceLevels;
        kind = builder.kind;
        onUnlocked = builder.onUnlocked;
        unlockCondition = builder.unlockCondition;
        experiencePoints = builder.experiencePoints;
        itemCost = builder.itemCost;
        costItem = builder.costItem;
        powersAdded = Set.copyOf(builder.powersAdded);
        powersRemoved = Set.copyOf(builder.powersRemoved);
        iconX = builder.iconX;
        iconY = builder.iconY;
        implemented = builder.implemented;
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
    public int experiencePoints() { return experiencePoints; }
    public int itemCost() { return itemCost; }
    public ResourceLocation costItem() { return costItem; }
    public Set<ResourceLocation> powersAdded() { return powersAdded; }
    public Set<ResourceLocation> powersRemoved() { return powersRemoved; }
    public int iconX() { return iconX; }
    public int iconY() { return iconY; }
    public boolean implemented() { return implemented; }

    /** Declares how this node participates in gameplay; it is not inferred from its tree shape. */
    public Kind kind() {
        return kind;
    }

    public enum Kind {
        /** A one-time server-side action supplied through {@link Builder#onUnlocked(Consumer)}. */
        NON_POWER,
        /** A continuously evaluated ability, normally queried by a server/client tick service. */
        PASSIVE_POWER,
        /** A player-triggered ability, normally reached through an input packet. */
        ACTIVE_POWER,
        /** A graph-only prerequisite with no inherent behavior. */
        GATE
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
        private Kind kind = Kind.NON_POWER;
        private Consumer<ServerPlayer> onUnlocked = NO_OP;
        private Predicate<ServerPlayer> unlockCondition = ALWAYS;
        private int experiencePoints;
        private int itemCost;
        private ResourceLocation costItem;
        private final Set<ResourceLocation> powersAdded = new java.util.LinkedHashSet<>();
        private final Set<ResourceLocation> powersRemoved = new java.util.LinkedHashSet<>();
        private int iconX = -1, iconY = -1;
        private boolean implemented = true;

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

        public Builder experiencePoints(int points) {
            if (points < 0) throw new IllegalArgumentException("Experience points cannot be negative");
            experiencePoints = points;
            return this;
        }
        public Builder icon(int x, int y) {
            if (x < 0 || y < 0 || x > 31 || y > 31) throw new IllegalArgumentException("Invalid perk icon");
            iconX = x;
            iconY = y;
            return this;
        }
        public Builder implemented(boolean value) { implemented = value; return this; }

        public Builder itemCost(ResourceLocation item, int count) {
            costItem = Objects.requireNonNull(item, "item");
            if (count <= 0) throw new IllegalArgumentException("Item cost must be positive");
            itemCost = count;
            return this;
        }

        public Builder addPowers(ResourceLocation... ids) {
            java.util.Collections.addAll(powersAdded, ids);
            return this;
        }

        public Builder removePowers(ResourceLocation... ids) {
            java.util.Collections.addAll(powersRemoved, ids);
            return this;
        }

        public Builder kind(Kind kind) {
            this.kind = Objects.requireNonNull(kind, "kind");
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
