package net.kunmc.lab.tallerandtaller.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.kunmc.lab.tallerandtaller.Config;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

public class StartCommand {
    public static void register(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(Commands.literal("start").executes(ctx -> {
            if (Config.enabled) {
                ctx.getSource().sendErrorMessage(new StringTextComponent("巨大化はすでに有効化されています."));
                return 1;
            }
            Config.enabled = true;
            ctx.getSource().sendFeedback(new StringTextComponent("巨大化を有効化しました."), true);
            return 1;
        }));
    }
}
