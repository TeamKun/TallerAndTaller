package net.kunmc.lab.tallerandtaller.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.resources.IResourceManagerReloadListener;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements IResourceManagerReloadListener, AutoCloseable {
    @Final
    @Shadow
    private Minecraft mc;

    /**
     * @author
     */
    @Overwrite
    public void getMouseOver(float partialTicks) {
        Entity player = mc.getRenderViewEntity();
        if (player == null) {
            return;
        }

        if (mc.world == null) {
            return;
        }

        if (mc.playerController == null) {
            return;
        }

        mc.getProfiler().startSection("pick");
        mc.pointedEntity = null;

        double blockReachDistance = mc.playerController.getBlockReachDistance();
        mc.objectMouseOver = player.pick(blockReachDistance, partialTicks, false);
        Vector3d eyePosition = player.getEyePosition(partialTicks);

        double d1 = blockReachDistance * blockReachDistance;
        if (mc.objectMouseOver != null) {
            d1 = mc.objectMouseOver.getHitVec().squareDistanceTo(eyePosition);
        }

        Vector3d look = player.getLook(1.0F);
        Vector3d endVec = eyePosition.add(look.x * blockReachDistance, look.y * blockReachDistance, look.z * blockReachDistance);
        AxisAlignedBB axisalignedbb = player.getBoundingBox().expand(look.scale(blockReachDistance)).grow(1.0D, 1.0D, 1.0D);
        EntityRayTraceResult rayTraceResult = ProjectileHelper.rayTraceEntities(player, eyePosition, endVec, axisalignedbb, (x) -> {
            return !x.isSpectator() && x.canBeCollidedWith();
        }, d1);
        if (rayTraceResult != null) {
            Entity entity1 = rayTraceResult.getEntity();
            Vector3d hitVec = rayTraceResult.getHitVec();
            double d2 = eyePosition.squareDistanceTo(hitVec);
            if (d2 < d1 || mc.objectMouseOver == null) {
                mc.objectMouseOver = rayTraceResult;
                if (entity1 instanceof LivingEntity || entity1 instanceof ItemFrameEntity) {
                    mc.pointedEntity = entity1;
                }
            }
        }

        mc.getProfiler().endSection();
    }
}
