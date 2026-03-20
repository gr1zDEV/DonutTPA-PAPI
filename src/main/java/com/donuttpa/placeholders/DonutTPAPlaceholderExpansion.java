package com.donuttpa.placeholders;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion that exposes per-player DonutTPA settings.
 *
 * <p>Supported placeholders:</p>
 * <ul>
 *   <li>{@code %donuttpa_tpa_status%} - Returns {@code ON} when the player accepts normal TPA requests.</li>
 *   <li>{@code %donuttpa_tpa_status_formatted%} - Returns {@code &l&aON} or {@code &l&cOFF} for TPA requests.</li>
 *   <li>{@code %donuttpa_tpahere_status%} - Returns {@code ON} when the player accepts TPAHERE requests.</li>
 *   <li>{@code %donuttpa_tpahere_status_formatted%} - Returns formatted TPAHERE status text.</li>
 *   <li>{@code %donuttpa_tpauto_status%} - Returns {@code ON} when TPA auto-accept is enabled.</li>
 *   <li>{@code %donuttpa_tpauto_status_formatted%} - Returns formatted auto-accept status text.</li>
 *   <li>{@code %donuttpa_gui_status%} - Returns {@code ON} when the player's GUI preference is enabled.</li>
 *   <li>{@code %donuttpa_gui_status_formatted%} - Returns formatted GUI preference text.</li>
 * </ul>
 *
 * <p>The expansion reads the live DonutTPA {@code managers1} object directly through reflection, so
 * it works without patching DonutTPA and remains compatible with Java and Geyser/Floodgate players
 * as long as DonutTPA stores those players by UUID.</p>
 */
public final class DonutTPAPlaceholderExpansion extends PlaceholderExpansion {

    private static final String PLAIN_ON = "ON";
    private static final String PLAIN_OFF = "OFF";
    private static final String FORMATTED_ON = "&l&aON";
    private static final String FORMATTED_OFF = "&l&cOFF";

    private final DonutTPAPlaceholdersPlugin plugin;

    /**
     * Creates a new expansion tied to the main plugin.
     *
     * @param plugin owning plugin instance
     */
    public DonutTPAPlaceholderExpansion(DonutTPAPlaceholdersPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "donuttpa";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", this.plugin.getDescription().getAuthors().isEmpty()
            ? List.of("OpenAI")
            : this.plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || player.getUniqueId() == null) {
            return "";
        }

        Object managers = this.plugin.getManagersInstance();
        if (managers == null) {
            return "";
        }

        UUID uniqueId = player.getUniqueId();
        return switch (params.toLowerCase()) {
            case "tpa_status" -> booleanPlaceholder(!containsPlayer(managers, "tpaDisabled", uniqueId), false);
            case "tpa_status_formatted" -> booleanPlaceholder(!containsPlayer(managers, "tpaDisabled", uniqueId), true);
            case "tpahere_status" -> booleanPlaceholder(!containsPlayer(managers, "tpaHereDisabled", uniqueId), false);
            case "tpahere_status_formatted" -> booleanPlaceholder(!containsPlayer(managers, "tpaHereDisabled", uniqueId), true);
            case "tpauto_status" -> booleanPlaceholder(containsPlayer(managers, "tpaAutoEnabled", uniqueId), false);
            case "tpauto_status_formatted" -> booleanPlaceholder(containsPlayer(managers, "tpaAutoEnabled", uniqueId), true);
            case "gui_status" -> booleanPlaceholder(containsPlayer(managers, "guiPreferences", uniqueId), false);
            case "gui_status_formatted" -> booleanPlaceholder(containsPlayer(managers, "guiPreferences", uniqueId), true);
            default -> null;
        };
    }

    /**
     * Converts a boolean state into the placeholder output requested by the user.
     *
     * @param enabled whether the setting is enabled
     * @param formatted whether color-code formatting should be returned
     * @return plain or formatted ON/OFF text
     */
    private String booleanPlaceholder(boolean enabled, boolean formatted) {
        if (formatted) {
            return enabled ? FORMATTED_ON : FORMATTED_OFF;
        }
        return enabled ? PLAIN_ON : PLAIN_OFF;
    }

    /**
     * Checks whether DonutTPA's field contains the given player UUID.
     *
     * <p>The helper accepts common collection types used by plugins: {@link Collection},
     * {@link Map}, and arrays. String UUID entries are also supported for resilience.</p>
     *
     * @param managers DonutTPA's {@code managers1} object
     * @param fieldName field to inspect on the manager
     * @param uniqueId player UUID to search for
     * @return true when the player is present in the target field
     */
    private boolean containsPlayer(Object managers, String fieldName, UUID uniqueId) {
        Object fieldValue = readField(managers, fieldName);
        if (fieldValue == null) {
            return false;
        }

        if (fieldValue instanceof Map<?, ?> map) {
            return matches(map.keySet(), uniqueId) || matches(map.values(), uniqueId);
        }

        if (fieldValue instanceof Collection<?> collection) {
            return matches(collection, uniqueId);
        }

        if (fieldValue.getClass().isArray()) {
            int length = Array.getLength(fieldValue);
            for (int index = 0; index < length; index++) {
                if (matchesValue(Array.get(fieldValue, index), uniqueId)) {
                    return true;
                }
            }
        }

        return matchesValue(fieldValue, uniqueId);
    }

    private boolean matches(Iterable<?> iterable, UUID uniqueId) {
        for (Object value : iterable) {
            if (matchesValue(value, uniqueId)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesValue(Object value, UUID uniqueId) {
        if (value == null) {
            return false;
        }
        if (Objects.equals(value, uniqueId)) {
            return true;
        }
        return uniqueId.toString().equalsIgnoreCase(String.valueOf(value));
    }

    private @Nullable Object readField(Object source, String fieldName) {
        Class<?> current = source.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(source);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (ReflectiveOperationException exception) {
                this.plugin.getLogger().warning("Failed to read DonutTPA field '" + fieldName + "': " + exception.getMessage());
                return null;
            }
        }

        this.plugin.getLogger().warning("Could not find DonutTPA field '" + fieldName + "' on managers1.");
        return null;
    }
}
