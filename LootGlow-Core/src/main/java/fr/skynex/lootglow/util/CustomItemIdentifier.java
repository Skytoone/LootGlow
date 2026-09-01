package fr.skynex.lootglow.util;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Handles custom item identification across various custom item plugins
 * (Oraxen, ItemsAdder, Nexo, AdvancedItems, ItemEdit, EcoItems, MMOItems, MythicItems, MythicDrops).
 */
public class CustomItemIdentifier {

    private static final NamespacedKey ORAXEN_KEY = new NamespacedKey("oraxen", "id");
    private static final NamespacedKey ORAXEN_KEY_ALT = new NamespacedKey("oraxen", "item_id");
    private static final NamespacedKey ORAXEN_KEY_ID = new NamespacedKey("oraxen", "oraxen_id");

    private static final NamespacedKey ITEMSADDER_KEY = new NamespacedKey("itemsadder", "id");
    private static final NamespacedKey ITEMSADDER_KEY_ALT = new NamespacedKey("itemsadder", "item_id");
    private static final NamespacedKey ITEMSADDER_NAMESPACE = new NamespacedKey("itemsadder", "namespace");
    private static final NamespacedKey ITEMSADDER_NAME = new NamespacedKey("itemsadder", "name");
    private static final NamespacedKey ITEMSADDER_NS_ID = new NamespacedKey("itemsadder", "namespace_id");

    private static final NamespacedKey NEXO_KEY = new NamespacedKey("nexo", "id");
    private static final NamespacedKey NEXO_KEY_ALT = new NamespacedKey("nexo", "item_id");
    private static final NamespacedKey NEXO_NEXO_ID = new NamespacedKey("nexo", "nexo_id");

    private static final NamespacedKey ADVANCEDITEMS_KEY = new NamespacedKey("advanceditems", "id");
    private static final NamespacedKey ITEMEDIT_KEY = new NamespacedKey("itemedit", "id");
    private static final NamespacedKey ECO_KEY = new NamespacedKey("ecoitems", "id");
    private static final NamespacedKey ECO_KEY_ALT = new NamespacedKey("auxilium", "id");

    private static final NamespacedKey EXECUTABLEITEMS_KEY = new NamespacedKey("executableitems", "id");
    private static final NamespacedKey EXECUTABLEITEMS_KEY_EI_ID = new NamespacedKey("executableitems", "ei-id");
    private static final NamespacedKey EXECUTABLEITEMS_KEY_EI_ID2 = new NamespacedKey("executableitems", "ei_id");
    private static final NamespacedKey SSOMAR_EI_KEY = new NamespacedKey("ssomar", "ei-id");
    private static final NamespacedKey SSOMAR_EI_KEY2 = new NamespacedKey("ssomar", "ei_id");
    private static final NamespacedKey SSOMAR_EI_KEY3 = new NamespacedKey("ssomar_ei", "id");

    private static final NamespacedKey EXECUTABLEBLOCKS_KEY = new NamespacedKey("executableblocks", "id");
    private static final NamespacedKey EXECUTABLEBLOCKS_KEY_EB_ID = new NamespacedKey("executableblocks", "eb-id");
    private static final NamespacedKey EXECUTABLEBLOCKS_KEY_EB_ID2 = new NamespacedKey("executableblocks", "eb_id");
    private static final NamespacedKey SSOMAR_EB_KEY = new NamespacedKey("ssomar", "eb-id");
    private static final NamespacedKey SSOMAR_EB_KEY2 = new NamespacedKey("ssomar", "eb_id");
    private static final NamespacedKey SSOMAR_EB_KEY3 = new NamespacedKey("ssomar_eb", "id");

    private static final NamespacedKey MMO_TYPE_KEY = new NamespacedKey("mmoitems", "item_type");
    private static final NamespacedKey MMO_ID_KEY = new NamespacedKey("mmoitems", "item_id");
    private static final NamespacedKey MMO_TYPE_KEY_ALT = new NamespacedKey("mmoitems", "type");
    private static final NamespacedKey MMO_ID_KEY_ALT = new NamespacedKey("mmoitems", "id");

    private static final NamespacedKey ML_TYPE_KEY = new NamespacedKey("mythiclib", "item_type");
    private static final NamespacedKey ML_ID_KEY = new NamespacedKey("mythiclib", "item_id");
    private static final NamespacedKey ML_TYPE_KEY_ALT = new NamespacedKey("mythiclib", "type");
    private static final NamespacedKey ML_ID_KEY_ALT = new NamespacedKey("mythiclib", "id");

    private static final NamespacedKey PUB_TYPE_KEY = new NamespacedKey("public", "mmoitems_item_type");
    private static final NamespacedKey PUB_ID_KEY = new NamespacedKey("public", "mmoitems_item_id");

