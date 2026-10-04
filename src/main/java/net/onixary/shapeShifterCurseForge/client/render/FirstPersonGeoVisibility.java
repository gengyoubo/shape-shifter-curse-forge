package net.onixary.shapeShifterCurseForge.client.render;

import software.bernie.geckolib.cache.object.GeoBone;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Draw only the requested arm subtrees, preserving parent transforms and authored visibility. */
public final class FirstPersonGeoVisibility implements AutoCloseable {
    private final Map<GeoBone, Visibility> original = new IdentityHashMap<>();

    public FirstPersonGeoVisibility(List<GeoBone> roots, Set<String> arms) {
        for (GeoBone root : roots) mask(root, arms, false);
    }

    private void mask(GeoBone bone, Set<String> arms, boolean inArm) {
        boolean visible = inArm || arms.contains(bone.getName());
        Visibility previous = new Visibility(bone.isHidden(), bone.isHidingChildren());
        original.put(bone, previous);
        bone.setHidden(previous.hidden || !visible);
        // setHidden also hides children. Keep traversing hidden torso/root bones so
        // nested arms retain their parent transforms; preserve authored subtree hiding.
        bone.setChildrenHidden(previous.childrenHidden);
        for (GeoBone child : bone.getChildBones()) mask(child, arms, visible);
    }

    @Override
    public void close() {
        original.forEach((bone, visibility) -> {
            bone.setHidden(visibility.hidden);
            bone.setChildrenHidden(visibility.childrenHidden);
        });
        original.clear();
    }

    private record Visibility(boolean hidden, boolean childrenHidden) {}
}
