package net.kunmc.lab.tallerandtaller.packet;

import net.kunmc.lab.tallerandtaller.TallerAndTaller;
import net.kunmc.lab.tallerandtaller.event.ScaleChangeEvent;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ScaleChangePacket {
    public UUID playerUUID;
    public float newScale;

    public ScaleChangePacket() {

    }

    public ScaleChangePacket(UUID playerUUID, float newScale) {
        this.playerUUID = playerUUID;
        this.newScale = newScale;
    }

    public static void encodeMessage(ScaleChangePacket packet, PacketBuffer buffer) {
        buffer.writeUniqueId(packet.playerUUID);
        buffer.writeFloat(packet.newScale);
    }

    public static ScaleChangePacket decodeMessage(PacketBuffer buffer) {
        ScaleChangePacket packet = new ScaleChangePacket();
        packet.playerUUID = buffer.readUniqueId();
        packet.newScale = buffer.readFloat();
        return packet;
    }

    public static void receiveMessage(ScaleChangePacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().setPacketHandled(true);
        TallerAndTaller.uuidScaleMap.put(packet.playerUUID, packet.newScale);

        MinecraftForge.EVENT_BUS.post(new ScaleChangeEvent(packet.playerUUID, packet.newScale));
    }
}