    private static final NamespacedKey MYTHIC_KEY = new NamespacedKey("mythicmobs", "item_type");
    private static final NamespacedKey MYTHIC_TYPE_KEY = new NamespacedKey("mythicmobs", "type");
    private static final NamespacedKey MYTHIC_ID_KEY = new NamespacedKey("mythicmobs", "id");
    private static final NamespacedKey MD_KEY = new NamespacedKey("mythicdrops", "tier");

    // Reflection cache for MMOItems / MythicLib NBT resolution
    private static Class<?> nbtItemClass = null;
    private static Method nbtItemResolver = null;
    private static Object nbtItemResolverTarget = null;
    private static Constructor<?> nbtItemConstructor = null;
    private static Method getStringMethod = null;
    private static Method getTypeMethod = null;
    private static boolean reflectionInitialized = false;

    private static void initReflection(boolean debug, Logger log) {
        if (reflectionInitialized) return;
        reflectionInitialized = true;

        ClassLoader mlLoader = null;
        ClassLoader mmoLoader = null;

        Plugin mlPlugin = Bukkit.getPluginManager().getPlugin("MythicLib");
        if (mlPlugin != null) mlLoader = mlPlugin.getClass().getClassLoader();

        Plugin mmoPlugin = Bukkit.getPluginManager().getPlugin("MMOItems");
        if (mmoPlugin != null) mmoLoader = mmoPlugin.getClass().getClassLoader();

        if (mlLoader != null) {
            try {
                nbtItemClass = Class.forName("io.lumine.mythic.lib.api.item.NBTItem", true, mlLoader);
            } catch (ClassNotFoundException ignored) {}
        }
        if (nbtItemClass == null && mmoLoader != null) {
            try {
                nbtItemClass = Class.forName("net.Indyuce.mmoitems.api.util.NBTItem", true, mmoLoader);
            } catch (ClassNotFoundException e2) {
                try {
                    nbtItemClass = Class.forName("net.Indyuce.mmoitems.api.item.NBTItem", true, mmoLoader);
                } catch (ClassNotFoundException ignored) {}
            }
        }

        if (nbtItemClass == null) {
            if (debug && log != null) log.warning("[Debug] [Reflection] NBTItem class not found in MythicLib/MMOItems classloaders.");
            return;
        }

        if (debug && log != null) log.info("[Debug] [Reflection] NBTItem class found: " + nbtItemClass.getName());

        try {
            getStringMethod = nbtItemClass.getMethod("getString", String.class);
        } catch (NoSuchMethodException ignored) {}
        try {
            getTypeMethod = nbtItemClass.getMethod("getType");
        } catch (NoSuchMethodException ignored) {}

        try {
            Class<?> mythicLibClass = Class.forName("io.lumine.mythic.lib.MythicLib");
            Object pluginObj = mythicLibClass.getField("plugin").get(null);
            Object version = pluginObj.getClass().getMethod("getVersion").invoke(pluginObj);
            Object wrapper = version.getClass().getMethod("getWrapper").invoke(version);
            nbtItemResolver = wrapper.getClass().getMethod("getNBTItem", ItemStack.class);
            nbtItemResolverTarget = wrapper;
        } catch (Exception e) {
            try {
                nbtItemResolver = nbtItemClass.getMethod("get", ItemStack.class);
                nbtItemResolverTarget = null;
            } catch (NoSuchMethodException e2) {
                try {
                    nbtItemConstructor = nbtItemClass.getConstructor(ItemStack.class);
                } catch (NoSuchMethodException ignored) {}
            }
        }
    }

    private static String getMMOItemsIdFromReflection(ItemStack item, boolean debug, Logger log) {
        initReflection(debug, log);
        if (nbtItemClass == null) return null;
        if (nbtItemResolver == null && nbtItemConstructor == null) return null;

        try {
            Object nbtItem = null;
            if (nbtItemResolver != null) {
                nbtItem = nbtItemResolver.invoke(nbtItemResolverTarget, item);
            } else {
                nbtItem = nbtItemConstructor.newInstance(item);
            }

            if (nbtItem == null) return null;

            String type = null;
            if (getTypeMethod != null) {
                try {
                    type = (String) getTypeMethod.invoke(nbtItem);
                } catch (Exception ignored) {}
            }
            if (type == null || type.trim().isEmpty()) {
                type = getTagValue(nbtItem, getStringMethod, "MMOITEMS_ITEM_TYPE", "mmoitems_item_type", "item_type", "type", "TYPE");
            }

            String id = getTagValue(nbtItem, getStringMethod, "MMOITEMS_ITEM_ID", "mmoitems_item_id", "item_id", "id", "ID");

            if (type != null && !type.isEmpty() && id != null && !id.isEmpty()) {
                return "MMOITEMS:" + type.toUpperCase() + ":" + id.toUpperCase();
            }
        } catch (Exception e) {
            if (debug && log != null) {
                log.warning("[Debug] [Reflection] Exception during NBT read: " + e.getMessage());
            }
        }
        return null;
    }

