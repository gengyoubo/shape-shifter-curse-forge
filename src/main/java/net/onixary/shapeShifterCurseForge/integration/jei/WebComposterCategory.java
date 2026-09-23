package net.onixary.shapeShifterCurseForge.integration.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.ITextWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.registry.ModBlocks;
import net.onixary.shapeShifterCurseForge.registry.ModItems;

import java.util.List;

/** JEI category for Web Composter input -> Nutrient Sac (Fabric parity). */
public final class WebComposterCategory extends AbstractRecipeCategory<WebComposterRecipe> {
    public WebComposterCategory(IGuiHelper guiHelper) {
        super(SscJeiPlugin.WEB_COMPOSTING,
                Component.translatable("gui.shape_shifter_curse.category.web_compostable"),
                guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.WEB_COMPOSTER.get())),
                120, 18);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, WebComposterRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(1, 1).setStandardSlotBackground().addItemStacks(recipe.getInputs());
        builder.addOutputSlot(103, 1).setStandardSlotBackground()
                .addItemStack(new ItemStack(ModItems.SPIDER_FLUID_COCOON.get(), 1));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, WebComposterRecipe recipe, IFocusGroup focuses) {
        int chancePercent = (int) Math.floor(recipe.getChance() * 100.0F);
        Component text = Component.translatable("gui.jei.category.compostable.chance", chancePercent);
        ITextWidget widget = builder.addText(text, getWidth() - 40, getHeight()).setPosition(12, 0);
        widget.setTextAlignment(mezz.jei.api.gui.placement.HorizontalAlignment.CENTER);
        widget.setTextAlignment(mezz.jei.api.gui.placement.VerticalAlignment.CENTER);
        widget.setColor(0xFF808080);
    }

    @Override
    public ResourceLocation getRegistryName(WebComposterRecipe recipe) {
        return recipe.getUid();
    }

    @Override
    public void draw(WebComposterRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
    }
}