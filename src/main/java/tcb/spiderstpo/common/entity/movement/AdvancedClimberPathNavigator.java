package tcb.spiderstpo.common.entity.movement;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import tcb.spiderstpo.common.Config;
import tcb.spiderstpo.common.SpiderDebug;
import tcb.spiderstpo.common.entity.mob.SpiderClimber;

/** 1.7.10 navigation with progress measured in all three dimensions. */
public class AdvancedClimberPathNavigator extends PathNavigate {

    private final EntityCreature climber;
    private PathEntity path;
    private double speed;
    private int stuckTicks;
    private double lastX, lastY, lastZ;
    private boolean traceSearch;

    public AdvancedClimberPathNavigator(EntityCreature entity, World world) {
        super(entity, world);
        climber = entity;
    }

    @Override
    public PathEntity getPathToXYZ(double x, double y, double z) {
        if (climber.ridingEntity != null) return null;
        AdvancedWalkNodeProcessor processor = new AdvancedWalkNodeProcessor(climber);
        int startX = MathHelper.floor_double(climber.posX - Math.ceil(climber.width) / 2 + 0.5);
        int startY = MathHelper.floor_double(climber.boundingBox.minY + 0.001);
        int startZ = MathHelper.floor_double(climber.posZ - Math.ceil(climber.width) / 2 + 0.5);
        // Rounding a wall climber upwards can put the start in unsupported air. Only round up
        // for collision shapes such as bottom slabs when the lower cell itself is unusable.
        if (!processor.isClearCell(startX, startY, startZ) && processor.isSupportedCell(startX, startY + 1, startZ))
            startY++;
        CustomPathFinder finder = new CustomPathFinder();
        List<CustomPathFinder.Node> nodes = finder.find(
            processor,
            startX,
            startY,
            startZ,
            MathHelper.floor_double(x - Math.ceil(climber.width) / 2 + 0.5),
            MathHelper.floor_double(y),
            MathHelper.floor_double(z - Math.ceil(climber.width) / 2 + 0.5),
            Math.min(64, (int) getPathSearchRange()),
            Config.pathSearchBudget,
            SpiderClimber.get(climber)
                .getWalkingSide()
                .getLeft()
                .ordinal());
        if (traceSearch) SpiderClimber.get(climber).debug.event(
            "SEARCH requested=" + SpiderDebug.vector(x, y, z)
                + " startCell="
                + startX
                + ","
                + startY
                + ","
                + startZ
                + " outcome="
                + finder.searchResult
                + " examined="
                + finder.examinedNodes
                + " budget="
                + Config.pathSearchBudget
                + " range="
                + getPathSearchRange()
                + " nodes="
                + nodes.size());
        if (nodes.isEmpty()) return null;
        PathPoint[] points = new PathPoint[nodes.size()];
        for (int i = 0; i < points.length; i++) {
            CustomPathFinder.Node node = nodes.get(i);
            points[i] = node.drop ? new DropPoint(node.x, node.y, node.z)
                : new SurfacePoint(node.x, node.y, node.z, node.face);
        }
        return new PathEntity(points);
    }

    @Override
    public PathEntity getPathToEntityLiving(Entity entity) {
        traceSearch = SpiderClimber.get(climber).debug.beginSearch();
        try {
            PathEntity selected = findEntityPath(entity);
            if (traceSearch) SpiderClimber.get(climber).debug.event("SELECTED_PATH " + SpiderDebug.path(selected));
            return selected;
        } finally {
            traceSearch = false;
        }
    }

