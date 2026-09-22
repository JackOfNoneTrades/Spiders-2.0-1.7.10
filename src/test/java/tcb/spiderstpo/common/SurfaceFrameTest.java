package tcb.spiderstpo.common;

import static org.junit.Assert.*;

import org.junit.Test;

public class SurfaceFrameTest {

    @Test
    public void crossingWallCeilingYawSeamKeepsWorldHeading() {
        Vec3d heading = new Vec3d(0, 0, 1);
        for (int degrees = 100; degrees < 140; degrees++) {
            Vec3d before = normal(degrees), after = normal(degrees + 1);
            for (double t = 0; t <= 1; t += 0.1) {
                SurfaceFrame frame = SurfaceFrame.interpolate(before, after, heading, heading, t);
                assertEquals(1, frame.forward.dotProduct(heading), 1e-6);
                assertEquals(0, frame.forward.dotProduct(frame.up), 1e-6);
                assertEquals(1, frame.right.lengthVector(), 1e-6);
            }
        }
    }

    @Test
    public void genuineHalfTurnInterpolatesWithoutCollapsing() {
        Vec3d up = new Vec3d(0, 1, 0), before = new Vec3d(0, 0, 1), after = new Vec3d(0, 0, -1);
        SurfaceFrame middle = SurfaceFrame.interpolate(up, up, before, after, 0.5);
        assertEquals(1, middle.forward.lengthVector(), 1e-6);
        assertEquals(0, middle.forward.dotProduct(before), 1e-6);
        assertEquals(1, SurfaceFrame.interpolate(up, up, before, after, 1).forward.dotProduct(after), 1e-6);
    }

    @Test
    public void parallelOrOpposingInputsRemainFinite() {
        SurfaceFrame frame = SurfaceFrame
            .interpolate(new Vec3d(0, -1, 0), new Vec3d(0, 1, 0), new Vec3d(0, 1, 0), new Vec3d(0, 1, 0), 0.5);
        assertEquals(1, frame.forward.lengthVector(), 1e-6);
        assertEquals(1, frame.up.lengthVector(), 1e-6);
        assertEquals(0, frame.forward.dotProduct(frame.up), 1e-6);
    }

    @Test
    public void ceilingRightingHasBoundedTurnsAndFiniteIntermediateFrames() {
        Vec3d up = new Vec3d(0, -1, 0), target = new Vec3d(0, 1, 0), forward = new Vec3d(0, 0, 1);
        for (int tick = 0; tick < 12; tick++) {
            Vec3d next = SurfaceFrame.turnNormal(up, target, forward, Math.PI / 9);
            assertTrue(up.dotProduct(next) >= Math.cos(Math.PI / 9) - 1e-6);
            for (double t = 0; t <= 1; t += 0.1) {
                SurfaceFrame frame = SurfaceFrame.interpolate(up, next, forward, forward, t);
                assertEquals(1, frame.up.lengthVector(), 1e-6);
                assertEquals(1, frame.forward.lengthVector(), 1e-6);
                assertEquals(0, frame.up.dotProduct(frame.forward), 1e-6);
            }
            up = next;
        }
        assertEquals(1, up.y, 1e-6);
    }

    @Test
    public void riderHeadingAndViewFollowFloorWallAndCeiling() {
        Vec3d up = new Vec3d(0, 1, 0), heading = new Vec3d(1, 0, 0);
        for (int degrees = 1; degrees <= 180; degrees++) {
            Vec3d next = normal(-degrees);
            heading = SurfaceFrame.transport(heading, up, next);
            SurfaceFrame frame = SurfaceFrame.interpolate(next, next, heading, heading, 1);
            assertEquals(0, heading.dotProduct(next), 1e-6);
            assertEquals(1, heading.lengthVector(), 1e-6);
            assertEquals(1, frame.look(0, 0).forward.dotProduct(heading), 1e-6);
            assertEquals(-1, frame.look(90, 0).forward.dotProduct(frame.right), 1e-6);
            assertEquals(-1, frame.look(0, 90).forward.dotProduct(next), 1e-6);
            up = next;
        }
        assertEquals(-1, heading.x, 1e-6);
        assertEquals(-1, up.y, 1e-6);
    }

    private static Vec3d normal(double degrees) {
        return new Vec3d(Math.sin(Math.toRadians(degrees)), Math.cos(Math.toRadians(degrees)), 0);
    }
}
