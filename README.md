# DonutTPAPlaceholders

`DonutTPAPlaceholders` is a small Paper/Folia plugin that registers a PlaceholderAPI expansion for the `DonutTpa` plugin.

It exposes each player's DonutTPA preference states as placeholders, so you can show them in scoreboards, tab lists, chat formats, GUIs, holograms, and any other PlaceholderAPI-compatible plugin.

## What this plugin does

This plugin:

- waits for `PlaceholderAPI` to be available before enabling fully
- waits for `DonutTpa` if it loads after this plugin
- reads DonutTPA's runtime data through reflection instead of depending on DonutTPA internals at compile time
- provides placeholders for:
  - normal TPA requests
  - TPAHERE requests
  - auto-accept status
  - GUI preference status

## Requirements

To use this plugin, your server should have:

- Java 21
- Paper / Folia compatible with the Paper 1.21.4 API
- [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)
- `DonutTpa`

## Installation

1. Build the plugin jar:
   ```bash
   mvn package
   ```
2. Copy the generated jar from `target/` into your server's `plugins/` folder.
3. Make sure `PlaceholderAPI` and `DonutTpa` are also installed.
4. Start or restart the server.
5. Check the console for a successful registration message.

## Load behavior

On startup, the plugin behaves like this:

- if `PlaceholderAPI` is missing, the plugin disables itself
- if `DonutTpa` is not loaded yet, the plugin waits for it and registers placeholders once `DonutTpa` is enabled
- if both dependencies are present, the PlaceholderAPI expansion is registered immediately

Example console usage mentioned by the plugin:

- `%donuttpa_tpa_status%`
- `%donuttpa_gui_status_formatted%`

## Placeholder list

All placeholders use the `donuttpa` identifier.

### Plain placeholders

These return `ON` or `OFF`.

| Placeholder | Meaning when it returns `ON` |
| --- | --- |
| `%donuttpa_tpa_status%` | Player accepts regular TPA requests |
| `%donuttpa_tpahere_status%` | Player accepts TPAHERE requests |
| `%donuttpa_tpauto_status%` | Player has TPA auto-accept enabled |
| `%donuttpa_gui_status%` | Player has the GUI preference enabled |

### Formatted placeholders

These return:

- `&l&aON` when enabled
- `&l&cOFF` when disabled

| Placeholder | Meaning |
| --- | --- |
| `%donuttpa_tpa_status_formatted%` | Formatted regular TPA status |
| `%donuttpa_tpahere_status_formatted%` | Formatted TPAHERE status |
| `%donuttpa_tpauto_status_formatted%` | Formatted auto-accept status |
| `%donuttpa_gui_status_formatted%` | Formatted GUI preference status |

## How values are determined

The plugin reads the `managers1` object from the running `DonutTpa` plugin and checks these fields:

- `tpaDisabled`
- `tpaHereDisabled`
- `tpaAutoEnabled`
- `guiPreferences`

The placeholder results are derived as follows:

- `tpa_status` is `ON` when the player's UUID is **not** in `tpaDisabled`
- `tpahere_status` is `ON` when the player's UUID is **not** in `tpaHereDisabled`
- `tpauto_status` is `ON` when the player's UUID **is** in `tpaAutoEnabled`
- `gui_status` is `ON` when the player's UUID **is** in `guiPreferences`

For compatibility, the lookup supports values stored as:

- collections
- maps
- arrays
- UUID strings

## Example usage

### Scoreboard line

```text
TPA: %donuttpa_tpa_status%
```

### Colored status in chat or GUI text

```text
TPA Here: %donuttpa_tpahere_status_formatted%
```

```text
Auto Accept: %donuttpa_tpauto_status_formatted%
```

### Multiple settings display

```text
TPA: %donuttpa_tpa_status% | TPAHERE: %donuttpa_tpahere_status% | GUI: %donuttpa_gui_status%
```

## Building from source

### Maven

```bash
mvn package
```

The built jar will be created in:

```text
target/DonutTPAPlaceholders-1.0.0.jar
```

## Project structure

```text
src/main/java/com/donuttpa/placeholders/
├── DonutTPAPlaceholdersPlugin.java
└── DonutTPAPlaceholderExpansion.java

src/main/resources/
└── plugin.yml
```

## Troubleshooting

### The plugin disables itself on startup

Make sure `PlaceholderAPI` is installed. This plugin requires it and will disable itself if it is missing.

### Placeholders return empty values

Possible causes:

- `DonutTpa` is not installed or not enabled
- the expected DonutTpa fields are named differently in your DonutTpa build
- the target player data is not available yet

### Placeholder registration never happens

Check that the DonutTPA plugin name is exactly `DonutTpa`, because that is the plugin name this bridge listens for.

## Developer notes

- group ID: `com.donuttpa`
- artifact ID: `DonutTPAPlaceholders`
- version: `1.0.0`
- Java release target: `21`
- main class: `com.donuttpa.placeholders.DonutTPAPlaceholdersPlugin`
- PlaceholderAPI identifier: `donuttpa`

## License

No license file is currently included in this repository. Add one if you want to define reuse terms explicitly.
