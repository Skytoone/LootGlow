# ✨ LootGlow

[![Plugin Version](https://img.shields.io/badge/Version-1.6.1-gold.svg)](https://github.com/Skytoone/LootGlow)
[![Java Version](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Server Support](https://img.shields.io/badge/Platform-Paper%20%7C%20Spigot%20%7C%20Folia-blue.svg)](https://papermc.io/)
[![License](https://img.shields.io/badge/License-GPLv3-green.svg)](https://www.gnu.org/licenses/gpl-3.0.html)

**LootGlow** brings a professional, RPG-like feel to your Minecraft server by adding subtle glowing effects, custom color gradients, 3D flat-item rendering, holographic labels, and dynamic particles to item drops.

---

## 🌟 Core Features

- **✨ Dynamic Glowing & Colors**: Configurable glowing colors based on item rarity, materials, or custom NBT tags.
- **🏷️ Ultra-Smooth Holograms**: 100% jitter-free holographic item labels using modern Display Entities.
- **🛡️ Dynamic Shadows**: Subtle semi-transparent 3D shadows beneath dropped items for enhanced visual depth.
- **💫 Beacon Beams**: Vertical light beams for legendary and epic loot drops.
- **⏳ Despawn Timer**: Live countdown on item holograms before item disappearance.
- **🌾 Farming Highlights**: Visual markers ('!') on fully grown crops, with custom WorldGuard region flags.
- **🧲 VIP Item Magnet**: Auto-pickup feature for high-rarity loot (toggleable per-player).
- **🔒 Loot Protection**: Owner-only locks and ownership labels for valuable boss or player drops.
- **📦 Grouping System & Container GUI**: Group identical dropped items in mob grinders and right-click to open a Loot Container GUI.
- **⚡ Folia & Paper Native**: Fully multi-threaded and compatible with Folia regionized threading and Paper 1.20+.
- **⚙️ Level Of Detail (LOD)**: Industrial-grade optimization system to maintain 20 TPS with thousands of ground items.

---

## 🔌 Plug & Play Integrations

LootGlow seamlessly integrates out of the box with your favorite plugins:

- **ItemsAdder**
- **Oraxen**
- **Nexo**
- **MythicMobs**
- **MMOItems**
- **EcoItems**
- **WorldGuard** (custom region flag `lootglow-farming`)
- **PlaceholderAPI**
- **ProtocolLib** / **packetevents**

## 📦 Developer Integration (LootGlow-API)

Add `LootGlow-API` to your project using **JitPack**:

### Maven (`pom.xml`)
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
        <version>1.6.1</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

### Gradle (`build.gradle`)
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    compileOnly 'com.github.Skytoone:LootGlow-API:1.6.1'
}
```

### 💻 API Example Usage
```java
import fr.skynex.lootglow.api.util.LootGlowHook;
import org.bukkit.Color;

public class MyPlugin {

    public void applyGoldGlow(Item droppedItem) {
        LootGlowHook.getAPI().ifPresent(api -> {
            // Set custom glow color
            api.setGlowColor(droppedItem, Color.fromRGB(255, 215, 0));

            // Set custom hologram label
            api.setCustomHologram(droppedItem, "<gold>⭐ Legendary Loot ⭐</gold>");

            // Enable vertical beacon beam
            api.setBeaconBeam(droppedItem, true);
        });
    }
}
```

---

## 🛠️ Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/lg` | Display plugin info & status | None |
| `/lg help` | Show help menu | None |
| `/lg toggle` | Toggle visual effects for yourself | None |
| `/lg magnet` | Toggle personal VIP item magnet | `lootglow.command.magnet` |
| `/lg reload` | Reload configuration & messages | `lootglow.admin` |

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

The compiled JAR file will be available in `target/LootGlow-1.6.1.jar`.

---

## 📄 License

LootGlow is licensed under the [GNU General Public License v3.0](https://www.gnu.org/licenses/gpl-3.0.html).
