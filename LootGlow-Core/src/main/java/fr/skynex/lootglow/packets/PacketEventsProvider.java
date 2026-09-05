package fr.skynex.lootglow.packets;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketHandler;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import fr.skynex.lootglow.LootGlow;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PacketEventsProvider extends PacketListenerAbstract implements PacketProvider {
    private LootGlow plugin;

    @Override
    public void register(LootGlow plugin) {
        this.plugin = plugin;
        PacketEvents.getAPI().getEventManager().registerListener(this);
    }

    @Override
    public void unregister() {
        PacketEvents.getAPI().getEventManager().unregisterListener(this);
    }

    @Override
    public String getName() {
        return "PacketEvents";
    }

    @Override
    public PacketListenerPriority getPriority() {
        return PacketListenerPriority.HIGHEST;
    }

    @Override
    @PacketHandler
    public void onPacketSend(PacketSendEvent event) {
        Player player = (Player) event.getPlayer();
        if (player == null) return;

        if (event.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
            WrapperPlayServerSpawnEntity wrapper = new WrapperPlayServerSpawnEntity(event);
            int entityId = wrapper.getEntityId();
            var cfgMgr = plugin.getConfigManager();
            boolean rpgEnabled = cfgMgr != null && cfgMgr.isRpgDropsEnabled();
            if (rpgEnabled && plugin.getStateRepository().getHiddenVanillaItems().contains(entityId)) {
                if (!plugin.getStateRepository().getHiddenVisuals().contains(player.getUniqueId())) {
                    event.setCancelled(true);
                }
            }
        } else if (event.getPacketType() == PacketType.Play.Server.ENTITY_METADATA) {
            WrapperPlayServerEntityMetadata wrapper = new WrapperPlayServerEntityMetadata(event);
            int entityId = wrapper.getEntityId();

            if (!plugin.getStateRepository().getEntityIdMap().containsKey(entityId))
                return;

            var cfgMgr = plugin.getConfigManager();
            boolean isHiddenVanilla = cfgMgr != null && cfgMgr.isRpgDropsEnabled() && plugin.getStateRepository().getHiddenVanillaItems().contains(entityId);
            boolean isHiddenToggle = plugin.getStateRepository().getHiddenVisuals().contains(player.getUniqueId());

            boolean needsInvisible = isHiddenVanilla && !isHiddenToggle;
            boolean needsNoGlow = isHiddenToggle;

            if (!needsInvisible && !needsNoGlow)
                return;

            List<EntityData> data = new ArrayList<>(wrapper.getEntityMetadata());
            boolean modified = false;
            boolean foundIndex0 = false;

            for (EntityData val : data) {
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
                data.add(new EntityData(0, EntityDataTypes.BYTE, flags));
                modified = true;
            }

            if (modified) {
                wrapper.setEntityMetadata(data);
            }
        }
    }
}
