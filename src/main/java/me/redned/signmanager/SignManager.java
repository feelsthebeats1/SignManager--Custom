package me.redned.signmanager;

import me.redned.signmanager.command.SignCommand;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class SignManager extends JavaPlugin implements Listener {
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.builder()
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    public static final Component HEADER = Component.text("------------------", NamedTextColor.GRAY)
            .append(Component.text("[", NamedTextColor.DARK_GREEN))
            .append(Component.text(" SignManager ", NamedTextColor.GREEN, TextDecoration.BOLD))
            .append(Component.text("]", NamedTextColor.DARK_GREEN))
            .append(Component.text("------------------", NamedTextColor.GRAY));

    private BukkitAudiences adventure;

    private File signsFile;
    private FileConfiguration signsConfig;

    private final Map<StoredPosition, StoredSign> storedSigns = new HashMap<>();

    @Override
    public void onEnable() {
        this.adventure = BukkitAudiences.create(this);

        this.getCommand("sign").setExecutor(new SignCommand(this));

        this.getServer().getPluginManager().registerEvents(this, this);

        if (!this.getDataFolder().exists()) {
            this.getDataFolder().mkdirs();
        }

        this.signsFile = new File(this.getDataFolder(), "signs.yml");
        if (!this.signsFile.exists()) {
            try {
                this.signsFile.createNewFile();
            } catch (IOException e) {
                this.getLogger().severe("Failed to create signs.yml file!");
                e.printStackTrace();
            }
        }

        this.signsConfig = YamlConfiguration.loadConfiguration(this.signsFile);

        // Load stored signs from the config
        for (String key : this.signsConfig.getKeys(false)) {
            ConfigurationSection section = this.signsConfig.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            Location location = section.getLocation("location");
            List<String> frontText = section.getStringList("front-text");
            List<String> backText = section.getStringList("back-text");
            String frontCommand = section.getString("front-command");
            String backCommand = section.getString("back-command");
            int updateInterval = section.getInt("update-interval");

            this.storedSigns.put(new StoredPosition(location), new StoredSign(
                    location,
                    frontText,
                    backText,
                    frontCommand,
                    backCommand,
                    updateInterval
            ));
        }

        AtomicReference<Integer> tickValue = new AtomicReference<>(0);
        this.getServer().getScheduler().runTaskTimer(this, () -> {
            int currentTick = tickValue.get();
            List<StoredSign> toRemove = new ArrayList<>();
            this.storedSigns.values().forEach(storedSign -> {
                if (storedSign.updateInterval > 0 && currentTick % storedSign.updateInterval == 0) {
                    BlockState state = storedSign.location.getBlock().getState();
                    if (!(state instanceof Sign sign)) {
                        // Remove the sign from the stored signs if it is no longer a sign
                        toRemove.add(storedSign);

                        this.getLogger().warning("Sign at " + storedSign.location + " is no longer a sign! Removing from stored signs.");
                        this.saveSigns();
                        return;
                    }

                    SignSide frontSide = sign.getSide(Side.FRONT);
                    SignSide backSide = sign.getSide(Side.BACK);

                    for (int i = 0; i < storedSign.frontText.size(); i++) {
                        frontSide.setLine(i, this.getDisplayedText(storedSign.frontText.get(i), true, true));
                    }

                    for (int i = 0; i < storedSign.backText.size(); i++) {
                        backSide.setLine(i, this.getDisplayedText(storedSign.backText.get(i), true, true));
                    }

                    sign.update();
                }
            });

            if (!toRemove.isEmpty()) {
                toRemove.forEach(storedSign -> this.storedSigns.remove(new StoredPosition(storedSign.location)));
                this.saveSigns();
            }

            tickValue.set(currentTick + 1);
        }, 1, 1);
    }

    @Override
    public void onDisable() {
        if (this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }

        this.saveSigns();
    }

    @EventHandler
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) {
            return;
        }

        BlockState state = event.getClickedBlock().getState();
        if (!(state instanceof Sign sign)) {
            return;
        }

        StoredSign storedSign = this.getStoredSign(sign.getLocation());
        if (storedSign == null) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission("signmanager.sign.click")) {
            return;
        }

        Side side = this.getSignSide(player.getLocation(), sign);
        String command = side == Side.FRONT ? storedSign.frontCommand() : storedSign.backCommand();
        if (command.isBlank()) {
            return;
        }

        this.getServer().dispatchCommand(player, PlaceholderParser.replacePlaceholders(event.getPlayer(), command));
    }

    public Audience getPlayer(Player player) {
        return this.adventure.player(player);
    }

    public Audience getSender(CommandSender sender) {
        return this.adventure.sender(sender);
    }

    public Side getSignSide(Location playerLocation, Sign sign) {
        Location signLocation = sign.getLocation();

        Vector signNormal = getSignVector(sign);
        Vector toPlayer = playerLocation.toVector().subtract(signLocation.toVector());

        double dot = toPlayer.normalize().dot(signNormal);
        return dot > 0 ? Side.FRONT : Side.BACK;
    }

    public String getDisplayedText(String text, boolean tags, boolean placeholders) {
        if (placeholders) {
            text = PlaceholderParser.replacePlaceholders(null, text);
        }

        if (tags) {
            // Replace all legacy colors with '&' to stop Adventure complaining about
            // legacy components (boo Spigot)
            text = text.replace(ChatColor.COLOR_CHAR, '&');

            Component displayedText = MiniMessage.miniMessage().deserialize(text);

            text = serializeToWeirdSpigotFormat(displayedText);

            // And then replace any color codes with the weird spigot format
            text = ChatColor.translateAlternateColorCodes('&', text);
        }

        return text;
    }

    public void setSignText(Location signLocation, Side side, int line, String text) {
        // Update saved text if we have a stored sign
        StoredSign storedSign = this.getStoredSign(signLocation);
        if (storedSign == null) {
            return;
        }

        if (side == Side.FRONT) {
            String[] newFrontText = new String[4];
            storedSign.frontText.toArray(newFrontText);
            newFrontText[line] = text;

            storedSign = new StoredSign(
                    storedSign.location,
                    List.of(newFrontText),
                    storedSign.backText,
                    storedSign.frontCommand,
                    storedSign.backCommand,
                    storedSign.updateInterval
            );
        } else {
            String[] newBackText = new String[4];
            storedSign.backText.toArray(newBackText);
            newBackText[line] = text;

            storedSign = new StoredSign(
                    storedSign.location,
                    storedSign.frontText,
                    List.of(newBackText),
                    storedSign.frontCommand,
                    storedSign.backCommand,
                    storedSign.updateInterval
            );
        }

        this.storedSigns.put(new StoredPosition(signLocation), storedSign);
        this.saveSigns();
    }

    public void setSignUpdateInterval(Sign sign, int updateInterval) {
        StoredSign prevSign = this.getStoredSign(sign.getLocation());

        StoredSign storedSign = new StoredSign(
                sign.getLocation(),
                prevSign == null ? List.of(sign.getSide(Side.FRONT).getLines()) : prevSign.frontText(),
                prevSign == null ? List.of(sign.getSide(Side.BACK).getLines()) : prevSign.backText(),
                prevSign == null ? "" : prevSign.frontCommand(),
                prevSign == null ? "" : prevSign.backCommand(),
                updateInterval
        );

        this.storedSigns.put(new StoredPosition(sign.getLocation()), storedSign);
        this.saveSigns();
    }

    public void setSignCommand(Sign sign, Side side, String command) {
        StoredSign prevSign = this.getStoredSign(sign.getLocation());

        StoredSign storedSign = new StoredSign(
                sign.getLocation(),
                prevSign == null ? List.of(sign.getSide(Side.FRONT).getLines()) : prevSign.frontText(),
                prevSign == null ? List.of(sign.getSide(Side.BACK).getLines()) : prevSign.backText(),
                side == Side.FRONT ? command : (prevSign == null ? "" : prevSign.frontCommand()),
                side == Side.BACK ? command : (prevSign == null ? "" : prevSign.backCommand()),
                prevSign == null ? 0 : prevSign.updateInterval()
        );

        this.storedSigns.put(new StoredPosition(sign.getLocation()), storedSign);
        this.saveSigns();
    }

    public void clearSign(Location location) {
        this.storedSigns.remove(new StoredPosition(location));
        this.saveSigns();
    }

    public void saveSigns() {
        int i = 0;

        // Clear sections
        for (String key : this.signsConfig.getKeys(false)) {
            this.signsConfig.set(key, null);
        }

        for (Map.Entry<StoredPosition, StoredSign> entry : this.storedSigns.entrySet()) {
            StoredSign storedSign = entry.getValue();
            ConfigurationSection section = this.signsConfig.createSection("sign" + i++);
            section.set("location", storedSign.location);
            section.set("front-text", storedSign.frontText);
            section.set("back-text", storedSign.backText);
            section.set("front-command", storedSign.frontCommand);
            section.set("back-command", storedSign.backCommand);
            section.set("update-interval", storedSign.updateInterval);
        }

        try {
            this.signsConfig.save(this.signsFile);
        } catch (IOException e) {
            this.getLogger().severe("Failed to save signs.yml file!");
            e.printStackTrace();
        }
    }

    private static @NotNull Vector getSignVector(Sign sign) {
        BlockFace facing;
        BlockData blockData = sign.getBlockData();
        if (blockData instanceof org.bukkit.block.data.type.Sign signData) {
            facing = signData.getRotation();
        } else if (blockData instanceof Directional directional) {
            facing = directional.getFacing();
        } else {
            throw new IllegalArgumentException("Sign block data is not a Sign or Directional block data");
        }

        // Normal vector pointing out from the sign's front
        return facing.getDirection();
    }

    private static String serializeToWeirdSpigotFormat(Component component) {
        return SERIALIZER.serialize(component);
    }

    public StoredSign getStoredSign(Location location) {
        return this.storedSigns.get(new StoredPosition(location));
    }

    public record StoredSign(Location location, List<String> frontText, List<String> backText, String frontCommand, String backCommand, int updateInterval) {
    }

    public record StoredPosition(int x, int y, int z) {

        public StoredPosition(Location loc) {
            this(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        }
    }
}
