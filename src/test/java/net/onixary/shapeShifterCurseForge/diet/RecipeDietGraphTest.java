package net.onixary.shapeShifterCurseForge.diet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static net.onixary.shapeShifterCurseForge.diet.RecipeDietGraph.*;

/** Executable regression cases, without a test framework or a Minecraft launch. */
public final class RecipeDietGraphTest {
    private static int assertions;

    public static void main(String[] args) {
        var chain = infer(Map.of("fish", FISH), List.of(
                recipe("platter", "soup"), recipe("soup", "fillet", "water"),
                recipe("fillet", "fish")), Set.of("water"), Map.of());
        expect(chain.get("platter") == FISH, "Fish inherits through intermediate items in reverse recipe order");

        var veggie = infer(Map.of("wheat", VEGETARIAN, "egg", VEGETARIAN), List.of(
                recipe("noodles", "flour", "egg", "bowl"), recipe("flour", "wheat")),
                Set.of("bowl"), Map.of());
        expect(isVegetarian(veggie.get("noodles")), "Known vegetarian ingredients and a bowl make vegetarian food");
        expect(!veggie.containsKey("bowl"), "Neutral containers stay unclassified");

        var unknown = infer(Map.of("plant", VEGETARIAN, "meat", MEAT), List.of(
                recipe("mystery_salad", "plant", "unknown"), recipe("mystery_meat", "meat", "unknown")),
                Set.of(), Map.of());
        expect(!unknown.containsKey("mystery_salad"), "Unknown ingredients prevent vegetarian assumptions");
        expect(unknown.get("mystery_meat") == MEAT, "Known meat propagates despite another unknown ingredient");

        var mixed = infer(Map.of("plant", VEGETARIAN, "meat", MEAT, "fish", FISH), List.of(
                recipe("surf_and_turf", "plant", "meat", "fish")), Set.of(), Map.of());
        expect(mixed.get("surf_and_turf") == (MEAT | FISH), "Mixed food inherits both animal categories");
        expect(!isVegetarian(mixed.get("surf_and_turf")), "Meat and fish dishes are not vegetarian");

        var alternatives = infer(Map.of("plant", VEGETARIAN, "meat", MEAT), List.of(
                new Rule("flexible", "dish", List.of(List.of("plant", "meat"))),
                new Rule("unknown", "uncertain", List.of(List.of("plant", "unknown"))),
                new Rule("all_plants", "salad", List.of(List.of("plant", "plant")))), Set.of(), Map.of());
        expect(alternatives.get("dish") == MEAT, "One animal alternative makes a static recipe potentially meat");
        expect(!alternatives.containsKey("uncertain"), "Unknown ingredient alternatives remain conservative");
        expect(isVegetarian(alternatives.get("salad")), "All vegetarian alternatives are accepted");

        // Vegetarian bits may be discovered first; later animal discoveries must reach all consumers.
        var lateAnimal = infer(Map.of("plant", VEGETARIAN, "meat", MEAT), List.of(
                recipe("dish", "plant"), recipe("snack", "dish"),
                recipe("dish", "patty"), recipe("patty", "mince"), recipe("mince", "meat")),
                Set.of(), Map.of());
        expect(!isVegetarian(lateAnimal.get("dish")), "An additional meat recipe overrides earlier vegetarian classification");
        expect(!isVegetarian(lateAnimal.get("snack")), "Late animal discovery invalidates downstream vegetarian matches");
        expect((lateAnimal.get("snack") & MEAT) != 0, "All dependent recipes are re-evaluated after changes");

        var cycles = infer(Map.of("root", MEAT), List.of(recipe("a", "root"), recipe("b", "a"),
                recipe("a", "b"), recipe("c", "d"), recipe("d", "c")), Set.of(), Map.of());
        expect(cycles.get("b") == MEAT, "Seeded cyclic recipe graphs terminate and inherit");
        expect(!cycles.containsKey("c") && !cycles.containsKey("d"), "Unseeded cycles cannot invent a diet");

        var denied = infer(Map.of("root", MEAT), List.of(recipe("mince", "root"), recipe("burger", "mince")),
                Set.of(), Map.of("mince", ALL));
        expect(!denied.containsKey("mince") && !denied.containsKey("burger"), "Food blacklist cuts recipe propagation");
        var deniedSeed = infer(Map.of("root", MEAT), List.of(recipe("burger", "root")),
                Set.of(), Map.of("root", MEAT));
        expect(deniedSeed.isEmpty(), "Category blacklist also blocks directly tagged seeds");
        var corrected = infer(Map.of("vegan_meat", VEGETARIAN | MEAT), List.of(recipe("burger", "vegan_meat")),
                Set.of(), Map.of("vegan_meat", MEAT));
        expect(isVegetarian(corrected.get("burger")), "Whitelist plus category blacklist can correct vegan substitutes");

        var exempt = infer(Map.of("apple", VEGETARIAN | IGNORE_DIET, "potion", IGNORE_DIET), List.of(
                recipe("pie", "apple"), recipe("drink", "potion")), Set.of(), Map.of());
        expect(exempt.get("apple") == (VEGETARIAN | IGNORE_DIET), "Exempt seed keeps its direct exemption");
        expect(exempt.get("pie") == VEGETARIAN, "Diet exemption is never inherited by recipes");
        expect(!exempt.containsKey("drink"), "An exemption alone is not a food origin category");

        var neutral = infer(Map.of("meat", MEAT, "bowl", MEAT), List.of(
                recipe("bowl", "meat"), recipe("soup", "bowl", "plant")),
                Set.of("bowl"), Map.of());
        expect(!neutral.containsKey("bowl"), "Containers cannot become meat through recycling or direct seeds");
        expect(!neutral.containsKey("soup"), "A neutral container cannot contaminate later recipes");
        expect(infer(Map.of(), List.of(recipe("water_soup", "water")), Set.of("water"), Map.of()).isEmpty(),
                "All-neutral recipes do not invent vegetarian origins");

        var malformed = infer(Map.of("plant", VEGETARIAN), List.of(
                new Rule("empty", "free_food", List.of()),
                new Rule("unresolved", "mystery", List.of(List.of("plant"), List.of()))), Set.of(), Map.of());
        expect(!malformed.containsKey("free_food") && !malformed.containsKey("mystery"),
                "Recipes without enumerable ingredients are skipped");

        List<Rule> longChain = new ArrayList<>();
        for (int i = 1; i <= 5_000; i++) longChain.add(recipe("item" + i, "item" + (i - 1)));
        Collections.reverse(longChain);
        var large = infer(Map.of("item0", FISH), longChain, Set.of(), Map.of());
        expect(large.get("item5000") == FISH, "A 5,000-step reverse chain has no fixed iteration limit");
        expect(large.size() == 5_001, "Long-chain cache contains every classified intermediate");

        var reload = infer(Map.of(), longChain, Set.of(), Map.of());
        expect(reload.isEmpty(), "A rebuild removes stale classifications when seeds disappear");
        expect(!isVegetarian(VEGETARIAN | FISH), "Conflicting plant/fish seeds never match vegetarian diet");
        expect(!isVegetarian(IGNORE_DIET), "Diet exemption does not mean vegetarian");
        System.out.println("Recipe diet graph: " + assertions + " assertions passed");
    }

    private static Rule recipe(String output, String... inputs) {
        return new Rule(output, output, java.util.Arrays.stream(inputs).map(List::of).toList());
    }

    private static void expect(boolean condition, String description) {
        assertions++;
        if (!condition) throw new AssertionError(description);
    }
}
