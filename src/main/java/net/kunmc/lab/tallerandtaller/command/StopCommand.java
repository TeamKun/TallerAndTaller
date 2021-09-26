package net.kunmc.lab.tallerandtaller.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.kunmc.lab.tallerandtaller.Config;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

public class StopCommand {
    public static void register(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(Commands.literal("stop").executes(ctx -> {
            if (!Config.enabled) {
                ctx.getSource().sendErrorMessage(new StringTextComponent("巨大化は有効化されていません."));
                return 1;
            }

            Config.enabled = false;
            ctx.getSource().sendFeedback(new StringTextComponent("巨大化を無効化しました."), true);
            return 1;
        }));
    }
}
