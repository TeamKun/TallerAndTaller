package net.kunmc.lab.tallerandtaller.mixin;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.IServerPlayNetHandler;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.ForgeMod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(ServerPlayNetHandler.class)
public abstract class ServerPlayNetHandlerMixin implements IServerPlayNetHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    @Shadow
    public ServerPlayerEntity player;

    @Shadow
    public abstract void disconnect(ITextComponent textComponent);

    /**
     * @author
     */
    @Overwrite
    public void processUseEntity(CUseEntityPacket packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.player.getServerWorld());
        ServerWorld serverworld = this.player.getServerWorld();
        Entity entity = packet.getEntityFromWorld(serverworld);
        if (entity == null) {
            return;
        }

        this.player.markPlayerActive();
        this.player.setSneaking(packet.isSneaking());

        double reachDistance = this.player.getAttributeValue(ForgeMod.REACH_DISTANCE.get());
        if (this.player.getDistanceSq(entity) < reachDistance * reachDistance) {
            Hand hand = packet.getHand();
            ItemStack itemstack = hand != null ? this.player.getHeldItem(hand).copy() : ItemStack.EMPTY;
            Optional<ActionResultType> optional = Optional.empty();
            if (packet.getAction() == CUseEntityPacket.Action.INTERACT) {
                optional = Optional.of(this.player.interactOn(entity, hand));
            } else if (packet.getAction() == CUseEntityPacket.Action.INTERACT_AT) {
                if (net.minecraftforge.common.ForgeHooks.onInteractEntityAt(player, entity, packet.getHitVec(), hand) != null)
                    return;
                optional = Optional.of(entity.applyPlayerInteraction(this.player, packet.getHitVec(), hand));
            } else if (packet.getAction() == CUseEntityPacket.Action.ATTACK) {
                if (entity instanceof ItemEntity || entity instanceof ExperienceOrbEntity || entity instanceof AbstractArrowEntity || entity == this.player) {
                    disconnect(new TranslationTextComponent("multiplayer.disconnect.invalid_entity_attacked"));
                    LOGGER.warn("Player {} tried to attack an invalid entity", (Object) this.player.getName().getString());
                    return;
                }

                this.player.attackTargetEntityWithCurrentItem(entity);
            }

            if (optional.isPresent() && optional.get().isSuccessOrConsume()) {
                CriteriaTriggers.PLAYER_ENTITY_INTERACTION.test(this.player, itemstack, entity);
                if (optional.get().isSuccess()) {
                    this.player.swing(hand, true);
                }
            }
        }
    }
}
