package net.onixary.shapeShifterCurseForge.client.render;

/** The same 3-by-3 screen anchors used by Fabric's UIPositionUtils. */
public final class HudPosition {
    private HudPosition() {}
    public static int x(int screenWidth, int anchor, int offset) {
        return switch ((anchor - 1) % 3) {
            case 0 -> offset;
            case 2 -> screenWidth + offset;
            default -> screenWidth / 2 + offset;
        };
    }
    public static int y(int screenHeight, int anchor, int offset) {
        return switch ((anchor - 1) / 3) {
            case 0 -> offset;
            case 2 -> screenHeight + offset;
            default -> screenHeight / 2 + offset;
        };
    }
}
