package net.onixary.shapeShifterCurseForge.client.codex;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/** Integer slider over [minValue, maxValue] with a change callback. */
public class SimpleIntSliderWidget extends AbstractSliderButton {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/slider.png");

    public final int minValue;
    public final int maxValue;

    public int intValue = 0;
    public Consumer<SimpleIntSliderWidget> onChanged = null;

    public SimpleIntSliderWidget(int x, int y, int width, int height, Component message,
                                 double value, int minValue, int maxValue) {
        super(x, y, width, height, message, value);
        this.minValue = minValue;
        this.maxValue = maxValue;
        if (this.maxValue == this.minValue) {
            throw new IllegalArgumentException("Max value must be greater than min value");
        }
    }

    @Override
    protected void updateMessage() {
    }

    @Override
    protected void applyValue() {
        double value = this.value;
        this.intValue = (int) (value * (this.maxValue - this.minValue) + this.minValue);
        if (this.onChanged != null) {
            this.onChanged.accept(this);
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        graphics.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        graphics.blitNineSliced(TEXTURE, this.getX(), this.getY(), this.getWidth(), this.getHeight(),
                20, 4, 200, 20, 0, this.getTextureY());
        graphics.blitNineSliced(TEXTURE, this.getX() + (int) (this.value * (double) (this.width - 8)),
                this.getY(), 8, this.getHeight(), 20, 4, 200, 20, 0, this.getHandleTextureY());
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        int color = this.active ? 16777215 : 10526880;
        int textWidth = minecraft.font.width(this.getMessage());
        graphics.drawString(minecraft.font, this.getMessage(),
                this.getX() + (this.width - textWidth) / 2,
                this.getY() + (this.height - 8) / 2,
                color | net.minecraft.util.Mth.ceil(this.alpha * 255.0F) << 24, false);
    }

    public void setIntValue(int value) {
        this.value = (value - this.minValue) / (double) (this.maxValue - this.minValue);
        this.applyValue();
    }

    public int getIntValue() {
        return this.intValue;
    }
}
