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

    public static Vec3d turnNormal(Vec3d from, Vec3d to, Vec3d forward, double maxAngle) {
        double dot = Math.max(-1, Math.min(1, from.dotProduct(to)));
        double angle = Math.acos(dot);
        if (angle <= maxAngle) return to;
        Vec3d axis = dot < -0.99 ? tangent(forward, from) : cross(from, to).normalize();
        if (axis.lengthVector() < 0.5) axis = tangent(new Vec3d(1, 0, 0), from);
        return from.scale(Math.cos(maxAngle))
            .add(cross(axis, from).scale(Math.sin(maxAngle)))
            .normalize();
    }

    /** Parallel transport preserves the rider's heading through wall and ceiling transitions. */
    public static Vec3d transport(Vec3d direction, Vec3d fromUp, Vec3d toUp) {
        double dot = Math.max(-1, Math.min(1, fromUp.dotProduct(toUp)));
        Vec3d axis = cross(fromUp, toUp);
        double sin = axis.lengthVector();
        if (sin < 1.0E-6) return tangent(direction, toUp);
        axis = axis.scale(1 / sin);
        return tangent(
            direction.scale(dot)
                .add(cross(axis, direction).scale(sin))
                .add(axis.scale(axis.dotProduct(direction) * (1 - dot))),
            toUp);
    }

    private static Vec3d tangent(Vec3d direction, Vec3d normal) {
        return direction.subtract(normal.scale(direction.dotProduct(normal)))
            .normalize();
    }

    /** Mouse yaw and pitch in this surface's coordinates; positive pitch looks down. */
    public SurfaceFrame look(double yawDegrees, double pitchDegrees) {
        double yaw = Math.toRadians(yawDegrees), pitch = Math.toRadians(pitchDegrees);
        Vec3d heading = forward.scale(Math.cos(yaw))
            .subtract(right.scale(Math.sin(yaw)));
        return new SurfaceFrame(
            up.scale(Math.cos(pitch))
                .add(heading.scale(Math.sin(pitch))),
            heading.scale(Math.cos(pitch))
                .subtract(up.scale(Math.sin(pitch))));
    }

    public Vec3d toWorld(double x, double y, double z) {
        return right.scale(x)
            .add(up.scale(y))
            .add(forward.scale(z));
    }

    public static Vec3d cross(Vec3d a, Vec3d b) {
        return new Vec3d(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x);
    }
}
