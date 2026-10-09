package net.onixary.shapeShifterCurseForge.mixin.integration;

import java.util.function.Consumer;

import net.ltxprogrammer.changed.entity.TransfurContext;
import net.ltxprogrammer.changed.entity.ai.AssimilationBehavior;
import net.ltxprogrammer.changed.entity.ai.ImmediateTransfurDecision;
import net.ltxprogrammer.changed.entity.ai.LatexAssimilationDecision;
import net.ltxprogrammer.changed.entity.ai.NonLatexAssimilationDecision;
import net.ltxprogrammer.changed.entity.variant.TransfurVariant;
import net.ltxprogrammer.changed.entity.variant.TransfurVariantInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.integration.changed.ChangedIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Block both assimilation and direct assignments before Changed starts their side effects. */
@Pseudo
@Mixin(targets = "net.ltxprogrammer.changed.process.ProcessTransfur", remap = false)
public abstract class ChangedProcessTransfurMixin {
    @Inject(method = "computeAssimilationBehavior(Lnet/minecraft/world/entity/LivingEntity;Lnet/ltxprogrammer/changed/entity/ai/LatexAssimilationDecision;)Lnet/ltxprogrammer/changed/entity/ai/AssimilationBehavior;",
            at = @At("HEAD"), cancellable = true)
    private static void ssc$blockLatexAssimilation(LivingEntity entity, LatexAssimilationDecision<?> decision,
                                                  CallbackInfoReturnable<AssimilationBehavior> cir) {
        if (entity instanceof Player player && ChangedIntegration.hasSscVariant(player)) {
            // Changed also queries this while selecting targets, without an actual attempt.
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "computeAssimilationBehavior(Lnet/minecraft/world/entity/LivingEntity;Lnet/ltxprogrammer/changed/entity/ai/NonLatexAssimilationDecision;)Lnet/ltxprogrammer/changed/entity/ai/AssimilationBehavior;",
            at = @At("HEAD"), cancellable = true)
    private static void ssc$blockNonLatexAssimilation(LivingEntity entity, NonLatexAssimilationDecision<?> decision,
                                                     CallbackInfoReturnable<AssimilationBehavior> cir) {
        if (entity instanceof Player player && ChangedIntegration.hasSscVariant(player)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "computeAssimilationBehavior(Lnet/minecraft/world/entity/LivingEntity;Lnet/ltxprogrammer/changed/entity/ai/ImmediateTransfurDecision;)Lnet/ltxprogrammer/changed/entity/ai/AssimilationBehavior;",
            at = @At("HEAD"), cancellable = true)
    private static void ssc$blockImmediateTransfur(LivingEntity entity, ImmediateTransfurDecision<?> decision,
                                                  CallbackInfoReturnable<AssimilationBehavior> cir) {
        if (entity instanceof Player player && ChangedIntegration.hasSscVariant(player)) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "progressTransfur(Lnet/minecraft/world/entity/LivingEntity;Lnet/ltxprogrammer/changed/entity/ai/LatexAssimilationDecision;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void ssc$blockLatexProgress(LivingEntity entity, LatexAssimilationDecision<?> decision,
                                               CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player player && ChangedIntegration.hasSscVariant(player)) {
            if (decision != null && decision.transfurProgress() > 0.0F) {
                ChangedIntegration.notifyBlocked(player);
            }
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "progressTransfur(Lnet/minecraft/world/entity/LivingEntity;Lnet/ltxprogrammer/changed/entity/ai/NonLatexAssimilationDecision;)Z",
            at = @At("HEAD"), cancellable = true)
    private static void ssc$blockNonLatexProgress(LivingEntity entity, NonLatexAssimilationDecision<?> decision,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Player player && ChangedIntegration.hasSscVariant(player)) {
            if (decision != null && decision.transfurProgress() > 0.0F) {
                ChangedIntegration.notifyBlocked(player);
            }
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "transfur(Lnet/minecraft/world/entity/LivingEntity;Lnet/ltxprogrammer/changed/entity/ai/ImmediateTransfurDecision;)V",
            at = @At("HEAD"), cancellable = true)
    private static void ssc$blockImmediateAttempt(LivingEntity entity, ImmediateTransfurDecision<?> decision,
                                                  CallbackInfo ci) {
        if (entity instanceof Player player && ChangedIntegration.hasSscVariant(player)) {
            if (decision != null) {
                ChangedIntegration.notifyBlocked(player);
            }
            ci.cancel();
        }
    }

    @Inject(method = "setPlayerTransfurVariant(Lnet/minecraft/world/entity/player/Player;Lnet/ltxprogrammer/changed/entity/variant/TransfurVariant;Lnet/ltxprogrammer/changed/entity/TransfurContext;FZLjava/util/function/Consumer;)Lnet/ltxprogrammer/changed/entity/variant/TransfurVariantInstance;",
            at = @At("HEAD"), cancellable = true)
    private static void ssc$blockVariantAssignment(Player player, TransfurVariant<?> variant, TransfurContext context,
                                                   float progress, boolean temporary,
                                                   Consumer<TransfurVariantInstance<?>> consumer,
                                                   CallbackInfoReturnable<TransfurVariantInstance<?>> cir) {
        // Null assignments must remain available to cure old conflicting saves.
        if (variant != null && ChangedIntegration.hasSscVariant(player)) {
            ChangedIntegration.notifyBlocked(player);
            cir.setReturnValue(null);
        }
    }
}
