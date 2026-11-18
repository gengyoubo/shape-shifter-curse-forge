package net.onixary.shapeShifterCurseForge.items;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.onixary.shapeShifterCurseForge.network.ModNetwork;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.form.FormRegistry;
import net.onixary.shapeShifterCurseForge.form.TransformManager;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Hold to choose a cosmetic variant of the current master form.
 */
public class RippleMirror extends Item {
    // One selection per completed use; weak player keys also discard disconnected sessions.
    private static final Map<ServerPlayer, PendingSelection> SELECTIONS = new WeakHashMap<>();

    private record PendingSelection(ResourceLocation sourceForm, ItemStack mirror) {}

    public RippleMirror(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 36;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (availableForms(FormManager.current(player).id()).isEmpty()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable(
                        "message.shape-shifter-curse.ripple_mirror.no_variants"), true);
            }
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user) {
        if (user instanceof ServerPlayer player) {
            SELECTIONS.remove(player);
            ResourceLocation source = FormManager.current(player).id();
            List<ResourceLocation> forms = availableForms(source);
            if (!forms.isEmpty() && !TransformManager.isTransforming(player)) {
                SELECTIONS.put(player, new PendingSelection(source, stack));
                ModNetwork.sendOpenRippleMirror(player, forms);
            }
        }
        return stack;
    }

    /** Includes the master so a cosmetic variant can be switched back. */
    public static List<ResourceLocation> availableForms(ResourceLocation current) {
        ResourceLocation master = FormRegistry.masterFormOf(current);
        ResourceLocation root = master == null ? current : master;
        List<ResourceLocation> family = FormRegistry.forms().keySet().stream()
                .filter(id -> id.equals(root) || root.equals(FormRegistry.masterFormOf(id)))
                .toList();
        return family.size() < 2 ? List.of() : family.stream().filter(id -> !id.equals(current)).toList();
    }

    /** The client only chooses an id: the server owns the player, family and use authorization. */
    public static boolean selectForm(ServerPlayer player, ResourceLocation formId) {
        PendingSelection pending = SELECTIONS.remove(player);
        if (pending == null || pending.mirror().isEmpty()
                || (player.getMainHandItem() != pending.mirror() && player.getOffhandItem() != pending.mirror())
                || !FormManager.current(player).id().equals(pending.sourceForm())
                || TransformManager.isTransforming(player)
                || !availableForms(pending.sourceForm()).contains(formId)) {
            return false;
        }
        return TransformManager.startTransform(player, formId);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.shape-shifter-curse.ripple_mirror.tooltip")
                .withStyle(ChatFormatting.GRAY));
    }
}
