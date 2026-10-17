package net.onixary.shapeShifterCurseForge.items;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Shared morph-scale recognition for smithing and equipment restrictions. */
public final class MorphScaleItems {
    public static final String MARKER_KEY = "MorphScaleItem";
    private static final TagKey<Item> ITEMS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("shape-shifter-curse", "morph_scale_item"));

    private MorphScaleItems() {}

    public static boolean isMorphScaleItem(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ITEMS) || hasUpgradeMarker(stack));
    }

    /** Keep equipment marked by earlier Forge versions and add-ons usable. */
    public static boolean hasUpgradeMarker(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && (tag.getBoolean(MARKER_KEY) || tag.getBoolean("MorphScale")
                || tag.getBoolean("morphscale") || tag.getBoolean("shape_shifter_curse_morphscale"));
    }
}
