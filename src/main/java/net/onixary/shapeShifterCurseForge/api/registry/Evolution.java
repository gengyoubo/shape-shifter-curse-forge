package net.onixary.shapeShifterCurseForge.api.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A pure directed graph describing which forms can evolve into which other forms.
 *
 * <p>This class deliberately contains no form properties, powers, models, inheritance, or target
 * selection policy. Those belong respectively to {@link SscForm}, its subclasses, and SSC's
 * progression service. An Evolution only answers reachability through {@link #next(ResourceLocation)}
 * and {@link #previous(ResourceLocation)}. It must be explicitly passed to
 * {@link SscJavaRegistries#registerEvolution(Evolution)}.</p>
 */
public final class Evolution {
    private final Set<Edge> edges;

    private Evolution(Set<Edge> edges) {
        this.edges = Set.copyOf(edges);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Returns every form directly reachable from {@code formId}. */
    public Set<ResourceLocation> next(ResourceLocation formId) {
        return edges.stream()
                .filter(edge -> edge.from().equals(formId))
                .map(Edge::to)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Returns every form that can directly evolve into {@code formId}. */
    public Set<ResourceLocation> previous(ResourceLocation formId) {
        return edges.stream()
                .filter(edge -> edge.to().equals(formId))
                .map(Edge::from)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Immutable graph edges, useful to registries that validate the form ids. */
    public Set<Edge> edges() {
        return edges;
    }

    public record Edge(ResourceLocation from, ResourceLocation to) {
        public Edge {
            Objects.requireNonNull(from, "from");
            Objects.requireNonNull(to, "to");
            if (from.equals(to)) {
                throw new IllegalArgumentException("Evolution edge cannot point to itself: '" + from + "'");
            }
        }
    }

    public static final class Builder {
        private final Set<Edge> edges = new LinkedHashSet<>();

        /** Adds sequential edges: {@code first -> second -> ... -> last}. */
        public Builder chain(ResourceLocation first, ResourceLocation... remaining) {
            ResourceLocation previous = Objects.requireNonNull(first, "first");
            if (remaining == null || remaining.length == 0) {
                throw new IllegalArgumentException("Evolution chain must contain at least two forms");
            }
            for (ResourceLocation next : remaining) {
                add(previous, next);
                previous = next;
            }
            return this;
        }

        /** Adds one or more direct alternatives from the same source form. */
        public Builder branch(ResourceLocation from, ResourceLocation... targets) {
            Objects.requireNonNull(from, "from");
            if (targets == null || targets.length == 0) {
                throw new IllegalArgumentException("Evolution branch must contain at least one target form");
            }
            for (ResourceLocation target : targets) add(from, target);
            return this;
        }

        /** Adds one direct edge when neither chain nor branch notation is clearer. */
        public Builder edge(ResourceLocation from, ResourceLocation to) {
            add(from, to);
            return this;
        }

        public Evolution build() {
            if (edges.isEmpty()) throw new IllegalStateException("Evolution must contain at least one edge");
            return new Evolution(edges);
        }

        private void add(ResourceLocation from, ResourceLocation to) {
            edges.add(new Edge(from, to));
        }
    }
}
