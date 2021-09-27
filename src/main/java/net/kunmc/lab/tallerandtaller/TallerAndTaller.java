package net.kunmc.lab.tallerandtaller;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.kunmc.lab.tallerandtaller.command.ConfigCommand;
import net.kunmc.lab.tallerandtaller.command.SetScaleCommand;
import net.kunmc.lab.tallerandtaller.command.StartCommand;
import net.kunmc.lab.tallerandtaller.command.StopCommand;
import net.kunmc.lab.tallerandtaller.event.ScaleChangeEvent;
import net.kunmc.lab.tallerandtaller.packet.ScaleChangePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.util.*;

@Mod(TallerAndTaller.ModId)
public class TallerAndTaller {
    public static final String ModId = "tallerandtaller";
    public static final Map<UUID, Float> uuidScaleMap = new HashMap<>();
    private static final Logger LOGGER = LogManager.getLogger();
    public static final SimpleChannel channel = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(ModId, ModId + "_channel"))
            .clientAcceptedVersions(a -> true)
            .serverAcceptedVersions(a -> true)
            .networkProtocolVersion(() -> "1")
            .simpleChannel();

    public TallerAndTaller() {
        MinecraftForge.EVENT_BUS.register(this);
        channel.registerMessage(0, ScaleChangePacket.class, ScaleChangePacket::encodeMessage, ScaleChangePacket::decodeMessage, ScaleChangePacket::receiveMessage);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onScaleChange(ScaleChangeEvent e) {
        PlayerEntity clientPlayer = Minecraft.getInstance().player;
        if (clientPlayer == null) {
            return;
        }

        PlayerEntity player = clientPlayer.getEntityWorld().getPlayerByUuid(e.playerUUID());
        if (player == null) {
            return;
        }
        player.recalculateSize();

        if (player.getUniqueID().equals((clientPlayer.getUniqueID()))) {
            changeScale(player, e.newScale());
        }
    }

    @SubscribeEvent
    public void onEntitySize(EntityEvent.Size e) {
        Entity entity = e.getEntity();
        if (entity instanceof PlayerEntity) {
            float scale = uuidScaleMap.getOrDefault(e.getEntity().getUniqueID(), Config.defaultScale);
            e.setNewSize(e.getNewSize().scale(scale));
            e.setNewEyeHeight(entity.getSize(e.getPose()).height * scale * 0.85F);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onPlayerRenderPre(RenderPlayerEvent.Pre e) {
        float scale = uuidScaleMap.getOrDefault(e.getEntity().getUniqueID(), Config.defaultScale);
        e.getMatrixStack().scale(scale, scale, scale);
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            int passedSecs = 0;

            @Override
            public void run() {
                if (!Config.enabled) {
                    passedSecs = 0;
                    return;
                }

                List<ServerPlayerEntity> playerList = new ArrayList<>(event.getServer().getPlayerList().getPlayers());
                for (ServerPlayerEntity player : playerList) {
                    player.sendStatusMessage(new StringTextComponent(String.valueOf(Config.timeToBeTaller - passedSecs)), true);
                }

                passedSecs++;
                if (passedSecs <= Config.timeToBeTaller) {
                    return;
                }
                passedSecs = 0;

                for (ServerPlayerEntity player : playerList) {
                    UUID uuid = player.getUniqueID();
                    float scale = uuidScaleMap.getOrDefault(uuid, Config.defaultScale) + Config.increasingScale;
                    if (scale > Config.maxScale) {
                        scale = Config.maxScale;
                    }
                    uuidScaleMap.put(uuid, scale);
                    changeScale(player, scale);

                    channel.send(PacketDistributor.ALL.noArg(), new ScaleChangePacket(uuid, scale));
                }
            }
        };

        timer.scheduleAtFixedRate(task, 0, 1000);
    }

    public static void changeScale(Entity entity, float scale) {
        try {
            //set eyeHeight
            Field field = Util.getFieldFromClass(entity.getClass(), "field_213326_aJ");
            field.setAccessible(true);
            field.set(entity, entity.getSize(entity.getPose()).height * scale * 0.85F);
        } catch (Exception e) {
            e.printStackTrace();
        }
        entity.recalculateSize();

        if (entity instanceof PlayerEntity) {
            PlayerEntity player = ((PlayerEntity) entity);

            ModifiableAttributeInstance reachDistance = player.getAttribute(ForgeMod.REACH_DISTANCE.get());
            if (reachDistance != null) {
                reachDistance.setBaseValue(5.0 + (player.getEyeHeight() - 1.53));
            }

            ModifiableAttributeInstance movementSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementSpeed != null) {
                movementSpeed.setBaseValue(0.1 * Math.pow(uuidScaleMap.get(player.getUniqueID()), 0.6));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJump(LivingEvent.LivingJumpEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof PlayerEntity) {
            PlayerEntity player = ((PlayerEntity) entity);
            float scale = uuidScaleMap.getOrDefault(player.getUniqueID(), Config.defaultScale);
            float jumpHeight = Math.max(1.0F, (float) Math.pow(scale, 0.6F));
            player.setMotion(player.getMotion().mul(1, jumpHeight, 1));
        }
    }

    @SubscribeEvent
    public void onPlayerFall(LivingFallEvent e) {
        if (e.getEntity() instanceof PlayerEntity) {
            PlayerEntity player = ((PlayerEntity) e.getEntity());
            float scale = uuidScaleMap.getOrDefault(player.getUniqueID(), Config.defaultScale);
            e.setDistance(e.getDistance() / scale);

            if (scale < 0.45F) {
                e.setDistance(0.0F);
            }
        }
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSource> builder = Commands.literal(ModId).requires(s -> s.hasPermissionLevel(2));

        StartCommand.register(builder);
        StopCommand.register(builder);
        ConfigCommand.register(builder);
        SetScaleCommand.register(builder);

        e.getDispatcher().register(builder);
    }

    @OnlyIn(Dist.DEDICATED_SERVER)
    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent e) {
        uuidScaleMap.putIfAbsent(e.getPlayer().getUniqueID(), Config.defaultScale);
        uuidScaleMap.forEach((k, v) -> {
            channel.send(PacketDistributor.PLAYER.with(() -> {
                return (ServerPlayerEntity) e.getPlayer();
            }), new ScaleChangePacket(k, v));
        });
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent e) {
        uuidScaleMap.clear();
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent e) {
        PlayerEntity player = Minecraft.getInstance().player;
        float scale = uuidScaleMap.getOrDefault(player.getUniqueID(), Config.defaultScale);

        if (Minecraft.getInstance().gameSettings.getPointOfView().equals(PointOfView.THIRD_PERSON_FRONT)) {
            if (player.getHeight() * scale > 1.8F) {
                e.getMatrixStack().translate(0, 0, -scale * 2);
            }
        }

        if (Minecraft.getInstance().gameSettings.getPointOfView().equals(PointOfView.THIRD_PERSON_BACK)) {
            if (player.getHeight() * scale > 1.8F) {
                e.getMatrixStack().translate(0, 0, scale * 2);
            }
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent e) {
        Entity entity = e.getEntity();
        if (entity instanceof PlayerEntity) {
            uuidScaleMap.put(entity.getUniqueID(), Config.defaultScale);
            changeScale(entity, Config.defaultScale);
            channel.send(PacketDistributor.ALL.noArg(), new ScaleChangePacket(entity.getUniqueID(), Config.defaultScale));
        }
    }
}
