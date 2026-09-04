package net.onixary.shapeShifterCurseForge.client.codex;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.util.ArrayList;
import java.util.List;

/** Codex page one: title, equipment, appearance, entity preview. */
public class BookOfShapeShifterScreenV2_P1 extends Screen implements WidgetEXUtils.IWidgetEX {
    private static final ResourceLocation PAGE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/gui/codex_page_1.png");
    private static final ResourceLocation CURSED_MOON_ICON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/gui/book_cursed_moon_icon.png");

    public static final Component OPEN_FCS_MENU_BUTTON_LABEL =
            Component.translatable("gui.shape_shifter_curse_fabric.book_2_1.open_fcs_menu");

    public final Player currentPlayer;
    public static final int BOOK_SIZE_X = 350;
    public static final int BOOK_SIZE_Y = 220;

    public BookOfShapeShifterScreenV2_P1(Player currentPlayer) {
        super(Component.literal("ShapeShifterCurse_Book_Screen_V2"));
        this.currentPlayer = currentPlayer;
    }

    @Override
    protected void init() {
        // TODO: big-screen config (Fabric newStartBookForBiggerScreen); fixed scale for now.
        int bookScale = 1;
        float scale = 0.5F;
        int bookPosX = this.width / 2 - (BOOK_SIZE_X * bookScale) / 2;
        int bookPosY = this.height / 2 - (BOOK_SIZE_Y * bookScale) / 2;
        int defaultTextColor = 0x222222;
        int headerTextColor = 0xDDDDDD;
        // Title
        // D -> (9, 9), (19, 95)
        // Size -> (108, 48) Pos -> (17, 92)
        this.addRenderableWidget(this.buildDetailScreenButton(19, 95, 9, 9,
                CodexData.getContentText(CodexData.ContentType.TITLE, this.currentPlayer)));
        ScaleScrollTextWidget titleLabel = new ScaleScrollTextWidget(
                bookPosX + 17 * bookScale, bookPosY + 105 * bookScale, 108 * bookScale, 5 * bookScale,
                scale, CodexData.getContentText(CodexData.ContentType.TITLE, this.currentPlayer), this.font)
                .shadow(false).setTextColor(defaultTextColor);
        titleLabel.setEnableScrollableIconRender(true);
        this.getWidgetList().add(titleLabel);
        this.addRenderableWidget(titleLabel);
        // Equip
        // D -> (9, 9), (116, 143)
        // Size -> (107, 56) Pos -> (17, 153)
        this.addRenderableWidget(this.buildDetailScreenButton(116, 143, 9, 9,
                CodexData.getContentText(CodexData.ContentType.EQUIP, this.currentPlayer)));
        this.addRenderableWidget(new StringWidget(
                bookPosX + 17 * bookScale, bookPosY + 143 * bookScale, 107 * bookScale, 6 * bookScale,
                CodexData.headerEquip, this.font).setColor(headerTextColor));
        ScaleScrollTextWidget statusLabel = new ScaleScrollTextWidget(
                bookPosX + 17 * bookScale, bookPosY + 153 * bookScale, 107 * bookScale, 6 * bookScale,
                scale, CodexData.getContentText(CodexData.ContentType.EQUIP, this.currentPlayer), this.font)
                .shadow(false).setTextColor(defaultTextColor);
        statusLabel.setEnableScrollableIconRender(true);
        this.getWidgetList().add(statusLabel);
        this.addRenderableWidget(statusLabel);
        // Open FCS Menu Button
        // 21,194,98,11
        this.addRenderableWidget(Button.builder(OPEN_FCS_MENU_BUTTON_LABEL, button -> {
            if (FormColorSelectMenuV2.instance == null) {
                this.minecraft.setScreen(new FormColorSelectMenuV2(
                        Component.literal("text.shape-shifter-curse.config.form_color_select_menu_v2"), this));
            }
        }).pos(bookPosX + 31 * bookScale, bookPosY + 194 * bookScale).size(78 * bookScale, 14 * bookScale).build());
        // Appearance
        // D -> (9, 9), (311, 13)
        // Size -> (176, 184) Pos -> (142, 23)
        this.addRenderableWidget(this.buildDetailScreenButton(311, 13, 9, 9,
                CodexData.getContentText(CodexData.ContentType.APPEARANCE, this.currentPlayer)));
        this.addRenderableWidget(new StringWidget(
                bookPosX + 142 * bookScale, bookPosY + 11 * bookScale, 176 * bookScale, 8 * bookScale,
                CodexData.headerAppearance, this.font).setColor(headerTextColor));
        ScaleScrollTextWidget appearanceLabel = new ScaleScrollTextWidget(
                bookPosX + 142 * bookScale, bookPosY + 26 * bookScale, 176 * bookScale, 20 * bookScale,
                scale, CodexData.getContentText(CodexData.ContentType.APPEARANCE, this.currentPlayer), this.font)
                .shadow(false).setTextColor(defaultTextColor);
        appearanceLabel.setEnableScrollableIconRender(true);
        this.getWidgetList().add(appearanceLabel);
        this.addRenderableWidget(appearanceLabel);
        // Next page button
        int nextPageButtonSizeX = 15 * bookScale;
        int nextPageButtonSizeY = 30 * bookScale;
        int nextPageButtonPosX = this.width / 2 + (BOOK_SIZE_X * bookScale) / 2 - 18 * bookScale;
        int nextPageButtonPosY = this.height / 2 - nextPageButtonSizeY / 2;
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.nextPage())
                .pos(nextPageButtonPosX, nextPageButtonPosY)
                .size(nextPageButtonSizeX, nextPageButtonSizeY).build());
    }

    private void renderBook(GuiGraphics graphics) {
        int finalBookSizeX = BOOK_SIZE_X;
        int finalBookSizeY = BOOK_SIZE_Y;
        int bookPosX = this.width / 2 - finalBookSizeX / 2;
        int bookPosY = this.height / 2 - finalBookSizeY / 2;
        graphics.blit(PAGE_TEXTURE, bookPosX, bookPosY, 0, 0,
                finalBookSizeX, finalBookSizeY, finalBookSizeX, finalBookSizeY);
    }

    private void nextPage() {
        Minecraft.getInstance().setScreen(new BookOfShapeShifterScreenV2_P2(this.currentPlayer));
    }

    private Button buildDetailScreenButton(int inBookPosX, int inBookPosY, int sizeX, int sizeY,
                                           Component detailText) {
        int bookScale = 1;
        int bookPosX = this.width / 2 - (BOOK_SIZE_X * bookScale) / 2;
        int bookPosY = this.height / 2 - (BOOK_SIZE_Y * bookScale) / 2;
        int fixedPosX = bookPosX + inBookPosX * bookScale;
        int fixedPosY = bookPosY + inBookPosY * bookScale;
        return Button.builder(Component.literal("+"), button ->
                Minecraft.getInstance().setScreen(new DetailScreen(this, detailText)))
                .pos(fixedPosX, fixedPosY).size(sizeX * bookScale, sizeY * bookScale).build();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int bookScale = 1;
        int finalBookSizeX = BOOK_SIZE_X;
        int finalBookSizeY = BOOK_SIZE_Y;
        int bookPosX = this.width / 2 - finalBookSizeX / 2;
        int bookPosY = this.height / 2 - finalBookSizeY / 2;
        this.renderBook(graphics);
        // Entity preview, origin at the entity's feet center.
        // Size -> (70, 66) Pos -> (35, 15)
        int playerX = bookPosX + 70 * bookScale;
        int playerY = bookPosY + 75 * bookScale;
        InventoryScreen.renderEntityInInventoryFollowsMouse(graphics, playerX, playerY, 30 * bookScale,
                (float) (playerX - mouseX), (float) (playerY - 37 * bookScale - mouseY), this.currentPlayer);
        // Cursed moon icon, Size -> (8, 8), Pos -> (115, 92).
        boolean isCursedMoon = CursedMoonData.isCursedMoonDay(Minecraft.getInstance().level);
        graphics.blit(CURSED_MOON_ICON_TEXTURE, bookPosX + 115 * bookScale, bookPosY + 92 * bookScale,
                isCursedMoon ? 8 : 0, 0, 8 * bookScale, 8 * bookScale, 16, 8);
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
