package net.onixary.shapeShifterCurseForge.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;
import net.onixary.shapeShifterCurseForge.client.ColorAutoSync;
import net.onixary.shapeShifterCurseForge.client.codex.FormColorSelectMenu;
import net.onixary.shapeShifterCurseForge.client.codex.FormColorSelectMenuV2;
import net.onixary.shapeShifterCurseForge.client.color.FormColorData;
import net.onixary.shapeShifterCurseForge.other.config.SscClientConfig;
import net.onixary.shapeShifterCurseForge.other.config.SscCommonConfig;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Forge Mods screen entry, backed by the same Forge config values used in gameplay. */
public final class SscConfigScreen extends Screen {
    private final Screen parent;
    private int page;
    private int offset;
    private final List<Row> rows = new ArrayList<>();
    private record Row(String key, Supplier<String> value, Consumer<String> set, boolean toggle) {}

    public SscConfigScreen(Screen parent) {
        super(Component.translatable("text.shape-shifter-curse.config.title"));
        this.parent = parent;
    }

    private void bool(String key, ForgeConfigSpec.BooleanValue value) {
        rows.add(new Row(key, () -> Boolean.toString(value.get()), text -> value.set(Boolean.parseBoolean(text)), true));
    }
    private void integer(String key, ForgeConfigSpec.IntValue value, int min, int max) {
        rows.add(new Row(key, () -> value.get().toString(), text -> {
            try { int n = Integer.parseInt(text); if (n >= min && n <= max) value.set(n); }
            catch (NumberFormatException ignored) { }
        }, false));
    }
    private void decimal(String key, ForgeConfigSpec.DoubleValue value) {
        rows.add(new Row(key, () -> value.get().toString(), text -> {
            try { double n = Double.parseDouble(text); if (Double.isFinite(n) && n >= 0 && n <= 1) value.set(n); }
            catch (NumberFormatException ignored) { }
        }, false));
    }
    private void populate() {
        rows.clear();
        if (page == 0) {
            bool("big_screen", SscClientConfig.BIG_CODEX);
            bool("first_person_form_arms", SscClientConfig.FIRST_PERSON_FORM_ARMS);
            bool("better_combat_fix", SscClientConfig.BETTER_COMBAT_FIX);
            bool("ignore_hide_arms", SscClientConfig.IGNORE_HIDE_ARMS);
            bool("change_fpm_config", SscClientConfig.CHANGE_FPM_CONFIG);
            integer("blink_min_interval", SscClientConfig.BLINK_MIN_INTERVAL, 1, 12000);
            integer("blink_max_interval", SscClientConfig.BLINK_MAX_INTERVAL, 1, 12000);
            integer("blink_duration", SscClientConfig.BLINK_DURATION, 1, 1200);
            integer("instinct_position", SscClientConfig.INSTINCT_POSITION, 1, 9);
            integer("instinct_offset_x", SscClientConfig.INSTINCT_OFFSET_X, -10000, 10000);
            integer("instinct_offset_y", SscClientConfig.INSTINCT_OFFSET_Y, -10000, 10000);
            integer("mana_position", SscClientConfig.MANA_POSITION, 1, 9);
            integer("mana_offset_x", SscClientConfig.MANA_OFFSET_X, -10000, 10000);
            integer("mana_offset_y", SscClientConfig.MANA_OFFSET_Y, -10000, 10000);
            integer("magic_position", SscClientConfig.MAGIC_POSITION, 1, 9);
            integer("magic_offset_x", SscClientConfig.MAGIC_OFFSET_X, -10000, 10000);
            integer("magic_offset_y", SscClientConfig.MAGIC_OFFSET_Y, -10000, 10000);
            integer("item_store_position", SscClientConfig.ITEM_STORE_POSITION, 1, 9);
            integer("item_store_offset_x", SscClientConfig.ITEM_STORE_OFFSET_X, -10000, 10000);
            integer("item_store_offset_y", SscClientConfig.ITEM_STORE_OFFSET_Y, -10000, 10000);
        } else if (page == 1) {
            bool("disable_color_unlock_check", SscClientConfig.DISABLE_COLOR_UNLOCK_CHECK);
            bool("fcs_use_v1_menu", SscClientConfig.FCS_USE_V1_MENU);
            bool("auto_sync_config", SscClientConfig.CUSTOM_AUTO_SYNC_CONFIG);
            bool("enable_server_modify_fcd", SscClientConfig.CUSTOM_ENABLE_SERVER_MODIFY_FCD);
            bool("enable_form_default_color_system", SscClientConfig.CUSTOM_ENABLE_FORM_DEFAULT_COLOR_SYSTEM);
            bool("auto_sync_color_config", SscClientConfig.CUSTOM_AUTO_SYNC_COLOR_CONFIG);
            bool("keep_original_skin", SscClientConfig.CUSTOM_KEEP_ORIGINAL_SKIN);
            bool("enable_form_color", SscClientConfig.CUSTOM_ENABLE_FORM_COLOR);
            bool("enable_form_random_sound", SscClientConfig.CUSTOM_ENABLE_FORM_RANDOM_SOUND);
        } else {
            bool("immediate_transform", SscCommonConfig.IMMEDIATE_TRANSFORM);
            bool("food_habit", SscCommonConfig.FOOD_HABIT);
            bool("full_stack_upgrade", SscCommonConfig.FULL_STACK_UPGRADE);
            bool("initial_form_enabled", SscCommonConfig.ENABLE_INITIAL_FORM);
            rows.add(new Row("initial_forms", () -> String.join(", ", SscCommonConfig.INITIAL_FORMS.get()), text -> {
                List<String> entries = java.util.Arrays.stream(text.split(","))
                        .map(String::trim).filter(part -> !part.isEmpty()).toList();
                SscCommonConfig.INITIAL_FORMS.set(entries);
            }, false));
            bool("allow_sleep", SscCommonConfig.ALLOW_SLEEP_IN_CURSED_MOON);
            bool("enable_transform", SscCommonConfig.ENABLE_CURSED_MOON_TRANSFORM);
            bool("movement_logging", SscCommonConfig.ENABLE_MOVEMENT_DEBUG_LOGGING);
            bool("witch_potion_for_pre_book", SscCommonConfig.WITCH_POTION_FOR_PRE_BOOK);
            bool("status_potion_with_curse", SscCommonConfig.STATUS_POTION_WITH_CURSE);
            decimal("bat_chance", SscCommonConfig.TRANSFORMATIVE_BAT_SPAWN_CHANCE);
            decimal("axolotl_chance", SscCommonConfig.TRANSFORMATIVE_AXOLOTL_SPAWN_CHANCE);
            decimal("ocelot_chance", SscCommonConfig.TRANSFORMATIVE_OCELOT_SPAWN_CHANCE);
            decimal("wolf_chance", SscCommonConfig.TRANSFORMATIVE_WOLF_SPAWN_CHANCE);
            decimal("spider_chance", SscCommonConfig.TRANSFORMATIVE_SPIDER_SPAWN_CHANCE);
            rows.add(new Row("moon_phases", () -> SscCommonConfig.CURSED_MOON_PHASES.get().toString().replace("[", "").replace("]", ""), text -> {
                try {
                    List<Integer> phases = new ArrayList<>();
                    if (!text.isBlank()) for (String part : text.split(",")) {
                        int n = Integer.parseInt(part.trim());
                        if (n < 0 || n > 7) return;
                        phases.add(n);
                    }
                    SscCommonConfig.CURSED_MOON_PHASES.set(phases);
                } catch (NumberFormatException ignored) { }
            }, false));
        }
    }
    private static Component label(String key) { return Component.translatable("screen.shape-shifter-curse.config." + key); }
    @Override protected void init() {
        populate();
        int left = Math.max(8, width / 2 - 170), top = 52;
        int visible = Math.max(1, (height - 125) / 24);
        offset = Math.max(0, Math.min(offset, Math.max(0, rows.size() - visible)));
        for (int i = offset; i < Math.min(rows.size(), offset + visible); i++) {
            Row row = rows.get(i); int y = top + (i - offset) * 24;
            if (row.toggle) {
                Button button = Button.builder(Component.empty(), b -> {
                    row.set.accept(Boolean.toString(!Boolean.parseBoolean(row.value.get())));
                    b.setMessage(Component.translatable(Boolean.parseBoolean(row.value.get()) ? "options.on" : "options.off"));
                }).pos(left + 235, y).size(100, 20).build();
                button.setMessage(Component.translatable(Boolean.parseBoolean(row.value.get()) ? "options.on" : "options.off"));
                addRenderableWidget(button);
            } else {
                EditBox edit = new EditBox(font, left + 235, y, 100, 20, label(row.key));
                edit.setMaxLength(2048); edit.setValue(row.value.get()); edit.setResponder(row.set); addRenderableWidget(edit);
            }
        }
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { page = (page + 2) % 3; offset = 0; rebuildWidgets(); }).bounds(left, 25, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { page = (page + 1) % 3; offset = 0; rebuildWidgets(); }).bounds(left + 305, 25, 30, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▲"), b -> { offset = Math.max(0, offset - 1); rebuildWidgets(); }).bounds(left + 180, height - 67, 35, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> { offset = Math.min(Math.max(0, rows.size() - visible), offset + 1); rebuildWidgets(); }).bounds(left + 220, height - 67, 35, 20).build());
        if (page == 1) {
            addRenderableWidget(Button.builder(label("colors"), b -> minecraft.setScreen(new FormColorSelectMenuV2(label("colors"), this))).bounds(left, height - 67, 85, 20).build());
            addRenderableWidget(Button.builder(label("colors_v1"), b -> minecraft.setScreen(new FormColorSelectMenu(label("colors_v1"), this))).bounds(left + 88, height - 67, 85, 20).build());
            addRenderableWidget(Button.builder(label("unlock_all"), b -> FormColorData.client().unlockAll()).bounds(left, height - 44, 165, 20).build());
            addRenderableWidget(Button.builder(label("clear_unlocks"), b -> { FormColorData.client().unlockedForms.clear(); FormColorData.client().writeToConfig(); }).bounds(left + 170, height - 44, 165, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(width / 2 - 50, height - 21, 100, 20).build());
    }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xffffff);
        graphics.drawCenteredString(font, label(page == 0 ? "client" : page == 1 ? "custom" : "common"), width / 2, 31, 0xffffff);
        int left = Math.max(8, width / 2 - 170), visible = Math.max(1, (height - 125) / 24);
        for (int i = offset; i < Math.min(rows.size(), offset + visible); i++)
            graphics.drawString(font, label(rows.get(i).key), left, 58 + (i - offset) * 24, 0xffffff);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) {
        int visible = Math.max(1, (height - 125) / 24);
        offset = Math.max(0, Math.min(Math.max(0, rows.size() - visible), offset + (delta < 0 ? 1 : -1)));
        rebuildWidgets(); return true;
    }
    @Override public void onClose() {
        SscClientConfig.SPEC.save(); SscCommonConfig.SPEC.save(); ColorAutoSync.syncNow();
        var server = minecraft.getSingleplayerServer();
        if (server != null) server.execute(() -> {
            var packet = new net.onixary.shapeShifterCurseForge.network.GameplayConfigPacket(
                    SscCommonConfig.FOOD_HABIT.get(), SscCommonConfig.FULL_STACK_UPGRADE.get());
            net.onixary.shapeShifterCurseForge.network.ModNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.ALL.noArg(), packet);
        });
        minecraft.setScreen(parent);
    }
}
