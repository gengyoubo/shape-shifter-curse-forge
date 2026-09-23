package net.onixary.shapeShifterCurseForge.api.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable directed graph of perks offered to one Form group.
 *
 * <p>This intentionally mirrors {@link Evolution}: {@link Builder#chain(ResourceLocation,
 * ResourceLocation...)} and {@link Builder#branch(ResourceLocation, ResourceLocation...)} are
 * concise declarations, while {@link Builder#edge(ResourceLocation, ResourceLocation)} handles
 * the exceptional graph shape. Roots are explicit nodes with no incoming edge.</p>
 *
 * <pre>{@code
 * SscRegistrar perks = SscJavaRegistries.registrar("my_addon");
 * SscRegistryObject<Perk> claws = perks.perk("claws",
 *     () -> Perk.builder().experienceLevels(2).requiredAttunerLevel(1).build());
 * SscRegistryObject<Perk> leap = perks.perk("leap",
 *     () -> Perk.builder().experienceLevels(4).requiredAttunerLevel(2).build());
 * perks.perkTree("wolf", () -> PerkTree.builder(id("wolf_form"))
 *     .chain(claws.id(), leap.id()).build());
 * perks.init();
 * }</pre>
 */
public final class PerkTree {
    private final ResourceLocation formGroupId;
    private final Set<ResourceLocation> perks;
    private final Set<Edge> edges;

    private PerkTree(Builder builder) {
        formGroupId = builder.formGroupId;
        perks = Collections.unmodifiableSet(new LinkedHashSet<>(builder.perks));
        edges = Collections.unmodifiableSet(new LinkedHashSet<>(builder.edges));
    }

    public static Builder builder(ResourceLocation formGroupId) {
        return new Builder(formGroupId);
    }

    /** Form group permitted to buy perks from this tree. */
    public ResourceLocation formGroupId() {
        return formGroupId;
    }

    /** All perk ids in stable registration order, including roots and disconnected display nodes. */
    public Set<ResourceLocation> perks() {
        return perks;
    }

    public Set<Edge> edges() {
        return edges;
    }

    public Set<ResourceLocation> next(ResourceLocation perkId) {
        return edges.stream().filter(edge -> edge.from().equals(perkId)).map(Edge::to)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<ResourceLocation> previous(ResourceLocation perkId) {
        return edges.stream().filter(edge -> edge.to().equals(perkId)).map(Edge::from)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<ResourceLocation> roots() {
        return perks.stream().filter(perk -> previous(perk).isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    public record Edge(ResourceLocation from, ResourceLocation to) {
        public Edge {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            if (from.equals(to)) throw new IllegalArgumentException("Perk edge cannot point to itself: '" + from + "'");
        }
    }

    public static final class Builder {
        private final ResourceLocation formGroupId;
        private final Set<ResourceLocation> perks = new LinkedHashSet<>();
        private final Set<Edge> edges = new LinkedHashSet<>();

        private Builder(ResourceLocation formGroupId) {
            this.formGroupId = Objects.requireNonNull(formGroupId, "formGroupId");
        }

        /** Adds one or more root or disconnected display nodes. */
        public Builder perks(ResourceLocation... perkIds) {
            if (perkIds == null || perkIds.length == 0) throw new IllegalArgumentException("Perk tree needs at least one perk");
            for (ResourceLocation perkId : perkIds) perks.add(Objects.requireNonNull(perkId, "perkId"));
            return this;
        }

        public Builder chain(ResourceLocation first, ResourceLocation... remaining) {
            ResourceLocation previous = Objects.requireNonNull(first, "first");
            if (remaining == null || remaining.length == 0) {
                throw new IllegalArgumentException("Perk chain must contain at least two perks");
            }
            perks.add(previous);
            for (ResourceLocation next : remaining) {
                add(previous, next);
                previous = next;
            }
            return this;
        }

        public Builder branch(ResourceLocation from, ResourceLocation... targets) {
            Objects.requireNonNull(from, "from");
            if (targets == null || targets.length == 0) {
                throw new IllegalArgumentException("Perk branch must contain at least one target");
            }
            for (ResourceLocation target : targets) add(from, target);
            return this;
        }

        public Builder edge(ResourceLocation from, ResourceLocation to) {
            add(from, to);
            return this;
        }

        public PerkTree build() {
            if (perks.isEmpty()) throw new IllegalStateException("Perk tree must contain at least one perk");
            validateAcyclic();
            return new PerkTree(this);
        }

        private void add(ResourceLocation from, ResourceLocation to) {
            Edge edge = new Edge(from, to);
            perks.add(edge.from());
            perks.add(edge.to());
            edges.add(edge);
        }

        private void validateAcyclic() {
            Set<ResourceLocation> visiting = new LinkedHashSet<>();
            Set<ResourceLocation> visited = new LinkedHashSet<>();
            for (ResourceLocation perkId : perks) visit(perkId, visiting, visited);
        }

        private void visit(ResourceLocation perkId, Set<ResourceLocation> visiting, Set<ResourceLocation> visited) {
            if (visited.contains(perkId)) return;
            if (!visiting.add(perkId)) {
                throw new IllegalStateException("Perk tree contains a prerequisite cycle at '" + perkId + "'");
            }
            for (Edge edge : edges) {
                if (edge.from().equals(perkId)) visit(edge.to(), visiting, visited);
            }
            visiting.remove(perkId);
            visited.add(perkId);
        }
    }
}
