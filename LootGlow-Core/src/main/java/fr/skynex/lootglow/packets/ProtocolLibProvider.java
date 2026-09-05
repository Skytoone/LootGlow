package fr.skynex.lootglow.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import fr.skynex.lootglow.LootGlow;
import org.bukkit.entity.Player;

import java.util.List;

public class ProtocolLibProvider implements PacketProvider {
    private LootGlow main;
    private PacketAdapter listener;

    @Override
    public void register(LootGlow plugin) {
        this.main = plugin;
        ProtocolManager manager = ProtocolLibrary.getProtocolManager();
        this.listener = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.SPAWN_ENTITY) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                int entityId = event.getPacket().getIntegers().read(0);

                var cfgMgr = main.getConfigManager();
                boolean rpgEnabled = cfgMgr != null && cfgMgr.isRpgDropsEnabled();

                if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
                    if (rpgEnabled && main.getStateRepository().getHiddenVanillaItems().contains(entityId)) {
                        if (!main.getStateRepository().getHiddenVisuals().contains(player.getUniqueId())) {
                            event.setCancelled(true);
                        }
                    }
                    return;
                }

                if (!main.getStateRepository().getEntityIdMap().containsKey(entityId))
                    return;

                try {
                    boolean isHiddenVanilla = rpgEnabled && main.getStateRepository().getHiddenVanillaItems().contains(entityId);
                    boolean isHiddenToggle = main.getStateRepository().getHiddenVisuals().contains(player.getUniqueId());

                    boolean needsInvisible = isHiddenVanilla && !isHiddenToggle;
                    boolean needsNoGlow = isHiddenToggle;

                    if (!needsInvisible && !needsNoGlow)
                        return;

                    PacketContainer packet = event.getPacket().deepClone();
                    List<WrappedDataValue> dataValues = packet.getDataValueCollectionModifier().read(0);
                    if (dataValues == null)
                        return;

                    boolean modified = false;
                    boolean foundIndex0 = false;

                    for (WrappedDataValue val : dataValues) {
                        if (val.getIndex() == 0) {
                            foundIndex0 = true;
                            byte flags = (byte) val.getValue();
                            if (needsInvisible)
                                flags |= 0x20;
                            if (needsNoGlow)
                                flags &= ~0x40;
                            val.setValue(flags);
                            modified = true;
                        }
                    }

                    if (!foundIndex0 && needsInvisible) {
                        byte flags = 0x20;
                        if (needsNoGlow)
                            flags &= ~0x40;
                        dataValues.add(new WrappedDataValue(0, WrappedDataWatcher.Registry.get(Byte.class), flags));
                        modified = true;
                    }

                    if (modified) {
                        packet.getDataValueCollectionModifier().write(0, dataValues);
                        event.setPacket(packet);
                    }
                } catch (Exception ignored) {
                }
            }
        };
        manager.addPacketListener(listener);
    }

    @Override
    public void unregister() {
        if (listener != null) {
            ProtocolLibrary.getProtocolManager().removePacketListener(listener);
        }
    }

    @Override
    public String getName() {
        return "ProtocolLib";
    }
}
