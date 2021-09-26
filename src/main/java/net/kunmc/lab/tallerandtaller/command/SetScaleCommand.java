package net.kunmc.lab.tallerandtaller.command;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.kunmc.lab.tallerandtaller.TallerAndTaller;
import net.kunmc.lab.tallerandtaller.packet.ScaleChangePacket;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.Entity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.PacketDistributor;

public class SetScaleCommand {
    public static void register(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(Commands.literal("setScale")
                .then(Commands.argument("scale", FloatArgumentType.floatArg(0.0F))
                        .then(Commands.argument("player", EntityArgument.players())
                                .executes(ctx -> {
                                    float newScale = FloatArgumentType.getFloat(ctx, "scale");
                                    int n = 0;
                                    for (Entity player : EntityArgument.getEntities(ctx, "player")) {
                                        TallerAndTaller.uuidScaleMap.put(player.getUniqueID(), newScale);
                                        TallerAndTaller.channel.send(PacketDistributor.ALL.noArg(), new ScaleChangePacket(player.getUniqueID(), newScale));
                                        TallerAndTaller.changeScale(player, newScale);
                                        n++;
                                    }
                                    ctx.getSource().sendFeedback(new StringTextComponent(n + "人のスケールを" + newScale + "に設定しました."), true);
                                    return 1;
                                }))));
    }
}
