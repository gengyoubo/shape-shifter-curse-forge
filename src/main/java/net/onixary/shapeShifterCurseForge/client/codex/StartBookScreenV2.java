package net.onixary.shapeShifterCurseForge.client.codex;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.network.ValidateStartBookPacket;

import java.util.ArrayList;
import java.util.List;

/** Pre-enable book screen: lore text plus the confirm button that enables the mod. */
public class StartBookScreenV2 extends Screen implements WidgetEXUtils.IWidgetEX {
    private static final ResourceLocation START_BOOK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/gui/start_book.png");

    public final Player currentPlayer;

    public static final int BOOK_SIZE_X = 360;
    public static final int BOOK_SIZE_Y = 330;
    public static final int TEXT_SIZE_X = 270;
    public static final int TEXT_SIZE_Y = 300;
    public static final int BUTTON_SIZE_X = 200;
    public static final int BUTTON_SIZE_Y = 30;

    public StartBookScreenV2(Player currentPlayer) {
        super(Component.literal("ShapeShifterCurse_StartBook_Screen_V2"));
        this.currentPlayer = currentPlayer;
    }

    @Override
    protected void init() {
        // TODO: big-screen config (Fabric newStartBookForBiggerScreen); fixed offsets for now.
        int textPosYFix = 75;
        int buttonPosYFix = -100;
        int textPosX = this.width / 2 - TEXT_SIZE_X / 2;
        int textPosY = this.height / 2 - TEXT_SIZE_Y / 2 + textPosYFix;
        ScaleScrollTextWidget startBookLabel = new ScaleScrollTextWidget(textPosX, textPosY,
                TEXT_SIZE_X, TEXT_SIZE_Y / 9, 1.0F,
                Component.translatable("screen.shape-shifter-curse.book_of_shape_shifter.start_content_text"),
                this.font);
        startBookLabel.setEnableScrollableIconRender(true);
        this.getWidgetList().add(startBookLabel);
        this.addRenderableWidget(startBookLabel);
        int bookBottomY = this.height / 2 + BOOK_SIZE_Y / 2;
        int buttonPosX = this.width / 2 - BUTTON_SIZE_X / 2;
        int buttonPosY = bookBottomY + buttonPosYFix;
        this.addRenderableWidget(Button.builder(
                Component.translatable("screen.shape-shifter-curse.book_of_shape_shifter.start_button_text"),
                button -> {
                    ModNetwork.CHANNEL.sendToServer(new ValidateStartBookPacket());
                    if (Minecraft.getInstance().screen instanceof StartBookScreenV2) {
                        Minecraft.getInstance().setScreen(null);
                    }
                    this.onClose();
                }).pos(buttonPosX, buttonPosY).size(BUTTON_SIZE_X, BUTTON_SIZE_Y).build());
    }

    private void renderBook(GuiGraphics graphics) {
        int bookPosX = this.width / 2 - BOOK_SIZE_X / 2;
        int bookPosY = this.height / 2 - BOOK_SIZE_Y / 2;
        graphics.blit(START_BOOK_TEXTURE, bookPosX, bookPosY, 0, 0,
                BOOK_SIZE_X, BOOK_SIZE_Y, BOOK_SIZE_X, BOOK_SIZE_Y);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBook(graphics);
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
