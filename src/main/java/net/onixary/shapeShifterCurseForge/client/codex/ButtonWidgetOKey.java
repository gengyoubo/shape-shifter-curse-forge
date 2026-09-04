package net.onixary.shapeShifterCurseForge.client.codex;

import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.function.BiPredicate;

/** Button with configurable accepted mouse buttons (left/right/middle click). */
public class ButtonWidgetOKey extends Button {
    public BiPredicate<ButtonWidgetOKey, Integer> canClick = null;

    public static final BiPredicate<ButtonWidgetOKey, Integer> LEFT_CLICK = (button, key) -> key == 0;
    public static final BiPredicate<ButtonWidgetOKey, Integer> RIGHT_CLICK = (button, key) -> key == 1;
    public static final BiPredicate<ButtonWidgetOKey, Integer> MIDDLE_CLICK = (button, key) -> key == 2;

    public static final Button.CreateNarration DEFAULT_NARRATION_SUPPLIER =
            textSupplier -> (MutableComponent) textSupplier.get();

    public ButtonWidgetOKey(int x, int y, int width, int height, Component message,
                            OnPress onPress, Button.CreateNarration narrationSupplier) {
        super(x, y, width, height, message, onPress, narrationSupplier);
    }

    @Override
    protected boolean isValidClickButton(int button) {
        if (canClick != null) {
            return canClick.test(this, button);
        }
        return super.isValidClickButton(button);
    }
}
