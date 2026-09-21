package tcb.spiderstpo.common;

import java.util.List;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

/** Keep the visual contact on real geometry; the smoothed SDF is only an orientation estimate. */
public final class SurfaceAttachment {

    public static Vec3d findContact(List<AxisAlignedBB> boxes, Vec3d center, Vec3d normal, double reach) {
        Vec3 start = Vec3.createVectorHelper(center.x, center.y, center.z);
        Vec3 end = Vec3
            .createVectorHelper(center.x - normal.x * reach, center.y - normal.y * reach, center.z - normal.z * reach);
        Vec3d contact = null;
        double nearest = Double.POSITIVE_INFINITY;
        for (AxisAlignedBB box : boxes) {
            if (box.maxX <= box.minX || box.maxY <= box.minY || box.maxZ <= box.minZ) continue;
            MovingObjectPosition hit = box.calculateIntercept(start, end);
            if (hit == null) continue;
            double distance = start.squareDistanceTo(hit.hitVec);
            if (distance < nearest) {
                nearest = distance;
                contact = new Vec3d(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord);
            }
        }
        if (contact != null) return contact;
        // At an outside corner the blended normal may miss both faces. Use the nearest actual
        // point on either face instead of letting the smoothing distance pull the model away.
        for (AxisAlignedBB box : boxes) {
            if (box.maxX <= box.minX || box.maxY <= box.minY || box.maxZ <= box.minZ) continue;
            Vec3d point = new Vec3d(
                MathHelper.clamp_double(center.x, box.minX, box.maxX),
                MathHelper.clamp_double(center.y, box.minY, box.maxY),
                MathHelper.clamp_double(center.z, box.minZ, box.maxZ));
            Vec3d delta = point.subtract(center);
            double distance = delta.dotProduct(delta);
            if (distance < nearest) {
                nearest = distance;
                contact = point;
            }
        }
        return contact;
    }
}
