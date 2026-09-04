package net.onixary.shapeShifterCurseForge.client.codex;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.network.SetFormPacket;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Admin form select menu: two columns of eight, paged, acting on a target player. */
public class NormalFormSelectScreen extends Screen {
    private static final int BG_WIDTH = 470;
    private static final int BG_HEIGHT = 247;
    private static final ResourceLocation BG_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/gui/normal_form_select_menu.png");

    private final String targetName;
    private final UUID targetUUID;

    private List<ResourceLocation> availableForms = new ArrayList<>();
    private int nowPage;
    private static final int PAGE_SIZE = 16;
    private final List<ResourceLocation> buttonForms = new ArrayList<>();
    private final List<Button> buttonWidgetList = new ArrayList<>();

    public NormalFormSelectScreen(Component title, String targetName, UUID targetUUID) {
        super(title);
        this.targetName = targetName;
        this.targetUUID = targetUUID;
    }

    private List<ResourceLocation> getAvailableForms() {
        return new ArrayList<>(FormRegistry.forms().keySet());
    }

    private void sendSetForm(ResourceLocation formId) {
        ModNetwork.CHANNEL.sendToServer(new SetFormPacket(this.targetUUID, formId, false));
    }

    private void loadPage() {
        buttonForms.clear();
        for (int i = nowPage * PAGE_SIZE; i < (nowPage + 1) * PAGE_SIZE; i++) {
            if (i < availableForms.size()) {
                buttonForms.add(availableForms.get(i));
            } else {
                buttonForms.add(null);
            }
        }
        refreshButtons();
    }

    private void refreshButtons() {
        if (buttonForms.size() != buttonWidgetList.size()) {
            return;
        }
        for (int i = 0; i < buttonForms.size(); i++) {
            Button buttonWidget = buttonWidgetList.get(i);
            if (buttonForms.get(i) != null) {
                Component label;
                try {
                    label = CodexData.getContentText(buttonForms.get(i), CodexData.ContentType.NAME);
                } catch (Exception exception) {
                    label = Component.literal(buttonForms.get(i).toString());
                }
                buttonWidget.setMessage(label);
                buttonWidget.visible = true;
            } else {
                buttonWidget.visible = false;
            }
        }
    }

    @Override
    protected void init() {
        super.init();
        availableForms = getAvailableForms();
        // One column of 8, two columns total. Change PAGE_SIZE together with the count.
        int buttonWidth = 180;
        int buttonHeight = 20;
        int buttonStartX = this.width / 2 - (buttonWidth + 10);
        int buttonStartY = this.height / 2 - 4 * (buttonHeight + 5) - 12;
        int infoStartY = this.height / 2 + 4 * (buttonHeight + 5) + 5;
        int totalButtonWidth = 2 * buttonWidth + 20;
        int textX = this.width / 2 - totalButtonWidth / 2;
        StringWidget targetInfoTextName = new StringWidget(textX, infoStartY - 9, totalButtonWidth, 20,
                Component.translatable("message.shape-shifter-curse.select_form_ui.target_name", targetName),
                this.font);
        targetInfoTextName.alignCenter();
        this.addRenderableWidget(targetInfoTextName);
        for (int col = 0; col < 2; col++) {
            for (int row = 0; row < 8; row++) {
                int buttonX = buttonStartX + col * (buttonWidth + 20);
                int buttonY = buttonStartY + row * (buttonHeight + 5);
                Button button = Button.builder(Component.literal("<-------->"), buttonWidget -> {
                    int id = buttonWidgetList.indexOf(buttonWidget);
                    if (id >= 0 && id < buttonForms.size() && buttonForms.get(id) != null) {
                        sendSetForm(buttonForms.get(id));
                    }
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(null);
                    }
                }).pos(buttonX, buttonY).size(buttonWidth, buttonHeight).build();
                button.visible = false;
                buttonWidgetList.add(button);
                this.addRenderableWidget(button);
            }
        }
        Button pagePrevButton = Button.builder(Component.literal("<"), buttonWidget -> prevPage())
                .pos(this.width / 2 - 100, this.height / 2 + 4 * (buttonHeight + 5) - 5)
                .size(20, 20).build();
        this.addRenderableWidget(pagePrevButton);
        Button pageNextButton = Button.builder(Component.literal(">"), buttonWidget -> nextPage())
                .pos(this.width / 2 + 80, this.height / 2 + 4 * (buttonHeight + 5) - 5)
                .size(20, 20).build();
        this.addRenderableWidget(pageNextButton);
        loadPage();
    }

    public void nextPage() {
        int maxPage = availableForms.size() / PAGE_SIZE;
        maxPage += availableForms.size() % PAGE_SIZE == 0 ? 0 : 1;
        if (maxPage <= 0) {
            return;
        }
        this.nowPage++;
        if (this.nowPage >= maxPage) {
            this.nowPage = 0;
        }
        loadPage();
    }

    public void prevPage() {
        int maxPage = availableForms.size() / PAGE_SIZE;
        maxPage += availableForms.size() % PAGE_SIZE == 0 ? 0 : 1;
        if (maxPage <= 0) {
            return;
        }
        this.nowPage--;
        if (this.nowPage < 0) {
            this.nowPage = maxPage - 1;
        }
        loadPage();
    }

    public void renderBackgroundTexture(GuiGraphics graphics) {
        int bgX = (this.width - BG_WIDTH) / 2;
        int bgY = (this.height - BG_HEIGHT) / 2;
        graphics.blit(BG_TEXTURE, bgX, bgY, 0, 0, BG_WIDTH, BG_HEIGHT, BG_WIDTH, BG_HEIGHT);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackgroundTexture(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
