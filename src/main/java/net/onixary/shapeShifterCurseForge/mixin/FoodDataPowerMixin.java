package net.onixary.shapeShifterCurseForge.mixin;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/** Applies Apoli modify_food before vanilla clamps hunger and saturation. */
@Mixin(FoodData.class)
public abstract class FoodDataPowerMixin {
    @ModifyArgs(method = "eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(IF)V"))
    private void ssc$modifyFood(Args args, Item item, ItemStack stack, LivingEntity entity) {
        if (!(entity instanceof Player player)) return;
        List<JsonObject> foodModifiers = new ArrayList<>();
        List<JsonObject> saturationModifiers = new ArrayList<>();
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:modify_food".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))
                    || !FormPowerRuntime.matchesItem(stack, power.getAsJsonObject("item_condition"))) return;
            ssc$collect(power, "food_modifier", "food_modifiers", foodModifiers);
            ssc$collect(power, "saturation_modifier", "saturation_modifiers", saturationModifiers);
        });
        if (foodModifiers.isEmpty() && saturationModifiers.isEmpty()) return;
        int food = args.get(0);
        float saturation = args.get(1);
        args.set(0, (int) FormPowerRuntime.applyModifierList(player, food, foodModifiers));
        args.set(1, (float) FormPowerRuntime.applyModifierList(player, saturation, saturationModifiers));
    }

    private static void ssc$collect(JsonObject power, String single, String plural, List<JsonObject> into) {
        if (power.has(single) && power.get(single).isJsonObject()) into.add(power.getAsJsonObject(single));
        if (power.has(plural) && power.get(plural).isJsonArray()) {
            for (var entry : power.getAsJsonArray(plural)) {
                if (entry.isJsonObject()) into.add(entry.getAsJsonObject());
            }
        }
    }
}
