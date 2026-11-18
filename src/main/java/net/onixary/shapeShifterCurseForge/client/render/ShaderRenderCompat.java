package net.onixary.shapeShifterCurseForge.client.render;

import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/** Optional shader bridges. Camera perspective alone cannot identify a shadow render. */
public final class ShaderRenderCompat {
    private static ShadowPassAccessor accessor = findAccessor();

    private ShaderRenderCompat() {}

    public static boolean isShadowPass() {
        ShadowPassAccessor current = accessor;
        if (current == null) return false;
        try {
            return current.isShadowPass();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            accessor = null;
            ShapeShifterCurseForge.LOGGER.warn("Disabling shader shadow-pass detection", exception);
            return false;
        }
    }

    private static ShadowPassAccessor findAccessor() {
        // Oculus 1.7+ / modern Iris, followed by the older Oculus / Iris namespace.
        for (String name : new String[]{"net.irisshaders.iris.shadows.ShadowRenderingState",
                "net.coderbot.iris.shadows.ShadowRenderingState"}) {
            try {
                Class<?> state = Class.forName(name, false, ShaderRenderCompat.class.getClassLoader());
                Method method = state.getMethod("areShadowsCurrentlyBeingRendered");
                if (method.getReturnType() == boolean.class && Modifier.isStatic(method.getModifiers())) {
                    return () -> (boolean) method.invoke(null);
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // A missing optional renderer must not prevent vanilla rendering.
            }
        }
        try {
            Class<?> shaders = Class.forName("net.optifine.shaders.Shaders", false,
                    ShaderRenderCompat.class.getClassLoader());
            Field field = shaders.getField("isShadowPass");
            if (field.getType() == boolean.class && Modifier.isStatic(field.getModifiers())) {
                return () -> field.getBoolean(null);
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
        }
        return null;
    }

    @FunctionalInterface
    private interface ShadowPassAccessor {
        boolean isShadowPass() throws ReflectiveOperationException;
    }
}
