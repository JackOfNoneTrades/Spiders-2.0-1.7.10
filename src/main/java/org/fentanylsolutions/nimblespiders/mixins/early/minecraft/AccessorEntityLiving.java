package org.fentanylsolutions.nimblespiders.mixins.early.minecraft;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.pathfinding.PathNavigate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Installs the climbing navigator and controllers on spiders. */
@Mixin(EntityLiving.class)
public interface AccessorEntityLiving {

    @Accessor
    void setNavigator(PathNavigate navigator);

    @Accessor
    void setLookHelper(EntityLookHelper lookHelper);

    @Accessor
    void setMoveHelper(EntityMoveHelper moveHelper);
}
