package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.form.FormBodyType;
import net.onixary.shapeShifterCurseForge.form.FormDefinition;
import net.onixary.shapeShifterCurseForge.form.FormManager;
import net.onixary.shapeShifterCurseForge.other.config.SscClientConfig;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Fabric's First-person Model (FPM) linkage: feral forms shift FPM's body X offsets so the
 * quadruped body sits behind the camera (12 on FPM 2.6.0+, -25 before), and a form change
 * resets them to 0 for one second. Done by reflection so FPM stays an optional mod.
 */
@Mod.EventBusSubscriber(modid = ShapeShifterCurseForge.MOD_ID, value = Dist.CLIENT)
public final class FirstPersonModelCompat {
    private static final String MOD_ID = "firstperson";
    private static final int RESET_TICKS = 20;
    private static boolean resolved;
    private static Field instanceField;
    private static Method getConfig;
    private static Method isRenderingPlayer;
    private static boolean newVersion;
    private static boolean versionCheckFailed;
    private static ResourceLocation lastForm;
    private static int resetTicks;

    private FirstPersonModelCompat() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !SscClientConfig.CHANGE_FPM_CONFIG.get()
                || !resolve() || getConfig == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            lastForm = null;
            resetTicks = 0;
            return;
        }
        FormDefinition form = FormManager.current(minecraft.player);
        if (lastForm != null && !lastForm.equals(form.id())) {
            resetTicks = RESET_TICKS;
        }
        lastForm = form.id();
        if (resetTicks > 0) {
            resetTicks--;
            setOffsets(0);
        }
        if (form.bodyType() == FormBodyType.FERAL && isLocalFirstPerson(minecraft.player)
                && !versionCheckFailed && getConfig != null) {
            setOffsets(newVersion ? 12 : -25);
        }
    }

    static boolean isLocalFirstPerson(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        return player == minecraft.player && player == minecraft.getCameraEntity()
                && minecraft.options.getCameraType().isFirstPerson();
    }

    /** Whether the current player render is First-person Model's body pass. */
    static boolean isRenderingPlayer() {
        if (!resolve() || isRenderingPlayer == null) {
            return false;
        }
        try {
            Object core = instanceField.get(null);
            return core != null && (boolean) isRenderingPlayer.invoke(core);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LogUtils.getLogger().warn("Disabling First-person Model render-pass detection", exception);
            isRenderingPlayer = null;
            return false;
        }
    }

    private static boolean resolve() {
        if (resolved) {
            return instanceField != null;
        }
        resolved = true;
        if (!ModList.get().isLoaded(MOD_ID)) {
            return false;
        }
        try {
            Class<?> core = Class.forName("dev.tr7zw.firstperson.FirstPersonModelCore");
            instanceField = core.getField("instance");
            getConfig = core.getMethod("getConfig");
            isRenderingPlayer = core.getMethod("isRenderingPlayer");
        } catch (ReflectiveOperationException | LinkageError exception) {
            LogUtils.getLogger().warn("First-person Model is loaded but its integration API was not found", exception);
            instanceField = null;
            return false;
        }
        try {
            newVersion = ModList.get().getModContainerById(MOD_ID)
                    .map(container -> container.getModInfo().getVersion()
                            .compareTo(new DefaultArtifactVersion("2.6.0")) >= 0)
                    .orElse(false);
        } catch (RuntimeException exception) {
            LogUtils.getLogger().error("Failed to check FirstPerson Mod version");
            versionCheckFailed = true;
        }
        return true;
    }

    private static void setOffsets(int offset) {
        try {
            Object core = instanceField.get(null);
            Object config = core == null ? null : getConfig.invoke(core);
            if (config == null) {
                return;
            }
            Class<?> type = config.getClass();
            type.getField("xOffset").setInt(config, offset);
            type.getField("sitXOffset").setInt(config, offset);
            type.getField("sneakXOffset").setInt(config, offset);
        } catch (ReflectiveOperationException | RuntimeException exception) {
            LogUtils.getLogger().warn("Disabling First-person Model offset sync", exception);
            getConfig = null;
        }
    }
}
