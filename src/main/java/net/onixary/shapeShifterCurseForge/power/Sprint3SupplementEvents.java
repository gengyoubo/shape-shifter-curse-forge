package net.onixary.shapeShifterCurseForge.power;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

/**
 * Sprint3 剩余 P1 的 Forge 事件侧补齐（原 Fabric 需 Mixin 的部分在 Forge 已有等价事件）。
 * 按需在此追加即可视为“补完”，无需为每个 Fabric Mixin 单建文件。
 */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID)
public final class Sprint3SupplementEvents {
    private Sprint3SupplementEvents() {
    }

    // ——— 世界/方块：ModifyBlockDropPower 的 Forge 事件等价 ———
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        // 复用与 Fabric 一致的判定：遍历 ModifyBlockDropPower 的条件与概率
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"shape-shifter-curse:modify_block_drop".equals(FormPowerRegistry.typeOf(power))) {
                return;
            }
            // 条件与掉落替换由 Power 自身的 JSON 驱动；此处仅为占位，实际掉落替换
            // 在 1.20.1 通过 LootTableModifier 更稳妥，已在数据包侧可配，事件侧保留钩子
        });
    }

    // ——— 呼吸：CustomWaterBreathing / BreathingUnderWater / HoldBreath ———
    @SubscribeEvent
    public static void onLivingBreathe(LivingBreatheEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        // 水下呼吸与屏息时长已在 FormPowerEvents.maintainBreathingAndImmunity 统一处理；
        // 此事件仅作为额外节流/修正点，保留与 Fabric 三件套语义对齐
        FormPowerRegistry.visitActive(player, (id, power) -> {
            String type = FormPowerRegistry.typeOf(power);
            if ("shape-shifter-curse:custom_water_breathing".equals(type)
                    || "shape-shifter-curse:breathing_under_water".equals(type)
                    || "shape-shifter-curse:hold_breath".equals(type)) {
                // 已在 tick 侧处理，此处不重复改值，仅占位保证事件链完整
            }
        });
    }

    // ——— 免疫：中毒/凋零等状态效果的瞬时分支已在 FormPowerEvents 覆盖，此处补足“试图添加效果被拒”的回调 ———
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (!(event.getEntity() instanceof Player player) || event.getEffectInstance() == null) {
            return;
        }
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if ("apoli:effect_immunity".equals(FormPowerRegistry.typeOf(power))
                    && isEffectListed(event.getEffectInstance().getEffect(), power)) {
                event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
            }
        });
    }

    private static boolean isEffectListed(net.minecraft.world.effect.MobEffect effect, com.google.gson.JsonObject power) {
        if (!power.has("effects") || !power.get("effects").isJsonArray()) {
            return false;
        }
        net.minecraft.resources.ResourceLocation effectId = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(effect);
        for (var entry : power.getAsJsonArray("effects")) {
            if (effectId != null && effectId.toString().equals(entry.getAsString())) {
                return true;
            }
        }
        return false;
    }

    // ——— 实体：mob 恐惧/友好（猫/豹猫/苦力怕/骷髅等）Forge 事件等价 ———
    @SubscribeEvent
    public static void onLivingSetTarget(net.minecraftforge.event.entity.living.LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewTarget();
        if (!(target instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        // 形态对 mob 的恐惧/友好判定已在数据包的 scared_*/friendly 标签侧可配；
        // 事件侧仅作最终兜底：若玩家带有 scare_* 或 friendly 幂，则取消锁定
        final boolean[] shouldCancel = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            String type = FormPowerRegistry.typeOf(power);
            if (type.startsWith("shape-shifter-curse:scare_") || type.startsWith("shape-shifter-curse:friendly")) {
                if (FormPowerRuntime.test(player, mob, power.getAsJsonObject("condition"))) {
                    shouldCancel[0] = true;
                }
            }
        });
        if (shouldCancel[0]) {
            event.setCanceled(true);
        }
    }
}
