package net.onixary.shapeShifterCurseForge.diet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Monotone, cycle-safe inference over item IDs; deliberately independent of Minecraft. */
public final class RecipeDietGraph {
    public static final int VEGETARIAN = 1;
    public static final int MEAT = 2;
    public static final int FISH = 4;
    public static final int IGNORE_DIET = 8;
    public static final int ALL = VEGETARIAN | MEAT | FISH | IGNORE_DIET;
    private static final int ANIMAL = MEAT | FISH;

    /** Each ingredient slot contains all possible item alternatives. */
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
        Map<String, Integer> flags = new HashMap<>();
        seeds.forEach((id, value) -> {
            int allowed = value & ALL & ~denied.getOrDefault(id, 0);
            if (allowed != 0 && !neutral.contains(id)) flags.put(id, allowed);
        });
        List<Rule> rules = recipes.stream()
                .filter(rule -> !neutral.contains(rule.output()) && !rule.ingredients().isEmpty()
                        && rule.ingredients().stream().noneMatch(List::isEmpty))
                .toList();
        Map<String, List<Integer>> dependents = new HashMap<>();
        for (int index = 0; index < rules.size(); index++) {
            Set<String> inputs = new HashSet<>();
            rules.get(index).ingredients().forEach(inputs::addAll);
            for (String input : inputs) {
                dependents.computeIfAbsent(input, unused -> new ArrayList<>()).add(index);
            }
        }
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
            int inherited = classify(rule, flags, neutral) & ~denied.getOrDefault(rule.output(), 0);
            int previous = flags.getOrDefault(rule.output(), 0);
            int next = previous | inherited;
            if (next == previous) continue;
            flags.put(rule.output(), next);
            for (int dependent : dependents.getOrDefault(rule.output(), List.of())) {
                if (!queued[dependent]) {
                    pending.addLast(dependent);
                    queued[dependent] = true;
                }
            }
        }
        return Map.copyOf(flags);
    }

    private static int classify(Rule rule, Map<String, Integer> flags, Set<String> neutral) {
        int animal = 0;
        boolean allVegetarian = true;
        boolean hasPlant = false;
        for (List<String> alternatives : rule.ingredients()) {
            for (String item : alternatives) {
                if (neutral.contains(item)) continue;
                int value = flags.getOrDefault(item, 0);
                animal |= value & ANIMAL;
                if (!isVegetarian(value)) allVegetarian = false;
                else hasPlant = true;
            }
        }
        // Exemptions never propagate: an exempt golden apple is still an ordinary ingredient.
        return animal | (allVegetarian && hasPlant ? VEGETARIAN : 0);
    }
}
