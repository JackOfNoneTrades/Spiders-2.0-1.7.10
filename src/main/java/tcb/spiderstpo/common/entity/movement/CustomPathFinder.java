package tcb.spiderstpo.common.entity.movement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/** Bounded A* over supported surface cells, including walls, ceilings and outside corners. */
public final class CustomPathFinder {

    public int examinedNodes;
    public String searchResult;

    public interface Surface {

        boolean canMove(int x, int y, int z, int nx, int ny, int nz);

        default int getDropY(int x, int y, int z) {
            return y;
        }
    }

    public static final class Node implements Comparable<Node> {

        public final int x, y, z;
        public final boolean drop;
        private final double cost, estimate;
        private final Node previous;

        Node(int x, int y, int z, double cost, double estimate, Node previous) {
            this(x, y, z, cost, estimate, previous, false);
        }

        Node(int x, int y, int z, double cost, double estimate, Node previous, boolean drop) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.cost = cost;
            this.estimate = estimate;
            this.previous = previous;
            this.drop = drop;
        }

        @Override
        public int compareTo(Node other) {
            return Double.compare(cost + estimate, other.cost + other.estimate);
        }
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x3ffffff) << 38 | ((long) z & 0x3ffffff) << 12 | (y & 0xfff);
    }

    private static double distance(int x, int y, int z, int tx, int ty, int tz) {
        double dx = x - tx, dy = y - ty, dz = z - tz;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public List<Node> find(Surface surface, int sx, int sy, int sz, int tx, int ty, int tz, int range, int budget) {
        examinedNodes = 0;
        PriorityQueue<Node> open = new PriorityQueue<>();
        Map<Long, Node> visited = new HashMap<>();
        Node start = new Node(sx, sy, sz, 0, distance(sx, sy, sz, tx, ty, tz), null), closest = start;
        open.add(start);
        visited.put(key(sx, sy, sz), start);
        while (!open.isEmpty() && budget-- > 0) {
            examinedNodes++;
            Node node = open.poll();
            if (visited.get(key(node.x, node.y, node.z)) != node) continue;
            if (node.estimate < closest.estimate) closest = node;
            if (node.estimate == 0) break;
            int dropY = surface.getDropY(node.x, node.y, node.z);
            if (dropY < node.y && Math.abs(dropY - sy) <= range) {
                long dropKey = key(node.x, dropY, node.z);
                double cost = node.cost + node.y - dropY;
                Node old = visited.get(dropKey);
                if (old == null || cost < old.cost) {
                    Node next = new Node(
                        node.x,
                        dropY,
                        node.z,
                        cost,
                        distance(node.x, dropY, node.z, tx, ty, tz),
                        node,
                        true);
                    visited.put(dropKey, next);
                    open.add(next);
                }
            }
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                int x = node.x + dx, y = node.y + dy, z = node.z + dz;
                if (Math.abs(x - sx) > range || Math.abs(y - sy) > range || Math.abs(z - sz) > range) continue;
                double cost = node.cost + Math.sqrt(dx * dx + dy * dy + dz * dz);
                long key = key(x, y, z);
                Node old = visited.get(key);
                if (old != null && old.cost <= cost) continue;
                if (!surface.canMove(node.x, node.y, node.z, x, y, z)) continue;
                Node next = new Node(x, y, z, cost, distance(x, y, z, tx, ty, tz), node);
                visited.put(key, next);
                open.add(next);
            }
        }
        searchResult = closest.estimate == 0 ? "reached" : budget < 0 ? "budget_exhausted" : "no_more_nodes";
        if (closest == start) return Collections.emptyList();
        List<Node> result = new ArrayList<>();
        for (Node node = closest; node != null; node = node.previous) result.add(node);
        Collections.reverse(result);
        return result;
    }
}
