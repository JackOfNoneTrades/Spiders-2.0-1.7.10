package tcb.spiderstpo.common;

/** Immutable vectors for the upstream surface-orientation math. */
public final class Vec3d {

    public final double x, y, z;

    public Vec3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3d add(Vec3d v) {
        return new Vec3d(x + v.x, y + v.y, z + v.z);
    }

    public Vec3d addVector(double x, double y, double z) {
        return new Vec3d(this.x + x, this.y + y, this.z + z);
    }

    public Vec3d subtract(Vec3d v) {
        return new Vec3d(x - v.x, y - v.y, z - v.z);
    }

    public Vec3d scale(double s) {
        return new Vec3d(x * s, y * s, z * s);
    }

    public double dotProduct(Vec3d v) {
        return x * v.x + y * v.y + z * v.z;
    }

    public double lengthVector() {
        return Math.sqrt(dotProduct(this));
    }

    public Vec3d normalize() {
        double length = lengthVector();
        return length < 1.0e-8 ? new Vec3d(0, 0, 0) : scale(1 / length);
    }
}
