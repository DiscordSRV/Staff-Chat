package com.rezzedup.discordsrv.staffchat;

import com.rezzedup.discordsrv.staffchat.placeholders.MappedPlaceholder;
import com.rezzedup.discordsrv.staffchat.placeholders.Placeholder;
import com.vdurmont.emoji.EmojiParser;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreProcessEvent;
import github.scarsz.discordsrv.dependencies.jda.core.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.core.entities.TextChannel;
import github.scarsz.discordsrv.dependencies.jda.core.entities.User;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class StaffChatPlugin extends JavaPlugin implements Listener
{
    public static final String CHANNEL = "staff-chat";
    
    public static String color(String message)
    {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    private final Set<UUID> toggles = new HashSet<>();
    
    private final DiscordListener discord = new DiscordListener();
    
    private boolean isDiscordSrvHookEnabled = false;
    
    private Debugger debugger;
    
    @Override
    public void onEnable()
    {
        try
        {
            debugger = new Debugger();
        }
        catch (IOException io)
        {
            io.printStackTrace();
        }
        
        debugger.debug("----- Starting. -----");
        
        saveDefaultConfig();
    
        PluginManager plugins = getServer().getPluginManager();
        plugins.registerEvents(new InGameListener(), this);
        
        if (plugins.isPluginEnabled("DiscordSRV"))
        {
            debugger.debug("DiscordSRV is enabled: subscribing to API.");
            
            this.isDiscordSrvHookEnabled = true;
            DiscordSRV.api.subscribe(discord);
        }
        else 
        {
            debugger.debug("DiscordSRV is not enabled: not subscribing to API.");
            
            getLogger().warning("DiscordSRV is not currently enabled (messages will not be sent to Discord).");
            getLogger().warning("The plugin will still work in-game, however.");
        }
        
        if (debugger.isEnabled)
        {
            getServer().getScheduler().runTask(this, debugger::logInitialState);
        }
    }
    
    @Override
    public void onDisable()
    {
        debugger.debug("Disabling plugin...");
        
        getServer().getOnlinePlayers().stream().filter(this::isChatToggled).forEach(this::toggle);
        
        if (isDiscordSrvHookEnabled)
        {
            debugger.debug("Unsubscribing from DiscordSRV's API.");
            DiscordSRV.api.unsubscribe(discord);
        }
        
        debugger.debug("----- Disabled. -----");
    }
    
    public TextChannel getDiscordChannel()
    {
        return (isDiscordSrvHookEnabled) ? DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(CHANNEL) : null;
    }
    
    public void inGameAnnounce(String message)
    {
        String content = color(message);
    
        getServer().getConsoleSender().sendMessage(content);
        
        getServer().getOnlinePlayers().stream()
            .filter(Permissions.any(Permissions.ALL, Permissions.ACCESS)).forEach(p -> p.sendMessage(content));
    }
    
    private void inGameUpdateThenAnnounce(String format, MappedPlaceholder placeholder)
    {
        // If the value of %message% doesn't exist for some reason, don't announce.
        if (Placeholder.isValid(placeholder.get("message"))) 
        {
            inGameAnnounce(placeholder.update(format));
        }
    }
    
    public void submitFromInGame(Player player, String message)
    {
        debugger.debug("[In-Game-Message] From:'%s' Message:'%s'", player.getName(), message);
        
        inGameUpdateThenAnnounce(getConfig().getString("in-game-message-format"), new MessagePlaceholder(player, message));
    
        if (getDiscordChannel() != null)
        {
            debugger.debug("Sending message to discord channel: %s => %s", CHANNEL, getDiscordChannel());
            DiscordSRV.getPlugin().processChatMessage(player, message, CHANNEL, false);
        }
        else 
        {
            debugger.debug("Unable to send message to discord: %s => null", CHANNEL);
        }
    }
    
    public void submitFromDiscord(User user, Message message)
    {
        debugger.debug
        (
            "[Discord-Message] From:'%s#%s' Channel:'%s' Message:'%s'",
            user.getName(), user.getDiscriminator(), message.getChannel(), message
        );
        
        inGameUpdateThenAnnounce(getConfig().getString("discord-message-format"), new MessagePlaceholder(user, message));
    }
    
    public boolean isChatToggled(Player player)
    {
        return toggles.contains(player.getUniqueId());
    }
    
    private void forceToggle(Player player, boolean state)
    {
        if (state)
        {
            toggles.add(player.getUniqueId());
            player.sendMessage(color(getConfig().getString("enable-staff-chat")));
            debugger.debug("Enabled automatic staff-chat for player %s", player.getName());
        }
        else
        {
            toggles.remove(player.getUniqueId());
            player.sendMessage(color(getConfig().getString("disable-staff-chat")));
            debugger.debug("Disabled automatic staff-chat for player %s", player.getName());
        }
    }
    
    private void toggle(Player player)
    {
        forceToggle(player, !isChatToggled(player));
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if ("staffchat".equals(command.getName()))
        {
            if (sender instanceof ConsoleCommandSender)
            {
                sender.sendMessage("Only players may use this command.");
                return true;
            }
    
            Player player = (Player) sender;
    
            if (args.length <= 0)
            {
                toggle(player);
            }
            else
            {
                submitFromInGame(player, String.join(" ", args));
            }
        }
        else if ("managestaffchat".equals(command.getName()))
        {
            if (args.length <= 0)
            {
                sender.sendMessage(color("&9&lDiscordSRV-Staff-Chat &fv" + getDescription().getVersion() + " Usage:"));
                sender.sendMessage(color("&f- &7/staffchat &9Toggle automatic staff chat"));
                sender.sendMessage(color("&f- &7/staffchat <message> &9Send a message to staff chat"));
                sender.sendMessage(color("&f- &7/" + label.toLowerCase() + " reload &9Reload the config"));
                sender.sendMessage(color("&f- &7/" + label.toLowerCase() + " debug &9Toggle debugging"));
                
                if (debugger.isEnabled)
                {
                    sender.sendMessage(color("&aDebugging is currently enabled."));
                }
                else 
                {
                    sender.sendMessage(color("&cDebugging is currently disabled."));
                }
                
                return true;
            }
            
            switch (args[0].toLowerCase())
            {
                case "reload": case "refresh": case "restart":
                {
                    debugger.debug("Reloading config...");
                    reloadConfig();
                    debugger.logInitialState();
                    sender.sendMessage(color("&9&lDiscordSRV-Staff-Chat&f: Reloaded."));
                    break;
                }
                
                case "debug":
                {
                    debugger.toggle();
                    
                    if (debugger.isEnabled)
                    {
                        sender.sendMessage(color("&aEnabled debugging."));
                        
                        if (sender instanceof Player)
                        {
                            sender.sendMessage("Sending a test message...");
                            getServer().dispatchCommand(sender, "staffchat Hello! Just testing things...");
                        }
                    }
                    else 
                    {
                        sender.sendMessage(color("&cDisabled debugging."));
                    }
                    break;
                }
                
                case "help": case "?":
                {
                    onCommand(sender, command, label, new String[0]);
                    break;
                }
                
                default:
                {
                    sender.sendMessage(color("&9&lDiscordSRV-Staff-Chat&f: &7&oUnknown arguments."));
                    break;
                }
            }
        }
        return true;
    }
    
    class DiscordListener
    {
        @Subscribe
        public void onDiscordChat(DiscordGuildMessagePreProcessEvent event)
        {
            if (event.getChannel().equals(getDiscordChannel()))
            {
                submitFromDiscord(event.getAuthor(), event.getMessage());
                event.setCancelled(true);
            }
        }
    }
    
    class InGameListener implements Listener
    {
        @EventHandler(priority = EventPriority.LOWEST)
        public void onGameChat(AsyncPlayerChatEvent event)
        {
            if (!isChatToggled(event.getPlayer()))
            {
                return;
            }
            
            if (Permissions.any(Permissions.ALL, Permissions.ACCESS).test(event.getPlayer()))
            {
                debugger.debug("Player %s has automatic staff-chat enabled.", event.getPlayer().getName());
                
                submitFromInGame(event.getPlayer(), event.getMessage());
                event.setCancelled(true);
            }
            else
            {
                debugger.debug
                (
                    "Player %s has automatic staff-chat enabled but they don't have permission to use the staff chat.",
                    event.getPlayer().getName()
                );
                
                forceToggle(event.getPlayer(), false);
            }
        }
        
        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event)
        {
            Player player = event.getPlayer();
            
            boolean isNotifiable = getConfig().getBoolean("notify-staff-chat-enabled-on-join")
                && Permissions.any(Permissions.ALL, Permissions.ACCESS).test(player)
                && toggles.contains(player.getUniqueId());
            
            if (!isNotifiable) { return; }
            
            debugger.debug("Player %s joined: reminding them that they have automatic staff-chat enabled.", event.getPlayer().getName());
            
            getServer().getScheduler()
                .runTaskLater(StaffChatPlugin.this, () -> player.sendMessage(color(getConfig().getString("staff-chat-enabled-notification"))), 10L);
        }
    }
    
    class MessagePlaceholder extends MappedPlaceholder
    {
        MessagePlaceholder(Player player, String message)
        {
            map("message", "content", "text").to(() -> message);
            map("user", "name", "username", "player", "sender").to(player::getName);
            map("nickname", "displayname").to(player::getDisplayName);
        }
        
        MessagePlaceholder(User user, Message message)
        {
            // Emoji Unicode -> Alias (library included with DiscordSRV)
            String text = EmojiParser.parseToAliases(message.getContentStripped());
            
            map("message", "content", "text").to(() -> text);
            map("user", "name", "username", "sender").to(user::getName);
            map("nickname", "displayname").to(message.getGuild().getMember(user)::getNickname);
            map("discriminator", "discrim").to(user::getDiscriminator);
        }
    }
    
    private class Debugger
    {
        final DateTimeFormatter timestamp = DateTimeFormatter.ofPattern("YYYY-MM-dd => HH:mm:ss");
        
        final Path debugToggleFile;
        final Path debugLogFile;
        
        boolean isEnabled;
        
        Debugger() throws IOException
        {
            Path root = StaffChatPlugin.this.getDataFolder().toPath();
            
            this.debugToggleFile = root.resolve("debugging-is-enabled");
            this.debugLogFile = root.resolve("debug.log");
            
            this.isEnabled = Files.isRegularFile(debugToggleFile);
        }
        
        void toggle()
        {
            if (!isEnabled)
            {
                isEnabled = true;
                debug("===== Enabled Debugging. =====");
                
                try { Files.createFile(debugToggleFile); }
                catch (IOException io) { io.printStackTrace(); }
            }
            else 
            {
                debug("===== Disabled debugging. =====");
                isEnabled = false;
                
                try { Files.deleteIfExists(debugToggleFile); }
                catch (IOException io) { io.printStackTrace(); }
            }
        }
        
        String now()
        {
            return OffsetDateTime.now().format(timestamp);
        }
        
        void debug(String message, Object ... placeholders)
        {
            if (!isEnabled) { return; }
            
            String content = String.format(message, placeholders);
            
            try
            {
                if (!Files.isRegularFile(debugLogFile))
                {
                    Files.createFile(debugLogFile);
                }
                Files.write(debugLogFile, String.format("[%s]: %s\n", now(), content).getBytes(), StandardOpenOption.APPEND);
            }
            catch (IOException io)
            {
                io.printStackTrace();
            }
            
            StaffChatPlugin.this.getLogger().info(String.format("[Debug] " + message, placeholders));
        }
        
        void logInitialState()
        {
            debug(" - isDiscordSrvHookEnabled: %s", isDiscordSrvHookEnabled);
            debug(" - Discord channel: %s => %s", CHANNEL, getDiscordChannel());
        }
    }
}
