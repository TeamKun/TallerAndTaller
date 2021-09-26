package net.kunmc.lab.tallerandtaller.command;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.kunmc.lab.tallerandtaller.Config;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;

import java.lang.reflect.Field;

public class ConfigCommand {
    public static void register(LiteralArgumentBuilder<CommandSource> builder) {
        LiteralArgumentBuilder<CommandSource> configBuilder = Commands.literal("config");

        configBuilder.then(Commands.literal("show").executes(ctx -> {
            for (Field field : Config.class.getDeclaredFields()) {
                try {
                    ctx.getSource().sendFeedback(new StringTextComponent(field.getName() + ": " + field.get(null)), true);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return 1;
        }));

        configBuilder.then(Commands.literal("set")
                .then(createConfigArgumentBuilder("defaultScale", FloatArgumentType.floatArg(0.0F)))
                .then(createConfigArgumentBuilder("maxScale", FloatArgumentType.floatArg(0.0F)))
                .then(createConfigArgumentBuilder("increasingScale", FloatArgumentType.floatArg(0.0F)))
                .then(createConfigArgumentBuilder("timeToBeTaller", IntegerArgumentType.integer(0))));

        builder.then(configBuilder);
    }

    private static <T> ArgumentBuilder<CommandSource, LiteralArgumentBuilder<CommandSource>> createConfigArgumentBuilder(String name, ArgumentType<T> type) {
        return Commands.literal(name).then(Commands.argument("value", type).executes(ctx -> {
            Object value = ctx.getArgument("value", Object.class);
            try {
                Field field = Config.class.getDeclaredField(name);
                field.setAccessible(true);
                field.set(null, value);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ctx.getSource().sendFeedback(new StringTextComponent(name + "の値を" + value + "に設定しました."), true);

            return 1;
        }));
    }
}
