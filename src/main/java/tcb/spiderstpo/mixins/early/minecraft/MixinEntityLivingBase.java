package tcb.spiderstpo.mixins.early.minecraft;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySpider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import tcb.spiderstpo.common.Config;
import tcb.spiderstpo.common.entity.mob.SpiderClimber;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

    @ModifyConstant(method = "fall", constant = @Constant(floatValue = 3.0F))
    private float spiderstpo$safeFallDistance(float vanillaDistance) {
        if ((Object) this instanceof EntitySpider && SpiderClimber.get((EntitySpider) (Object) this) != null) {
            return Config.getSafeFallDistance();
        }
        return vanillaDistance;
    }
}
