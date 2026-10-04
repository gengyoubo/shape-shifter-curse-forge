package net.onixary.shapeShifterCurseForge.diet;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.thread.EffectiveSide;
import net.minecraftforge.network.PacketDistributor;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.network.DietSyncPacket;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Server recipe inference, authoritative client mirror, and SSC's virtual diet tag view. */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class DietInheritanceService {
    private static final Snapshot EMPTY = new Snapshot(false, Map.of(), 0, 0);
    private static volatile Snapshot serverSnapshot = EMPTY;
    private static volatile Snapshot clientSnapshot = EMPTY;
    private static MinecraftServer owner;

    public record Snapshot(boolean ready, Map<ResourceLocation, Integer> items, int recipes, int skipped) {
        public Snapshot {
            items = Map.copyOf(items);
        }
    }

    private DietInheritanceService() {}

    public static boolean matchesTag(ItemStack stack, ResourceLocation tag) {
        if (!tag.getNamespace().equals(ShapeShifterCurseForge.RESOURCE_NAMESPACE)) {
            return stack.is(TagKey.create(Registries.ITEM, tag));
        }
        int category = switch (tag.getPath()) {
            case "diet_vegetarian" -> RecipeDietGraph.VEGETARIAN;
            case "diet_raw_meat" -> RecipeDietGraph.MEAT | RecipeDietGraph.FISH;
            case "diet_raw_fish" -> RecipeDietGraph.FISH;
            case "ignore_diet" -> RecipeDietGraph.IGNORE_DIET;
            default -> 0;
        };
        Snapshot snapshot = EffectiveSide.get() == LogicalSide.SERVER ? serverSnapshot : clientSnapshot;
        if (category == 0 || !snapshot.ready()) return stack.is(TagKey.create(Registries.ITEM, tag));
        int flags = snapshot.items().getOrDefault(BuiltInRegistries.ITEM.getKey(stack.getItem()), 0);
        return category == RecipeDietGraph.VEGETARIAN ? RecipeDietGraph.isVegetarian(flags)
                : (flags & category) != 0;
    }

    @SubscribeEvent
    public static void addListener(AddReloadListenerEvent event) {
        event.addListener(new DietInheritanceConfig.ReloadListener());
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        rebuild(event.getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        if (owner == event.getServer()) {
            owner = null;
            serverSnapshot = EMPTY;
        }
    }

    @SubscribeEvent
    public static void sync(OnDatapackSyncEvent event) {
        MinecraftServer server = event.getPlayerList().getServer();
        // Reload listeners run before tag binding; here both tags and recipes are usable.
        if (event.getPlayer() == null || owner != server || !serverSnapshot.ready()) rebuild(server);
        Map<Integer, Integer> networkItems = new HashMap<>();
        serverSnapshot.items().forEach((id, flags) -> networkItems.put(
                BuiltInRegistries.ITEM.getId(BuiltInRegistries.ITEM.get(id)), flags));
        DietSyncPacket packet = new DietSyncPacket(networkItems);
        for (var player : event.getPlayers()) {
            ModNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    public static void acceptClientSnapshot(Map<Integer, Integer> entries) {
        Map<ResourceLocation, Integer> items = new HashMap<>();
        entries.forEach((registryId, flags) -> {
            Item item = BuiltInRegistries.ITEM.byId(registryId);
            if (item != null && BuiltInRegistries.ITEM.getId(item) == registryId) {
                items.put(BuiltInRegistries.ITEM.getKey(item), flags & RecipeDietGraph.ALL);
            }
        });
        clientSnapshot = new Snapshot(true, items, 0, 0);
    }

    public static void clearClientSnapshot() {
        clientSnapshot = EMPTY;
    }

    private static void rebuild(MinecraftServer server) {
        long start = System.nanoTime();
        DietInheritanceConfig config = DietInheritanceConfig.current;
        Map<String, Integer> seeds = new HashMap<>();
        addSelectors(seeds, config.seeds());
        // FoodProperties supplies a useful fallback for mods without meat tags.
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = item.getDefaultInstance();
            var food = stack.getFoodProperties(null);
            if (food != null && food.isMeat()) {
                seeds.merge(BuiltInRegistries.ITEM.getKey(item).toString(), RecipeDietGraph.MEAT, (a, b) -> a | b);
            }
        }
        addSelectors(seeds, config.whitelist());
        Map<String, Integer> denied = new HashMap<>();
        addSelectors(denied, config.blacklist());
        for (String id : resolve(config.foodBlacklist())) denied.put(id, RecipeDietGraph.ALL);
        Set<String> neutral = resolve(config.neutralIngredients());
        List<Pattern> blockedRecipes = config.recipeBlacklist().stream()
                .map(DietInheritanceConfig::recipePattern).toList();
        List<RecipeDietGraph.Rule> rules = new ArrayList<>();
        int skipped = 0;
        for (Recipe<?> recipe : server.getRecipeManager().getRecipes()) {
            String recipeId = recipe.getId().toString();
            if (blockedRecipes.stream().anyMatch(pattern -> pattern.matcher(recipeId).matches())) {
                skipped++;
                continue;
            }
            try {
                ItemStack output = recipe.getResultItem(server.registryAccess());
                if (output.isEmpty()) {
                    skipped++;
                    continue;
                }
                List<List<String>> slots = new ArrayList<>();
                boolean unresolved = false;
                for (Ingredient ingredient : recipe.getIngredients()) {
                    // Empty slots of shaped recipes are not ingredients.
                    if (ingredient == Ingredient.EMPTY) continue;
                    ingredient.getStackingIds(); // Observe Forge tag invalidation before enumerating.
                    List<String> alternatives = java.util.Arrays.stream(ingredient.getItems())
                            .filter(stack -> !stack.isEmpty())
                            .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                            .distinct().toList();
                    if (alternatives.isEmpty()) {
                        unresolved = true;
                        break;
                    }
                    slots.add(alternatives);
                }
                if (unresolved || slots.isEmpty()) {
                    skipped++;
                    continue;
                }
                rules.add(new RecipeDietGraph.Rule(recipeId,
                        BuiltInRegistries.ITEM.getKey(output.getItem()).toString(), slots));
            } catch (RuntimeException exception) {
                skipped++;
                ShapeShifterCurseForge.LOGGER.debug("Cannot infer diet from recipe {}", recipeId, exception);
            }
        }
        Map<ResourceLocation, Integer> classified = new HashMap<>();
        RecipeDietGraph.infer(seeds, rules, neutral, denied).forEach((id, flags) ->
                classified.put(ResourceLocation.parse(id), flags));
        serverSnapshot = new Snapshot(true, classified, rules.size(), skipped);
        owner = server;
        ShapeShifterCurseForge.LOGGER.info("SSC diet inference: {} classified items, {} recipes, {} skipped, {} ms",
                classified.size(), rules.size(), skipped, (System.nanoTime() - start) / 1_000_000);
    }

    private static void addSelectors(Map<String, Integer> destination, Map<Integer, List<String>> categories) {
        categories.forEach((flags, selectors) -> {
            for (String id : resolve(selectors)) destination.merge(id, flags, (a, b) -> a | b);
        });
    }

    private static Set<String> resolve(Collection<String> selectors) {
        Set<String> result = new HashSet<>();
        for (String selector : selectors) {
            if (selector.startsWith("#")) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, ResourceLocation.parse(selector.substring(1)));
                BuiltInRegistries.ITEM.getTag(tag).ifPresent(holders -> holders.forEach(holder ->
                        result.add(BuiltInRegistries.ITEM.getKey(holder.value()).toString())));
            } else {
                ResourceLocation id = ResourceLocation.parse(selector);
                if (BuiltInRegistries.ITEM.containsKey(id)) result.add(id.toString());
            }
        }
        return result;
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ssc").then(Commands.literal("diet")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("stats").executes(context -> {
                    Snapshot snapshot = serverSnapshot;
                    context.getSource().sendSuccess(() -> Component.translatable("command.ssc.diet.stats",
                            snapshot.items().size(), snapshot.recipes(), snapshot.skipped()), false);
                    return 1;
                }))
                .then(Commands.literal("inspect").then(Commands.argument("item", ItemArgument.item(event.getBuildContext()))
                        .executes(context -> {
                            Item item = ItemArgument.getItem(context, "item").getItem();
                            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                            int flags = serverSnapshot.items().getOrDefault(id, 0);
                            context.getSource().sendSuccess(() -> Component.translatable("command.ssc.diet.inspect",
                                    id.toString(), RecipeDietGraph.isVegetarian(flags),
                                    (flags & RecipeDietGraph.MEAT) != 0, (flags & RecipeDietGraph.FISH) != 0,
                                    (flags & RecipeDietGraph.IGNORE_DIET) != 0), false);
                            return 1;
                        })))));
    }
}
