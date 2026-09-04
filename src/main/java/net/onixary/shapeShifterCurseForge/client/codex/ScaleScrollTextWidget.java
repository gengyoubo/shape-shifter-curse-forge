package net.onixary.shapeShifterCurseForge.client.codex;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.util.ArrayList;
import java.util.List;

/**
 * Scrollable text area rendering glyphs at a fractional scale (Fabric parity).
 * Book coordinates are used for layout; glyphs are drawn through a scaled matrix so
 * {@code width} book pixels fit {@code width / scale} font pixels, with
 * {@code round(9 * scale)} row pitch.
 */
public class ScaleScrollTextWidget extends AbstractWidget implements WidgetEXUtils.IWidgetEX {
    private final float scale;
    private boolean shadow;
    private int textColor = 0xFFFFFF;

    private int realWidth;
    private int realHeight;
    private int maxWidth;
    private int maxRows;

    private boolean textDone = false;

    private final List<WidgetEXUtils.IWidgetEX> widgetList = List.of();
    private WidgetEXUtils.WidgetRect rect;

    private List<FormattedCharSequence> texts = new ArrayList<>();
    private List<FormattedCharSequence> currentTexts = new ArrayList<>();

    public boolean enableScrollableIconRender = false;
    public int iconSize = 8;
    public ResourceLocation iconTextureId = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/gui/scrollable_icon.png");

    public int textsLineCount = 0;
    public int scroll = 0;
    public int modMaxWidth = 0;

    private final Font font;

    public ScaleScrollTextWidget(int x, int y, int width, int maxRow, float scale,
                                 Component message, Font font) {
        super(x, y, width, maxRow * 9, message);
        this.scale = scale;
        this.font = font;
        this.rect = new WidgetEXUtils.WidgetRect(x, y, width, maxRow * 9);
        this.setMaxWidth(width);
        this.setMaxRows(maxRow);
        this.calculateText();
    }

    @Override
    public WidgetEXUtils.WidgetRect getRect() {
        return this.rect;
    }

    @Override
    public List<WidgetEXUtils.IWidgetEX> getWidgetList() {
        return this.widgetList;
    }

    private double deltaYTotal = 0;
    private double scrollZTotal = 0;

    @Override
    public void onClickWidget(double mouseX, double mouseY, int button) {
        if (this.enableScrollableIconRender) {
            if (mouseX >= this.realWidth - this.iconSize && mouseX <= this.realWidth
                    && mouseY >= 0 && mouseY < this.iconSize) {
                this.scroll(-this.maxRows);
            }
            if (mouseX >= this.realWidth - this.iconSize && mouseX <= this.realWidth
                    && mouseY >= this.realHeight - this.iconSize && mouseY < this.realHeight) {
                this.scroll(this.maxRows);
            }
        }
    }

    @Override
    public void onDragWidget(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (this.enableScrollableIconRender && mouseX >= this.realWidth) {
            return;
        }
        this.deltaYTotal += deltaY;
        if (this.deltaYTotal > 9 || this.deltaYTotal < -9) {
            int amount = (int) (this.deltaYTotal / 9);
            this.deltaYTotal -= amount * 9;
            this.scroll(-amount);
        }
    }

    @Override
    public void onScrollWidget(double mouseX, double mouseY, double scrollDelta) {
        if (this.enableScrollableIconRender && mouseX >= this.realWidth) {
            return;
        }
        this.scrollZTotal += scrollDelta;
        if (this.scrollZTotal > 0.5F || this.scrollZTotal < -0.5F) {
            int amount = (int) (this.scrollZTotal * 2);
            this.scrollZTotal -= amount * 0.5F;
            this.scroll(-amount);
        }
    }

    private void calculateCurrentText() {
        if (this.texts.size() < this.scroll + this.maxRows) {
            this.currentTexts = this.texts.subList(this.scroll, this.texts.size());
        } else {
            this.currentTexts = this.texts.subList(this.scroll, this.scroll + this.maxRows);
        }
    }

    private void calculateText() {
        this.texts = new ArrayList<>(this.font.split(this.getMessage(), this.getTextWidth()));
        this.textsLineCount = this.texts.size();
        this.calculateCurrentText();
        this.textDone = true;
    }

    public ScaleScrollTextWidget shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public ScaleScrollTextWidget setTextColor(int color) {
        this.textColor = color;
        return this;
    }

    public ScaleScrollTextWidget setEnableScrollableIconRender(boolean enableScrollableIconRender) {
        if (this.enableScrollableIconRender != enableScrollableIconRender) {
            if (enableScrollableIconRender) {
                this.modMaxWidth(-this.iconSize);
            } else {
                this.modMaxWidth(0);
            }
            this.enableScrollableIconRender = enableScrollableIconRender;
            this.reloadText();
        }
        return this;
    }

    public void reloadText() {
        this.textDone = false;
        this.calculateText();
        this.scroll = 0;
    }

    public void reloadText(Component message) {
        // setMessage already recalculates once; calling reloadText() again here
        // would wrap twice and reset the scroll position redundantly.
        this.setMessage(message);
    }

    @Override
    public void setMessage(Component message) {
        super.setMessage(message);
        this.reloadText();
    }

    public void scroll(int amount) {
        if (!this.textDone) {
            this.calculateText();
        }
        this.scroll += amount;
        if (this.scroll > this.texts.size() - this.maxRows) {
            this.scroll = this.texts.size() - this.maxRows;
        }
        if (this.scroll < 0) {
            this.scroll = 0;
        }
        this.calculateCurrentText();
    }

    public void modMaxWidth(int value) {
        this.modMaxWidth = value;
    }

    public void setMaxWidth(int maxWidth) {
        this.realWidth = maxWidth;
        this.maxWidth = Math.round(maxWidth * (1.0F / this.scale));
        this.width = maxWidth;
    }

    public void setMaxRows(int maxRows) {
        this.realHeight = maxRows * 9;
        this.maxRows = Math.round(maxRows * (1.0F / this.scale));
        this.height = maxRows * 9;
    }

    public int getTextWidth() {
        return this.maxWidth + this.modMaxWidth;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.textDone) {
            this.calculateText();
        }
        int x = this.getX();
        int y = this.getY();
        if (this.enableScrollableIconRender) {
            if (this.scroll > 0) {
                graphics.blit(this.iconTextureId, x + this.realWidth - this.iconSize, y,
                        0, 0, this.iconSize, this.iconSize, this.iconSize, this.iconSize * 2);
            }
            if (this.scroll < this.texts.size() - this.maxRows) {
                graphics.blit(this.iconTextureId, x + this.realWidth - this.iconSize,
                        y + this.realHeight - this.iconSize,
                        0, this.iconSize, this.iconSize, this.iconSize, this.iconSize, this.iconSize * 2);
            }
        }
        int rowPitch = Math.round(9 * this.scale);
        float inverse = 1.0F / this.scale;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.scale(this.scale, this.scale, 1.0F);
        for (int i = 0; i < this.currentTexts.size(); i++) {
            graphics.drawString(this.font, this.currentTexts.get(i),
                    x * inverse, (y + i * rowPitch) * inverse, this.textColor, this.shadow);
        }
        pose.popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Never consume clicks through vanilla dispatch: vanilla forwards children in
        // insertion order, so this full-area text widget would otherwise swallow clicks
        // meant for buttons overlapping its rect (StartBook confirm, P1 detail buttons).
        // Scroll interaction arrives separately through WidgetEX dispatch instead.
        return false;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        narrationElementOutput.add(NarratedElementType.TITLE, this.getMessage());
    }
}
