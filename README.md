# ✨ LootGlow

<div align="center">

[![Plugin Version](https://img.shields.io/badge/Version-1.6.3--beta1-FFD700?style=for-the-badge&logo=minecraft)](https://www.spigotmc.org/resources/134648)
[![Java Version](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![Platform](https://img.shields.io/badge/Platform-Paper%20%7C%20Spigot%20%7C%20Folia-005B9A?style=for-the-badge)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-GPLv3-4CAF50?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0.html)
[![bStats](https://img.shields.io/bstats/servers/30993?style=for-the-badge&label=Servers&color=7B68EE)](https://bstats.org/plugin/bukkit/LootGlow/30993)
[![Discord](https://img.shields.io/badge/Discord-Join-5865F2?style=for-the-badge&logo=discord)](https://discord.gg/3QzcDHC6)

**Transform your Minecraft server's item drops into a stunning, RPG-grade visual experience.**

*Glowing effects • Holographic labels • Beacon beams • Particles • Loot protection — all in one plugin.*

</div>

---

## 🌟 What is LootGlow?

**LootGlow** brings a professional, RPG-like feel to your Minecraft server by replacing the plain vanilla item-drop experience with a rich visual system. Every item on the ground can glow, display a holographic label with a live despawn countdown, emit particles, project a vertical beacon beam, cast a 3D shadow, and much more — all fully configurable per item category, material, or custom plugin item.

Designed from the ground up for **performance and compatibility**, LootGlow supports Folia's regionized threading, a built-in Level-of-Detail (LOD) system, and integrates seamlessly with the most popular Minecraft plugins.

---

## 🎮 Core Features

### ✨ Visual Effects
| Feature | Description |
|---|---|
| **Dynamic Glowing** | Per-category or per-item configurable glow colors using scoreboard team coloring |
| **Ultra-Smooth Holograms** | 100% jitter-free floating labels via modern `TextDisplay` entities |
| **3D Flat-Item Rendering** | RPG-style `ItemDisplay` rendering — items lie flat on the ground or stand upright |
| **Dynamic Shadows** | Semi-transparent 3D block shadow beneath each dropped item |
| **Beacon Beams** | Vertical light beams for legendary/epic loot, with pulsing animation |
| **Particles** | Per-category particle effects with configurable animation type (still, orbit, spiral…) |
| **Dynamic Lighting** | Real-time light blocks spawned near glowing items for ambient illumination |

### 🏷️ Hologram & Labeling
| Feature | Description |
|---|---|
| **Item Name Display** | Shows item name with full MiniMessage + legacy color code support |
| **Despawn Timer** | Live countdown on the hologram before item disappears |
| **Amount Badge** | Shows stack size for grouped item stacks |
| **Owner Label** | Displays owner name during loot protection window |
| **Economy Label** | Shows coin/money value on economic item drops |
| **PlaceholderAPI** | Full PAPI support in hologram templates |

### 🛡️ Loot & Interaction
| Feature | Description |
|---|---|
| **Loot Protection** | Owner-only lock window after mob or player drops (configurable duration, hard-lock mode) |
| **VIP Item Magnet** | Permission-based auto-pickup for high-rarity loot (toggleable per-player) |
| **Item Grouping** | Groups identical dropped items in mob grinders into a single visual pile |
| **Loot Container GUI** | Right-click a grouped pile to open an inventory GUI showing all contained items |
| **Right-Click Pickup** | Optional RMB-to-pickup interaction mode |
| **Aspiration** | Smooth flying animation when items get picked up |

### 🌾 Farming Highlights
- Detects fully-grown crops and displays a `!` symbol above them
- Custom configurable symbol material (block display)
- Per-region control via a custom **WorldGuard** flag: `lootglow-farming`
- Configurable crops list, glow color, scale, height offset and animation

### ⚡ Performance & Compatibility
| Feature | Description |
|---|---|
| **Folia Native** | Full support for Folia's regionized threading scheduler |
| **Level of Detail (LOD)** | Holograms, beams, and particles are hidden beyond configurable distances |
| **Async-safe tracking** | All item tracking uses concurrent-safe structures |
| **Auto Config Updater** | Automatically migrates config keys on plugin update |
| **Update Checker** | Notifies admins of new SpigotMC releases on startup |
| **bStats Metrics** | Anonymous usage metrics via bStats (plugin ID `30993`) |

---

## 🔌 Plugin Integrations

LootGlow auto-detects and integrates with the following plugins at runtime — no configuration required:

| Plugin | Integration |
|---|---|
| **ItemsAdder** | Reads custom item IDs (PDC `id`, `item_id`, `namespace_id`, `namespace:name`) |
| **Oraxen** | Reads custom item IDs (PDC `id`, `item_id`, `oraxen_id`) |
| **Nexo** | Reads custom item IDs (PDC `id`, `item_id`, `nexo_id`) |
| **MythicMobs** | Reads MythicMobs item type (PDC `item_type`, `type`, `id`) |
| **MMOItems** | Reads MMOItems type/ID via PDC + reflection fallback |
| **EcoItems** | Reads EcoItems ID via PDC for category matching |
| **MythicDrops** | Reads tier tag for category matching |
| **AdvancedItems** | Reads custom item ID |
| **ItemEdit** | Reads item ID tag |
| **ExecutableItems** | Reads custom item ID (PDC `executableitems:id`) |
| **ExecutableBlocks** | Reads block ID (PDC `executableblocks:id`) |
| **WorldGuard** | Custom `lootglow-farming` region flag + blocked regions |
| **PlaceholderAPI** | Available in hologram text templates |
| **ProtocolLib** | Packet-based entity visibility (RPG drop hiding) |
| **packetevents** | Alternative packet provider for RPG item rendering |

---

## 🛠️ Commands & Permissions

| Command | Aliases | Description | Permission |
|---|---|---|---|
| `/lootglow` | `/lg`, `/glow`, `/loot` | Display plugin info & status | None |
| `/lg help` | | Show full help menu | None |
| `/lg toggle` | | Toggle all visual effects for yourself | None |
| `/lg magnet` | | Toggle your personal VIP item magnet | `lootglow.command.magnet` |
| `/lg reload` | | Reload configuration & messages | `lootglow.admin` |

### Permission Nodes
| Permission | Description |
|---|---|
| `lootglow.admin` | Access to `/lg reload` and admin commands |
| `lootglow.command.magnet` | Access to toggle personal item magnet |
| `lootglow.magnet` | *(configurable)* Required to have the magnet active |
| `lootglow.bypass.lock` | *(configurable)* Bypass loot protection hard-lock |

---

## 📦 Developer Integration (LootGlow-API)

LootGlow exposes a clean, stable API through the `LootGlow-API` module, published on **JitPack**.

### Adding the Dependency

#### Maven (`pom.xml`)
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Skytoone</groupId>
        <artifactId>LootGlow-API</artifactId>
        <version>1.6.3-beta1</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

#### Gradle (`build.gradle`)
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.Skytoone:LootGlow-API:1.6.3-beta1'
}
```

> **Note:** Always use `scope: provided` / `compileOnly` — LootGlow-API is provided at runtime by the plugin.

---

### Obtaining the API Instance

```java
import fr.skynex.lootglow.api.LootGlowAPI;
import fr.skynex.lootglow.api.util.LootGlowHook;

// Recommended: via LootGlowHook (null-safe Optional)
LootGlowHook.getAPI().ifPresent(api -> {
    // use api here
});

// Alternative: via Bukkit Services Manager
LootGlowAPI api = getServer().getServicesManager()
    .load(LootGlowAPI.class);
```

---

### 🔧 API Method Reference

#### Glow Color
```java
// Set a custom gold glow color on a dropped item (all players)
api.setGlowColor(item, Color.fromRGB(255, 215, 0));

// Set a custom glow color for a specific player only
api.setGlowColor(item, Color.fromRGB(255, 215, 0), player);

// Reset to the default category color (global or per-player)
api.resetGlowColor(item);
api.resetGlowColor(item, player);
```

#### Holograms
```java
// Set a custom holographic label (supports MiniMessage & legacy codes)
api.setCustomHologram(item, "<gold>⭐ Legendary Loot ⭐</gold>");

// Set a custom holographic label for a specific player only
api.setCustomHologram(item, "<green>Your Personal Drop</green>", player);

// Remove custom hologram (reverts to default)
api.setCustomHologram(item, null);
```

#### Beacon Beams
```java
// Enable or disable the vertical beacon beam
api.setBeaconBeam(item, true);

// Enable beacon beam with a custom RGB color
api.setBeaconBeam(item, true, Color.fromRGB(0, 255, 200));
```

#### Loot Protection
```java
// Lock an item for 30 seconds to a specific player
api.setLootProtection(item, player.getUniqueId(), 30L);

// Check loot protection state & permissions
boolean protectedItem = api.isLootProtected(item);
boolean allowed = api.isPlayerAllowedToPickup(player, item);
UUID ownerUuid = api.getLootOwner(item);
```

#### VIP Magnet
```java
// Check and control magnet state
boolean active = api.isMagnetEnabled(player);
api.setMagnetEnabled(player, true);

// Manually pull all items within 10 blocks
api.pullItemsToPlayer(player, 10.0);
```

#### Particles
```java
// Assign a particle effect
api.setParticleEffect(item, Particle.FLAME);

// Clear the custom particle
api.clearParticleEffect(item);
```

#### Sounds & Animations
```java
// Play a custom drop sound (simplified or with custom volume/pitch)
api.setDropSound(item, Sound.ENTITY_ITEM_PICKUP);
api.setDropSound(item, Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.2f);

// Trigger a pop-jump animation with particle burst
api.triggerPopAnimation(item, 0.4);

// Enable bouncing physics
api.setBouncingEnabled(item, true);
```

#### Visuals & Farming
```java
// Hide / show all LootGlow visuals for a player
api.setVisualsHidden(player, true);
boolean hidden = api.isVisualsHidden(player);

// Check if player has direct line of sight to item within 32 blocks (raycast block collision)
boolean canSee = api.hasLineOfSight(player, item, 32.0);

// Automatically update item glow/visual visibility for player based on raycast occlusion
boolean isVisible = api.updateOcclusionVisibility(player, item, 32.0);

// Farming highlights on crop blocks
api.setCropHighlight(cropBlock, true);
boolean highlighted = api.isCropHighlighted(cropBlock);
```

#### Categories & Queries
```java
// Dynamically override or read the LootGlow category assigned to an item
api.setItemCategory(item, "legendary");
String category = api.getItemCategory(item); // e.g. "legendary", "rare"

// Find all glowing items near a location
List<Item> nearby = api.getNearbyGlowingItems(location, 20.0);
```

---

### 📡 Custom Events

LootGlow fires three cancellable Bukkit events you can listen to:

#### `LootGlowItemSpawnEvent`
Fired when a dropped item receives LootGlow visual effects. Cancel to skip effects on specific items, or modify the glow color, hologram text, and beacon beam before they are applied.

```java
@EventHandler
public void onLootGlowSpawn(LootGlowItemSpawnEvent event) {
    Item item = event.getItem();

    // Prevent effects on dirt
    if (item.getItemStack().getType() == Material.DIRT) {
        event.setCancelled(true);
        return;
    }

    // Override color and label dynamically
    event.setGlowColor(Color.fromRGB(255, 0, 128));
    event.setHologramText("<light_purple>✦ Custom Label ✦</light_purple>");
    event.setBeaconBeam(true);
}
```

#### `LootGlowMagnetPickupEvent`
Fired when a player's VIP magnet pulls an item. Cancel to block specific items from being attracted.

```java
@EventHandler
public void onMagnetPickup(LootGlowMagnetPickupEvent event) {
    Player player = event.getPlayer();
    Item item = event.getItem();

    // Prevent magnet from picking up quest items
    if (isQuestItem(item.getItemStack())) {
        event.setCancelled(true);
    }
}
```

#### `LootGlowContainerOpenEvent`
Fired when a player right-clicks a grouped item pile to open the Loot Container GUI.

```java
@EventHandler
public void onContainerOpen(LootGlowContainerOpenEvent event) {
    Player player = event.getPlayer();
    List<ItemStack> items = event.getItems();

    getLogger().info(player.getName()
        + " opened a loot container with " + items.size() + " items.");
}
```

---

### 💡 Full Integration Example

```java
import fr.skynex.lootglow.api.util.LootGlowHook;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Item;

public class MyPlugin extends JavaPlugin {

    public void applyLegendaryEffects(Item droppedItem) {
        LootGlowHook.getAPI().ifPresent(api -> {
            // Golden glow
            api.setGlowColor(droppedItem, Color.fromRGB(255, 215, 0));

            // Custom holographic label
            api.setCustomHologram(droppedItem,
                "<gradient:gold:yellow>⭐ LEGENDARY ⭐</gradient>");

            // Beacon beam
            api.setBeaconBeam(droppedItem, true);

            // Flame particles
            api.setParticleEffect(droppedItem, Particle.FLAME);

            // Pop animation on spawn
            api.triggerPopAnimation(droppedItem, 0.35);

            // Epic drop sound
            api.setDropSound(droppedItem,
                Sound.ENTITY_ENDER_DRAGON_GROWL, 0.3f, 1.5f);
        });
    }
}
```

---

## ⚙️ Building from Source

### Prerequisites
- **JDK 21** or higher
- **Apache Maven 3.8+**

### Compilation
```bash
git clone https://github.com/Skytoone/LootGlow.git
cd LootGlow
mvn clean package
```

The compiled plugin JAR will be available at `target/LootGlow-1.6.3-beta1.jar`.

### Project Structure
```
LootGlow/
├── LootGlow-API/                               # Public API module
│   └── src/main/java/fr/skynex/lootglow/api/
│       ├── LootGlowAPI.java                    # Main API interface
│       ├── util/LootGlowHook.java              # Safe null-safe API accessor
│       └── events/
│           ├── LootGlowItemSpawnEvent.java
│           ├── LootGlowMagnetPickupEvent.java
│           └── LootGlowContainerOpenEvent.java
└── LootGlow-Core/                              # Plugin implementation module
    └── src/main/java/fr/skynex/lootglow/
        ├── LootGlow.java                       # Main plugin class & API impl
        ├── ConfigUpdater.java                  # Auto config migration
        ├── config/
        │   ├── LootGlowConfigManager.java      # Category, world filter & crop config parsing
        │   └── ConfigParser.java
        ├── commands/
        │   └── LootGlowCommandManager.java
        ├── database/
        │   └── DatabaseManager.java
        ├── integration/
        │   ├── WorldGuardHook.java             # WorldGuard flag & region checks
        │   └── IntegrationManager.java
        ├── listeners/
        │   ├── ItemListener.java
        │   ├── FarmingListener.java
        │   ├── LootContainerListener.java
        │   └── MythicListener.java
        ├── managers/
        │   ├── TrackedItemManager.java         # Entity tracking, GC, spatial queries
        │   ├── VisualSpawner.java              # Glow entity spawning & despawning
        │   ├── VisualDisplayManager.java       # ItemDisplay creation & head textures
        │   ├── PluginTickManager.java          # Unified tick scheduler
        │   ├── ParticleAnimationManager.java   # Particles, pop & bounce animations
        │   ├── FarmingManager.java             # Crop scanning & highlighting
        │   ├── BeamManager.java                # Beacon beam animations
        │   ├── GlowTeamManager.java            # Scoreboard glow team management
        │   ├── GlowManager.java                # Per-player glow color overrides
        │   ├── LootProtectionManager.java      # PDC-backed loot protection & ownership
        │   ├── LootWorldManager.java           # World filtering
        │   ├── LODManager.java                 # Level-of-Detail visibility system
        │   ├── ItemMagnetManager.java          # VIP item magnet auto-pickup
        │   ├── GroupContainerManager.java      # Loot container GUI
        │   ├── RPGDropManager.java             # RPG drop shadow & visibility hiding
        │   ├── EconomyDropManager.java         # Economy drop label
        │   ├── HologramManager.java            # Hologram lifecycle
        │   ├── HologramRenderer.java           # Hologram rendering
        │   ├── OcclusionManager.java           # Line-of-sight raycast checks
        │   ├── SurfaceAlignmentManager.java    # Item surface/floor alignment
        │   ├── PlayerSettingsManager.java      # Per-player toggle settings
        │   ├── VisibilityPacketManager.java    # Packet provider (ProtocolLib/packetevents)
        │   └── VanillaItemVisibilityManager.java
        ├── service/
        │   ├── ItemGlowApplyService.java       # Glow effect application pipeline
        │   ├── HologramService.java            # Hologram label creation & refresh
        │   ├── HologramTickService.java        # Hologram per-tick updater
        │   ├── ItemPhysicsService.java         # Display entity position physics
        │   ├── ItemGroupingService.java        # Item grouping & merging
        │   ├── ItemVisualSpawnService.java     # ItemDisplay entity spawning
        │   ├── ItemRotationService.java        # Item rotation animation
        │   ├── BeamTickService.java            # Beam pulsing animation
        │   ├── EntityVisibilityService.java    # LOD-based show/hide logic
        │   ├── LightService.java               # Dynamic light block management
        │   ├── MessageService.java             # Configurable message system
        │   └── PluginDisableService.java       # Clean shutdown & entity removal
        ├── packets/                            # ProtocolLib / packetevents support
        └── util/
            ├── FoliaScheduler.java             # Folia-compatible scheduler wrapper
            ├── UpdateChecker.java              # Spigot update checker & semver compare
            ├── CustomItemIdentifier.java       # Multi-engine custom item detection
            └── ItemNameFormatter.java
```

---

## 🐛 Support & Community

- **Discord**: [discord.gg/3QzcDHC6](https://discord.gg/3QzcDHC6) — Bug reports, suggestions & support
- **SpigotMC**: [Resource Page #134648](https://www.spigotmc.org/resources/134648) — Download & reviews
- **bStats**: [Plugin statistics](https://bstats.org/plugin/bukkit/LootGlow/30993)

> ⭐ If you enjoy LootGlow, please leave a rating on SpigotMC — it really helps!

---

## 📄 License

LootGlow is licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html).

You are free to use, study and modify this code under the terms of the GPLv3. Derivative works must also be distributed under the GPLv3.
