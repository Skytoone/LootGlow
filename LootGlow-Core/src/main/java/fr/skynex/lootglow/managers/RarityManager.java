package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/**
 * Manages item rarity classification and custom header banners above holograms.
 */
public class RarityManager {

    private final LootGlow plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();
    private final java.util.Map<ItemRarity, Component> rarityHeaderCache = new java.util.EnumMap<>(ItemRarity.class);

    public enum ItemRarity {
        COMMON("Commun", "<gray>COMMUN</gray>"),
        UNCOMMON("Atypique", "<green>✦ ATYPIQUE ✦</green>"),
        RARE("Rare", "<blue><b>✦ RARE ✦</b></blue>"),
        EPIC("Épique", "<light_purple><b>✦ ÉPIQUE ✦</b></light_purple>"),
        LEGENDARY("Légendaire", "<gold><b>★ LÉGENDAIRE ★</b></gold>"),
        MYTHIC("Mythique", "<gradient:#FF0055:#FF8800><b>✧ MYTHIQUE ✧</b></gradient>");

        private final String displayName;
        private final String defaultBanner;

        ItemRarity(String displayName, String defaultBanner) {
            this.displayName = displayName;
            this.defaultBanner = defaultBanner;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDefaultBanner() {
            return defaultBanner;
        }
    }

    public RarityManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public void clearCache() {
        rarityHeaderCache.clear();
    }

    public ItemRarity detectRarity(ItemStack itemStack) {
        if (itemStack == null || itemStack.getType().isAir()) return ItemRarity.COMMON;

        boolean raritiesEnabled = getConfigBoolean("enabled", true);
        if (!raritiesEnabled) return ItemRarity.COMMON;

        boolean checkDisplayName = getConfigBoolean("check-display-name", true);
        boolean useVanillaRarity = getConfigBoolean("use-vanilla-rarity", true);

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            // 1. Check Display Name for Rarity Keywords
            if (checkDisplayName && meta.hasDisplayName()) {
                Component nameComp = meta.displayName();
                if (nameComp != null) {
                    String nameText = PLAIN_SERIALIZER.serialize(nameComp).toUpperCase();
                    ItemRarity matched = matchKeywords(nameText);
                    if (matched != null) return matched;
                }
            }

            // 2. Check Lore for Rarity Keywords
            if (meta.hasLore()) {
                List<Component> lore = meta.lore();
                if (lore != null) {
                    for (Component lineComp : lore) {
                        String lineText = PLAIN_SERIALIZER.serialize(lineComp).toUpperCase();
                        ItemRarity matched = matchKeywords(lineText);
                        if (matched != null) return matched;
                    }
                }
            }

            // 3. Check Vanilla Item Rarity (Minecraft 1.20.5+ API)
            if (useVanillaRarity) {
                try {
                    if (meta.hasRarity()) {
                        Object bukkitRarity = meta.getRarity();
                        if (bukkitRarity != null) {
                            String rarityName = bukkitRarity.toString().toUpperCase();
                            switch (rarityName) {
                                case "EPIC" -> { return ItemRarity.EPIC; }
                                case "RARE" -> { return ItemRarity.RARE; }
                                case "UNCOMMON" -> { return ItemRarity.UNCOMMON; }
                                default -> {}
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
        }

        // 4. Configured & Default Material Fallbacks
        Material mat = itemStack.getType();
        for (ItemRarity rarity : ItemRarity.values()) {
            List<String> configuredMaterials = getConfigList(rarity.name().toLowerCase() + ".materials");
            for (String matStr : configuredMaterials) {
                if (mat.name().equalsIgnoreCase(matStr)) {
                    return rarity;
                }
            }
        }

        // Hardcoded default fallbacks if not configured
        if (mat == Material.NETHER_STAR || mat == Material.DRAGON_EGG || mat == Material.ENCHANTED_GOLDEN_APPLE || mat == Material.BEACON) {
            return ItemRarity.MYTHIC;
        }
        if (mat == Material.NETHERITE_SWORD || mat == Material.NETHERITE_CHESTPLATE || mat == Material.NETHERITE_INGOT || mat == Material.TOTEM_OF_UNDYING || mat == Material.ELYTRA) {
            return ItemRarity.LEGENDARY;
        }
        if (mat == Material.DIAMOND_SWORD || mat == Material.DIAMOND_CHESTPLATE || mat == Material.DIAMOND_BLOCK || mat == Material.HEART_OF_THE_SEA) {
            return ItemRarity.EPIC;
        }
        if (mat == Material.DIAMOND || mat == Material.GOLDEN_APPLE || mat == Material.EMERALD_BLOCK) {
            return ItemRarity.RARE;
        }

        return ItemRarity.COMMON;
    }

    private ItemRarity matchKeywords(String textUpper) {
        ItemRarity[] raritiesInOrder = new ItemRarity[]{
                ItemRarity.MYTHIC, ItemRarity.LEGENDARY, ItemRarity.EPIC, ItemRarity.RARE, ItemRarity.UNCOMMON
        };

        for (ItemRarity rarity : raritiesInOrder) {
            String rarityKey = rarity.name().toLowerCase();
            List<String> keywords = getConfigList(rarityKey + ".keywords");
            if (keywords.isEmpty()) {
                keywords = getDefaultKeywords(rarity);
            }
            for (String kw : keywords) {
                if (textUpper.contains(kw.toUpperCase())) {
                    return rarity;
                }
            }
        }
        return null;
    }

    private List<String> getDefaultKeywords(ItemRarity rarity) {
        return switch (rarity) {
            case MYTHIC -> List.of("MYTHIC", "MYTHIQUE", "✧");
            case LEGENDARY -> List.of("LEGENDARY", "LÉGENDAIRE", "★");
            case EPIC -> List.of("EPIC", "ÉPIQUE");
            case RARE -> List.of("RARE");
            case UNCOMMON -> List.of("UNCOMMON", "ATYPIQUE");
            default -> List.of();
        };
    }

    public Component getRarityHeaderComponent(ItemRarity rarity) {
        if (rarity == ItemRarity.COMMON) return null;

        return rarityHeaderCache.computeIfAbsent(rarity, r -> {
            String rarityKey = r.name().toLowerCase();
            String customBanner = getConfigString(rarityKey + ".banner");
            if (customBanner != null && !customBanner.isBlank()) {
                return miniMessage.deserialize(customBanner);
            }
            return miniMessage.deserialize(r.getDefaultBanner());
        });
    }

    private boolean getConfigBoolean(String subKey, boolean def) {
        if (plugin.getConfig().contains("rarities." + subKey)) {
            return plugin.getConfig().getBoolean("rarities." + subKey);
        }
        return plugin.getConfig().getBoolean("settings.rarities." + subKey, def);
    }

    private String getConfigString(String subKey) {
        if (plugin.getConfig().contains("rarities." + subKey)) {
            return plugin.getConfig().getString("rarities." + subKey);
        }
        return plugin.getConfig().getString("settings.rarities." + subKey);
    }

    private List<String> getConfigList(String subKey) {
        if (plugin.getConfig().contains("rarities." + subKey)) {
            return plugin.getConfig().getStringList("rarities." + subKey);
        }
        return plugin.getConfig().getStringList("settings.rarities." + subKey);
    }
}
