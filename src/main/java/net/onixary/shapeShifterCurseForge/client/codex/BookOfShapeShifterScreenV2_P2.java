package net.onixary.shapeShifterCurseForge.client.codex;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.util.ArrayList;
import java.util.List;

/** Codex page two: pros, cons, instincts. */
public class BookOfShapeShifterScreenV2_P2 extends Screen implements WidgetEXUtils.IWidgetEX {
    private static final ResourceLocation PAGE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/gui/codex_page_2.png");

    public final Player currentPlayer;
    public static final int BOOK_SIZE_X = 350;
    public static final int BOOK_SIZE_Y = 220;

    public BookOfShapeShifterScreenV2_P2(Player currentPlayer) {
        super(Component.literal("ShapeShifterCurse_Book_Screen_V2"));
        this.currentPlayer = currentPlayer;
    }

    @Override
    protected void init() {
        float scale = 0.5F;
        int bookScale = 1;
        int bookPosX = this.width / 2 - (BOOK_SIZE_X * bookScale) / 2;
        int bookPosY = this.height / 2 - (BOOK_SIZE_Y * bookScale) / 2;
        int defaultTextColor = 0x222222;
        int headerTextColor = 0xDDDDDD;
        // Pros
        // D -> (9, 9), (80, 12)
        // Size -> (83, 181) Pos -> (13, 26)
        this.addRenderableWidget(this.buildDetailScreenButton(80, 12, 9, 9,
                CodexData.getContentText(CodexData.ContentType.PROS, this.currentPlayer)));
        this.addRenderableWidget(new StringWidget(
                bookPosX + 26 * bookScale, bookPosY + 10 * bookScale, 53 * bookScale, 11 * bookScale,
                CodexData.headerPros, this.font).setColor(headerTextColor));
        ScaleScrollTextWidget pros = new ScaleScrollTextWidget(
                bookPosX + 13 * bookScale, bookPosY + 26 * bookScale, 83 * bookScale, 18 * bookScale,
                scale, CodexData.getContentText(CodexData.ContentType.PROS, this.currentPlayer), this.font)
                .shadow(false).setTextColor(defaultTextColor);
        pros.setEnableScrollableIconRender(true);
        this.getWidgetList().add(pros);
        this.addRenderableWidget(pros);
        // Cons
        // D -> (9, 9), (185, 12)
        // Size -> (82, 182) Pos -> (110, 26)
        this.addRenderableWidget(this.buildDetailScreenButton(185, 12, 9, 9,
                CodexData.getContentText(CodexData.ContentType.CONS, this.currentPlayer)));
        this.addRenderableWidget(new StringWidget(
                bookPosX + 120 * bookScale, bookPosY + 10 * bookScale, 63 * bookScale, 11 * bookScale,
                CodexData.headerCons, this.font).setColor(headerTextColor));
        ScaleScrollTextWidget cons = new ScaleScrollTextWidget(
                bookPosX + 110 * bookScale, bookPosY + 26 * bookScale, 82 * bookScale, 18 * bookScale,
                scale, CodexData.getContentText(CodexData.ContentType.CONS, this.currentPlayer), this.font)
                .shadow(false).setTextColor(defaultTextColor);
        cons.setEnableScrollableIconRender(true);
        this.getWidgetList().add(cons);
        this.addRenderableWidget(cons);
        // Instincts
        // D -> (9, 9), (308, 13)
        // Size -> (106, 136) Pos -> (220, 24)
        this.addRenderableWidget(this.buildDetailScreenButton(308, 13, 9, 9,
                CodexData.getContentText(CodexData.ContentType.INSTINCTS, this.currentPlayer)));
        this.addRenderableWidget(new StringWidget(
                bookPosX + 242 * bookScale, bookPosY + 10 * bookScale, 63 * bookScale, 12 * bookScale,
                CodexData.headerInstincts, this.font).setColor(headerTextColor));
        ScaleMultilineTextWidget instinctsDesc = new ScaleMultilineTextWidget(
                bookPosX + 220 * bookScale, bookPosY + 24 * bookScale,
                CodexData.getDescText(CodexData.ContentType.INSTINCTS, this.currentPlayer), this.font, scale)
                .shadow(false);
        instinctsDesc.setMaxWidth(106 * bookScale);
        this.addRenderableWidget(instinctsDesc);
        int instinctsDescHeight = instinctsDesc.getHeight();
        ScaleScrollTextWidget instincts = new ScaleScrollTextWidget(
                bookPosX + 220 * bookScale, bookPosY + 24 * bookScale + instinctsDescHeight + Math.round(9 * scale),
                106 * bookScale, ((112 - instinctsDescHeight) / 9 + 1) * bookScale,
                scale, CodexData.getContentText(CodexData.ContentType.INSTINCTS, this.currentPlayer), this.font)
                .shadow(false).setTextColor(defaultTextColor);
        instincts.setEnableScrollableIconRender(true);
        this.getWidgetList().add(instincts);
        this.addRenderableWidget(instincts);
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
        Minecraft.getInstance().setScreen(new BookOfShapeShifterScreenV2_P1(this.currentPlayer));
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
