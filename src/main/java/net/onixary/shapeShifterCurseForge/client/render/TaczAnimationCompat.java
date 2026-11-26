package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormManager;

/** Optional gun detection without linking TACZ classes on clients that lack the mod. */
public final class TaczAnimationCompat {
    private static final Class<?> GUN_TYPE = findGunType();

    private TaczAnimationCompat() { }

    public static boolean usesSscThirdPerson(LivingEntity entity) {
        return entity instanceof Player player && FormManager.current(player).bodyType() == FormBodyType.FERAL
                && !(FirstPersonModelCompat.isLocalFirstPerson(player) && FirstPersonModelCompat.isRenderingPlayer());
    }

    public static boolean isGun(ItemStack stack) {
        return GUN_TYPE != null && GUN_TYPE.isInstance(stack.getItem());
    }

    private static Class<?> findGunType() {
        try {
            return Class.forName("com.tacz.guns.api.item.IGun", false,
                    TaczAnimationCompat.class.getClassLoader());
        } catch (ClassNotFoundException | LinkageError exception) {
            return null;
        }
    }
}
