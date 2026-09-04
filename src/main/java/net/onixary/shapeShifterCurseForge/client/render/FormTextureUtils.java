package net.onixary.shapeShifterCurseForge.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.player.Player;
import net.onixary.shapeShifterCurseForge.ShapeShifterCurseForge;
import net.onixary.shapeShifterCurseForge.capability.IPlayerSkinData;
import net.onixary.shapeShifterCurseForge.capability.ModCapabilities;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/** Color baking and conversion utilities plus the temporary preview hooks. */
public final class FormTextureUtils {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Live preview hook used by the color menu for the local player. */
    public interface TempFormTextureProcessor {
        ResourceLocation getTexture(String formPath, ResourceLocation texture,
                                    ResourceLocation mask, boolean onlyMultiply);
    }

    public static boolean useTempFormTexture = false;
    public static TempFormTextureProcessor tempFormTextureProcessor = null;

    private FormTextureUtils() {
    }

    public record ColorSetting(int primaryColor, int accentColor1, int accentColor2,
                               int eyeColorA, int eyeColorB,
                               boolean primaryGreyReverse, boolean accent1GreyReverse,
                               boolean accent2GreyReverse) {
    }

    /** Grayscale triple (R, G, B channel averages) used by the mask processor. */
    public record GreyAverage(int red, int green, int blue) {
    }

