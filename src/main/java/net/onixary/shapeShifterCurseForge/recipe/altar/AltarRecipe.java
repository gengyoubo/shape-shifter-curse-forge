package net.onixary.shapeShifterCurseForge.recipe.altar;

import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public interface AltarRecipe extends Recipe<Container> {
    int getRecipeTime();
    int getFuelCost();
    /** Total fuel units, or -1 when getFuelCost() is charged per tick. */
    default int getTotalFuelCost() { return -1; }
    default int fuelUsage(int progress) {
        int total = getTotalFuelCost();
        if (total < 0) return getFuelCost();
        return (int) (((long) (progress + 1) * total / getRecipeTime())
                - ((long) progress * total / getRecipeTime()));
    }
    static int totalFuelCost(com.google.gson.JsonObject json, int time, int perTick) {
        int moondust = net.minecraft.util.GsonHelper.getAsInt(json, "moondust_cost", -1);
        if (time <= 0 || perTick < 0 || (long) time * perTick > Integer.MAX_VALUE
                || moondust < -1 || moondust > Integer.MAX_VALUE / 800) {
            throw new com.google.gson.JsonParseException("Invalid altar time or fuel cost");
        }
        return moondust < 0 ? -1 : moondust * 800;
    }
    Ingredient getCatalyst();
    boolean canCraft(net.minecraft.world.entity.player.Player player);
}
