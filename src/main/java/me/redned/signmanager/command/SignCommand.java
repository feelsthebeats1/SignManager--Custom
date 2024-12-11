package me.redned.signmanager.command;

import me.redned.signmanager.SignManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.DyeColor;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class SignCommand implements TabExecutor {
    private final SignManager plugin;

    public SignCommand(SignManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Audience audience = this.plugin.getSender(sender);
        if (!(sender instanceof Player player)) {
            audience.sendMessage(Component.text("You must be a player to execute this command.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            this.sendHelp(audience);
            return true;
        }

        String subCommand = args[0];
        if (args.length < 3) {
            this.sendHelp(audience);
            return true;
        }

        switch (subCommand) {
            case "edit" -> {
                if (!player.hasPermission("signmanager.command.sign.edit")) {
                    audience.sendMessage(Component.text("You do not have permission to execute this command!", NamedTextColor.RED));
                    return true;
                }

                BlockState state = player.getTargetBlock(null, 5).getState();
                if (!(state instanceof Sign sign)) {
                    audience.sendMessage(Component.text("You must be looking at a sign to execute this command.", NamedTextColor.RED));
                    return true;
                }

                String editSubCommand = args[1];
                switch (editSubCommand) {
                    case "line" -> {
                        int line = Integer.parseInt(args[2]);
                        if (line < 1 || line > 4) {
                            audience.sendMessage(Component.text("Line must be between 1 and 4.", NamedTextColor.RED));
                            return true;
                        }

                        String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                        Component displayedText = Component.text(text);
                        boolean tags = player.hasPermission("signmanager.command.sign.edit.tags");
                        boolean placeholders = player.hasPermission("signmanager.command.sign.edit.placeholders");

                        if (tags) {
                            displayedText = MiniMessage.miniMessage().deserialize(text);
                        }

                        String rawText = text;
                        text = this.plugin.getDisplayedText(text, tags, placeholders);

                        // Get the side based on which way the player is looking
                        Side direction = this.plugin.getSignSide(player.getLocation(), sign);
                        SignSide side = sign.getSide(direction);
                        side.setLine(line - 1, text);
                        sign.update();

                        this.plugin.setSignText(sign.getLocation(), direction, line - 1, rawText);

                        audience.sendMessage(Component.text("Successfully set line " + line + " to ", NamedTextColor.GREEN).append(displayedText).append(Component.text(".", NamedTextColor.GREEN)));
                    }
                    case "editable" -> {
                        boolean editable = Boolean.parseBoolean(args[2]);
                        sign.setEditable(editable);
                        sign.update();

                        audience.sendMessage(Component.text("Successfully made sign " + (editable ? "editable" : "uneditable") + ".", NamedTextColor.GREEN));
                    }
                    case "glowing" -> {
                        boolean glowing = Boolean.parseBoolean(args[2]);

                        Side direction = this.plugin.getSignSide(player.getLocation(), sign);
                        SignSide side = sign.getSide(direction);
                        side.setGlowingText(glowing);
                        sign.update();

                        audience.sendMessage(Component.text("Successfully made sign " + (glowing ? "glowing" : "not glowing") + ".", NamedTextColor.GREEN));
                    }
                    case "color" -> {
                        try {
                            DyeColor color = DyeColor.valueOf(args[2].toUpperCase(Locale.ROOT));
                            Side direction = this.plugin.getSignSide(player.getLocation(), sign);
                            SignSide side = sign.getSide(direction);
                            side.setColor(color);
                            sign.update();

                            audience.sendMessage(Component.text("Successfully set sign color to " + color.name().toLowerCase() + ".", NamedTextColor.GREEN));
                        } catch (IllegalArgumentException e) {
                            String validColors = String.join(", ", Arrays.stream(DyeColor.values()).map(color -> color.name().toLowerCase()).toList());

                            audience.sendMessage(Component.text("Invalid color! Valid colors: " + validColors + ".", NamedTextColor.RED));
                            return true;
                        }
                    }
                }
            }
            case "admin" -> {
                if (!player.hasPermission("signmanager.command.sign.admin")) {
                    audience.sendMessage(Component.text("You do not have permission to execute this command!", NamedTextColor.RED));
                    return true;
                }

                BlockState state = player.getTargetBlock(null, 5).getState();
                if (!(state instanceof Sign sign)) {
                    audience.sendMessage(Component.text("You must be looking at a sign to execute this command.", NamedTextColor.RED));
                    return true;
                }

                String adminSubCommand = args[1];
                switch (adminSubCommand) {
                    case "interval" -> {
                        int interval = Integer.parseInt(args[2]);
                        if (interval < 1) {
                            audience.sendMessage(Component.text("Interval must be greater than 0. If you wish to clear the interval, use /sign admin clear.", NamedTextColor.RED));
                            return true;
                        }

                        this.plugin.setSignUpdateInterval(sign, interval);
                        audience.sendMessage(Component.text("Successfully set update interval to " + interval + " ticks.", NamedTextColor.GREEN));
                    }
                    case "clear" -> {
                        this.plugin.clearSign(sign.getLocation());
                        audience.sendMessage(Component.text("Successfully cleared metadata from sign.", NamedTextColor.GREEN));
                    }
                    case "command" -> {
                        String side = args[2];
                        String commandToRun = String.join(" ", Arrays.copyOfRange(args, 3, args.length));

                        // Make the sign uneditable
                        sign.setEditable(false);
                        sign.update();

                        if (side.equalsIgnoreCase("all")) {
                            this.plugin.setSignCommand(sign, Side.FRONT, commandToRun);
                            this.plugin.setSignCommand(sign, Side.BACK, commandToRun);

                            audience.sendMessage(Component.text("Successfully set command to run on all sides of the sign.", NamedTextColor.GREEN));
                        } else if (side.equalsIgnoreCase("front")) {
                            this.plugin.setSignCommand(sign, Side.FRONT, commandToRun);
                            audience.sendMessage(Component.text("Successfully set command to run on the front side of the sign.", NamedTextColor.GREEN));
                        } else if (side.equalsIgnoreCase("back")) {
                            this.plugin.setSignCommand(sign, Side.BACK, commandToRun);
                            audience.sendMessage(Component.text("Successfully set command to run on the back side of the sign.", NamedTextColor.GREEN));
                        } else {
                            audience.sendMessage(Component.text("Invalid side! Valid sides: all, front, back.", NamedTextColor.RED));
                            return true;
                        }
                    }
                }
            }
            default -> {
                this.sendHelp(audience);
                return true;
            }
        }

        return true;
    }

    private void sendHelp(Audience audience) {
        audience.sendMessage(SignManager.HEADER);
        audience.sendMessage(Component.text("Editor Commands:"));
        audience.sendMessage(Component.text("/sign edit line <line> <text> ", NamedTextColor.GREEN)
                .append(Component.text("Edit a line of a sign.", NamedTextColor.DARK_GREEN)));
        audience.sendMessage(Component.text("/sign edit editable <true|false> ", NamedTextColor.GREEN)
                .append(Component.text("Set whether the sign is editable.", NamedTextColor.DARK_GREEN)));
        audience.sendMessage(Component.text("/sign edit glowing <true|false> ", NamedTextColor.GREEN)
                .append(Component.text("Set whether the sign is glowing.", NamedTextColor.DARK_GREEN)));
        audience.sendMessage(Component.text("/sign edit color <color> ", NamedTextColor.GREEN)
                .append(Component.text("Set the color of the sign.", NamedTextColor.DARK_GREEN)));
        audience.sendMessage(Component.text("Admin Commands:"));
        audience.sendMessage(Component.text("/sign admin interval <interval> ", NamedTextColor.GREEN)
                .append(Component.text("Set the update interval of the sign.", NamedTextColor.DARK_GREEN)));
        audience.sendMessage(Component.text("/sign admin command <front|back|all> <command> ", NamedTextColor.GREEN)
                .append(Component.text("Set a command to run when the sign is clicked.", NamedTextColor.DARK_GREEN)));
        audience.sendMessage(Component.text("/sign admin clear ", NamedTextColor.GREEN)
                .append(Component.text("Clear any metadata from the sign.", NamedTextColor.DARK_GREEN)));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], List.of("edit", "admin"), completions);
        }

        if (args.length >= 2) {
            if (args[0].equalsIgnoreCase("edit")) {
                if (args.length == 2) {
                    return StringUtil.copyPartialMatches(args[1], List.of("line", "editable", "glowing", "color"), completions);
                }

                switch (args[1]) {
                    case "line" -> {
                        if (args.length == 3) {
                            return StringUtil.copyPartialMatches(args[2], List.of("1", "2", "3", "4"), completions);
                        }
                    }
                    case "editable", "glowing" -> {
                        if (args.length == 3) {
                            return StringUtil.copyPartialMatches(args[2], List.of("true", "false"), completions);
                        }
                    }
                    case "color" -> {
                        if (args.length == 3) {
                            return StringUtil.copyPartialMatches(args[2], Arrays.stream(DyeColor.values()).map(Enum::name).toList(), completions);
                        }
                    }
                }
            }

            if (args[0].equalsIgnoreCase("admin")) {
                if (args.length == 2) {
                    return StringUtil.copyPartialMatches(args[1], List.of("interval", "clear", "command"), completions);
                }

                if (args[1].equalsIgnoreCase("command")) {
                    if (args.length == 3) {
                        return StringUtil.copyPartialMatches(args[2], List.of("front", "back", "all"), completions);
                    }
                }
            }
        }

        return completions;
    }
}
