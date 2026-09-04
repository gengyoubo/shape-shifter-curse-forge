package net.onixary.shapeShifterCurseForge.client.codex;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Full-text detail view opened by the "+" buttons in the Codex. */
public class DetailScreen extends Screen implements WidgetEXUtils.IWidgetEX {
    private final Screen previousScreen;
    private final Component detailText;

    public DetailScreen(Screen previousScreen, Component detailText) {
        super(Component.literal("Detail Screen"));
        this.previousScreen = previousScreen;
        this.detailText = detailText;
    }

    @Override
    protected void init() {
        int textX = 20;
        int textY = 40;
        int textSizeX = this.width - textX * 2;
        int textSizeY = this.height - 60;
        int textDefaultColor = 0xFFFFFF;
        ScaleScrollTextWidget detailTextWidget = new ScaleScrollTextWidget(
                textX, textY, textSizeX, textSizeY / 9, 1.0F, this.detailText, this.font)
                .setTextColor(textDefaultColor);
        detailTextWidget.setEnableScrollableIconRender(true);
        this.getWidgetList().add(detailTextWidget);
        this.addRenderableWidget(detailTextWidget);
        int buttonX = this.width - 30;
        int buttonY = 10;
        Button closeButton = Button.builder(Component.literal("X"), button -> this.onClose())
                .pos(buttonX, buttonY).size(20, 20).build();
        this.addRenderableWidget(closeButton);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.previousScreen);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public WidgetEXUtils.WidgetRect getRect() {
        return null;
    }

    public List<WidgetEXUtils.IWidgetEX> widgetList = new ArrayList<>();

    @Override
    public List<WidgetEXUtils.IWidgetEX> getWidgetList() {
        return this.widgetList;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.onClickWidget(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.onReleaseWidget(mouseX, mouseY, button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        this.onDragWidget(mouseX, mouseY, button, deltaX, deltaY);
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        this.onScrollWidget(mouseX, mouseY, delta);
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
