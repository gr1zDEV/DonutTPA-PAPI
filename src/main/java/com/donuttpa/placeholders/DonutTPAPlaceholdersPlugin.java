package com.donuttpa.placeholders;

import java.lang.reflect.Field;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin entry point for the DonutTPA PlaceholderAPI bridge.
 *
 * <p>This plugin registers a PlaceholderAPI expansion during {@link #onEnable()} and reads all
 * DonutTPA data via reflection so the DonutTPA plugin does not need to be modified.</p>
 */
public final class DonutTPAPlaceholdersPlugin extends JavaPlugin {

    private DonutTPAPlaceholderExpansion expansion;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().severe("PlaceholderAPI is required but was not found. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("DonutTPA") == null) {
            getLogger().severe("DonutTPA is required but was not found. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.expansion = new DonutTPAPlaceholderExpansion(this);
        if (this.expansion.register()) {
            getLogger().info("Registered DonutTPA PlaceholderAPI expansion.");
            getLogger().info("Example usage: %donuttpa_tpa_status% or %donuttpa_gui_status_formatted%.");
        } else {
            getLogger().severe("Failed to register DonutTPA PlaceholderAPI expansion. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.expansion != null) {
            this.expansion.unregister();
        }
    }

    /**
     * Resolves DonutTPA's {@code managers1} instance from the running DonutTPA plugin.
     *
     * <p>The lookup uses reflection so this project has no compile-time dependency on DonutTPA's
     * internal classes, keeping the integration non-invasive.</p>
     *
     * @return the DonutTPA manager container, or {@code null} if it could not be found
     */
    public Object getManagersInstance() {
        Plugin donutTpa = Bukkit.getPluginManager().getPlugin("DonutTPA");
        if (donutTpa == null) {
            return null;
        }

        try {
            Field field = findField(donutTpa.getClass(), "managers1");
            if (field == null) {
                getLogger().warning("Unable to locate DonutTPA.managers1 field.");
                return null;
            }

            field.setAccessible(true);
            return field.get(donutTpa);
        } catch (ReflectiveOperationException exception) {
            getLogger().warning("Failed to read DonutTPA managers1 field: " + exception.getMessage());
            return null;
        }
    }

    private Field findField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
