package net.onixary.shapeShifterCurseForge.client.codex;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

/** 15x15 textured slot button; texture column switches by state (0/15/30). */
public class FcsButtonWidget extends Button {
    public final ResourceLocation widgetsTexture = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/gui/form_color_select_menu_part.png");
    public int textureX = 0;

    public FcsButtonWidget(int x, int y, Component message, OnPress onPress,
                           Button.CreateNarration narrationSupplier, int textureX) {
        super(x, y, 15, 15, message, onPress, narrationSupplier);
        this.textureX = textureX;
    }

    private int getTextureY() {
        int row = 0;
        if (!this.active) {
            row = 2;
        } else if (this.isFocused()) {
            row = 1;
        }
        return row * 15;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        graphics.blit(this.widgetsTexture, this.getX(), this.getY(), this.textureX, this.getTextureY(),
                15, 15, 45, 45);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int color = this.active ? 16777215 : 10526880;
        int textWidth = minecraft.font.width(this.getMessage());
        graphics.drawString(minecraft.font, this.getMessage(),
                this.getX() + (this.width - textWidth) / 2,
                this.getY() + (this.height - 8) / 2,
                color | Mth.ceil(this.alpha * 255.0F) << 24, false);
    }
}
