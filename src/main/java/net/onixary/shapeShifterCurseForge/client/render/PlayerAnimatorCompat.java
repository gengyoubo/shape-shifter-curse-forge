package net.onixary.shapeShifterCurseForge.client.render;

import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/** Optional PlayerAnimator 1.x bridge; SSC also runs without this client library. */
public final class PlayerAnimatorCompat {
    private static final Api API = findApi();
    private static boolean failed;

    private PlayerAnimatorCompat() {}

    public record ArmVisibility(boolean left, boolean right) {}

    /** Null for normal world/vanilla-hand rendering, including every remote player. */
    public static ArmVisibility firstPersonArms(Player player) {
        if (API == null || failed || player != Minecraft.getInstance().player || !API.playerType.isInstance(player)) {
            return null;
        }
        try {
            if (!(boolean) API.firstPersonPass.invoke(null)) return null;
            Object processor = API.getAnimation.invoke(player);
            Object config = API.firstPersonConfig.invoke(processor);
            return new ArmVisibility((boolean) API.showLeftArm.invoke(config), (boolean) API.showRightArm.invoke(config));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            failed = true;
            ShapeShifterCurseForge.LOGGER.warn("PlayerAnimator first-person bridge unavailable", exception);
            return null;
        }
    }

    /** Set the same frame clock vanilla's renderer will use and copy its external root motion. */
    public static BedrockAnimationPlayer.BodyTransform prepare(Player player, float partialTick) {
        if (API == null || failed || !API.playerType.isInstance(player)) {
            return BedrockAnimationPlayer.BodyTransform.IDENTITY;
        }
        try {
            Object processor = API.getAnimation.invoke(player);
            API.setTickDelta.invoke(processor, partialTick);
            if (!(boolean) API.isActive.invoke(processor)) return BedrockAnimationPlayer.BodyTransform.IDENTITY;
            Object zero = API.vector.newInstance(0.0F, 0.0F, 0.0F);
            Object position = API.transform.invoke(processor, "body", API.position, zero);
            Object rotation = API.transform.invoke(processor, "body", API.rotation, zero);
            return new BedrockAnimationPlayer.BodyTransform(API.x(position), API.y(position), API.z(position),
                    API.x(rotation), API.y(rotation), API.z(rotation));
        } catch (ReflectiveOperationException | RuntimeException exception) {
            failed = true;
            ShapeShifterCurseForge.LOGGER.warn("PlayerAnimator root-motion bridge unavailable", exception);
            return BedrockAnimationPlayer.BodyTransform.IDENTITY;
        }
    }

    private static Api findApi() {
        try {
            Class<?> player = Class.forName("dev.kosmx.playerAnim.impl.IAnimatedPlayer");
            Class<?> processor = Class.forName("dev.kosmx.playerAnim.core.impl.AnimationProcessor");
            Class<?> type = Class.forName("dev.kosmx.playerAnim.api.TransformType");
            Class<?> vector = Class.forName("dev.kosmx.playerAnim.core.util.Vec3f");
            Class<?> mode = Class.forName("dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode");
            Class<?> config = Class.forName("dev.kosmx.playerAnim.api.firstPerson.FirstPersonConfiguration");
            return new Api(player, player.getMethod("playerAnimator_getAnimation"),
                    processor.getMethod("setTickDelta", float.class), processor.getMethod("isActive"),
                    processor.getMethod("get3DTransform", String.class, type, vector),
                    vector.getConstructor(float.class, float.class, float.class),
                    vector.getMethod("getX"), vector.getMethod("getY"), vector.getMethod("getZ"),
                    type.getField("POSITION").get(null), type.getField("ROTATION").get(null),
                    mode.getMethod("isFirstPersonPass"), processor.getMethod("getFirstPersonConfiguration"),
                    config.getMethod("isShowLeftArm"), config.getMethod("isShowRightArm"));
        } catch (ClassNotFoundException exception) {
            return null;
        } catch (ReflectiveOperationException | LinkageError exception) {
            ShapeShifterCurseForge.LOGGER.warn("Unsupported PlayerAnimator API; root-motion bridge disabled", exception);
            return null;
        }
    }

    private record Api(Class<?> playerType, Method getAnimation, Method setTickDelta, Method isActive,
                       Method transform, Constructor<?> vector, Method getX, Method getY, Method getZ,
                       Object position, Object rotation, Method firstPersonPass, Method firstPersonConfig,
                       Method showLeftArm, Method showRightArm) {
        float x(Object value) throws ReflectiveOperationException { return (float) getX.invoke(value); }
        float y(Object value) throws ReflectiveOperationException { return (float) getY.invoke(value); }
        float z(Object value) throws ReflectiveOperationException { return (float) getZ.invoke(value); }
    }
}
