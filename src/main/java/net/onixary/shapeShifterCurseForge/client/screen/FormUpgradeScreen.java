package net.onixary.shapeShifterCurseForge.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.api.registry.Perk;
import net.onixary.shapeShifterCurseForge.api.registry.PerkTree;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.network.UnlockPerkPacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Evolution-style perk upgrade view, Forge port of Fabric's {@code FormUpgradeScreen}.
 *
 * <p>Drawn from Fabric's {@code shape_shifter_tuner_ui.png} sprite sheet with Fabric's layout:
 * node columns by perk tier (required attuner level), tier lines with level numbers, node
 * state frames, elbow dependency lines and the info panel with its cost icon. Camera pan
 * (drag) and zoom (scroll) match Fabric; unlocking goes through {@link UnlockPerkPacket} and the
 * server stays authoritative.</p>
 */
// TODO[TEST] Verify pan, zoom, icons and purchases in a running client.
public final class FormUpgradeScreen extends Screen {
    private static final String KEY = "screen." + ShapeShifterCurseForge.RESOURCE_NAMESPACE + ".form_attuner.";

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/gui/shape_shifter_tuner_ui.png");
    private static final ResourceLocation PERK_ATLAS = ResourceLocation.fromNamespaceAndPath(
            ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/perk/textures.png");
    private static final int TEXTURE_WIDTH = 454;
    private static final int TEXTURE_HEIGHT = 190;

    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 190;

    private static final int PERK_UI_X = 110;
    private static final int PERK_UI_Y = 8;
    private static final int PERK_UI_VIEW_Y = 20;
    private static final int PERK_UI_WIDTH = 200;
    private static final int PERK_UI_HEIGHT = 174;

    // Fabric sprite sheet regions (u, v); node frames are 20x20, cost icons 18x18.
    private static final int FRAME_U = 434;
    private static final int GAINED_V = 112;
    private static final int SELECTED_V = 72;
    private static final int NOT_GAINED_V = 132;
    private static final int CAN_NOT_GAIN_V = 152;
    private static final int DEPEND_V = 92;
    private static final int XP_ICON_V = 17;
    private static final int ITEM_ICON_V = 54;
    private static final int LEVEL_ICON_U = 420;
    private static final int LEVEL_LINE_U = 431;

    private static final int NODE_BASE_X = 25;
    private static final int NODE_X_PER_TIER = 50;
    private static final int NODE_Y_PER_INDEX = 28;
    private static final int NODE_RECT = 18;
    private static final int LINE_COLOR = -6316129;
    private static final int LINE_FROM_OFFSET = -10;
    private static final int LINE_TO_OFFSET = 11;

    private int tier;
    private final ResourceLocation formGroupId;
    private Set<ResourceLocation> unlockedPerks;
    private String statusKey;

    private PerkTree tree;
    private final List<NodeLayout> nodes = new ArrayList<>();
    private ResourceLocation nowSelectNode;
    private int lowestTier;
    private int maxTier;

    private double cameraX;
    private double cameraY;
    private double cameraScale = 1.0;
    private boolean dragging;
    private double lastDragX;
    private double lastDragY;

    private int baseX;
    private int baseY;
    private int nodeWindowX;
    private int nodeWindowY;

    private Button gainButton;

    public FormUpgradeScreen(int tier, ResourceLocation formGroupId, Set<ResourceLocation> unlockedPerks, String statusKey) {
        super(Component.translatable(KEY + "title"));
        this.tier = Math.max(0, tier);
        this.formGroupId = formGroupId;
        this.unlockedPerks = Set.copyOf(unlockedPerks == null ? Set.of() : unlockedPerks);
        this.statusKey = statusKey == null ? "" : statusKey;
    }

    public ResourceLocation formGroupId() {
        return formGroupId;
    }

    /** Server refresh after an unlock attempt; keeps the camera and selection like Fabric. */
    public void update(int tier, Set<ResourceLocation> unlockedPerks, String statusKey) {
        this.tier = Math.max(0, tier);
        this.unlockedPerks = Set.copyOf(unlockedPerks == null ? Set.of() : unlockedPerks);
        this.statusKey = statusKey == null ? "" : statusKey;
        if (gainButton != null) gainButton.active = nowSelectNode != null && isSelectable(nowSelectNode);
    }

    @Override
    protected void init() {
        baseX = width / 2 - PANEL_WIDTH / 2;
        baseY = height / 2 - PANEL_HEIGHT / 2;
        nodeWindowX = baseX + PERK_UI_X;
        nodeWindowY = baseY + PERK_UI_Y;
        this.tree = firstTree();
        this.nodes.clear();
        layoutNodes();
        this.gainButton = addRenderableWidget(Button.builder(Component.translatable(KEY + "gain"),
                        button -> onGain())
                .bounds(baseX + 316, baseY + 164, 91, 14).build());
        this.gainButton.active = nowSelectNode != null && isSelectable(nowSelectNode);
    }

    private PerkTree firstTree() {
        var player = Minecraft.getInstance().player;
        var form = player == null ? null : net.onixary.shapeShifterCurseForge.form.FormManager.current(player).id();
        Map<ResourceLocation, PerkTree> trees = SscJavaRegistries.perkTreesForForm(formGroupId, form);
        if (trees.isEmpty()) {
            return null;
        }
        return trees.values().iterator().next();
    }

    private static int perkTier(ResourceLocation perkId) {
        return SscJavaRegistries.perk(perkId).map(Perk::requiredAttunerLevel).orElse(0);
    }

    /**
     * Fabric places a node at x = {@code nodeBaseX + 50·(tier - lowestTier)} from the left edge
     * of the node window, so each tier column sits on its tier line. Forge trees carry no y
     * positions, so nodes of a tier are spread evenly around the window centre.
     */
    private void layoutNodes() {
        if (tree == null) {
            return;
        }
        lowestTier = Integer.MAX_VALUE;
        maxTier = 0;
        Map<Integer, List<ResourceLocation>> columns = new HashMap<>();
        for (ResourceLocation perkId : tree.perks()) {
            int perkTier = perkTier(perkId);
            lowestTier = Math.min(lowestTier, perkTier);
            maxTier = Math.max(maxTier, perkTier);
            columns.computeIfAbsent(perkTier, ignored -> new ArrayList<>()).add(perkId);
        }
        if (lowestTier == Integer.MAX_VALUE) lowestTier = 0;
        columns.values().forEach(column -> column.sort(Comparator.comparing(ResourceLocation::toString)));
        for (Map.Entry<Integer, List<ResourceLocation>> column : columns.entrySet()) {
            List<ResourceLocation> perks = column.getValue();
            for (int index = 0; index < perks.size(); index++) {
                int x = columnX(column.getKey());
                int y = (int) Math.round((index - (perks.size() - 1) / 2.0) * NODE_Y_PER_INDEX);
                nodes.add(new NodeLayout(perks.get(index), column.getKey(), x, y));
            }
        }
    }

    /** Column x in virtual space, relative to the node window centre (Fabric's nodeCenter). */
    private int columnX(int perkTier) {
        return -PERK_UI_WIDTH / 2 + NODE_BASE_X + NODE_X_PER_TIER * (perkTier - lowestTier);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        baseX = width / 2 - PANEL_WIDTH / 2;
        baseY = height / 2 - PANEL_HEIGHT / 2;
        nodeWindowX = baseX + PERK_UI_X;
        nodeWindowY = baseY + PERK_UI_Y;

        graphics.blit(TEXTURE, baseX, baseY, 0, 0, PANEL_WIDTH, PANEL_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawFormPreview(graphics, mouseX, mouseY);

        if (tree == null) {
            graphics.drawCenteredString(font, Component.translatable(KEY + "no_skills"),
                    nodeWindowX + PERK_UI_WIDTH / 2, nodeWindowY + PERK_UI_HEIGHT / 2, 0xFFD2C2E8);
        } else {
            drawTierLines(graphics);
            drawTree(graphics, mouseX, mouseY);
        }

        if (nowSelectNode != null) {
            drawInfo(graphics, nowSelectNode, mouseX, mouseY);
        }
        if (!statusKey.isBlank()) {
            graphics.drawCenteredString(font, Component.translatable(statusKey), width / 2, baseY - 12,
                    statusKey.endsWith(".success") ? 0xFF9CFFB0 : 0xFFFF9C9C);
        }
    }

    /** Fabric's form model review: the player, following the mouse, in the left viewport. */
    private void drawFormPreview(GuiGraphics graphics, int mouseX, int mouseY) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        int viewportX = baseX + 8;
        int viewportY = baseY + 30;
        int entityX = viewportX + 50;
        int entityY = viewportY + 130 - 15;
        int size = 50;
        graphics.enableScissor(viewportX, viewportY, viewportX + 100, viewportY + 130);
        try {
            net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(
                    graphics, entityX, entityY, size, entityX - mouseX, entityY - mouseY - size, player);
        } finally {
            graphics.disableScissor();
        }
    }

    private int centerX() {
        return nodeWindowX + PERK_UI_WIDTH / 2;
    }

    private int centerY() {
        return nodeWindowY + PERK_UI_HEIGHT / 2;
    }

    /** Fabric's tier columns: a full-height line per tier with its level number on top. */
    private void drawTierLines(GuiGraphics graphics) {
        graphics.enableScissor(nodeWindowX, nodeWindowY, nodeWindowX + PERK_UI_WIDTH, nodeWindowY + PERK_UI_HEIGHT);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        for (int tierIndex = 1; tierIndex <= maxTier; tierIndex++) {
            float lineCenter = (float) (centerX() + cameraX + cameraScale * (columnX(tierIndex) + 1.0F));
            int lineLeftX = Math.round(lineCenter - 0.5F);
            graphics.blit(TEXTURE, lineLeftX - 1, nodeWindowY, LEVEL_LINE_U, 0, 3, PERK_UI_HEIGHT,
                    TEXTURE_WIDTH, TEXTURE_HEIGHT);
            int iconX = lineLeftX - 5;
            int iconY = nodeWindowY + 5;
            if (tierIndex <= 10) {
                graphics.blit(TEXTURE, iconX, iconY, LEVEL_ICON_U, (tierIndex - 1) * 11, 11, 11,
                        TEXTURE_WIDTH, TEXTURE_HEIGHT);
            } else {
                graphics.fill(iconX, iconY, iconX + 11, iconY + 11, LINE_COLOR);
            }
        }
        RenderSystem.disableBlend();
        graphics.disableScissor();
    }

    private void drawTree(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.enableScissor(nodeWindowX, nodeWindowY + PERK_UI_VIEW_Y,
                nodeWindowX + PERK_UI_WIDTH, nodeWindowY + PERK_UI_HEIGHT);
        graphics.pose().pushPose();
        graphics.pose().translate(centerX() + cameraX, centerY() + cameraY, 0.0D);
        graphics.pose().scale((float) cameraScale, (float) cameraScale, 1.0F);
        for (PerkTree.Edge edge : tree.edges()) {
            NodeLayout dependency = nodeById(edge.from());
            NodeLayout node = nodeById(edge.to());
            if (dependency != null && node != null) {
                drawDependencyLine(graphics, node, dependency);
            }
        }
        for (NodeLayout node : nodes) {
            drawNode(graphics, node);
        }
        graphics.pose().popPose();
        graphics.disableScissor();
    }

    /**
     * Fabric's {@code NormalDependent}: an elbow line from the left of the node to the right of
     * an adjacent-tier prerequisite; otherwise a short dashed stub with a marker.
     */
    private void drawDependencyLine(GuiGraphics graphics, NodeLayout node, NodeLayout dependency) {
        int x1 = node.x() + LINE_FROM_OFFSET;
        int x2 = dependency.x() + LINE_TO_OFFSET;
        int y1 = node.y();
        int y2 = dependency.y();
        if (node.tier() - 1 != dependency.tier()) {
            int dashLength = 15;
            int lastPixelX = x1;
            for (int i = 0; i < dashLength; i += 3) {
                int to = Math.min(i + 2, dashLength);
                graphics.fill(x1 - to, y1, x1 - i, y1 + 1, LINE_COLOR);
                lastPixelX = x1 - to;
            }
            graphics.fill(lastPixelX - 2, y1 - 1, lastPixelX - 1, y1 + 2, LINE_COLOR);
            return;
        }
        int halfX = (x1 + x2) / 2;
        graphics.fill(Math.min(x1, halfX), y1, Math.max(x1, halfX) + 1, y1 + 1, LINE_COLOR);
        graphics.fill(halfX, Math.min(y1, y2), halfX + 1, Math.max(y1, y2) + 1, LINE_COLOR);
        graphics.fill(Math.min(x2, halfX), y2, Math.max(x2, halfX) + 1, y2 + 1, LINE_COLOR);
    }

    private void drawNode(GuiGraphics graphics, NodeLayout node) {
        int posX = node.x();
        int posY = node.y();
        if (unlockedPerks.contains(node.perkId())) {
            frame(graphics, posX, posY, GAINED_V);
        } else if (!isAvailable(node.perkId())) {
            frame(graphics, posX, posY, CAN_NOT_GAIN_V);
        } else {
            frame(graphics, posX, posY, NOT_GAINED_V);
        }
        if (nowSelectNode != null) {
            if (node.perkId().equals(nowSelectNode)) {
                frame(graphics, posX, posY, SELECTED_V);
            } else if (tree.previous(nowSelectNode).contains(node.perkId())) {
                frame(graphics, posX, posY, DEPEND_V);
            }
        }
        Perk perk = SscJavaRegistries.perk(node.perkId()).orElse(null);
        if (perk != null && perk.iconX() >= 0) {
            graphics.blit(PERK_ATLAS, posX - 7, posY - 7, perk.iconX() * 16, perk.iconY() * 16, 16, 16, 512, 512);
        } else {
            graphics.blit(perkIcon(node.perkId()), posX - 7, posY - 7, 0, 0, 16, 16, 16, 16);
        }
    }

    private static void frame(GuiGraphics graphics, int posX, int posY, int v) {
        graphics.blit(TEXTURE, posX - 9, posY - 9, FRAME_U, v, 20, 20, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private void drawInfo(GuiGraphics graphics, ResourceLocation perkId, int mouseX, int mouseY) {
        Perk perk = SscJavaRegistries.perk(perkId).orElse(null);
        Component name = Component.translatable(perkKey(perkId, "name"));
        String shownName = font.plainSubstrByWidth(name.getString(), 91);
        graphics.drawString(font, shownName, baseX + 316 + (91 - font.width(shownName)) / 2, baseY + 15, 0xFFFFFFFF, false);
        List<net.minecraft.util.FormattedCharSequence> lines =
                font.split(Component.translatable(perkKey(perkId, "description")), 98);
        int ly = baseY + 34;
        for (int i = 0; i < Math.min(lines.size(), 110 / font.lineHeight); i++) {
            graphics.drawString(font, lines.get(i), baseX + 314, ly, 0xFFFFFFFF, false);
            ly += font.lineHeight;
        }
        if (perk == null) return;
        int iconX = baseX + 315;
        int iconY = baseY + 144;
        Component amount;
        if (!perk.implemented()) {
            amount = Component.translatable(KEY + "purchase.not_implemented");
        } else if (perk.itemCost() > 0 && perk.costItem() != null) {
            graphics.blit(TEXTURE, iconX, iconY, FRAME_U, ITEM_ICON_V, 18, 18, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(perk.costItem()));
            graphics.renderItem(stack, iconX + 1, iconY + 1);
            if (mouseX > iconX && mouseX < iconX + 18 && mouseY > iconY && mouseY < iconY + 18) {
                graphics.renderTooltip(font, stack, mouseX, mouseY);
            }
            amount = Component.literal(String.valueOf(perk.itemCost()));
        } else {
            graphics.blit(TEXTURE, iconX, iconY, FRAME_U, XP_ICON_V, 18, 18, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            amount = Component.literal(String.valueOf(
                    perk.experiencePoints() > 0 ? perk.experiencePoints() : perk.experienceLevels()));
        }
        graphics.drawString(font, amount, baseX + 334, baseY + 145 + (17 - font.lineHeight) / 2 + 1, 0xFFFFFFFF, false);
    }

    /** Not yet unlockable for a reason other than already being owned. */
    private boolean isAvailable(ResourceLocation perkId) {
        return unlockedPerks.contains(perkId) || isSelectable(perkId);
    }

    private boolean isSelectable(ResourceLocation perkId) {
        Perk perk = SscJavaRegistries.perk(perkId).orElse(null);
        if (perk == null || !perk.implemented() || unlockedPerks.contains(perkId)) {
            return false;
        }
        if (perk.requiredAttunerLevel() > tier) {
            return false;
        }
        return unlockedPerks.containsAll(tree.previous(perkId));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && tree != null && insideNodeWindow(mouseX, mouseY)) {
            NodeLayout node = nodeAtScreen(mouseX, mouseY);
            nowSelectNode = node == null ? null : node.perkId();
            gainButton.active = node != null && isSelectable(node.perkId());
            dragging = true;
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && insideNodeWindow(mouseX, mouseY)) {
            cameraX += mouseX - lastDragX;
            cameraY += mouseY - lastDragY;
            lastDragX = mouseX;
            lastDragY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!insideNodeWindow(mouseX, mouseY)) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }
        double oldScale = cameraScale;
        double newScale = Math.max(0.25, Math.min(4.0, oldScale * Math.pow(1.1, delta)));
        if (newScale == oldScale) {
            return true;
        }
        double worldX = (mouseX - centerX() - cameraX) / oldScale;
        double worldY = (mouseY - centerY() - cameraY) / oldScale;
        cameraX = mouseX - centerX() - worldX * newScale;
        cameraY = mouseY - centerY() - worldY * newScale;
        cameraScale = newScale;
        return true;
    }

    private void onGain() {
        if (nowSelectNode != null && isSelectable(nowSelectNode)) {
            ModNetwork.CHANNEL.sendToServer(new UnlockPerkPacket(nowSelectNode));
            gainButton.active = false;
        }
    }

    private boolean insideNodeWindow(double mouseX, double mouseY) {
        return mouseX >= nodeWindowX && mouseX < nodeWindowX + PERK_UI_WIDTH
                && mouseY >= nodeWindowY && mouseY < nodeWindowY + PERK_UI_HEIGHT;
    }

    private NodeLayout nodeAtScreen(double mouseX, double mouseY) {
        int vX = virtualX(mouseX);
        int vY = virtualY(mouseY);
        for (NodeLayout node : nodes) {
            if (vX >= node.x() - 8 && vX < node.x() + NODE_RECT - 8
                    && vY >= node.y() - 8 && vY < node.y() + NODE_RECT - 8) {
                return node;
            }
        }
        return null;
    }

    private NodeLayout nodeById(ResourceLocation perkId) {
        for (NodeLayout node : nodes) {
            if (node.perkId().equals(perkId)) {
                return node;
            }
        }
        return null;
    }

    private int virtualX(double screenX) {
        return (int) Math.floor((screenX - centerX() - cameraX) / cameraScale);
    }

    private int virtualY(double screenY) {
        return (int) Math.floor((screenY - centerY() - cameraY) / cameraScale);
    }

    private ResourceLocation perkIcon(ResourceLocation perkId) {
        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(
                ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/perk/" + perkId.getPath() + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(icon).isPresent()) {
            return icon;
        }
        return ResourceLocation.fromNamespaceAndPath(
                ShapeShifterCurseForge.RESOURCE_NAMESPACE, "textures/perk/system/select.png");
    }

    private static String perkKey(ResourceLocation perkId, String suffix) {
        if (perkId.getNamespace().equals(ShapeShifterCurseForge.RESOURCE_NAMESPACE)) {
            return "ssc_perk." + perkId.getNamespace() + "." + perkId.getPath() + "." + (suffix.equals("description") ? "desc" : suffix);
        }
        return "perk." + perkId.getNamespace() + "." + perkId.getPath() + "." + suffix;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record NodeLayout(ResourceLocation perkId, int tier, int x, int y) {
    }
}
