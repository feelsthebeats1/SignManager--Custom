# SignManager

**An all-in-one sign manager plugin that allows for easy editing of signs, and use of placeholders.**

Links
- Download: https://modrinth.com/plugin/signmanager
- Donate: https://patreon.com/redned

SignManager gives you full control over signs on a Minecraft server. This plugin comes with a large number of features that let you modify signs easily, display information and update the signs at a set interval.

This plugin also supports newer sign features from 1.20+, such as multiple sign sides, glowing text and configuring sign editability.

# Features
- Sign commands - Add commands that are ran when the sign is clicked
- Sign editing - Easily update signs with the use of the **/sign edit** command
- MiniMessage/RGB integration - Support for [MiniMessage](https://docs.advntr.dev/minimessage/format) and use of RGB colors
- PlaceholderAPI support - Display [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/Placeholders) placeholders on signs
- Updating intervals - Update sign text at a configured interval

# Commands
## Editor Commands
- **/sign edit line <line> <text>** - Edit the line of a sign.
- **/sign edit editable <true|false>** - Set whether the sign is editable.
- **/sign edit glowing <true|false>** - Set whether the sign is glowing.
- **/sign edit color <color>** - Set the color of the sign.

## Admin Commands
- **/sign admin interval <interval>** - Set the update interval of the sign.
- **/sign admin command <front|back|all> <command>** - Sets a command that is ran when a sign side is clicked (supports PlaceholderAPI).
- **/sign admin clear** - Clear any metadata from the sign (i.e. update interval).

# Permissions
- **signmanager.command.sign** - Access to the **/sign** command.
- **signmanager.command.sign.edit** - Access to the **/sign edit** command.
- **signmanager.command.sign.edit.tags** - Whether MiniMessage tags should be usable when using **/sign edit**.
- **signmanager.command.sign.edit.placeholders** - Whether placeholders should be replaced when using **/sign edit**.
- **signmanager.command.admin** - Access to the **/sign admin** command.
- **signmanager.sign.click** - Access to click signs with commands attached.
