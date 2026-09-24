package net.onixary.shapeShifterCurseForge.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.ITextWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.onixary.shapeShifterCurseForge.registry.ModBlocks;

import java.util.Arrays;

/** JEI recipe page shared by altar and alter. */
public final class MachineJeiCategory extends AbstractRecipeCategory<MachineJeiRecipe> {
    public MachineJeiCategory(IGuiHelper guiHelper, boolean altar) {
        super(recipeType(altar), Component.translatable(altar
                        ? "gui.shape_shifter_curse.category.altar"
                        : "gui.shape_shifter_curse.category.alter"),
                guiHelper.createDrawableItemStack(new ItemStack(altar
                        ? ModBlocks.ALTAR.get() : ModBlocks.ALTER.get())), 128, 58);
    }

    private static RecipeType<MachineJeiRecipe> recipeType(boolean altar) {
        return altar ? SscJeiPlugin.ALTAR_RECIPES : SscJeiPlugin.ALTER_RECIPES;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineJeiRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.grid().size(); i++) {
            Ingredient ingredient = recipe.grid().get(i);
            if (ingredient.isEmpty()) continue;
            builder.addInputSlot(1 + (i % 3) * 18, 1 + (i / 3) * 18)
                    .setStandardSlotBackground()
                    .addItemStacks(Arrays.asList(ingredient.getItems()));
        }
        if (recipe.catalyst() != null && !recipe.catalyst().isEmpty()) {
            builder.addInputSlot(56, 19).setStandardSlotBackground()
                    .addItemStacks(Arrays.asList(recipe.catalyst().getItems()));
        }
        builder.addOutputSlot(105, 19).setStandardSlotBackground().addItemStack(recipe.output());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MachineJeiRecipe recipe, IFocusGroup focuses) {
        Component details = Component.translatable("gui.shape_shifter_curse.jei.machine_details",
                recipe.recipeTime() / 20.0D, recipe.fuelCost());
        ITextWidget widget = builder.addText(details, 48, 42).setPosition(55, 4);
        widget.setColor(0xFF808080);
    }

    @Override
    public ResourceLocation getRegistryName(MachineJeiRecipe recipe) {
        return recipe.id();
    }

    @Override
    public void draw(MachineJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
    }
}
