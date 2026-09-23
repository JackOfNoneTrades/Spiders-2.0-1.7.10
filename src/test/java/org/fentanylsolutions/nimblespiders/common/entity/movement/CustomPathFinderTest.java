package org.fentanylsolutions.nimblespiders.common.entity.movement;

import static org.junit.Assert.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

public class CustomPathFinderTest {

    @Test
    public void routesFromFloorUpWallAndAcrossCeiling() {
        CustomPathFinder.Surface surface = (x, y, z, nx, ny,
            nz) -> nx >= 0 && nx <= 6 && ny >= 0 && ny <= 6 && nz == 0 && (ny == 0 || nx == 6 || ny == 6);
        List<CustomPathFinder.Node> path = new CustomPathFinder().find(surface, 0, 0, 0, 0, 6, 0, 16, 2048);
        assertFalse(path.isEmpty());
        CustomPathFinder.Node end = path.get(path.size() - 1);
        assertEquals(0, end.x);
        assertEquals(6, end.y);
        assertTrue(
            path.stream()
                .anyMatch(node -> node.x == 6 && node.y == 3));
    }

    @Test
    public void unreachableTargetsGiveReachablePartialRoute() {
        List<CustomPathFinder.Node> path = new CustomPathFinder()
            .find((x, y, z, nx, ny, nz) -> nx >= 0 && nx <= 3 && ny == 0 && nz == 0, 0, 0, 0, 10, 0, 0, 16, 128);
        assertEquals(3, path.get(path.size() - 1).x);
    }

    @Test
    public void searchBudgetBoundsImpossibleSearch() {
        AtomicInteger probes = new AtomicInteger();
        new CustomPathFinder().find((x, y, z, nx, ny, nz) -> {
            probes.incrementAndGet();
            return true;
        }, -20, 64, -20, 100, 64, 100, 16, 128);
        assertTrue(probes.get() <= 26 * 128);
    }

    @Test
    public void negativeCoordinatesDoNotAlias() {
        List<CustomPathFinder.Node> path = new CustomPathFinder()
            .find((x, y, z, nx, ny, nz) -> ny == 64 && nz == -1, -4, 64, -1, 4, 64, -1, 16, 128);
        assertEquals(4, path.get(path.size() - 1).x);
        assertEquals(9, path.size());
    }

    @Test
    public void keepsOppositeAttachmentsDistinctWhenRevisitingACell() {
        CustomPathFinder.Surface surface = new CustomPathFinder.Surface() {

            @Override
            public int getSupportFaces(int x, int y, int z) {
                if (z != 0) return 0;
                if (x == 0 && y == 0) return 1 << 3;
                if (x == 1 && y == 0) return 1 << 2 | 1 << 3;
                if (x == 2 && y == 0) return 1;
                if (x == 0 && y == 1) return 1 << 1;
                if (x == 1 && y == 1) return 1 << 2;
                return 0;
            }

            @Override
            public boolean canMove(int x, int y, int z, int nx, int ny, int nz) {
                return false;
            }

            @Override
            public boolean canMove(int x, int y, int z, int face, int nx, int ny, int nz, int nextFace) {
                if (x == 0 && y == 0 && face == 3)
                    return nx == 1 && ny == 0 && nextFace == 3 || nx == 0 && ny == 1 && nextFace == 1;
                if (x == 0 && y == 1) return nx == 1 && ny == 1 && nextFace == 2;
                if (x == 1 && y == 1) return nx == 1 && ny == 0 && nextFace == 2;
                return x == 1 && y == 0 && face == 2 && nx == 2 && ny == 0 && nextFace == 0;
            }
        };
        List<CustomPathFinder.Node> path = new CustomPathFinder().find(surface, 0, 0, 0, 2, 0, 0, 8, 128, 3);
        assertEquals(5, path.size());
        assertEquals(1, path.get(1).y);
        assertEquals(2, path.get(3).face);
        assertEquals(2, path.get(4).x);
    }

}
