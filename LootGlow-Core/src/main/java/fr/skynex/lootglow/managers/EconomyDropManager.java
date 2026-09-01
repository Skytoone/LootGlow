package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages economy item drop metadata and money amounts.
 */
public class EconomyDropManager {

    private final LootGlow plugin;
    private final List<NamespacedKey> economyKeys = new ArrayList<>();
    private final Map<UUID, Double> itemMoneyAmounts = new ConcurrentHashMap<>();

    private Object vaultEconomy = null;
    private boolean vaultAttempted = false;

    public EconomyDropManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public List<NamespacedKey> getEconomyKeys() {
        return economyKeys;
    }

    public Map<UUID, Double> getItemMoneyAmounts() {
        return itemMoneyAmounts;
    }

    public boolean isVaultAvailable() {
        setupVault();
        return vaultEconomy != null;
    }

    private void setupVault() {
        if (vaultAttempted) return;
        vaultAttempted = true;
        if (org.bukkit.Bukkit.getPluginManager().getPlugin("Vault") == null) return;
        try {
            Class<?> ecoClass = Class.forName("net.milkbowl.vault.economy.Economy");
            org.bukkit.plugin.RegisteredServiceProvider<?> rsp = plugin.getServer().getServicesManager().getRegistration(ecoClass);
            if (rsp != null) {
                vaultEconomy = rsp.getProvider();
                plugin.getLogger().info("Vault Economy integration enabled successfully.");
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Could not hook Vault Economy: " + t.getMessage());
        }
    }

    public String formatCurrency(double amount, String formatPattern, String prefix) {
        setupVault();
        if (vaultEconomy != null) {
            try {
                java.lang.reflect.Method formatMethod = vaultEconomy.getClass().getMethod("format", double.class);
                return (String) formatMethod.invoke(vaultEconomy, amount);
            } catch (Throwable ignored) {}
        }
        java.text.DecimalFormat df = new java.text.DecimalFormat(formatPattern != null ? formatPattern : "#,##0.00");
        return (prefix != null ? prefix : "$") + df.format(amount);
    }

    public void clearAll() {
        economyKeys.clear();
        itemMoneyAmounts.clear();
    }
}
