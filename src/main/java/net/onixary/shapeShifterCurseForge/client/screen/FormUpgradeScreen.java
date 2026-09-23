package net.onixary.shapeShifterCurseForge.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.api.registry.Perk;
import net.onixary.shapeShifterCurseForge.api.registry.PerkTree;
import net.onixary.shapeShifterCurseForge.api.registry.SscJavaRegistries;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.network.UnlockPerkPacket;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Evolution-style perk upgrade view, Forge port of Fabric's {@code FormUpgradeScreen}.
 * Supports camera pan (drag) and zoom (scroll), node selection and a GET button that
 * requests the unlock through {@link UnlockPerkPacket}; the server stays authoritative.
 */
public final class FormUpgradeScreen extends Screen {
    private static final String KEY = "screen." + ShapeShifterCurseForge.RESOURCE_NAMESPACE + ".form_attuner.";
    private static final String PERK_TEX = "textures/perk/";

    private static final int PANEL_WIDTH = 420;
    private static final int PANEL_HEIGHT = 190;

    private static final int PERK_UI_X = 110;
    private static final int PERK_UI_Y = 8;
    private static final int PERK_UI_WIDTH = 200;
    private static final int PERK_UI_HEIGHT = 174;

    private static final int NODE_BASE_X = 25;
    private static final int NODE_X_PER_TIER = 50;
    private static final int NODE_Y_PER_INDEX = 28;
    private static final int NODE_RECT = 18;
    private static final int NODE_ICON = 16;

    private final int tier;
    private final ResourceLocation formGroupId;
    private final Set<ResourceLocation> unlockedPerks;
    private final String statusKey;

    private PerkTree tree;
    private final List<NodeLayout> nodes = new ArrayList<>();
    private ResourceLocation nowSelectNode;

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

