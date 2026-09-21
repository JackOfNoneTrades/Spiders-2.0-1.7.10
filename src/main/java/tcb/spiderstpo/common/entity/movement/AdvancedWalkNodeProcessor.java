package tcb.spiderstpo.common.entity.movement;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import tcb.spiderstpo.common.Config;

/** Uses actual collision shapes; never loads chunks to search for a route. */
public final class AdvancedWalkNodeProcessor implements CustomPathFinder.Surface {

    private final World world;
    private final EntityCreature entity;
    private final Map<String, Integer> supported = new HashMap<>();

    public AdvancedWalkNodeProcessor(EntityCreature entity) {
        this.entity = entity;
        this.world = entity.worldObj;
    }

    private AxisAlignedBB box(double x, double y, double z) {
        double half = entity.width / 2.0;
        double center = Math.ceil(entity.width) / 2.0;
        return AxisAlignedBB.getBoundingBox(
            x + center - half,
            y,
            z + center - half,
            x + center + half,
            y + entity.height,
            z + center + half);
    }

    private boolean clear(AxisAlignedBB box) {
        int x1 = MathHelper.floor_double(box.minX), y1 = MathHelper.floor_double(box.minY),
            z1 = MathHelper.floor_double(box.minZ);
        int x2 = MathHelper.floor_double(box.maxX), y2 = MathHelper.floor_double(box.maxY),
            z2 = MathHelper.floor_double(box.maxZ);
        if (y1 < 0 || y2 >= world.getActualHeight() || !world.checkChunksExist(x1 - 1, y1, z1 - 1, x2 + 1, y2, z2 + 1))
            return false;
        for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) {
            Block block = world.getBlock(x, y, z);
            if (block.getMaterial() == Material.lava || block == Blocks.fire || block == Blocks.cactus) return false;
        }
        return world.func_147461_a(box)
            .isEmpty();
    }

    private int support(int x, int y, int z) {
        String key = x + ":" + y + ":" + z;
        Integer cached = supported.get(key);
        if (cached != null) return cached;
        AxisAlignedBB body = box(x, y, z);
        int result = !clear(body) ? 0
            : world.getBlock(x, y, z)
                .getMaterial() == Material.water ? 63 : supportFaces(body, x, y, z);
        supported.put(key, result);
        return result;
    }

    public boolean isSupportedCell(int x, int y, int z) {
        return support(x, y, z) != 0;
    }

    public int getSupportFaces(int x, int y, int z) {
        return support(x, y, z);
    }

    public boolean isClearCell(int x, int y, int z) {
        return clear(box(x, y, z));
    }

    private boolean collides(double x1, double y1, double z1, double x2, double y2, double z2) {
        for (Object collision : world.func_147461_a(AxisAlignedBB.getBoundingBox(x1, y1, z1, x2, y2, z2))) {
            AxisAlignedBB box = (AxisAlignedBB) collision;
            if (box.maxX > box.minX && box.maxY > box.minY && box.maxZ > box.minZ) return true;
        }
        return false;
    }

    private AxisAlignedBB faceBox(AxisAlignedBB body, double x, double y, double z, int face) {
        double width = Math.ceil(entity.width), height = Math.ceil(entity.height);
        // Bit order follows ForgeDirection: down, up, north, south, west, east.
        // Tangential bounds use the real body, excluding corner-only block contacts.
        switch (face) {
            case 0:
                return AxisAlignedBB.getBoundingBox(body.minX, y - 0.55, body.minZ, body.maxX, y + 0.001, body.maxZ);
            case 1:
                return AxisAlignedBB
                    .getBoundingBox(body.minX, y + height - 0.001, body.minZ, body.maxX, y + height + 0.05, body.maxZ);
            case 2:
                return AxisAlignedBB.getBoundingBox(body.minX, body.minY, z - 0.05, body.maxX, body.maxY, z + 0.001);
            case 3:
                return AxisAlignedBB
                    .getBoundingBox(body.minX, body.minY, z + width - 0.001, body.maxX, body.maxY, z + width + 0.05);
            case 4:
                return AxisAlignedBB.getBoundingBox(x - 0.05, body.minY, body.minZ, x + 0.001, body.maxY, body.maxZ);
            default:
                return AxisAlignedBB
                    .getBoundingBox(x + width - 0.001, body.minY, body.minZ, x + width + 0.05, body.maxY, body.maxZ);
        }
    }

    private int supportFaces(AxisAlignedBB body, int x, int y, int z) {
        int faces = 0;
        for (int face = 0; face < 6; face++) {
            AxisAlignedBB probe = faceBox(body, x, y, z, face);
            if (collides(probe.minX, probe.minY, probe.minZ, probe.maxX, probe.maxY, probe.maxZ)) faces |= 1 << face;
        }
        return faces;
    }

    private boolean continuousSupport(int x, int y, int z, int nx, int ny, int nz, int fromFace, int toFace) {
        // Supported endpoints alone do not connect a wall to a nearby floating floor. Find
        // the exact intervals of the sweep with face contact, so even a sub-block gap is rejected.
        List<double[]> intervals = new ArrayList<>();
        AxisAlignedBB body = box(x, y, z);
        double dx = nx - x, dy = ny - y, dz = nz - z;
        for (int face = 0; face < 6; face++) {
            if (face != fromFace && face != toFace) continue;
            AxisAlignedBB probe = faceBox(body, x, y, z, face);
            for (Object value : world.func_147461_a(probe.addCoord(dx, dy, dz))) {
                AxisAlignedBB block = (AxisAlignedBB) value;
                if (block.maxX <= block.minX || block.maxY <= block.minY || block.maxZ <= block.minZ) continue;
                double[] interval = { 0, 1 };
                if (overlapInterval(probe.minX, probe.maxX, block.minX, block.maxX, dx, interval)
                    && overlapInterval(probe.minY, probe.maxY, block.minY, block.maxY, dy, interval)
                    && overlapInterval(probe.minZ, probe.maxZ, block.minZ, block.maxZ, dz, interval))
                    intervals.add(interval);
            }
        }
        intervals.sort(Comparator.comparingDouble(interval -> interval[0]));
        double end = 0;
        for (double[] interval : intervals) {
            if (interval[0] > end + 1.0E-7) return false;
            end = Math.max(end, interval[1]);
            if (end >= 1) return true;
        }
        return false;
    }

    private static boolean overlapInterval(double min, double max, double blockMin, double blockMax, double motion,
        double[] interval) {
        if (motion == 0) return max > blockMin && min < blockMax;
        double a = (blockMin - max) / motion, b = (blockMax - min) / motion;
        interval[0] = Math.max(interval[0], Math.min(a, b));
        interval[1] = Math.min(interval[1], Math.max(a, b));
        return interval[0] < interval[1];
    }

    private static boolean connectedFaces(int from, int to) {
        for (int a = 0; a < 6; a++) for (int b = 0; b < 6; b++) {
            if ((from & 1 << a) != 0 && (to & 1 << b) != 0 && b != (a ^ 1)) return true;
        }
        return false;
    }

    @Override
    public boolean canMove(int x, int y, int z, int nx, int ny, int nz) {
        int from = support(x, y, z), to = support(nx, ny, nz);
        if (to == 0 || from == 0 && ny > y) return false;
        // Walking cannot transfer directly between facing walls across an air gap, or from a
        // floor to a disconnected ceiling. Reach a connecting face (or plan a drop) instead.
        if (from != 0 && !connectedFaces(from, to)) return false;
        if (sweepClear(x, y, z, nx, ny, nz)) return true;
        // Convex corners require an L-shaped sweep around the edge, not a diagonal through it.
        // Both endpoints must have face support; the intermediate point must be outside the block.
        if (from == 0) return false;
        return cornerClear(x, y, z, nx, y, z, nx, ny, nz) || cornerClear(x, y, z, x, ny, z, nx, ny, nz)
            || cornerClear(x, y, z, x, y, nz, nx, ny, nz);
    }

    @Override
    public boolean canMove(int x, int y, int z, int face, int nx, int ny, int nz, int nextFace) {
        if (nextFace == (face ^ 1) || !canMove(x, y, z, nx, ny, nz)) return false;
        if (support(x, y, z) == 0 || world.getBlock(x, y, z)
            .getMaterial() == Material.water
            || world.getBlock(nx, ny, nz)
                .getMaterial() == Material.water)
            return true;
        return continuousSupport(x, y, z, nx, ny, nz, face, nextFace);
    }

    private boolean cornerClear(int x, int y, int z, int mx, int my, int mz, int nx, int ny, int nz) {
        return clear(box(mx, my, mz)) && sweepClear(x, y, z, mx, my, mz) && sweepClear(mx, my, mz, nx, ny, nz);
    }

    private boolean sweepClear(int x, int y, int z, int nx, int ny, int nz) {
        for (int i = 1; i <= 4; i++) {
            double t = i / 4.0;
            if (!clear(box(x + (nx - x) * t, y + (ny - y) * t, z + (nz - z) * t))) return false;
        }
        return true;
    }

    public boolean canDropTo(double x, double y, double z, double landingY) {
        if (y <= landingY || y - landingY > Config.maxDropHeight) return false;
        double half = entity.width / 2.0;
        return clear(AxisAlignedBB.getBoundingBox(x - half, landingY, z - half, x + half, y + entity.height, z + half));
    }

    @Override
    public int getDropY(int x, int y, int z) {
        int faces = support(x, y, z);
        if (faces == 0 || (faces & 1) != 0) return y;
        double center = Math.ceil(entity.width) / 2.0;
        double departureY = y + ((faces & 2) != 0 ? Math.ceil(entity.height) - entity.height : 0);
        // A fall is a directed edge to the first supported floor below this wall/ceiling.
        // It can be followed by another climb, even when the target is above the landing.
        for (int landingY = y - 1; landingY >= Math.max(0, y - Config.maxDropHeight); landingY--) {
            if (!isClearCell(x, landingY, z)) return y;
            if ((support(x, landingY, z) & 1) != 0) {
                return canDropTo(x + center, departureY, z + center, landingY)
                    && canLandAt(x + center, landingY, z + center) ? landingY : y;
            }
        }
        return y;
    }

    public boolean canLandAt(double x, double y, double z) {
        double half = entity.width / 2.0;
        return collides(x - half, y - 0.15, z - half, x + half, y + 0.001, z + half);
    }

    public boolean canReachAfterLanding(double x, double y, double z, Entity target) {
        double halfCell = Math.ceil(entity.width) / 2;
        int sx = MathHelper.floor_double(x - halfCell + 0.5), sy = MathHelper.floor_double(y),
            sz = MathHelper.floor_double(z - halfCell + 0.5);
        int tx = MathHelper.floor_double(target.posX - halfCell + 0.5),
            ty = MathHelper.floor_double(target.boundingBox.minY),
            tz = MathHelper.floor_double(target.posZ - halfCell + 0.5);
        if (sx == tx && sy == ty && sz == tz) return true;
        List<CustomPathFinder.Node> route = new CustomPathFinder().find(this, sx, sy, sz, tx, ty, tz, 8, 256);
        if (route.isEmpty()) return false;
        CustomPathFinder.Node end = route.get(route.size() - 1);
        return end.x == tx && end.y == ty && end.z == tz;
    }

    public boolean hasCeiling(int x, int y, int z) {
        AxisAlignedBB body = box(x, y, z);
        return !world
            .func_147461_a(
                AxisAlignedBB.getBoundingBox(
                    body.minX,
                    y + Math.ceil(entity.height) - 0.05,
                    body.minZ,
                    body.maxX,
                    y + Math.ceil(entity.height) + 0.05,
                    body.maxZ))
            .isEmpty();
    }
}
