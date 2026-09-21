package tcb.spiderstpo.common;

/** Render basis built from world-space directions, independent of the navigation frame's yaw seam. */
public final class SurfaceFrame {

    public final Vec3d right, up, forward;

    private SurfaceFrame(Vec3d up, Vec3d forward) {
        this.up = up;
        right = cross(up, forward).normalize();
        this.forward = cross(right, up).normalize();
    }

    public static SurfaceFrame interpolate(Vec3d previousUp, Vec3d up, Vec3d previousForward, Vec3d forward, double t) {
        Vec3d normal = previousUp.scale(1 - t)
            .add(up.scale(t))
            .normalize();
        if (normal.lengthVector() < 0.5) normal = up.normalize();
        Vec3d before = tangent(previousForward, normal), after = tangent(forward, normal);
        if (before.lengthVector() < 0.5) before = after;
        if (after.lengthVector() < 0.5) after = before;
        if (before.lengthVector() < 0.5) {
            before = tangent(Math.abs(normal.z) < 0.9 ? new Vec3d(0, 0, 1) : new Vec3d(1, 0, 0), normal);
            after = before;
        }
        // Interpolate the turn around the surface normal, including genuine 180-degree turns.
        double angle = Math.atan2(normal.dotProduct(cross(before, after)), before.dotProduct(after)) * t;
        return new SurfaceFrame(
            normal,
            before.scale(Math.cos(angle))
                .add(cross(normal, before).scale(Math.sin(angle))));
    }

    private static Vec3d tangent(Vec3d direction, Vec3d normal) {
        return direction.subtract(normal.scale(direction.dotProduct(normal)))
            .normalize();
    }

    private static Vec3d cross(Vec3d a, Vec3d b) {
        return new Vec3d(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x);
    }
}