    @Override
    protected void init() {
        this.tree = firstTree();
        this.nodes.clear();
        layoutNodes();
        this.gainButton = addRenderableWidget(Button.builder(Component.translatable(KEY + "gain"),
                        button -> onGain())
                .bounds(baseX + 316, baseY + 164, 91, 14).build());
        this.gainButton.active = false;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 45, height / 2 + 98, 90, 20).build());
    }

    private PerkTree firstTree() {
        Map<ResourceLocation, PerkTree> trees = SscJavaRegistries.perkTreesForGroup(formGroupId);
        if (trees.isEmpty()) {
            return null;
        }
        return trees.values().iterator().next();
    }

    private void layoutNodes() {
        if (tree == null) {
            return;
        }
        Map<ResourceLocation, Integer> depths = new LinkedHashMap<>();
        for (ResourceLocation perkId : tree.perks()) {
            depthFor(tree, perkId, depths, new LinkedHashSet<>());
        }
        Map<Integer, List<ResourceLocation>> layers = new HashMap<>();
        depths.forEach((perkId, depth) -> layers.computeIfAbsent(depth, ignored -> new ArrayList<>()).add(perkId));
        layers.values().forEach(layer -> layer.sort(Comparator.comparing(ResourceLocation::toString)));
        for (Map.Entry<Integer, List<ResourceLocation>> layer : layers.entrySet()) {
            List<ResourceLocation> layerPerks = layer.getValue();
            for (int index = 0; index < layerPerks.size(); index++) {
                int x = NODE_BASE_X + NODE_X_PER_TIER * layer.getKey();
                int y = (int) Math.round((layerPerks.size() - 1) * NODE_Y_PER_INDEX / 2.0
                        + (index - (layerPerks.size() - 1) / 2.0) * NODE_Y_PER_INDEX);
                nodes.add(new NodeLayout(layerPerks.get(index), x, y));
            }
        }
    }

    private int depthFor(PerkTree tree, ResourceLocation perkId, Map<ResourceLocation, Integer> depths,
                         Set<ResourceLocation> visiting) {
        Integer cached = depths.get(perkId);
        if (cached != null) {
            return cached;
        }
        if (!visiting.add(perkId)) {
            return 0;
        }
        int depth = 0;
        for (ResourceLocation previous : tree.previous(perkId)) {
            depth = Math.max(depth, depthFor(tree, previous, depths, visiting) + 1);
        }
        visiting.remove(perkId);
        depths.put(perkId, depth);
        return depth;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        baseX = width / 2 - PANEL_WIDTH / 2;
        baseY = height / 2 - PANEL_HEIGHT / 2;
        nodeWindowX = baseX + PERK_UI_X;
        nodeWindowY = baseY + PERK_UI_Y;

        graphics.fill(baseX - 2, baseY - 2, baseX + PANEL_WIDTH + 2, baseY + PANEL_HEIGHT + 2, 0xFFB98BEE);
        graphics.fillGradient(baseX, baseY, baseX + PANEL_WIDTH, baseY + PANEL_HEIGHT, 0xEE211638, 0xEE120B20);
        graphics.drawCenteredString(font, title, width / 2, baseY + 6, 0xFFF3E9FF);

        if (tree == null) {
            graphics.drawCenteredString(font, Component.translatable(KEY + "no_skills"),
                    nodeWindowX + PERK_UI_WIDTH / 2, nodeWindowY + PERK_UI_HEIGHT / 2, 0xFFD2C2E8);
        } else {
            drawTree(graphics, mouseX, mouseY);
        }

        if (nowSelectNode != null) {
            drawInfo(graphics, nowSelectNode);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawTree(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.enableScissor(nodeWindowX, nodeWindowY, nodeWindowX + PERK_UI_WIDTH, nodeWindowY + PERK_UI_HEIGHT);
        for (PerkTree.Edge edge : tree.edges()) {
            NodeLayout from = nodeById(edge.from());
            NodeLayout to = nodeById(edge.to());
            if (from != null && to != null) {
                line(graphics, sx(from), sy(from), sx(to), sy(to), 0xFF8B65B7);
            }
        }
        int vMouseX = virtualX(mouseX);
        int vMouseY = virtualY(mouseY);
        for (NodeLayout node : nodes) {
            drawNode(graphics, node, vMouseX, vMouseY);
        }
        graphics.disableScissor();
    }

    private void drawNode(GuiGraphics graphics, NodeLayout node, int vMouseX, int vMouseY) {
        int posX = sx(node);
        int posY = sy(node);
        boolean unlocked = unlockedPerks.contains(node.perkId());
        boolean selected = node.perkId().equals(nowSelectNode);
        boolean hovered = vMouseX >= node.x() - 8 && vMouseX < node.x() + NODE_RECT - 8
                && vMouseY >= node.y() - 8 && vMouseY < node.y() + NODE_RECT - 8;

        ResourceLocation icon = perkIcon(node.perkId());
        if (unlocked) {
            graphics.blit(systemIcon("gained"), posX - 9, posY - 9, 0, 0, 20, 20, 20, 20);
        } else if (!isSelectable(node.perkId())) {
            graphics.blit(systemIcon("can_not_gain"), posX - 9, posY - 9, 0, 0, 20, 20, 20, 20);
        }
        if (selected) {
            graphics.blit(systemIcon("selected"), posX - 9, posY - 9, 0, 0, 20, 20, 20, 20);
        } else if (hovered) {
            graphics.blit(systemIcon("select"), posX - 9, posY - 9, 0, 0, 20, 20, 20, 20);
        }
        graphics.blit(icon, posX - 8, posY - 8, 0, 0, NODE_ICON, NODE_ICON, NODE_ICON, NODE_ICON);
    }

    private void drawInfo(GuiGraphics graphics, ResourceLocation perkId) {
        Perk perk = SscJavaRegistries.perk(perkId).orElse(null);
        int ix = baseX + 316, iy = baseY + 10;
        graphics.drawString(font, font.plainSubstrByWidth(Component.translatable(perkKey(perkId, "name")).getString(), 91), ix, iy, 0xFFDAFFD0);
        List<net.minecraft.util.FormattedCharSequence> lines =
                font.split(Component.translatable(perkKey(perkId, "description")), 91);
        int ly = iy + 12;
        for (int i = 0; i < Math.min(lines.size(), 6); i++) {
            graphics.drawString(font, lines.get(i), ix, ly, 0xFFF3E9FF, false);
            ly += 10;
        }
        int cost = perk == null ? 0 : perk.experienceLevels();
        graphics.drawString(font, Component.translatable(KEY + "perk_xp", cost), ix, baseY + 148, 0xFF9FEFFF);
    }

    private boolean isSelectable(ResourceLocation perkId) {
        Perk perk = SscJavaRegistries.perk(perkId).orElse(null);
        if (perk == null || unlockedPerks.contains(perkId)) {
            return false;
        }
        if (perk.requiredAttunerLevel() > tier) {
            return false;
        }
        return tree.previous(perkId).stream().allMatch(unlockedPerks::contains);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            NodeLayout node = nodeAtScreen(mouseX, mouseY);
            if (node != null) {
                nowSelectNode = node.perkId();
                gainButton.active = isSelectable(node.perkId());
                return true;
            }
            if (insideNodeWindow(mouseX, mouseY)) {
                dragging = true;
                lastDragX = mouseX;
                lastDragY = mouseY;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
        double worldX = (mouseX - nodeWindowX - PERK_UI_WIDTH / 2.0 - cameraX) / oldScale;
        double worldY = (mouseY - nodeWindowY - PERK_UI_HEIGHT / 2.0 - cameraY) / oldScale;
        cameraX = mouseX - nodeWindowX - PERK_UI_WIDTH / 2.0 - worldX * newScale;
        cameraY = mouseY - nodeWindowY - PERK_UI_HEIGHT / 2.0 - worldY * newScale;
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
        return (int) ((screenX - nodeWindowX - PERK_UI_WIDTH / 2.0 - cameraX) / cameraScale);
    }

    private int virtualY(double screenY) {
        return (int) ((screenY - nodeWindowY - PERK_UI_HEIGHT / 2.0 - cameraY) / cameraScale);
    }

    private int sx(NodeLayout node) {
        return nodeWindowX + PERK_UI_WIDTH / 2 + (int) Math.round(node.x() * cameraScale + cameraX);
    }

    private int sy(NodeLayout node) {
        return nodeWindowY + PERK_UI_HEIGHT / 2 + (int) Math.round(node.y() * cameraScale + cameraY);
    }

    private ResourceLocation perkIcon(ResourceLocation perkId) {
        ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(
                ShapeShifterCurseForge.RESOURCE_NAMESPACE, PERK_TEX + perkId.getPath() + ".png");
        if (Minecraft.getInstance().getResourceManager().getResource(icon).isPresent()) {
            return icon;
        }
        return systemIcon("select");
    }

    private static ResourceLocation systemIcon(String name) {
        return ResourceLocation.fromNamespaceAndPath(
                ShapeShifterCurseForge.RESOURCE_NAMESPACE, PERK_TEX + "system/" + name + ".png");
    }

    private static String perkKey(ResourceLocation perkId, String suffix) {
        return "perk." + perkId.getNamespace() + "." + perkId.getPath() + "." + suffix;
    }

    private static void line(GuiGraphics graphics, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1, dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1, error = dx + dy;
        while (true) {
            graphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) return;
            int twiceError = 2 * error;
            if (twiceError >= dy) { error += dy; x0 += sx; }
            if (twiceError <= dx) { error += dx; y0 += sy; }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private record NodeLayout(ResourceLocation perkId, int x, int y) {
    }
}