package fr.skynex.lootglow.service;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.ConfigUpdater;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Map;

/**
 * Handles messages.yml loading, formatting, MiniMessage parsing, and placeholders.
 */
public class MessageService {

    private final LootGlow plugin;
    private YamlConfiguration messagesConfig;

    private String rawPrefix = "";
    private String rawAmountFormat = " <gray>(x<amount>)</gray>";
    private String rawTimerFormat = " <gray>(<time>s)</gray>";
    private String rawOwnerFormat = "<newline><gray>Owned by</gray> <white><owner></white>";
    private String rawBundleFormat = "<gradient:gold:white>[Loot Bag]</gradient> <gray>(x<count> items)</gray>";

    public MessageService(LootGlow plugin) {
        this.plugin = plugin;
    }

    public String getRawPrefix() { return rawPrefix; }
    public String getRawAmountFormat() { return rawAmountFormat; }
    public String getRawTimerFormat() { return rawTimerFormat; }
    public String getRawOwnerFormat() { return rawOwnerFormat; }
    public String getRawBundleFormat() { return rawBundleFormat; }

    public void loadMessages(Map<Integer, Component> timerComponentCache) {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        } else {
            ConfigUpdater.update(plugin, "messages.yml", messagesFile);
        }
        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        this.rawPrefix = messagesConfig.getString("prefix", "");
        this.rawAmountFormat = messagesConfig.getString("item-amount-format", " <gray>(x<amount>)</gray>");
        this.rawTimerFormat = messagesConfig.getString("item-timer-format", " <gray>(<time>s)</gray>");
        this.rawOwnerFormat = messagesConfig.getString("owner-format",
                "<newline><gray>Owned by</gray> <white><owner></white>");
        this.rawBundleFormat = messagesConfig.getString("bundle-format",
                "<gradient:gold:white>[Loot Bag]</gradient> <gray>(x<count> items)</gray>");

        timerComponentCache.clear();
        for (int i = 0; i <= 305; i++) {
            timerComponentCache.put(i, fr.skynex.lootglow.util.ColorUtil.parse(rawTimerFormat.replace("<time>", String.valueOf(i))));
        }
    }

    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, null);
    }

    public void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        if (messagesConfig == null) return;
        if (messagesConfig.isList(key)) {
            for (String line : messagesConfig.getStringList(key)) {
                sendProcessedMessage(sender, line, placeholders);
            }
        } else {
            String msg = messagesConfig.getString(key, "Missing message: " + key);
            sendProcessedMessage(sender, msg, placeholders);
        }
    }

    private void sendProcessedMessage(CommandSender sender, String msg, Map<String, String> placeholders) {
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("<" + entry.getKey() + ">", entry.getValue());
            }
        }
        String fullMsg = msg.replace("<prefix>", rawPrefix);
        sender.sendMessage(fr.skynex.lootglow.util.ColorUtil.parse(fullMsg));
    }
}
