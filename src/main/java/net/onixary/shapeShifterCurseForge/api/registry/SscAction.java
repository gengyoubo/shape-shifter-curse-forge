package net.onixary.shapeShifterCurseForge.api.registry;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Java implementation of an SSC entity action type. */
@FunctionalInterface
public interface SscAction {
    /**
     * Executes the action. {@code target} is never {@code null}; SSC uses {@code actor} as the
     * target when the source JSON does not provide one.
     */
    void execute(Player actor, LivingEntity target, JsonObject data);
}
