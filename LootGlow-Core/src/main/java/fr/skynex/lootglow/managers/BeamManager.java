package fr.skynex.lootglow.managers;

import fr.skynex.lootglow.LootGlow;
import fr.skynex.lootglow.managers.TrackedItemManager.TrackedItem;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages BlockDisplay beacon light beam creation, colors, and entity tracking.
 */
public class BeamManager {

    public static class BeamConfig {
        public float height;
        public float width;
        public boolean animate;
        public boolean pulse;
        public Color color;
        public boolean doubleBeam;

        public BeamConfig(float height, float width, boolean animate, boolean pulse) {
            this.height = height;
            this.width = width;
            this.animate = animate;
            this.pulse = pulse;
        }

        public BeamConfig(Color color, boolean doubleBeam) {
            this.color = color;
            this.doubleBeam = doubleBeam;
        }
    }

    private final LootGlow plugin;
    private final Map<UUID, BeamConfig> activeBeamConfigs = new ConcurrentHashMap<>();

    public BeamManager(LootGlow plugin) {
        this.plugin = plugin;
    }

    public Map<UUID, BeamConfig> getActiveBeamConfigs() {
        return activeBeamConfigs;
    }

    public Material getColorStainedGlass(NamedTextColor color) {
        if (color == null) return Material.WHITE_STAINED_GLASS;
        if (color.equals(NamedTextColor.GOLD)) return Material.YELLOW_STAINED_GLASS;
        if (color.equals(NamedTextColor.LIGHT_PURPLE)) return Material.MAGENTA_STAINED_GLASS;
        if (color.equals(NamedTextColor.AQUA)) return Material.LIGHT_BLUE_STAINED_GLASS;
        if (color.equals(NamedTextColor.GREEN)) return Material.LIME_STAINED_GLASS;
        if (color.equals(NamedTextColor.RED)) return Material.RED_STAINED_GLASS;
        if (color.equals(NamedTextColor.BLUE)) return Material.BLUE_STAINED_GLASS;
        if (color.equals(NamedTextColor.DARK_PURPLE)) return Material.PURPLE_STAINED_GLASS;
        if (color.equals(NamedTextColor.YELLOW)) return Material.YELLOW_STAINED_GLASS;
        if (color.equals(NamedTextColor.WHITE)) return Material.WHITE_STAINED_GLASS;
        if (color.equals(NamedTextColor.GRAY)) return Material.LIGHT_GRAY_STAINED_GLASS;
        if (color.equals(NamedTextColor.DARK_GRAY)) return Material.GRAY_STAINED_GLASS;
        if (color.equals(NamedTextColor.BLACK)) return Material.BLACK_STAINED_GLASS;
        return Material.WHITE_STAINED_GLASS;
    }

    public void spawnBeam(Item item, String category, NamedTextColor color,
                          Map<UUID, BlockDisplay> activeBeams,
                          float beamHeight,
                          float beamWidth,
                          boolean beamsAnimate,
                          boolean beamsUseCategoryColor,
                          double lodBeamDistSq,
                          Set<UUID> hiddenVisuals,
                          Map<UUID, Set<UUID>> visibleEntities) {

        if (activeBeams.containsKey(item.getUniqueId())) return;

        float h = beamHeight;
        float w = beamWidth;
        boolean anim = beamsAnimate;
        boolean pulse = true;
        Material mat = beamsUseCategoryColor ? getColorStainedGlass(color) : Material.WHITE_STAINED_GLASS;

        if (category != null) {
            String path = "categories." + category + ".beam.";
            if (plugin.getConfig().contains(path + "height"))
                h = (float) plugin.getConfig().getDouble(path + "height");
            if (plugin.getConfig().contains(path + "width"))
                w = (float) plugin.getConfig().getDouble(path + "width");
            if (plugin.getConfig().contains(path + "animate"))
                anim = plugin.getConfig().getBoolean(path + "animate");
            if (plugin.getConfig().contains(path + "pulse"))
                pulse = plugin.getConfig().getBoolean(path + "pulse");
            if (plugin.getConfig().contains(path + "material")) {
                Material m = Material.matchMaterial(plugin.getConfig().getString(path + "material", ""));
                if (m != null) mat = m;
            }
        }

        final float finalH = h;
        final float finalW = w;
        final Material finalMat = mat;

        BlockDisplay beam = item.getWorld().spawn(item.getLocation(), BlockDisplay.class, ent -> {
            ent.setBlock(finalMat.createBlockData());
            ent.setGlowColorOverride(Color.fromRGB(color.red(), color.green(), color.blue()));
            ent.setGlowing(true);
            ent.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));

            Transformation transformation = ent.getTransformation();
            transformation.getScale().set(finalW, finalH, finalW);
            transformation.getTranslation().set(-finalW / 2, 0, -finalW / 2);
            ent.setTransformation(transformation);

            ent.setTeleportDuration(1);
            ent.setPersistent(false);
        });

        activeBeams.put(item.getUniqueId(), beam);
        activeBeamConfigs.put(item.getUniqueId(), new BeamConfig(finalH, finalW, anim, pulse));

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(item.getWorld())) continue;
            if (hiddenVisuals.contains(p.getUniqueId())
                    || p.getLocation().distanceSquared(item.getLocation()) >= lodBeamDistSq) {
                p.hideEntity(plugin, beam);
            } else {
                visibleEntities.computeIfAbsent(p.getUniqueId(), k -> java.util.concurrent.ConcurrentHashMap.newKeySet()).add(beam.getUniqueId());
            }
        }
    }

    public void removeBeam(Item item) {
        if (item == null) return;
        removeBeam(item.getUniqueId());
    }

    public void removeBeam(UUID uuid) {
        if (uuid == null) return;
        TrackedItem ti = plugin.getTrackedItemManager().getTrackedItems().get(uuid);
        if (ti != null && ti.beam != null && ti.beam.isValid()) {
            ti.beam.getPassengers().forEach(e -> { if (e != null) e.remove(); });
            ti.beam.remove();
        }
        activeBeamConfigs.remove(uuid);
    }

    public void clearAll() {
        activeBeamConfigs.clear();
    }
}
