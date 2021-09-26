package net.kunmc.lab.tallerandtaller.event;

import net.minecraftforge.eventbus.api.Event;

import java.util.UUID;

public class ScaleChangeEvent extends Event {
    private final UUID playerUUID;
    private final float newScale;

    public ScaleChangeEvent(UUID playerUUID, float newScale) {
        this.playerUUID = playerUUID;
        this.newScale = newScale;
    }

    public UUID playerUUID() {
        return playerUUID;
    }

    public float newScale() {
        return newScale;
    }
}
