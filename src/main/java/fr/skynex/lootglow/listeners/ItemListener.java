package fr.skynex.lootglow.listeners;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.util.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import java.util.Collection;
import java.util.UUID;

public class ItemListener implements Listener {

    private final LootGlow plugin;

    public ItemListener(LootGlow plugin) {
        this.plugin = plugin;
    }

    /**
     * Pré-enregistrement à priority LOWEST (s'exécute EN PREMIER, avant HIGHEST).
     * PlayerDropItemEvent se déclenche AVANT que l'item entre dans le monde
     * (avant ItemSpawnEvent), donc setVisibleByDefault(false) est posé avant que
     * Paper's entity tracker ne décide d'envoyer SPAWN_ENTITY aux joueurs proches.
     * Corrige la race condition où le joueur est à 1 bloc de l'item qu'il vient de lâcher.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPlayerDropPreRegister(PlayerDropItemEvent event) {
        plugin.preHideItem(event.getItemDrop());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (plugin.isOnlyPlayerDrops()) return;
        plugin.applyGlow(event.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDrop(PlayerDropItemEvent event) {
        if (plugin.isOnlyPlayerDrops()) {
            plugin.applyGlow(event.getItemDrop());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMerge(ItemMergeEvent event) {
        // Supprimer l'hologramme de l'item qui disparaît
        plugin.removeGlow(event.getEntity());
        
        // Si la cible a déjà son glow actif, on force une mise à jour instantanée du hologramme (montrant le nouveau montant)
        if (plugin.getActiveItems().containsKey(event.getTarget().getUniqueId())) {
            if (plugin.isHoloEnabled()) {
                FoliaScheduler.runSync(plugin, () -> {
                    if (event.getTarget().isValid()) {
                        plugin.refreshHologram(event.getTarget());
                    }
                });
            }
        } else {
            FoliaScheduler.runSync(plugin, () -> {
                if (event.getTarget().isValid()) {
                    plugin.applyGlow(event.getTarget(), false);
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            // Force RMB check
            if (plugin.isRmbPickupEnabled() && plugin.isRmbPickupForce()) {
                event.setCancelled(true);
                return;
            }

            // Prevent picking up grouped items if container require-click is enabled
            if (plugin.isContainerEnabled() && plugin.isContainerRequireClick()) {
                if (plugin.getGroupMembers().containsKey(event.getItem().getUniqueId()) ||
                    plugin.getGroupedItems().contains(event.getItem().getUniqueId())) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (plugin.isHardLockEnabled() && event.getItem().getThrower() != null) {
                if (!event.getItem().getThrower().equals(player.getUniqueId()) && !player.hasPermission(plugin.getBypassPermission())) {
                    long spawnTime = plugin.getItemSpawnTimes().getOrDefault(event.getItem().getUniqueId(), 0L);
                    int duration = plugin.getProtectionDuration();
                    long elapsed = (System.currentTimeMillis() - spawnTime) / 1000L;

                    if (elapsed < duration) {
                        event.setCancelled(true);

                        // Send message with placeholders
                        org.bukkit.entity.Player owner = Bukkit.getPlayer(event.getItem().getThrower());
                        String ownerName = owner != null ? owner.getName() : "Inconnu";
                        long remaining = duration - elapsed;

                        java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                        placeholders.put("owner", ownerName);
                        placeholders.put("time", String.valueOf(remaining));

                        plugin.sendMessage(player, "cannot-pickup", placeholders);
                        return;
                    }
                }
            }

            // Player pickup: play aspiration animation then remove glow
            plugin.playAspirationAnimation(event.getItem(), player);
            plugin.removeGlow(event.getItem());
        } else {
            // Non-player entity pickup (e.g. Piglins, Allays, etc.) — just clean up the glow
            plugin.removeGlow(event.getItem());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDespawn(ItemDespawnEvent event) {
        plugin.removeGlow(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryPickup(org.bukkit.event.inventory.InventoryPickupItemEvent event) {
        plugin.removeGlow(event.getItem());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPortal(EntityPortalEvent event) {
        if (event.getEntity() instanceof Item item) {
            plugin.removeGlow(item);
            FoliaScheduler.runLater(plugin, () -> {
                if (item.isValid()) {
                    plugin.applyGlow(item);
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof Item item) {
            if (event.getTo() != null && !event.getFrom().getWorld().equals(event.getTo().getWorld())) {
                plugin.removeGlow(item);
                FoliaScheduler.runLater(plugin, () -> {
                    if (item.isValid()) {
                        plugin.applyGlow(item);
                    }
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.loadPlayerData(event.getPlayer());

        FoliaScheduler.runLater(plugin, () -> {
            Player p = event.getPlayer();
            if (!p.isOnline() || !plugin.isPluginEnabled()) return;
            plugin.refreshGlowForPlayer(p, !plugin.getHiddenVisuals().contains(p.getUniqueId()));
        }, 40L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(org.bukkit.event.player.PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        plugin.clearVisualsForPlayer(player);
        FoliaScheduler.runLater(plugin, () -> {
            if (player.isOnline() && plugin.isPluginEnabled()) {
                plugin.refreshGlowForPlayer(player, !plugin.getHiddenVisuals().contains(player.getUniqueId()));
            }
        }, 20L); // 1 second delay to let chunks load
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleportPlayer(org.bukkit.event.player.PlayerTeleportEvent event) {
        if (event.getTo() != null && !event.getFrom().getWorld().equals(event.getTo().getWorld())) {
            plugin.clearVisualsForPlayer(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        // Use RMB pickup range if enabled, otherwise a default interaction range of 4.0
        double range = plugin.isRmbPickupEnabled() ? plugin.getRmbPickupRange() : 4.0;
        
        // Find nearest item in sight
        Item targetItem = null;
        double bestDist = range * range;
        
        org.bukkit.Location eyeLoc = player.getEyeLocation();
        org.bukkit.util.Vector eyeVec = eyeLoc.toVector();
        org.bukkit.util.Vector eyeDir = eyeLoc.getDirection();
        
        Collection<Entity> nearby = player.getWorld().getNearbyEntities(eyeLoc, range, range, range);
        org.bukkit.util.Vector toItem = new org.bukkit.util.Vector();

        for (Entity ent : nearby) {
            Item item = null;
            if (ent instanceof Item i) {
                item = i;
            } else if (ent instanceof org.bukkit.entity.ItemDisplay display) {
                item = plugin.getItemForDisplay(display);
            } else if (ent instanceof org.bukkit.entity.TextDisplay label) {
                item = plugin.getItemForLabel(label);
            }
            if (item == null || !item.isValid() || item.isDead()) continue;
            
            // Basic line of sight / angle check
            org.bukkit.Location itemLoc = item.getLocation();
            toItem.setX(itemLoc.getX() - eyeVec.getX());
            toItem.setY(itemLoc.getY() - eyeVec.getY());
            toItem.setZ(itemLoc.getZ() - eyeVec.getZ());
            
            double len = toItem.length();
            if (len < 0.0001) continue;
            toItem.multiply(1.0 / len); // normalize in-place
            
            double dot = eyeDir.dot(toItem);
            
            if (dot > 0.85) { // Looking generally towards the item or visual display
                double dist = eyeLoc.distanceSquared(itemLoc);
                if (dist < bestDist) {
                    bestDist = dist;
                    targetItem = item;
                }
            }
        }

        if (targetItem != null) {
            boolean isGroup = plugin.getGroupMembers().containsKey(targetItem.getUniqueId());
            // Check if it's a group leader for the container GUI
            if (plugin.isContainerEnabled() && isGroup) {
                plugin.openLootContainer(player, targetItem.getUniqueId());
                event.setCancelled(true);
                return;
            }

            // If it's a group item and enable-for-groups is false, bypass RMB pickup
            if (isGroup && !plugin.isRmbPickupEnableForGroups()) {
                return;
            }

            // Check protection before manual pickup
            if (plugin.isHardLockEnabled() && targetItem.getThrower() != null) {
                if (!targetItem.getThrower().equals(player.getUniqueId()) && !player.hasPermission(plugin.getBypassPermission())) {
                    long spawnTime = plugin.getItemSpawnTimes().getOrDefault(targetItem.getUniqueId(), 0L);
                    long elapsed = (System.currentTimeMillis() - spawnTime) / 1000L;
                    if (elapsed < plugin.getProtectionDuration()) {
                        return; 
                    }
                }
            }

            // Normal RMB pickup for single items or groups (if enabled)
            if (!plugin.isRmbPickupEnabled()) return;

            // Attempt pickup
            if (targetItem.isValid() && !targetItem.isDead()) {
                java.util.HashMap<Integer, org.bukkit.inventory.ItemStack> leftovers = player.getInventory().addItem(targetItem.getItemStack());
                if (leftovers.isEmpty()) {
                    plugin.playAspirationAnimation(targetItem, player);
                    plugin.removeGlow(targetItem);
                    targetItem.remove();
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.5f);
                } else {
                    // Adjust stack if inventory was partially full
                    targetItem.getItemStack().setAmount(leftovers.get(0).getAmount());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        // Si chunk pas neuf (rechargement après restart) → cleanup des refs stales
        // car les ItemDisplay/TextDisplay (setPersistent(false)) ont disparu mais leurs UUIDs
        // pourraient encore traîner dans des maps si un /reload a eu lieu
        if (!event.isNewChunk()) {
            for (Entity entity : event.getChunk().getEntities()) {
                // Gestion des symboles de farming persistants
                if (entity instanceof BlockDisplay bd && bd.getPersistentDataContainer().has(plugin.getFarmingKey(), org.bukkit.persistence.PersistentDataType.BYTE)) {
                    org.bukkit.block.Block block = bd.getLocation().getBlock();
                    // On vérifie si la plante est toujours là et mûre
                    if (plugin.isFarmingEnabled() && plugin.getFarmingCrops().contains(block.getType())) {
                        if (block.getBlockData() instanceof org.bukkit.block.data.Ageable age && age.getAge() == age.getMaximumAge()) {
                            // On ré-ajoute à la map active du plugin
                            plugin.relinkCropSymbol(block, bd);
                            continue;
                        }
                    }
                    // Si la plante n'est plus mûre ou a disparu, on vire le symbole
                    bd.remove();
                    continue;
                }
                if (entity instanceof Item item) {
                    UUID uuid = item.getUniqueId();
                    // On force respawn complet en virant les entrées stales
                    ItemDisplay oldDisplay = plugin.getActiveItemVisuals().remove(uuid);
                    if (oldDisplay != null && oldDisplay.isValid()) {
                        oldDisplay.remove();
                    }
                    TextDisplay oldLabel = plugin.getActiveLabels().remove(uuid);
                    if (oldLabel != null && oldLabel.isValid()) {
                        oldLabel.remove();
                    }
                    BlockDisplay oldBeam = plugin.getActiveBeams().remove(uuid);
                    if (oldBeam != null && oldBeam.isValid()) {
                        oldBeam.getPassengers().forEach(passenger -> passenger.remove());
                        oldBeam.remove();
                    }
                    org.bukkit.entity.Display oldShadow = plugin.getActiveShadows().get(uuid);
                    if (oldShadow != null) {
                        plugin.getActiveShadows().remove(uuid);
                        if (oldShadow.isValid()) oldShadow.remove();
                    }
                }
            }
        }

        // Délai d'1 tick : laisser Paper finir le chargement complet du chunk + entities
        // avant d'appliquer glow et de spawn les visuals (sinon race condition avec
        // les SPAWN_ENTITY packets qui partent vers les joueurs déjà connectés)
        FoliaScheduler.runSync(plugin, () -> {
            if (!plugin.isPluginEnabled()) return;

            for (Entity entity : event.getChunk().getEntities()) {
                if (!(entity instanceof Item item) || !item.isValid()) continue;

                plugin.applyGlow(item, false);

                // Force re-broadcast aux joueurs déjà connectés à proximité :
                // si l'item est un RPG drop hidden, on s'assure que les clients qui
                // l'ont déjà reçu en SPAWN_ENTITY le cachent bien via showEntity/hideEntity
                if (plugin.isProtocolLibEnabled()
                        && plugin.getHiddenVanillaItems().contains(item.getEntityId())) {
                    ItemDisplay visual = plugin.getActiveItemVisuals().get(item.getUniqueId());
                    
                    double ix = item.getX();
                    double iy = item.getY();
                    double iz = item.getZ();

                    for (Player p : item.getWorld().getPlayers()) {
                        double dx = p.getX() - ix;
                        double dy = p.getY() - iy;
                        double dz = p.getZ() - iz;
                        
                        if ((dx * dx + dy * dy + dz * dz) > 4096) continue; // 64 blocs
                        
                        if (!plugin.getHiddenVisuals().contains(p.getUniqueId())) {
                            p.hideEntity(plugin, item);
                            if (visual != null && visual.isValid()) {
                                p.showEntity(plugin, visual);
                            }
                        }
                    }
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity instanceof Item item) {
                plugin.removeGlow(item);
            }
        }
    }
}
