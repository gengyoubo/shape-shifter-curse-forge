package net.onixary.shapeShifterCurseForge.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.onixary.shapeShifterCurseForge.registry.ModBlocks;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

import java.util.Arrays;

/** JEI recipe page for the Fabric Altar workstation. */
public final class MachineJeiCategory extends AbstractRecipeCategory<MachineJeiRecipe> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            SscJeiPlugin.MOD_ID, "textures/gui/altar_craft_ui.png");
    private static final int FUEL_PER_MOONDUST = 800;
    private final IDrawable background;
    private final IDrawable arrow;

    public MachineJeiCategory(IGuiHelper guiHelper) {
        super(SscJeiPlugin.ALTAR_RECIPES, Component.translatable("gui.shape_shifter_curse.category.altar"),
                guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.ALTAR.get())), 174, 79);
        background = guiHelper.createDrawable(TEXTURE, 0, 0, 174, 79);
        arrow = guiHelper.createDrawable(TEXTURE, 174, 0, 43, 9);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineJeiRecipe recipe, IFocusGroup focuses) {
        for (int i = 0; i < recipe.grid().size(); i++) {
            Ingredient ingredient = recipe.grid().get(i);
            if (ingredient.isEmpty()) continue;
            builder.addInputSlot(26 + (i % 3) * 18, 17 + (i / 3) * 18)
                    .addItemStacks(Arrays.asList(ingredient.getItems()));
        }
        if (recipe.catalyst() != null && !recipe.catalyst().isEmpty()) {
            builder.addInputSlot(97, 22)
                    .addRichTooltipCallback((slot, tooltip) -> tooltip.add(
                            Component.translatable("gui.shape_shifter_curse.altar.catalyst")))
                    .addItemStacks(Arrays.asList(recipe.catalyst().getItems()));
        }
        if (recipe.fuelCost() > 0) {
            int moondust = (int) (((long) recipe.fuelCost() + FUEL_PER_MOONDUST - 1) / FUEL_PER_MOONDUST);
            builder.addInputSlot(84, 53).addItemStack(new ItemStack(ModItems.UNTREATED_MOONDUST.get(), moondust));
        }
        builder.addOutputSlot(134, 35).addItemStack(recipe.output());
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, MachineJeiRecipe recipe, IRecipeSlotsView recipeSlotsView,
                           double mouseX, double mouseY) {
        if (mouseX >= 84 && mouseX < 127 && mouseY >= 39 && mouseY < 48) {
            tooltip.add(Component.translatable("gui.shape_shifter_curse.jei.altar.recipe_id", recipe.id().toString()));
            tooltip.add(Component.translatable("gui.shape_shifter_curse.jei.altar.time", recipe.recipeTime() / 20.0D));
            tooltip.add(Component.translatable("gui.shape_shifter_curse.jei.altar.moondust",
                    recipe.fuelCost() / (double) FUEL_PER_MOONDUST));
        }
    }

    @Override
    public ResourceLocation getRegistryName(MachineJeiRecipe recipe) {
        return recipe.id();
    }

    @Override
    public void draw(MachineJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        background.draw(guiGraphics, 0, 0);
        arrow.draw(guiGraphics, 84, 39);
    }
}
