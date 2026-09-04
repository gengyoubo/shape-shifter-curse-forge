package net.onixary.shapeShifterCurseForge.client.codex;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Non-scrolling variant of the scaled book text: wraps once and reports content height. */
public class ScaleMultilineTextWidget extends AbstractWidget {
    private final float scale;
    private boolean shadow;
    private int textColor = 0xFFFFFF;
    private int wrapWidth = Integer.MAX_VALUE;

    private final Font font;
    private List<FormattedCharSequence> texts = new ArrayList<>();

    public ScaleMultilineTextWidget(int x, int y, Component message, Font font, float scale) {
        super(x, y, 0, 0, message);
        this.font = font;
        this.scale = scale;
        this.recalculate();
    }

    public ScaleMultilineTextWidget shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public ScaleMultilineTextWidget setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    public ScaleMultilineTextWidget setMaxWidth(int maxWidth) {
        this.wrapWidth = Math.round(maxWidth * (1.0F / this.scale));
        this.width = maxWidth;
        this.recalculate();
        return this;
    }

    private void recalculate() {
        this.texts = new ArrayList<>(this.font.split(this.getMessage(), this.wrapWidth));
        this.height = Math.round(this.texts.size() * 9 * this.scale);
    }

    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
        this.recalculate();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int rowPitch = Math.round(9 * this.scale);
        float inverse = 1.0F / this.scale;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(this.scale, this.scale, 1.0F);
        for (int i = 0; i < this.texts.size(); i++) {
            graphics.drawString(this.font, this.texts.get(i),
                    this.getX() * inverse, (this.getY() + i * rowPitch) * inverse,
                    this.textColor, this.shadow);
        }
        pose.popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.getMessage());
    }
}
