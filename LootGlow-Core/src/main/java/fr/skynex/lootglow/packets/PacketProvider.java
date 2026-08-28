package fr.skynex.lootglow.packets;

import fr.skynex.lootglow.LootGlow;

public interface PacketProvider {
    void register(LootGlow plugin);
    void unregister();
    String getName();
}
