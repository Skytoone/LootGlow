package fr.skynex.lootglow;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigUpdater {

    public static void update(Plugin plugin, String resourceName, File configFile) {
        if (!configFile.exists()) {
            plugin.saveResource(resourceName, false);
            return;
        }

        try {
            YamlConfiguration userConfig = new YamlConfiguration();
            try {
                userConfig.load(configFile);
            } catch (Exception e) {
                plugin.getLogger().warning("Corrupted or invalid YAML syntax detected in " + resourceName + "! Backing up to " + resourceName + ".corrupted.bak and resetting to clean defaults.");
                File backup = new File(configFile.getParentFile(), resourceName + ".corrupted.bak");
                if (backup.exists()) backup.delete();
                configFile.renameTo(backup);
                plugin.saveResource(resourceName, true);
                userConfig = YamlConfiguration.loadConfiguration(configFile);
            }
            InputStream inputStream = plugin.getResource(resourceName);
            if (inputStream == null) return;

            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                LinkedList<String> pathStack = new LinkedList<>();
                Map<Integer, String> indentMap = new HashMap<>();

                boolean skipTemplateListItems = false;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    
                    if (trimmed.startsWith("#") || trimmed.isEmpty()) {
                        lines.add(line);
                        continue;
                    }

                    Pattern pattern = Pattern.compile("^(\\s*)([a-zA-Z0-9_-]+):.*");
                    Matcher matcher = pattern.matcher(line);

                    if (matcher.find()) {
                        int indentation = matcher.group(1).length();
                        String key = matcher.group(2);

                        // Clear path stack based on indentation
                        indentMap.entrySet().removeIf(entry -> entry.getKey() >= indentation);
                        pathStack.clear();
                        List<Integer> sortedIndents = new ArrayList<>(indentMap.keySet());
                        Collections.sort(sortedIndents);
                        for (int i : sortedIndents) {
                            pathStack.add(indentMap.get(i));
                        }

                        String fullPath = pathStack.isEmpty() ? key : String.join(".", pathStack) + "." + key;

                        if (userConfig.contains(fullPath)) {
                            if (userConfig.isConfigurationSection(fullPath)) {
                                // It's a section header
                                lines.add(matcher.group(1) + key + ":");
                                indentMap.put(indentation, key);
                                skipTemplateListItems = false;
                            } else {
                                // It's a key-value pair or list
                                Object value = userConfig.get(fullPath);
                                lines.add(matcher.group(1) + key + ": " + formatValue(value));
                                skipTemplateListItems = (value instanceof List);
                            }
                        } else {
                            // New key from template
                            lines.add(line);
                            if (trimmed.endsWith(":")) {
                                indentMap.put(indentation, key);
                            }
                            skipTemplateListItems = false;
                        }
                    } else if (trimmed.startsWith("-")) {
                        if (skipTemplateListItems) {
                            continue;
                        } else {
                            lines.add(line);
                        }
                    } else {
                        lines.add(line);
                    }
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
                for (String l : lines) {
                    writer.write(l);
                    writer.newLine();
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Could not update config properly: " + e.getMessage());
        }
    }

    private static String formatValue(Object value) {
        if (value instanceof String) {
            return quoteValue((String) value);
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            if (list.isEmpty()) return "[]";
            
            // For simplicity in this updater, we'll use the flow style for short lists
            // and keep the template's look for complex ones. 
            // However, since we skip template list items, we must output the whole list here.
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < list.size(); i++) {
                Object item = list.get(i);
                if (item instanceof String) sb.append("\"").append(item).append("\"");
                else sb.append(item);
                if (i < list.size() - 1) sb.append(", ");
            }
            sb.append("]");
            return sb.toString();
        }
        return value != null ? value.toString() : "null";
    }

    private static String quoteValue(String value) {
        if (value == null || value.isEmpty()) return "\"\"";
        
        // Si c'est déjà entouré de guillemets ou si c'est un bloc scalaire (|), on ne touche à rien
        if ((value.startsWith("\"") && value.endsWith("\"")) || 
            (value.startsWith("'") && value.endsWith("'")) ||
            value.startsWith("|")) {
            return value;
        }

        // Si la valeur contient un retour à la ligne, c'est un message complexe, on ne le cite pas
        if (value.contains("\n") || value.contains("\r")) {
            return value;
        }

        // Sinon, on cite si ça contient des caractères spéciaux YAML
        if (value.contains("&") || value.contains("!") || value.contains(":") || value.contains("#") || 
            value.contains("[") || value.contains("]") || value.contains("*") || value.contains("@")) {
            return "\"" + value.replace("\"", "\\\"") + "\"";
        }
        return value;
    }
}
