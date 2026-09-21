// Adapted from TheCyberBrick's Spiders 2.0 for Minecraft 1.7.10, 2026-09-21.
package tcb.spiderstpo.common;

import java.util.List;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

import org.apache.commons.lang3.tuple.Pair;

public class CollisionSmoothingUtil {

    private static float invSqrt(float x) {
        float xhalf = 0.5f * x;
        int i = Float.floatToIntBits(x);
        i = 0x5f3759df - (i >> 1);
        x = Float.intBitsToFloat(i);
        x *= (1.5f - xhalf * x * x);
        return x;
    }

    private static float sampleSdf(float[] erx, float[] ery, float[] erz, float[] ecx, float[] ecy, float[] ecz,
        float x, float y, float z, float smoothingRange, float invSmoothingRange) {
        float sdfDst = 0.0f;

        boolean first = true;

        for (int i = 0; i < erx.length; i++) {
            float rsx = x - ecx[i];
            float rsy = y - ecy[i];
            float rsz = z - ecz[i];

            // Ellipsoid SDF - https://www.iquilezles.org/www/articles/ellipsoids/ellipsoids.htm
            float prx = rsx * erx[i];
            float pry = rsy * ery[i];
            float prz = rsz * erz[i];
            // float k1 = MathHelper.sqrt_double(prx * prx + pry * pry + prz * prz);
            // float ellipsoidDst = (k1 - 1.0f) * Math.min(Math.min(erx[i], ery[i]), erz[i]);
            float k1 = invSqrt(prx * prx + pry * pry + prz * prz);
            float ellipsoidDst = MathHelper.sqrt_double(rsx * rsx + rsy * rsy + rsz * rsz) * (1.0f - 1.0f * k1);
            /*
             * float prx2 = rsx * erx[i] * erx[i];
             * float pry2 = rsy * ery[i] * ery[i];
             * float prz2 = rsz * erz[i] * erz[i];
             * float k2 = invSqrt(prx2 * prx2 + pry2 * pry2 + prz2 * prz2);
             * float ellipsoidDst = k1 * (k1 - 1.0f) * k2;
             */

            if (!first) {
                // Smooth min - https://www.iquilezles.org/www/articles/smin/smin.htm
                float h = (float) MathHelper
                    .clamp_double(0.5f + 0.5f * (ellipsoidDst - sdfDst) * invSmoothingRange, 0.0f, 1.0f);
                sdfDst = ellipsoidDst + (sdfDst - ellipsoidDst) * h - smoothingRange * h * (1.0f - h);
            } else {
                sdfDst = ellipsoidDst;
                first = false;
            }
        }

        return sdfDst;
    }

    public static Pair<Vec3d, Vec3d> findClosestPoint(List<AxisAlignedBB> boxes, float smoothingRange, float boxScale,
        float dx, int iters, float threshold, Vec3d p) {
        // 1.7.10 snow layers can contribute zero-height boxes even to a nonempty collision query.
        // They have no surface volume, and their reciprocal radii would poison the entire SDF.
        int count = 0;
        for (AxisAlignedBB box : boxes) if (hasVolume(box)) count++;
        if (count == 0) {
            return null;
        }

        float px = 0.0f;
        float py = 0.0f;
        float pz = 0.0f;

        float invSmoothingRange = 1.0f / smoothingRange;

        float[] erx = new float[count];
        float[] ery = new float[count];
        float[] erz = new float[count];

        float[] ecx = new float[count];
        float[] ecy = new float[count];
        float[] ecz = new float[count];

        int i = 0;
        for (AxisAlignedBB box : boxes) {
            if (!hasVolume(box)) continue;
            erx[i] = 1.0f / ((float) (box.maxX - box.minX) / 2 * boxScale);
            ery[i] = 1.0f / ((float) (box.maxY - box.minY) / 2 * boxScale);
            erz[i] = 1.0f / ((float) (box.maxZ - box.minZ) / 2 * boxScale);

            ecx[i] = (float) ((box.minX + box.maxX) / 2 - p.x);
            ecy[i] = (float) ((box.minY + box.maxY) / 2 - p.y);
            ecz[i] = (float) ((box.minZ + box.maxZ) / 2 - p.z);

            i++;
        }

        for (int j = 0; j < iters; j++) {
            float dst = sampleSdf(erx, ery, erz, ecx, ecy, ecz, px, py, pz, smoothingRange, invSmoothingRange);

            float fx1 = sampleSdf(erx, ery, erz, ecx, ecy, ecz, px + dx, py, pz, smoothingRange, invSmoothingRange);
            float fy1 = sampleSdf(erx, ery, erz, ecx, ecy, ecz, px, py + dx, pz, smoothingRange, invSmoothingRange);
            float fz1 = sampleSdf(erx, ery, erz, ecx, ecy, ecz, px, py, pz + dx, smoothingRange, invSmoothingRange);

            float gx = dst - fx1;
            float gy = dst - fy1;
            float gz = dst - fz1;
            float m = invSqrt(gx * gx + gy * gy + gz * gz);
            gx *= m;
            gy *= m;
            gz *= m;

            if (Float.isNaN(gx) || Float
                .isNaN(gy) || Float.isNaN(gz) || Double.isNaN(px) || Double.isNaN(py) || Double.isNaN(pz)) {
                return null;
            }

            float step = Math.max(dst, threshold / 2.0f);

            px += gx * step;
            py += gy * step;
            pz += gz * step;

            if (dst < threshold) {
                return Pair.of(new Vec3d(p.x + px, p.y + py, p.z + pz), new Vec3d(-gx, -gy, -gz).normalize());
            }
        }

        return null;
    }

    private static boolean hasVolume(AxisAlignedBB box) {
        return box.maxX > box.minX && box.maxY > box.minY && box.maxZ > box.minZ;
    }
}
