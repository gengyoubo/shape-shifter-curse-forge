package net.onixary.shapeShifterCurseForge.client.screen;

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

/** Evolution-style Perk tree for the Form Attuner. Purchases are requested here but validated
 * and persisted only by {@link net.onixary.shapeShifterCurseForge.perk.PerkService} on server. */
public final class FormAttunerScreen extends Screen {
    private static final String KEY = "screen." + ShapeShifterCurseForge.RESOURCE_NAMESPACE + ".form_attuner.";
    private static final int NODE_WIDTH = 98;
    private static final int NODE_HEIGHT = 22;
    private final int attunementLevel;
    private final int maxAttunementLevel;
    private final ResourceLocation formGroupId;
    private final Set<ResourceLocation> unlockedPerks;
    private final String statusKey;
    private final List<NodeLayout> renderedNodes = new ArrayList<>();

    public FormAttunerScreen(int attunementLevel, int maxAttunementLevel, String formGroupId,
                             Set<String> unlockedPerks, String statusKey) {
        super(Component.translatable(KEY + "title"));
        this.attunementLevel = Math.max(0, attunementLevel);
        this.maxAttunementLevel = Math.max(1, maxAttunementLevel);
        this.formGroupId = ResourceLocation.tryParse(formGroupId);
        Set<ResourceLocation> parsed = new LinkedHashSet<>();
        if (unlockedPerks != null) for (String perkId : unlockedPerks) {
            ResourceLocation id = ResourceLocation.tryParse(perkId);
            if (id != null) parsed.add(id);
        }
        this.unlockedPerks = Set.copyOf(parsed);
        this.statusKey = statusKey == null ? "" : statusKey;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 45, height / 2 + 118, 90, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int panelWidth = 390, panelHeight = 267;
        int left = (width - panelWidth) / 2, top = (height - panelHeight) / 2;
        graphics.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, 0xFFB98BEE);
        graphics.fillGradient(left, top, left + panelWidth, top + panelHeight, 0xEE211638, 0xEE120B20);
        graphics.drawCenteredString(font, title, width / 2, top + 13, 0xFFF3E9FF);
        graphics.drawCenteredString(font, Component.translatable(KEY + "level", attunementLevel, maxAttunementLevel),
                width / 2, top + 36, 0xFF9FEFFF);
        if (!statusKey.isBlank()) {
            boolean success = statusKey.endsWith(".success");
            graphics.drawCenteredString(font, Component.translatable(statusKey), width / 2, top + 52,
                    success ? 0xFFB7FF93 : 0xFFFFB5A7);
        }

        Map<ResourceLocation, PerkTree> trees = SscJavaRegistries.perkTreesForGroup(formGroupId);
        renderedNodes.clear();
        if (trees.isEmpty()) {
            graphics.drawCenteredString(font, Component.translatable(KEY + "no_skills"), width / 2, top + 104, 0xFFD2C2E8);
        } else {
            graphics.drawCenteredString(font, Component.translatable(KEY + "skills"), width / 2, top + 66, 0xFFC7FFB7);
            renderTree(graphics, trees.entrySet().iterator().next().getValue(), left + 22, top + 80, panelWidth - 44);
        }
        renderHoveredTooltip(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTree(GuiGraphics graphics, PerkTree tree, int left, int top, int width) {
        Map<ResourceLocation, Integer> depths = new LinkedHashMap<>();
        for (ResourceLocation perkId : tree.perks()) depthFor(tree, perkId, depths, new LinkedHashSet<>());
        Map<Integer, List<ResourceLocation>> layers = new HashMap<>();
        depths.forEach((perkId, depth) -> layers.computeIfAbsent(depth, ignored -> new ArrayList<>()).add(perkId));
        layers.values().forEach(layer -> layer.sort(Comparator.comparing(ResourceLocation::toString)));

        Map<ResourceLocation, NodeLayout> nodes = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<ResourceLocation>> layer : layers.entrySet()) {
            List<ResourceLocation> perks = layer.getValue();
            for (int index = 0; index < perks.size(); index++) {
                int center = left + width * (index + 1) / (perks.size() + 1);
                nodes.put(perks.get(index), new NodeLayout(perks.get(index), center - NODE_WIDTH / 2,
                        top + layer.getKey() * 29));
            }
        }
        for (PerkTree.Edge edge : tree.edges()) {
            NodeLayout from = nodes.get(edge.from()), to = nodes.get(edge.to());
            if (from != null && to != null) line(graphics, from.centerX(), from.y + NODE_HEIGHT, to.centerX(), to.y, 0xFF8B65B7);
        }
        for (NodeLayout node : nodes.values()) renderNode(graphics, tree, node);
        renderedNodes.addAll(nodes.values());
    }

