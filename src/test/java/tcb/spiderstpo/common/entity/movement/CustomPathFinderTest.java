package tcb.spiderstpo.common.entity.movement;

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
}
