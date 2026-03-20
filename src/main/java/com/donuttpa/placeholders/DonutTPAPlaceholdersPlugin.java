package com.donuttpa.placeholders;

import java.lang.reflect.Field;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin entry point for the DonutTPA PlaceholderAPI bridge.
 *
 * <p>This plugin registers a PlaceholderAPI expansion once DonutTPA is available and reads all
 * DonutTPA data via reflection so the DonutTPA plugin does not need to be modified.</p>
 */
public final class DonutTPAPlaceholdersPlugin extends JavaPlugin implements Listener {

    private static final String PLACEHOLDER_API_PLUGIN = "PlaceholderAPI";
    private static final String DONUT_TPA_PLUGIN = "DonutTpa";

    private DonutTPAPlaceholderExpansion expansion;
    private boolean waitingForDonutTpa;

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().getPlugin(PLACEHOLDER_API_PLUGIN) == null) {
            getLogger().severe("PlaceholderAPI is required but was not found. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);

        if (tryRegisterExpansion()) {
            return;
        }

        this.waitingForDonutTpa = true;
        getLogger().info("DonutTPA is not loaded yet. Waiting to register PlaceholderAPI placeholders.");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll((Listener) this);

        if (this.expansion != null) {
            this.expansion.unregister();
            this.expansion = null;
        }

        this.waitingForDonutTpa = false;
    }

    @EventHandler
    public void onPluginEnable(PluginEnableEvent event) {
        if (!this.waitingForDonutTpa) {
            return;
        }

        if (!DONUT_TPA_PLUGIN.equals(event.getPlugin().getName())) {
            return;
        }

        if (tryRegisterExpansion()) {
            this.waitingForDonutTpa = false;
            HandlerList.unregisterAll((Listener) this);
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
        Plugin donutTpa = getServer().getPluginManager().getPlugin(DONUT_TPA_PLUGIN);
        if (donutTpa == null || !donutTpa.isEnabled()) {
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

    private boolean tryRegisterExpansion() {
        Plugin donutTpa = getServer().getPluginManager().getPlugin(DONUT_TPA_PLUGIN);
        if (donutTpa == null || !donutTpa.isEnabled()) {
            return false;
        }

        if (this.expansion != null) {
            return true;
        }

        DonutTPAPlaceholderExpansion newExpansion = new DonutTPAPlaceholderExpansion(this);
        if (!newExpansion.register()) {
            getLogger().severe("Failed to register DonutTPA PlaceholderAPI expansion. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return false;
        }

        this.expansion = newExpansion;
        getLogger().info("Registered DonutTPA PlaceholderAPI expansion.");
        getLogger().info("Example usage: %donuttpa_tpa_status% or %donuttpa_gui_status_formatted%.");
        return true;
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
