package org.fentanylsolutions.nimblespiders.common;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;

import net.minecraft.util.AxisAlignedBB;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class CollisionSmoothingTest {

    @Test
    public void findsFloorAndCeilingNormals() {
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(-1, -1, -1, 1, 0, 1);
        Pair<Vec3d, Vec3d> floor = CollisionSmoothingUtil
            .findClosestPoint(Collections.singletonList(box), 1.25F, 1, 0.005F, 20, 0.05F, new Vec3d(0, 0.4, 0));
        assertNotNull(floor);
        assertTrue(floor.getRight().y > 0.99);
        Pair<Vec3d, Vec3d> ceiling = CollisionSmoothingUtil
            .findClosestPoint(Collections.singletonList(box), 1.25F, 1, 0.005F, 20, 0.05F, new Vec3d(0, -1.4, 0));
        assertNotNull(ceiling);
        assertTrue(ceiling.getRight().y < -0.99);
    }

    @Test
    public void snowLayerDoesNotHideWallOrCeiling() {
        AxisAlignedBB snow = AxisAlignedBB.getBoundingBox(-1, 0, -1, 1, 0, 1);
        AxisAlignedBB wall = AxisAlignedBB.getBoundingBox(0, 0, -1, 1, 3, 1);
        Pair<Vec3d, Vec3d> attachment = CollisionSmoothingUtil
            .findClosestPoint(Arrays.asList(snow, wall), 1.25F, 1, 0.005F, 20, 0.05F, new Vec3d(-0.4, 1.5, 0));
        assertNotNull(attachment);
        assertTrue(attachment.getRight().x < -0.99);
        AxisAlignedBB roof = AxisAlignedBB.getBoundingBox(-1, 2, -1, 1, 3, 1);
        attachment = CollisionSmoothingUtil
            .findClosestPoint(Arrays.asList(roof, snow), 1.25F, 1, 0.005F, 20, 0.05F, new Vec3d(0, 1.6, 0));
        assertNotNull(attachment);
        assertTrue(attachment.getRight().y < -0.99);
        assertNull(
            CollisionSmoothingUtil
                .findClosestPoint(Collections.singletonList(snow), 1.25F, 1, 0.005F, 20, 0.05F, new Vec3d(0, 0.4, 0)));
    }

    @Test
    public void noSurfaceDoesNotProduceInvalidNormal() {
        assertNull(
            CollisionSmoothingUtil
                .findClosestPoint(Collections.emptyList(), 1.25F, 1, 0.005F, 20, 0.05F, new Vec3d(0, 0, 0)));
        assertEquals(
            0,
            new Vec3d(0, 0, 0).normalize()
                .lengthVector(),
            0);
    }
}