    private PathEntity findEntityPath(Entity entity) {
        PathEntity direct = getPathToXYZ(entity.posX, entity.boundingBox.minY, entity.posZ);
        if (traceSearch) SpiderClimber.get(climber).debug.event("DIRECT_PATH " + SpiderDebug.path(direct));
        int x = MathHelper.floor_double(entity.posX - Math.ceil(climber.width) / 2 + 0.5);
        int z = MathHelper.floor_double(entity.posZ - Math.ceil(climber.width) / 2 + 0.5);
        int y = MathHelper.floor_double(entity.boundingBox.minY);
        if (endsAt(direct, x, y, z)
            || climber.getDistanceSq(entity.posX, entity.boundingBox.minY, entity.posZ) <= 4 + entity.width
                && climber.getEntitySenses()
                    .canSee(entity))
            return direct;

        // A roof need not be directly above the target: land on a reachable part of their
        // platform, then walk the remaining distance. Search nearby landing cells first.
        AdvancedWalkNodeProcessor processor = new AdvancedWalkNodeProcessor(climber);
        double halfCell = Math.ceil(climber.width) / 2;
        for (int radius = 0; radius <= 3; radius++)
            for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
                if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                double landingX = x + dx + halfCell, landingZ = z + dz + halfCell;
                double distanceX = landingX - entity.posX, distanceZ = landingZ - entity.posZ;
                if (distanceX * distanceX + distanceZ * distanceZ >= 16
                    || !processor.canLandAt(landingX, entity.boundingBox.minY, landingZ)) continue;
                boolean landingChecked = false, landingReachable = false;
                for (int ceilingY = MathHelper.ceiling_double_int(entity.boundingBox.maxY); ceilingY
                    <= y + Config.maxDropHeight; ceilingY++) {
                    if (!processor.hasCeiling(x + dx, ceilingY, z + dz)) continue;
                    boolean clearDrop = processor.canDropTo(landingX, ceilingY, landingZ, entity.boundingBox.minY);
                    if (traceSearch) SpiderClimber.get(climber).debug.event(
                        "OVERHEAD_CANDIDATE cell=" + (x + dx)
                            + ","
                            + ceilingY
                            + ","
                            + (z + dz)
                            + " clearDrop="
                            + clearDrop);
                    if (!clearDrop) continue;
                    if (!landingChecked) {
                        landingReachable = processor
                            .canReachAfterLanding(landingX, entity.boundingBox.minY, landingZ, entity);
                        landingChecked = true;
                    }
                    if (!landingReachable) continue;
                    PathEntity overhead = getPathToXYZ(landingX, ceilingY, landingZ);
                    if (endsAt(overhead, x + dx, ceilingY, z + dz)) {
                        if (traceSearch) SpiderClimber.get(climber).debug.event(
                            "ROUTE overhead landing="
                                + SpiderDebug.vector(landingX, entity.boundingBox.minY, landingZ));
                        return overhead;
                    }
                }
            }
        if (traceSearch) SpiderClimber.get(climber).debug.event("ROUTE partial_direct_no_reachable_overhead");
        return direct;
    }

    private static boolean endsAt(PathEntity path, int x, int y, int z) {
        if (path == null) return false;
        PathPoint end = path.getFinalPathPoint();
        return end.xCoord == x && end.yCoord == y && end.zCoord == z;
    }

    @Override
    public boolean setPath(PathEntity next, double speed) {
        if (next == null) {
            clearPathEntity();
            return false;
        }
        if (path == null || !sameSurfacePath(next, path)) {
            path = next;
            // The first node describes our current cell, not a waypoint to walk back to.
            if (path.getCurrentPathIndex() == 0 && path.getCurrentPathLength() > 1) path.incrementPathIndex();
            stuckTicks = 0;
        }
        this.speed = speed;
        return !noPath();
    }

    private static boolean sameSurfacePath(PathEntity a, PathEntity b) {
        if (!a.isSamePath(b)) return false;
        for (int i = 0; i < a.getCurrentPathLength(); i++) {
            PathPoint first = a.getPathPointFromIndex(i), second = b.getPathPointFromIndex(i);
            if ((first instanceof DropPoint) != (second instanceof DropPoint)) return false;
            if (first instanceof SurfacePoint && second instanceof SurfacePoint
                && ((SurfacePoint) first).face != ((SurfacePoint) second).face) return false;
        }
        return true;
    }

    @Override
    public void setSpeed(double speed) {
        this.speed = speed;
    }

    @Override
    public PathEntity getPath() {
        return path;
    }

    @Override
    public boolean noPath() {
        return path == null || path.isFinished();
    }

    @Override
    public void clearPathEntity() {
        path = null;
    }

    @Override
    public void onUpdateNavigation() {
        if (!SpiderClimber.get(climber)
            .isActive() || SpiderClimber.get(climber)
                .isPlayerControlled()) {
            clearPathEntity();
            return;
        }
        if (noPath()) return;
        if (++stuckTicks >= 80) {
            if (climber.getDistanceSq(lastX, lastY, lastZ) < 0.04) {
                SpiderClimber.get(climber).debug.event("NAV_STUCK clearing path after 80 ticks");
                clearPathEntity();
                return;
            }
            lastX = climber.posX;
            lastY = climber.posY;
            lastZ = climber.posZ;
            stuckTicks = 0;
        }
        PathPoint point = path.getPathPointFromIndex(path.getCurrentPathIndex());
        if (point instanceof DropPoint) {
            AdvancedWalkNodeProcessor processor = new AdvancedWalkNodeProcessor(climber);
            if (processor.canDropTo(climber.posX, climber.posY, climber.posZ, point.yCoord)
                && processor.canLandAt(climber.posX, point.yCoord, climber.posZ)) {
                SpiderClimber.get(climber).debug.event("PLANNED_DROP landing=" + point);
                SpiderClimber.get(climber)
                    .dropFromSurface();
                return;
            }
            // Align with the planned departure column before letting go.
            PathPoint departure = path.getPathPointFromIndex(path.getCurrentPathIndex() - 1);
            climber.getMoveHelper()
                .setMoveTo(
                    departure.xCoord + Math.ceil(climber.width) / 2,
                    waypointY(departure),
                    departure.zCoord + Math.ceil(climber.width) / 2,
                    speed);
            return;
        }
        ForgeDirection side = waypointSide(point);
        double x = point.xCoord + Math.ceil(climber.width) / 2
            + side.offsetX * (Math.ceil(climber.width) - climber.width) / 2;
        double y = waypointY(point);
        double z = point.zCoord + Math.ceil(climber.width) / 2
            + side.offsetZ * (Math.ceil(climber.width) - climber.width) / 2;
        double dx = Math.abs(x - climber.posX), dy = Math.abs(y - climber.posY), dz = Math.abs(z - climber.posZ);
        if (dx < (side.offsetX != 0 ? 0.6 : 0.35) && dy < (side.offsetY != 0 ? 0.6 : 0.35)
            && dz < (side.offsetZ != 0 ? 0.6 : 0.35)) {
            path.incrementPathIndex();
            if (noPath()) return;
            onNextWaypoint();
        } else climber.getMoveHelper()
            .setMoveTo(x, y, z, speed);
    }

    private void onNextWaypoint() {
        PathPoint point = path.getPathPointFromIndex(path.getCurrentPathIndex());
        if (point instanceof DropPoint) return;
        ForgeDirection side = waypointSide(point);
        double padding = (Math.ceil(climber.width) - climber.width) / 2;
        climber.getMoveHelper()
            .setMoveTo(
                point.xCoord + Math.ceil(climber.width) / 2 + side.offsetX * padding,
                waypointY(point),
                point.zCoord + Math.ceil(climber.width) / 2 + side.offsetZ * padding,
                speed);
    }

    public ForgeDirection getClimbEntrySide() {
        if (noPath()) return ForgeDirection.UNKNOWN;
        PathPoint point = path.getPathPointFromIndex(path.getCurrentPathIndex());
        if (point instanceof DropPoint || point.yCoord <= climber.posY + 0.25) return ForgeDirection.UNKNOWN;
        ForgeDirection side = waypointSide(point);
        return side.offsetY == 0 ? side : ForgeDirection.UNKNOWN;
    }

    private ForgeDirection waypointSide(PathPoint point) {
        if (point instanceof SurfacePoint) return ForgeDirection.getOrientation(((SurfacePoint) point).face);
        ForgeDirection current = SpiderClimber.get(climber)
            .getWalkingSide()
            .getLeft();
        int faces = new AdvancedWalkNodeProcessor(climber).getSupportFaces(point.xCoord, point.yCoord, point.zCoord);
        if ((faces & 1 << current.ordinal()) != 0) return current;
        // Align with the destination surface before turning upward. Wide spiders otherwise stop
        // at the grid-cell center, beyond the wall's reach, and try to climb unsupported air.
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if ((faces & 1 << side.ordinal()) != 0) return side;
        }
        return current;
    }

    private static class SurfacePoint extends PathPoint {

        private final int face;

        private SurfacePoint(int x, int y, int z, int face) {
            super(x, y, z);
            this.face = face;
        }

        @Override
        public String toString() {
            return super.toString() + "@" + ForgeDirection.getOrientation(face);
        }
    }

    private static final class DropPoint extends SurfacePoint {

        private DropPoint(int x, int y, int z) {
            super(x, y, z, 0);
        }
    }

    private double waypointY(PathPoint point) {
        double size = Math.ceil(climber.width), top = point.yCoord + Math.ceil(climber.height);
        AxisAlignedBB ceiling = AxisAlignedBB.getBoundingBox(
            point.xCoord + 0.05,
            top - 0.05,
            point.zCoord + 0.05,
            point.xCoord + size - 0.05,
            top + 0.05,
            point.zCoord + size - 0.05);
        return point.yCoord + (climber.worldObj.func_147461_a(ceiling)
            .isEmpty() ? 0 : Math.ceil(climber.height) - climber.height);
    }
}
