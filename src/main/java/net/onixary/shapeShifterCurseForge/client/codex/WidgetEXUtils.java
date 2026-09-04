package net.onixary.shapeShifterCurseForge.client.codex;

import java.util.List;

/** Forge port of Fabric's WidgetEXUtils: extended widgets hit-tested by rect with local coordinates. */
public final class WidgetEXUtils {
    private WidgetEXUtils() {
    }

    public static final class WidgetRect {
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        public WidgetRect(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public boolean isMouseInside(double mouseX, double mouseY) {
            return mouseX >= this.x && mouseX < this.x + this.width
                    && mouseY >= this.y && mouseY < this.y + this.height;
        }

        public double[] getMousePos(double mouseX, double mouseY) {
            return new double[]{mouseX - this.x, mouseY - this.y};
        }
    }

    public interface IWidgetEX {
        WidgetRect getRect();

        List<IWidgetEX> getWidgetList();

        default void addWidget(IWidgetEX widget) {
            getWidgetList().add(widget);
        }

        default void onClickWidget(double mouseX, double mouseY, int button) {
            for (IWidgetEX widget : getWidgetList()) {
                WidgetRect rect = widget.getRect();
                if (rect != null && rect.isMouseInside(mouseX, mouseY)) {
                    double[] local = rect.getMousePos(mouseX, mouseY);
                    widget.onClickWidget(local[0], local[1], button);
                }
            }
        }

        default void onReleaseWidget(double mouseX, double mouseY, int button) {
            for (IWidgetEX widget : getWidgetList()) {
                WidgetRect rect = widget.getRect();
                if (rect != null && rect.isMouseInside(mouseX, mouseY)) {
                    double[] local = rect.getMousePos(mouseX, mouseY);
                    widget.onReleaseWidget(local[0], local[1], button);
                }
            }
        }

        default void onDragWidget(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
            for (IWidgetEX widget : getWidgetList()) {
                WidgetRect rect = widget.getRect();
                if (rect != null && rect.isMouseInside(mouseX, mouseY)) {
                    double[] local = rect.getMousePos(mouseX, mouseY);
                    widget.onDragWidget(local[0], local[1], button, deltaX, deltaY);
                }
            }
        }

        default void onScrollWidget(double mouseX, double mouseY, double scrollDelta) {
            for (IWidgetEX widget : getWidgetList()) {
                WidgetRect rect = widget.getRect();
                if (rect != null && rect.isMouseInside(mouseX, mouseY)) {
                    double[] local = rect.getMousePos(mouseX, mouseY);
                    widget.onScrollWidget(local[0], local[1], scrollDelta);
                }
            }
        }
    }
}
