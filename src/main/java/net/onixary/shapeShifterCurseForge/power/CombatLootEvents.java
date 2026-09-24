package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.util.Objects;

/** Data-driven loot replacement powers. */
@SuppressWarnings("deprecation")
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class CombatLootEvents {
    private CombatLootEvents() { }

    @SubscribeEvent
    public static void livingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;
        for (ItemEntity drop : event.getDrops()) {
            FormPowerRegistry.visitActive(player, (id, power) -> {
                if (!"shape-shifter-curse:modify_entity_loot".equals(FormPowerRegistry.typeOf(power))
                        || !FormPowerRuntime.matchesItem(drop.getItem(), power.getAsJsonObject("from_item_condition"))
                        || player.getRandom().nextFloat() >= Math.max(0.0F, Math.min(1.0F, FormPowerRuntime.floatValue(power, "chance", 0.0F)))) return;
                ItemStack replacement = stackFromLootPower(power, drop.getItem());
                if (!replacement.isEmpty()) drop.setItem(replacement);
            });
        }
    }

    /** Called from Block.dropResources so ordinary breaking and post-break behavior remain intact. */
    public static boolean replaceBlockDrops(BlockState state, ServerLevel level, BlockPos pos,
                                            Player player, ItemStack tool, boolean dropXp) {
        final boolean[] matched = {false};
        final boolean[] replaced = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (matched[0] || !"shape-shifter-curse:modify_block_drop".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))
                    || !matchesBlock(state, power.getAsJsonObject("block_condition"))) return;
            matched[0] = true;
            if (player.getRandom().nextFloat() >= Math.max(0.0F, Math.min(1.0F,
                    FormPowerRuntime.floatValue(power, "chance", 0.0F)))) return;
            if (power.has("target_item_stack_list") && power.get("target_item_stack_list").isJsonArray()) {
                for (var element : power.getAsJsonArray("target_item_stack_list")) {
                    if (!element.isJsonObject()) continue;
                    ItemStack stack = stackFromJson(element.getAsJsonObject());
                    if (!stack.isEmpty()) Block.popResource(level, pos, stack);
                }
            }
            state.spawnAfterBreak(level, pos, tool, dropXp);
            replaced[0] = true;
        });
        return replaced[0];
    }

    private static boolean matchesBlock(net.minecraft.world.level.block.state.BlockState state, com.google.gson.JsonObject condition) {
        if (condition == null) return true;
        String type = FormPowerRegistry.typeOf(condition);
        ResourceLocation id = ResourceLocation.tryParse(FormPowerRuntime.stringValue(condition, type.equals("apoli:in_tag") ? "tag" : "block", ""));
        return id != null && ("apoli:in_tag".equals(type)
                ? state.is(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, id))
                : BuiltInRegistries.BLOCK.get(id) == state.getBlock());
    }

    private static ItemStack stackFromLootPower(com.google.gson.JsonObject power, ItemStack original) {
        if (power.has("target_item_stack") && power.get("target_item_stack").isJsonObject()) return stackFromJson(power.getAsJsonObject("target_item_stack"));
        ResourceLocation id = ResourceLocation.tryParse(FormPowerRuntime.stringValue(power, "target_item", ""));
        Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
        if (item == null) return ItemStack.EMPTY;
        ItemStack replacement = new ItemStack(item, original.getCount());
        if (original.hasTag()) replacement.setTag(Objects.requireNonNull(original.getTag()).copy());
        return replacement;
    }

    private static ItemStack stackFromJson(com.google.gson.JsonObject data) {
        ResourceLocation id = ResourceLocation.tryParse(FormPowerRuntime.stringValue(data, "item", ""));
        Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item, FormPowerRuntime.intValue(data, "amount", 1));
    }
}
