package net.onixary.shapeShifterCurseForge.mixin;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.onixary.shapeShifterCurseForge.power.FormPowerRegistry;
import net.onixary.shapeShifterCurseForge.power.FormPowerRuntime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Apoli suppresses game-event emission, leaving the audible footstep intact. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelGameEventMixin {
    @Inject(method = "gameEvent(Lnet/minecraft/world/level/gameevent/GameEvent;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
            at = @At("HEAD"), cancellable = true)
    private void ssc$preventGameEvent(GameEvent event, Vec3 position, GameEvent.Context context, CallbackInfo ci) {
        if (!(context.sourceEntity() instanceof Player player)) return;
        String eventId = BuiltInRegistries.GAME_EVENT.getKey(event).toString();
        final boolean[] prevented = {false};
        FormPowerRegistry.visitActive(player, (id, power) -> {
            if (!"apoli:prevent_game_event".equals(FormPowerRegistry.typeOf(power))
                    || !FormPowerRuntime.test(player, player, power.getAsJsonObject("condition"))) return;
            boolean matches = eventId.equals(FormPowerRuntime.stringValue(power, "event", ""));
            if (!matches && power.has("events") && power.get("events").isJsonArray()) {
                for (JsonElement listed : power.getAsJsonArray("events")) {
                    if (listed.isJsonPrimitive() && eventId.equals(listed.getAsString())) {
                        matches = true;
                        break;
                    }
                }
            }
            if (!matches) return;
            prevented[0] = true;
            JsonObject action = power.getAsJsonObject("entity_action");
            if (action != null) FormPowerRuntime.execute(player, player, action);
        });
        if (prevented[0]) ci.cancel();
    }
}
