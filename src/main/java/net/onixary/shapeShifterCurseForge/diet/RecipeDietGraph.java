package net.onixary.shapeShifterCurseForge.diet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Cycle-safe dataflow: animal origins, possible vegetarian proofs, then universal validation. */
public final class RecipeDietGraph {
    public static final int VEGETARIAN = 1;
    public static final int MEAT = 2;
    public static final int FISH = 4;
    public static final int IGNORE_DIET = 8;
    public static final int ALL = VEGETARIAN | MEAT | FISH | IGNORE_DIET;
    private static final int ANIMAL = MEAT | FISH;

    /** An empty recipe/slot represents an unresolved recipe, which blocks vegetarian inference. */
    public record Rule(String id, String output, List<List<String>> ingredients) {
        public Rule {
            ingredients = ingredients.stream().map(List::copyOf).toList();
        }
    }

    private RecipeDietGraph() {}

    public static boolean isVegetarian(int flags) {
        return (flags & VEGETARIAN) != 0 && (flags & ANIMAL) == 0;
    }

    public static Map<String, Integer> infer(Map<String, Integer> seeds, Collection<Rule> recipes,
                                             Set<String> neutral, Map<String, Integer> denied) {
        return infer(seeds, recipes, neutral, denied, Set.of());
    }

    public static Map<String, Integer> infer(Map<String, Integer> seeds, Collection<Rule> recipes,
                                             Set<String> neutral, Map<String, Integer> denied,
                                             Set<String> explicitMeatSeeds) {
        Map<String, Integer> flags = new HashMap<>();
        seeds.forEach((id, value) -> {
            int allowed = value & ALL & ~denied.getOrDefault(id, 0);
            if (allowed != 0 && !neutral.contains(id)) flags.put(id, allowed);
        });
        List<Rule> rules = recipes.stream().filter(rule -> !neutral.contains(rule.output())).toList();
        Map<String, List<Integer>> dependents = new HashMap<>();
        Map<String, List<Integer>> producers = new HashMap<>();
        for (int index = 0; index < rules.size(); index++) {
            Rule rule = rules.get(index);
            producers.computeIfAbsent(rule.output(), unused -> new ArrayList<>()).add(index);
            Set<String> inputs = new HashSet<>();
            rule.ingredients().forEach(inputs::addAll);
            for (String input : inputs) dependents.computeIfAbsent(input, unused -> new ArrayList<>()).add(index);
        }
        // Origins/carnivore tags include fish. Identify the fish chain before using broad meat
        // tags or FoodProperties as land-meat roots. An explicit meat whitelist remains authoritative.
        propagate(FISH, rules, dependents, flags, neutral, denied);
        flags.replaceAll((id, value) -> (value & FISH) != 0 && !explicitMeatSeeds.contains(id)
                ? value & ~MEAT : value);
        propagate(MEAT, rules, dependents, flags, neutral, denied);
        flags.replaceAll((id, value) -> (value & ANIMAL) != 0 ? value & ~VEGETARIAN : value);
        Set<String> trustedVegetarian = new HashSet<>();
        flags.forEach((id, value) -> { if (isVegetarian(value)) trustedVegetarian.add(id); });

        // Find a finite seed-backed proof, then retract items with uncertain producer recipes.
        // Anchored cycles can pass; unseeded cycles cannot certify themselves.
        propagate(VEGETARIAN, rules, dependents, flags, neutral, denied);
        ArrayDeque<String> pending = new ArrayDeque<>(producers.keySet());
        Set<String> queued = new HashSet<>(producers.keySet());
        while (!pending.isEmpty()) {
            String output = pending.removeFirst();
            queued.remove(output);
            if (trustedVegetarian.contains(output) || !isVegetarian(flags.getOrDefault(output, 0))) continue;
            boolean certain = producers.get(output).stream()
                    .allMatch(index -> vegetarianRecipe(rules.get(index), flags, neutral));
            if (certain) continue;
            flags.computeIfPresent(output, (id, value) -> value & ~VEGETARIAN);
            for (int dependent : dependents.getOrDefault(output, List.of())) {
                String next = rules.get(dependent).output();
                if (queued.add(next)) pending.addLast(next);
            }
        }
        flags.values().removeIf(value -> value == 0);
        return Map.copyOf(flags);
    }

    private static void propagate(int category, List<Rule> rules, Map<String, List<Integer>> dependents,
                                  Map<String, Integer> flags, Set<String> neutral, Map<String, Integer> denied) {
        ArrayDeque<Integer> pending = new ArrayDeque<>();
        boolean[] queued = new boolean[rules.size()];
        for (int index = 0; index < rules.size(); index++) {
            pending.add(index);
            queued[index] = true;
        }
        while (!pending.isEmpty()) {
            int index = pending.removeFirst();
            queued[index] = false;
            Rule rule = rules.get(index);
            int previous = flags.getOrDefault(rule.output(), 0);
            if ((previous & category) != 0 || (denied.getOrDefault(rule.output(), 0) & category) != 0) continue;
            boolean inherited = category == VEGETARIAN
                    ? (previous & ANIMAL) == 0 && vegetarianRecipe(rule, flags, neutral)
                    : rule.ingredients().stream().flatMap(List::stream).anyMatch(
                            input -> !neutral.contains(input) && (flags.getOrDefault(input, 0) & category) != 0);
            if (!inherited) continue;
            flags.put(rule.output(), previous | category);
            for (int dependent : dependents.getOrDefault(rule.output(), List.of())) {
                if (!queued[dependent]) {
                    pending.addLast(dependent);
                    queued[dependent] = true;
                }
            }
        }
    }

    private static boolean vegetarianRecipe(Rule rule, Map<String, Integer> flags, Set<String> neutral) {
        if (rule.ingredients().isEmpty()) return false;
        boolean hasPlant = false;
        for (List<String> alternatives : rule.ingredients()) {
            if (alternatives.isEmpty()) return false;
            for (String item : alternatives) {
                if (neutral.contains(item)) continue;
                if (!isVegetarian(flags.getOrDefault(item, 0))) return false;
                hasPlant = true;
            }
        }
        return hasPlant;
    }
}