    private int depthFor(PerkTree tree, ResourceLocation perkId, Map<ResourceLocation, Integer> depths,
                         Set<ResourceLocation> visiting) {
        Integer cached = depths.get(perkId);
        if (cached != null) return cached;
        if (!visiting.add(perkId)) return 0;
        int depth = 0;
        for (ResourceLocation previous : tree.previous(perkId)) depth = Math.max(depth, depthFor(tree, previous, depths, visiting) + 1);
        visiting.remove(perkId);
        depths.put(perkId, depth);
        return depth;
    }

    private void renderNode(GuiGraphics graphics, PerkTree tree, NodeLayout node) {
        Perk perk = SscJavaRegistries.perk(node.perkId).orElse(null);
        boolean unlocked = unlockedPerks.contains(node.perkId);
        boolean prerequisitesMet = tree.previous(node.perkId).stream().allMatch(unlockedPerks::contains);
        boolean available = perk != null && prerequisitesMet && attunementLevel >= perk.requiredAttunerLevel();
        int accent = unlocked ? 0xFF88E871 : available ? 0xFF81E9FF : 0xFF896A9F;
        graphics.fill(node.x, node.y, node.x + NODE_WIDTH, node.y + NODE_HEIGHT, 0xFF150D24);
        graphics.fill(node.x, node.y, node.x + NODE_WIDTH, node.y + 2, accent);
        graphics.fill(node.x, node.y + NODE_HEIGHT - 1, node.x + NODE_WIDTH, node.y + NODE_HEIGHT, accent);
        String label = Component.translatable(perkKey(node.perkId, "name")).getString();
        graphics.drawCenteredString(font, font.plainSubstrByWidth(label, NODE_WIDTH - 8), node.centerX(), node.y + 7,
                unlocked ? 0xFFDAFFD0 : 0xFFF3E9FF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) for (NodeLayout node : renderedNodes) {
            if (node.contains(mouseX, mouseY) && !unlockedPerks.contains(node.perkId)) {
                ModNetwork.CHANNEL.sendToServer(new UnlockPerkPacket(node.perkId));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderHoveredTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        for (NodeLayout node : renderedNodes) {
            if (!node.contains(mouseX, mouseY)) continue;
            Perk perk = SscJavaRegistries.perk(node.perkId).orElse(null);
            if (perk == null) return;
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.translatable(perkKey(node.perkId, "name")));
            tooltip.add(Component.translatable(perkKey(node.perkId, "description")));
            tooltip.add(Component.translatable(KEY + "perk_kind." + perk.kind().name().toLowerCase(java.util.Locale.ROOT)));
            tooltip.add(Component.translatable(KEY + "perk_cost", perk.requiredAttunerLevel(), perk.experienceLevels()));
            if (unlockedPerks.contains(node.perkId)) tooltip.add(Component.translatable(KEY + "perk_unlocked"));
            graphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
            return;
        }
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

    private static String perkKey(ResourceLocation perkId, String suffix) {
        return "perk." + perkId.getNamespace() + "." + perkId.getPath() + "." + suffix;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private record NodeLayout(ResourceLocation perkId, int x, int y) {
        int centerX() { return x + NODE_WIDTH / 2; }
        boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + NODE_WIDTH && mouseY >= y && mouseY < y + NODE_HEIGHT;
        }
    }
}