    private static String getTagValue(Object nbtItem, Method getStringMethod, String... keys) {
        if (getStringMethod == null) return null;
        for (String key : keys) {
            try {
                String val = (String) getStringMethod.invoke(nbtItem, key);
                if (val != null && !val.trim().isEmpty()) {
                    return val.trim();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }

    public static String getInternalId(ItemStack item, boolean debug, Logger log) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        // Oraxen
        if (pdc.has(ORAXEN_KEY, PersistentDataType.STRING))
            return "ORAXEN:" + pdc.get(ORAXEN_KEY, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(ORAXEN_KEY_ALT, PersistentDataType.STRING))
            return "ORAXEN:" + pdc.get(ORAXEN_KEY_ALT, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(ORAXEN_KEY_ID, PersistentDataType.STRING))
            return "ORAXEN:" + pdc.get(ORAXEN_KEY_ID, PersistentDataType.STRING).toUpperCase();

        // ItemsAdder
        if (pdc.has(ITEMSADDER_KEY, PersistentDataType.STRING))
            return "ITEMSADDER:" + pdc.get(ITEMSADDER_KEY, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(ITEMSADDER_KEY_ALT, PersistentDataType.STRING))
            return "ITEMSADDER:" + pdc.get(ITEMSADDER_KEY_ALT, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(ITEMSADDER_NS_ID, PersistentDataType.STRING))
            return "ITEMSADDER:" + pdc.get(ITEMSADDER_NS_ID, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(ITEMSADDER_NAMESPACE, PersistentDataType.STRING) && pdc.has(ITEMSADDER_NAME, PersistentDataType.STRING))
            return "ITEMSADDER:" + pdc.get(ITEMSADDER_NAMESPACE, PersistentDataType.STRING).toUpperCase() + ":" + pdc.get(ITEMSADDER_NAME, PersistentDataType.STRING).toUpperCase();

        // Nexo
        if (pdc.has(NEXO_KEY, PersistentDataType.STRING))
            return "NEXO:" + pdc.get(NEXO_KEY, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(NEXO_KEY_ALT, PersistentDataType.STRING))
            return "NEXO:" + pdc.get(NEXO_KEY_ALT, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(NEXO_NEXO_ID, PersistentDataType.STRING))
            return "NEXO:" + pdc.get(NEXO_NEXO_ID, PersistentDataType.STRING).toUpperCase();

        // ExecutableItems & ExecutableBlocks
        if (pdc.has(EXECUTABLEITEMS_KEY, PersistentDataType.STRING))
            return "EXECUTABLEITEMS:" + pdc.get(EXECUTABLEITEMS_KEY, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(EXECUTABLEITEMS_KEY_EI_ID, PersistentDataType.STRING))
            return "EXECUTABLEITEMS:" + pdc.get(EXECUTABLEITEMS_KEY_EI_ID, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(EXECUTABLEITEMS_KEY_EI_ID2, PersistentDataType.STRING))
            return "EXECUTABLEITEMS:" + pdc.get(EXECUTABLEITEMS_KEY_EI_ID2, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(SSOMAR_EI_KEY, PersistentDataType.STRING))
            return "EXECUTABLEITEMS:" + pdc.get(SSOMAR_EI_KEY, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(SSOMAR_EI_KEY2, PersistentDataType.STRING))
            return "EXECUTABLEITEMS:" + pdc.get(SSOMAR_EI_KEY2, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(SSOMAR_EI_KEY3, PersistentDataType.STRING))
            return "EXECUTABLEITEMS:" + pdc.get(SSOMAR_EI_KEY3, PersistentDataType.STRING).toUpperCase();

        if (pdc.has(EXECUTABLEBLOCKS_KEY, PersistentDataType.STRING))
            return "EXECUTABLEBLOCKS:" + pdc.get(EXECUTABLEBLOCKS_KEY, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(EXECUTABLEBLOCKS_KEY_EB_ID, PersistentDataType.STRING))
            return "EXECUTABLEBLOCKS:" + pdc.get(EXECUTABLEBLOCKS_KEY_EB_ID, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(EXECUTABLEBLOCKS_KEY_EB_ID2, PersistentDataType.STRING))
            return "EXECUTABLEBLOCKS:" + pdc.get(EXECUTABLEBLOCKS_KEY_EB_ID2, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(SSOMAR_EB_KEY, PersistentDataType.STRING))
            return "EXECUTABLEBLOCKS:" + pdc.get(SSOMAR_EB_KEY, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(SSOMAR_EB_KEY2, PersistentDataType.STRING))
            return "EXECUTABLEBLOCKS:" + pdc.get(SSOMAR_EB_KEY2, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(SSOMAR_EB_KEY3, PersistentDataType.STRING))
            return "EXECUTABLEBLOCKS:" + pdc.get(SSOMAR_EB_KEY3, PersistentDataType.STRING).toUpperCase();

        // AdvancedItems
        if (pdc.has(ADVANCEDITEMS_KEY, PersistentDataType.STRING))
            return "ADVANCEDITEMS:" + pdc.get(ADVANCEDITEMS_KEY, PersistentDataType.STRING).toUpperCase();

        // ItemEdit
        if (pdc.has(ITEMEDIT_KEY, PersistentDataType.STRING))
            return "ITEMEDIT:" + pdc.get(ITEMEDIT_KEY, PersistentDataType.STRING).toUpperCase();

        // EcoItems & Auxilium
        if (pdc.has(ECO_KEY, PersistentDataType.STRING))
            return "ECOITEMS:" + pdc.get(ECO_KEY, PersistentDataType.STRING).toUpperCase();
        if (pdc.has(ECO_KEY_ALT, PersistentDataType.STRING))
            return "ECOITEMS:" + pdc.get(ECO_KEY_ALT, PersistentDataType.STRING).toUpperCase();

        // MMOItems (PDC variants)
        if (pdc.has(MMO_TYPE_KEY, PersistentDataType.STRING) && pdc.has(MMO_ID_KEY, PersistentDataType.STRING)) {
            return "MMOITEMS:" + pdc.get(MMO_TYPE_KEY, PersistentDataType.STRING).toUpperCase() + ":"
                    + pdc.get(MMO_ID_KEY, PersistentDataType.STRING).toUpperCase();
        } else if (pdc.has(MMO_TYPE_KEY_ALT, PersistentDataType.STRING)
                && pdc.has(MMO_ID_KEY_ALT, PersistentDataType.STRING)) {
            return "MMOITEMS:" + pdc.get(MMO_TYPE_KEY_ALT, PersistentDataType.STRING).toUpperCase() + ":"
                    + pdc.get(MMO_ID_KEY_ALT, PersistentDataType.STRING).toUpperCase();
        }

        if (pdc.has(ML_TYPE_KEY, PersistentDataType.STRING) && pdc.has(ML_ID_KEY, PersistentDataType.STRING)) {
            return "MMOITEMS:" + pdc.get(ML_TYPE_KEY, PersistentDataType.STRING).toUpperCase() + ":"
                    + pdc.get(ML_ID_KEY, PersistentDataType.STRING).toUpperCase();
        } else if (pdc.has(ML_TYPE_KEY_ALT, PersistentDataType.STRING)
                && pdc.has(ML_ID_KEY_ALT, PersistentDataType.STRING)) {
            return "MMOITEMS:" + pdc.get(ML_TYPE_KEY_ALT, PersistentDataType.STRING).toUpperCase() + ":"
                    + pdc.get(ML_ID_KEY_ALT, PersistentDataType.STRING).toUpperCase();
        }

        if (pdc.has(PUB_TYPE_KEY, PersistentDataType.STRING) && pdc.has(PUB_ID_KEY, PersistentDataType.STRING)) {
            return "MMOITEMS:" + pdc.get(PUB_TYPE_KEY, PersistentDataType.STRING).toUpperCase() + ":"
                    + pdc.get(PUB_ID_KEY, PersistentDataType.STRING).toUpperCase();
        }

        // NBT reflection fallback
        String mmoIdReflection = getMMOItemsIdFromReflection(item, debug, log);
        if (mmoIdReflection != null) {
            return mmoIdReflection;
        }

        // MythicItems / MythicMobs
        if (pdc.has(MYTHIC_KEY, PersistentDataType.STRING)) {
            return "MYTHIC:" + pdc.get(MYTHIC_KEY, PersistentDataType.STRING).toUpperCase();
        }
        if (pdc.has(MYTHIC_TYPE_KEY, PersistentDataType.STRING)) {
            return "MYTHIC:" + pdc.get(MYTHIC_TYPE_KEY, PersistentDataType.STRING).toUpperCase();
        }
        if (pdc.has(MYTHIC_ID_KEY, PersistentDataType.STRING)) {
            return "MYTHIC:" + pdc.get(MYTHIC_ID_KEY, PersistentDataType.STRING).toUpperCase();
        }

        // MythicDrops
        if (pdc.has(MD_KEY, PersistentDataType.STRING)) {
            return "MYTHICDROPS:" + pdc.get(MD_KEY, PersistentDataType.STRING).toUpperCase();
        }

        return null;
    }
}
