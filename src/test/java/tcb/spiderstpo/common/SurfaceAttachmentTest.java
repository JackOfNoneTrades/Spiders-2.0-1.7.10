package tcb.spiderstpo.common;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;

import net.minecraft.util.AxisAlignedBB;

import org.junit.Test;

public class SurfaceAttachmentTest {

    @Test
    public void contactsRealFloorWallAndCeilingForLargeBodies() {
        AxisAlignedBB wall = AxisAlignedBB.getBoundingBox(8, 0, -4, 9, 8, 4);
        AxisAlignedBB floor = AxisAlignedBB.getBoundingBox(-4, -1, -4, 9, 0, 4);
        AxisAlignedBB ceiling = AxisAlignedBB.getBoundingBox(-4, 8, -4, 9, 9, 4);
        assertEquals(
            8,
            SurfaceAttachment
                .findContact(Arrays.asList(wall, floor, ceiling), new Vec3d(6.6, 4, 0), new Vec3d(-1, 0, 0), 8).x,
            1e-6);
        assertEquals(
            0,
            SurfaceAttachment
                .findContact(Arrays.asList(wall, floor, ceiling), new Vec3d(0, 0.7, 0), new Vec3d(0, 1, 0), 8).y,
            1e-6);
        assertEquals(
            8,
            SurfaceAttachment
                .findContact(Arrays.asList(wall, floor, ceiling), new Vec3d(0, 7.3, 0), new Vec3d(0, -1, 0), 8).y,
            1e-6);
    }

    @Test
    public void outsideCornerStaysOnActualGeometryAndIgnoresEmptySnow() {
        AxisAlignedBB block = AxisAlignedBB.getBoundingBox(0, 0, 0, 1, 1, 1);
        AxisAlignedBB snow = AxisAlignedBB.getBoundingBox(-2, 1.5, -2, 2, 1.5, 2);
        Vec3d contact = SurfaceAttachment
            .findContact(Arrays.asList(block, snow), new Vec3d(1.5, 1.5, 0.5), new Vec3d(1, 0, 0), 4);
        assertEquals(1, contact.x, 1e-6);
        assertEquals(1, contact.y, 1e-6);
        assertNull(
            SurfaceAttachment.findContact(Collections.singletonList(snow), new Vec3d(0, 2, 0), new Vec3d(0, 1, 0), 4));
    }
}