    public static NativeImage toNativeImage(ResourceLocation texture) {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(texture);
            if (resource.isEmpty()) {
                return null;
            }
            try (InputStream inputStream = resource.get().open()) {
                return NativeImage.read(inputStream);
            }
        } catch (IOException exception) {
            LOGGER.warn("Failed to load texture {}", texture, exception);
            return null;
        }
    }

    public static int rgba2Abgr(int color) {
        return (color << 24) | ((color << 8) & 0x00FF0000) | ((color >> 8) & 0x0000FF00) | (color >>> 24);
    }

    public static int rgb2Abgr(int color) {
        return rgba2Abgr((color << 8) | 0xFF);
    }

    public static int argb2Abgr(int color) {
        int alpha = (color >> 24) & 0xFF;
        return rgba2Abgr((color << 8) | alpha);
    }

    public static int abgr2Rgba(int color) {
        return (color << 24) | ((color << 8) & 0x00FF0000) | ((color >> 8) & 0x0000FF00) | (color >>> 24);
    }

    public static int abgr2Rgb(int color) {
        return abgr2Rgba(color) >>> 8;
    }

    public static int abgr2Argb(int color) {
        return abgr2Rgb(color) | (color & 0xFF000000);
    }

    public static int colorMulBytes(int colorA, int bytes) {
        return (colorA & 0xFF000000)
                | ((((colorA >> 16) & 0xFF) * bytes) / 255 << 16)
                | ((((colorA >> 8) & 0xFF) * bytes) / 255 << 8)
                | (((colorA & 0xFF) * bytes) / 255);
    }

    public static int greyScaleMul(int color, float greyScale) {
        int red = Math.min(255, Math.max((int) (greyScale * (color & 0xFF)), 0));
        int green = Math.min(255, Math.max((int) (greyScale * ((color >> 8) & 0xFF)), 0));
        int blue = Math.min(255, Math.max((int) (greyScale * ((color >> 16) & 0xFF)), 0));
        return 0xFF000000 | (blue << 16) | (green << 8) | red;
    }

    public static int getGreyScale(int color) {
        int red = (color >> 16) & 0xFF;
        int green = (color >> 8) & 0xFF;
        int blue = color & 0xFF;
        return (red * 28 + green * 151 + blue * 77) >> 8;
    }

    public static int overrideGreyScale(int a, int b, float t) {
        return Math.min(a + (int) (t * b), 255);
    }

    public static GreyAverage getAverageGreyScale(NativeImage image, NativeImage maskImage) {
        int textureWidth = image.getWidth();
        int textureHeight = image.getHeight();
        long red = 0, green = 0, blue = 0;
        int redCount = 0, greenCount = 0, blueCount = 0;
        for (int x = 0; x < textureWidth; x++) {
            for (int y = 0; y < textureHeight; y++) {
                int mask = maskImage.getPixelRGBA(x, y);
                if ((mask & 0x00FF0000) > 0) {
                    blue += getGreyScale(image.getPixelRGBA(x, y));
                    blueCount++;
                } else if ((mask & 0x0000FF00) > 0) {
                    green += getGreyScale(image.getPixelRGBA(x, y));
                    greenCount++;
                } else if ((mask & 0x000000FF) > 0) {
                    red += getGreyScale(image.getPixelRGBA(x, y));
                    redCount++;
                }
            }
        }
        return new GreyAverage(redCount == 0 ? 255 : (int) red / redCount,
                greenCount == 0 ? 255 : (int) green / greenCount,
                blueCount == 0 ? 255 : (int) blue / blueCount);
    }

    public static int processMaskChannel(int color, int mask, int colorSetting, int averageGreyScale,
                                         boolean reverseGreyScale) {
        if (((colorSetting >> 24) & 0xFF) == 0) {
            return color;
        }
        int colorGreyScale = getGreyScale(color);
        int greyScaleOffset = reverseGreyScale ? averageGreyScale - colorGreyScale : colorGreyScale - averageGreyScale;
        int colorSettingGreyScale = getGreyScale(colorSetting);
        int targetGreyScale = Math.min(255, Math.max(colorSettingGreyScale + greyScaleOffset, 0));
        int colorResult = greyScaleMul(colorSetting | 0xFF000000, (float) targetGreyScale / colorSettingGreyScale);
        return colorMulBytes(colorResult, mask);
    }

    public static int processPixel(int color, int mask, ColorSetting colorSetting,
                                   GreyAverage maskLayerAverageGreyScale, boolean onlyMultiply) {
        if (mask == 0) {
            return color;
        }
        int maskAlpha = mask >>> 24;
        if (maskAlpha != 255) {
            if (maskAlpha == 1) {
                if ((colorSetting.eyeColorA() >>> 24) == 0) {
                    return color;
                }
                return (colorSetting.eyeColorA() & 0x00FFFFFF) | (color & 0xFF000000);
            }
            if (maskAlpha == 2) {
                if ((colorSetting.eyeColorB() >>> 24) == 0) {
                    return color;
                }
                return (colorSetting.eyeColorB() & 0x00FFFFFF) | (color & 0xFF000000);
            }
        }
        int blue = (mask >> 16) & 0xFF;
        if (blue > 0) {
            if (onlyMultiply) {
                return (colorMulBytes(colorSetting.accentColor2(), blue) & 0x00FFFFFF) | (color & 0xFF000000);
            }
            int result = processMaskChannel(color, blue, colorSetting.accentColor2(),
                    maskLayerAverageGreyScale.blue(), colorSetting.accent2GreyReverse());
            return (result & 0x00FFFFFF) | (color & 0xFF000000);
        }
        int green = (mask >> 8) & 0xFF;
        if (green > 0) {
            if (onlyMultiply) {
                return (colorMulBytes(colorSetting.accentColor1(), green) & 0x00FFFFFF) | (color & 0xFF000000);
            }
            int result = processMaskChannel(color, green, colorSetting.accentColor1(),
                    maskLayerAverageGreyScale.green(), colorSetting.accent1GreyReverse());
            return (result & 0x00FFFFFF) | (color & 0xFF000000);
        }
        int red = mask & 0xFF;
        if (red > 0) {
            if (onlyMultiply) {
                return (colorMulBytes(colorSetting.primaryColor(), red) & 0x00FFFFFF) | (color & 0xFF000000);
            }
            int result = processMaskChannel(color, red, colorSetting.primaryColor(),
                    maskLayerAverageGreyScale.red(), colorSetting.primaryGreyReverse());
            return (result & 0x00FFFFFF) | (color & 0xFF000000);
        }
        return color;
    }

    /** Bakes a recolored copy of a form texture plus its mask. Caller owns registration. */
    @Nullable
    public static NativeImage bakeTextureImage(ResourceLocation texture, ResourceLocation mask,
                                               ColorSetting colorSetting, boolean onlyMultiply) {
        if (texture == null || mask == null) {
            return null;
        }
        NativeImage textureImage = toNativeImage(texture);
        NativeImage maskImage = toNativeImage(mask);
        if (textureImage == null || maskImage == null) {
            return null;
        }
        GreyAverage average = getAverageGreyScale(textureImage, maskImage);
        for (int x = 0; x < textureImage.getWidth(); x++) {
            for (int y = 0; y < textureImage.getHeight(); y++) {
                textureImage.setPixelRGBA(x, y, processPixel(textureImage.getPixelRGBA(x, y),
                        maskImage.getPixelRGBA(x, y), colorSetting, average, onlyMultiply));
            }
        }
        return textureImage;
    }

    /** Registers a baked copy under a generated id. */
    public static ResourceLocation registerBakedTexture(NativeImage image, String prefix) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(ShapeShifterCurseForge.RESOURCE_NAMESPACE,
                prefix + Long.toHexString(System.nanoTime()));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        return id;
    }

    public static void releaseTexture(ResourceLocation id) {
        if (id != null) {
            Minecraft.getInstance().getTextureManager().release(id);
        }
    }

    /** Server-synced skin color for rendering (ABGR, as baked). Null when disabled. */
    @Nullable
    public static ColorSetting getPlayerColorSetting(Player player) {
        IPlayerSkinData data = player.getCapability(ModCapabilities.PLAYER_SKIN).orElse(null);
        if (data == null || !data.isEnableFormColor()) {
            return null;
        }
        return data.getFormColor();
    }

    // H(0~359) S(0~100) V(0~100) -> RGB(0~255)
    public static int[] hsvToRgb(int h, int s, int v) {
        double hue = Math.min(359, Math.max(0, h));
        double saturation = Math.min(100, Math.max(0, s)) / 100.0D;
        double value = Math.min(100, Math.max(0, v)) / 100.0D;
        double chroma = value * saturation;
        double x = chroma * (1 - Math.abs((hue / 60.0D) % 2 - 1));
        double m = value - chroma;
        double r1, g1, b1;
        if (hue < 60) {
            r1 = chroma;
            g1 = x;
            b1 = 0;
        } else if (hue < 120) {
            r1 = x;
            g1 = chroma;
            b1 = 0;
        } else if (hue < 180) {
            r1 = 0;
            g1 = chroma;
            b1 = x;
        } else if (hue < 240) {
            r1 = 0;
            g1 = x;
            b1 = chroma;
        } else if (hue < 300) {
            r1 = x;
            g1 = 0;
            b1 = chroma;
        } else {
            r1 = chroma;
            g1 = 0;
            b1 = x;
        }
        return new int[]{
                Math.min(255, Math.max(0, (int) Math.round((r1 + m) * 255))),
                Math.min(255, Math.max(0, (int) Math.round((g1 + m) * 255))),
                Math.min(255, Math.max(0, (int) Math.round((b1 + m) * 255)))};
    }

    // RGB(0~255) -> H(0~359) S(0~100) V(0~100)
    public static int[] rgbToHsv(int r, int g, int b) {
        double red = Math.min(255, Math.max(0, r)) / 255.0D;
        double green = Math.min(255, Math.max(0, g)) / 255.0D;
        double blue = Math.min(255, Math.max(0, b)) / 255.0D;
        double max = Math.max(red, Math.max(green, blue));
        double min = Math.min(red, Math.min(green, blue));
        double delta = max - min;
        double hue;
        if (delta == 0) {
            hue = 0;
        } else if (max == red) {
            hue = (green - blue) / delta;
        } else if (max == green) {
            hue = 2 + (blue - red) / delta;
        } else {
            hue = 4 + (red - green) / delta;
        }
        hue *= 60;
        if (hue < 0) {
            hue += 360;
        }
        double saturation = (max == 0) ? 0 : delta / max;
        int hueInt = Math.min(359, Math.max(0, (int) Math.round(hue)));
        int satInt = Math.min(100, Math.max(0, (int) Math.round(saturation * 100)));
        int valInt = Math.min(100, Math.max(0, (int) Math.round(max * 100)));
        if (satInt == 0) {
            hueInt = 0;
        }
        if (hueInt == 360) {
            hueInt = 0;
        }
        return new int[]{hueInt, satInt, valInt};
    }
}
