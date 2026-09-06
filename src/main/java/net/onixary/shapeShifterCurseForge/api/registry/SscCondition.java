package net.onixary.shapeShifterCurseForge.api.registry;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Java implementation of an SSC condition type.
 *
 * <p>{@code target} can be {@code null} when the condition is evaluated only against the
 * acting player. The JSON object is the complete condition object, including its {@code type}
 * and any implementation-specific fields. SSC applies the standard {@code inverted} field
 * after this method returns.</p>
 */
@FunctionalInterface
public interface SscCondition {
    boolean test(Player actor, Entity target, JsonObject data);
}
